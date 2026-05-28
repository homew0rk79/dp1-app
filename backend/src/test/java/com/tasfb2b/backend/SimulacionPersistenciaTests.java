package com.tasfb2b.backend;

import com.tasfb2b.model.Envio;
import com.tasfb2b.model.Ruta;
import com.tasfb2b.model.Simulacion;
import com.tasfb2b.model.TramoRuta;
import com.tasfb2b.model.Vuelo;
import com.tasfb2b.repository.EnvioRepository;
import com.tasfb2b.repository.RutaRepository;
import com.tasfb2b.repository.SimulacionRepository;
import com.tasfb2b.repository.TramoRutaRepository;
import com.tasfb2b.repository.VueloRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class SimulacionPersistenciaTests {

    @Autowired
    private SimulacionRepository simulacionRepository;

    @Autowired
    private EnvioRepository envioRepository;

    @Autowired
    private VueloRepository vueloRepository;

    @Autowired
    private RutaRepository rutaRepository;

    @Autowired
    private TramoRutaRepository tramoRutaRepository;

    @Test
    void persisteSimulacionRutaYTramosConRelaciones() {
        Simulacion simulacion = simulacionRepository.save(
            new Simulacion("PERIODO", LocalDate.of(2026, 1, 2), 7));

        Envio envio = new Envio(
            "000000001", "SPJC", "SKBO",
            "20260102", "08", "15",
            "12", "cliente-test");
        envio.setPlazoMaximoMinutos(1440);
        envio = envioRepository.save(envio);

        Vuelo vuelo = vueloRepository.save(new Vuelo("SPJC", "SKBO", 600, 780, 100));

        Ruta ruta = new Ruta(envio);
        ruta.setSimulacion(simulacion);
        ruta.agregarVuelo(vuelo);
        ruta.sincronizarDatosPersistentes();
        ruta = rutaRepository.save(ruta);

        TramoRuta tramo = new TramoRuta();
        tramo.setRuta(ruta);
        tramo.setVuelo(vuelo);
        tramo.setIdRuta(ruta.getId());
        tramo.setIdVuelo(vuelo.getId());
        tramo.setOrden(1);
        tramo.setHoraSalidaProgramada(vuelo.getSalidaMinutos());
        tramo.setHoraLlegadaProgramada(vuelo.getLlegadaMinutos());
        tramo.setSalidaAbs(2040);
        tramo.setLlegadaAbs(2220);
        tramo.setCapacidadReservada(envio.getCantidad());
        tramo.setEstado("PROGRAMADO");
        tramoRutaRepository.save(tramo);

        List<Ruta> rutas = rutaRepository.findBySimulacionId(simulacion.getId());
        List<TramoRuta> tramos = tramoRutaRepository.findByRutaIdOrderByOrdenAsc(ruta.getId());

        assertThat(rutas).hasSize(1);
        assertThat(rutas.get(0).getEnvio().getId()).isEqualTo("SPJC-000000001");
        assertThat(rutas.get(0).getSimulacion().getId()).isEqualTo(simulacion.getId());
        assertThat(tramos).hasSize(1);
        assertThat(tramos.get(0).getVuelo().getId()).isEqualTo(vuelo.getId());
        assertThat(tramos.get(0).getOrden()).isEqualTo(1);
    }
}
