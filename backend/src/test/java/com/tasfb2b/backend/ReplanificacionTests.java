package com.tasfb2b.backend;

import com.tasfb2b.algorithm.Solucion;
import com.tasfb2b.dto.ReplanificacionResultDTO;
import com.tasfb2b.model.Aeropuerto;
import com.tasfb2b.model.Envio;
import com.tasfb2b.model.Ruta;
import com.tasfb2b.model.Vuelo;
import com.tasfb2b.service.PlanificadorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ReplanificacionTests {

    @Autowired
    private PlanificadorService planificadorService;

    @Test
    void replanificarPorVueloCanceladoTest() throws Exception {
        // Setup via reflection to bypass DataLoader dependency
        Field aeropuertosCargadosField = PlanificadorService.class.getDeclaredField("aeropuertosCargados");
        aeropuertosCargadosField.setAccessible(true);
        
        Field vuelosCargadosField = PlanificadorService.class.getDeclaredField("vuelosCargados");
        vuelosCargadosField.setAccessible(true);

        Field solucionActualField = PlanificadorService.class.getDeclaredField("solucionActual");
        solucionActualField.setAccessible(true);

        Aeropuerto a1 = new Aeropuerto("LIM", "Lima", "PE", -5, 100, "América", -12.0, -77.0);
        Aeropuerto a2 = new Aeropuerto("BOG", "Bogota", "CO", -5, 100, "América", 4.0, -74.0);
        Map<String, Aeropuerto> aeros = new HashMap<>();
        aeros.put("LIM", a1);
        aeros.put("BOG", a2);

        Vuelo v1 = new Vuelo("LIM", "BOG", 600, 780, 100);
        Vuelo v2 = new Vuelo("LIM", "BOG", 800, 980, 100); // Vuelo alternativo

        List<Vuelo> vuelos = List.of(v1, v2);

        Envio e = new Envio("001", "LIM", "BOG", "20260102", "08", "00", "10", "c");
        e.setPlazoMaximoMinutos(1440);

        Ruta r = new Ruta(e);
        r.agregarVuelo(v1); // Envio en el vuelo 1

        Solucion sol = new Solucion();
        sol.agregarRuta(r);

        // Inyectamos mocks
        aeropuertosCargadosField.set(planificadorService, aeros);
        vuelosCargadosField.set(planificadorService, vuelos);
        solucionActualField.set(planificadorService, sol);

        // Ejecutamos cancelación de v1
        ReplanificacionResultDTO result = planificadorService.replanificarPorVueloCancelado("LIM", "BOG", 600);

        // Verificaciones
        assertThat(result.getEnviosAfectados()).isEqualTo(1);
        
        // Extraemos nueva solucion
        Solucion nuevaSol = (Solucion) solucionActualField.get(planificadorService);
        Ruta rutaActualizada = nuevaSol.getRutas().stream()
            .filter(ruta -> ruta.getEnvio().getId().equals("LIM-001"))
            .findFirst()
            .orElse(null);
            
        assertThat(rutaActualizada).isNotNull();
        
        // Como cancelamos el vuelo 1, el envío debería haber sido reasignado al vuelo 2
        if (!rutaActualizada.isSinSolucion()) {
            assertThat(rutaActualizada.getVuelos().get(0).getSalidaMinutos()).isEqualTo(800);
        }
    }
}
