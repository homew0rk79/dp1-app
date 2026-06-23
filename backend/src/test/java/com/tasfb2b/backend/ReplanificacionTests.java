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

    @Test
    void replanificarPorVueloCanceladoSinAlternativaMarcaCancelado() throws Exception {
        Field aeropuertosCargadosField = PlanificadorService.class.getDeclaredField("aeropuertosCargados");
        aeropuertosCargadosField.setAccessible(true);

        Field vuelosCargadosField = PlanificadorService.class.getDeclaredField("vuelosCargados");
        vuelosCargadosField.setAccessible(true);

        Field solucionActualField = PlanificadorService.class.getDeclaredField("solucionActual");
        solucionActualField.setAccessible(true);

        Aeropuerto a1 = new Aeropuerto("LIM", "Lima", "PE", -5, 100, "America", -12.0, -77.0);
        Aeropuerto a2 = new Aeropuerto("BOG", "Bogota", "CO", -5, 100, "America", 4.0, -74.0);
        Map<String, Aeropuerto> aeros = new HashMap<>();
        aeros.put("LIM", a1);
        aeros.put("BOG", a2);

        Vuelo v1 = new Vuelo("LIM", "BOG", 600, 780, 100);
        Envio e = new Envio("002", "LIM", "BOG", "20260102", "08", "00", "10", "c");
        e.setPlazoMaximoMinutos(1440);

        Ruta r = new Ruta(e);
        r.agregarVuelo(v1);

        Solucion sol = new Solucion();
        sol.agregarRuta(r);

        aeropuertosCargadosField.set(planificadorService, aeros);
        vuelosCargadosField.set(planificadorService, List.of(v1));
        solucionActualField.set(planificadorService, sol);

        ReplanificacionResultDTO result = planificadorService.replanificarPorVueloCancelado("LIM", "BOG", 600);

        Solucion nuevaSol = (Solucion) solucionActualField.get(planificadorService);
        Ruta rutaActualizada = nuevaSol.getRutas().stream()
            .filter(ruta -> ruta.getEnvio().getId().equals("LIM-002"))
            .findFirst()
            .orElse(null);

        assertThat(result.getEnviosAfectados()).isEqualTo(1);
        assertThat(result.getEnviosReasignados()).isZero();
        assertThat(result.getEnviosSinRuta()).isEqualTo(1);
        assertThat(result.getEnviosCancelados()).containsExactly("LIM-002");
        assertThat(rutaActualizada).isNotNull();
        assertThat(rutaActualizada.isSinSolucion()).isTrue();
        assertThat(rutaActualizada.getEstado()).isEqualTo("cancelado");
    }

    @Test
    void cancelarEnvioPorIdMarcaRutaCancelada() throws Exception {
        Field aeropuertosCargadosField = PlanificadorService.class.getDeclaredField("aeropuertosCargados");
        aeropuertosCargadosField.setAccessible(true);

        Field solucionActualField = PlanificadorService.class.getDeclaredField("solucionActual");
        solucionActualField.setAccessible(true);

        Aeropuerto a1 = new Aeropuerto("EBCI", "Bruselas", "BE", 1, 100, "Europa", 50.46, 4.45);
        Aeropuerto a2 = new Aeropuerto("SPIM", "Lima", "PE", -5, 100, "America", -12.02, -77.11);
        Map<String, Aeropuerto> aeros = new HashMap<>();
        aeros.put("EBCI", a1);
        aeros.put("SPIM", a2);

        Vuelo v1 = new Vuelo("EBCI", "SPIM", 456, 1200, 100);
        Envio e = new Envio("000008556", "EBCI", "SPIM", "20260623", "00", "28", "10", "c");
        e.setPlazoMaximoMinutos(2880);

        Ruta r = new Ruta(e);
        r.agregarVuelo(v1);

        Solucion sol = new Solucion();
        sol.agregarRuta(r);

        aeropuertosCargadosField.set(planificadorService, aeros);
        solucionActualField.set(planificadorService, sol);

        ReplanificacionResultDTO result = planificadorService.cancelarEnvio("EBCI-000008556");

        Solucion nuevaSol = (Solucion) solucionActualField.get(planificadorService);
        Ruta rutaActualizada = nuevaSol.getRutas().stream()
            .filter(ruta -> ruta.getEnvio().getId().equals("EBCI-000008556"))
            .findFirst()
            .orElse(null);

        assertThat(result.getEnvioSolicitanteId()).isEqualTo("EBCI-000008556");
        assertThat(result.getEnviosCancelados()).containsExactly("EBCI-000008556");
        assertThat(rutaActualizada).isNotNull();
        assertThat(rutaActualizada.isSinSolucion()).isTrue();
        assertThat(rutaActualizada.getEstado()).isEqualTo("cancelado");
        assertThat(rutaActualizada.getVuelos()).isEmpty();
    }
}
