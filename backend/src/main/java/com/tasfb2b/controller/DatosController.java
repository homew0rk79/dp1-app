package com.tasfb2b.controller;

import com.tasfb2b.dto.AeropuertoDTO;
import com.tasfb2b.dto.MaletaEnAeropuertoDTO;
import com.tasfb2b.dto.VueloDTO;
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
}
