package com.tasfb2b.controller;

import com.tasfb2b.dto.AeropuertoDTO;
import com.tasfb2b.dto.EnvioEnVueloDTO;
import com.tasfb2b.dto.MaletaEnAeropuertoDTO;
import com.tasfb2b.dto.MonitorEnviosDTO;
import com.tasfb2b.dto.PlanificadosAeropuertoDTO;
import com.tasfb2b.dto.VueloDTO;
import com.tasfb2b.dto.VueloProximoDTO;
import com.tasfb2b.service.PlanificadorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DatosController {

    private final PlanificadorService service;

    public DatosController(PlanificadorService service) {
        this.service = service;
    }

    /**
     * Lista todos los aeropuertos cargados con su ocupación actual.
     * Retorna lista vacía si aún no se han cargado datos.
     */
    @GetMapping("/aeropuertos")
    public ResponseEntity<List<AeropuertoDTO>> getAeropuertos() {
        return ResponseEntity.ok(service.getAeropuertos());
    }

    /**
     * Lista todos los vuelos del plan de vuelo cargado.
     * Retorna lista vacía si aún no se han cargado datos.
     */
    @GetMapping("/vuelos")
    public ResponseEntity<List<VueloDTO>> getVuelos() {
        return ResponseEntity.ok(service.getVuelos());
    }

    /**
     * Detalle de envíos presentes en un aeropuerto en un momento concreto del periodo.
     * Usado por el popup desplegable del visualizador.
     *
     * @param codigo    código ICAO del aeropuerto
     * @param tiempoMin minuto absoluto desde el inicio del periodo (default 0)
     */
    @GetMapping("/aeropuertos/{codigo}/maletas")
    public ResponseEntity<List<MaletaEnAeropuertoDTO>> getMaletasEnAeropuerto(
            @PathVariable String codigo,
            @RequestParam(defaultValue = "0") int tiempoMin) {
        return ResponseEntity.ok(service.getMaletasEnAeropuerto(codigo, tiempoMin));
    }

    /**
     * Ocupación real de todos los aeropuertos en el minuto exacto consultado.
     * Retorna { "UMMS": 12, "KJFK": 45, ... } con maletas físicamente presentes.
     */
    @GetMapping("/aeropuertos/ocupacion-actual")
    public ResponseEntity<Map<String, Integer>> getOcupacionActual(
            @RequestParam(defaultValue = "0") int tiempoMin) {
        return ResponseEntity.ok(service.getOcupacionActual(tiempoMin));
    }

    /**
     * Vuelos próximos a salir según la solución actual, ordenados por hora de salida.
     * Usado por el panel de cancelación interactiva del visualizador.
     *
     * @param tiempoMin minuto absoluto desde el inicio del periodo (default 0)
     * @param limite    cantidad máxima de vuelos a devolver (default 20)
     */
    @GetMapping("/vuelos/proximos")
    public ResponseEntity<List<VueloProximoDTO>> getVuelosProximos(
            @RequestParam(defaultValue = "0") int tiempoMin,
            @RequestParam(defaultValue = "20") int limite) {
        return ResponseEntity.ok(service.getVuelosProximos(tiempoMin, limite));
    }

    /**
     * Envíos asignados a una instancia concreta de vuelo (UT).
     * Usado por el drill-down del panel de unidades de transporte.
     *
     * @param origen            aeropuerto de origen del vuelo
     * @param destino           aeropuerto de destino del vuelo
     * @param horaSalidaMinutos hora de salida en minutos desde medianoche
     * @param dia               día simulado (0-indexado)
     */
    @GetMapping("/vuelos/envios")
    public ResponseEntity<List<EnvioEnVueloDTO>> getEnviosDeVuelo(
            @RequestParam String origen,
            @RequestParam String destino,
            @RequestParam int horaSalidaMinutos,
            @RequestParam(defaultValue = "0") int dia) {
        return ResponseEntity.ok(service.getEnviosDeVuelo(origen, destino, horaSalidaMinutos, dia));
    }

    /**
     * Información planificada de envíos que entran y salen de un almacén.
     * Usado por el drill-down del panel de aeropuertos.
     */
    @GetMapping("/aeropuertos/{codigo}/planificados")
    public ResponseEntity<PlanificadosAeropuertoDTO> getPlanificadosAeropuerto(
            @PathVariable String codigo,
            @RequestParam(defaultValue = "0") int tiempoMin,
            @RequestParam(defaultValue = "30") int limite) {
        return ResponseEntity.ok(service.getPlanificadosAeropuerto(codigo, tiempoMin, limite));
    }

    /**
     * Monitor de envíos: planificados por salir, en vuelo y entregados
     * en las últimas {@code ventanaHoras} horas simuladas.
     */
    @GetMapping("/envios/monitor")
    public ResponseEntity<MonitorEnviosDTO> getMonitorEnvios(
            @RequestParam(defaultValue = "0") int tiempoMin,
            @RequestParam(defaultValue = "4") int ventanaHoras,
            @RequestParam(defaultValue = "50") int limite) {
        return ResponseEntity.ok(service.getMonitorEnvios(tiempoMin, ventanaHoras, limite));
    }
}
