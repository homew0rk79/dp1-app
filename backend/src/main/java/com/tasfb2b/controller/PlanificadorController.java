package com.tasfb2b.controller;

import com.tasfb2b.dto.EstadoDTO;
import com.tasfb2b.dto.IniciarRequestDTO;
import com.tasfb2b.dto.MetricasDTO;
import com.tasfb2b.service.PlanificadorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/planificacion")
public class PlanificadorController {

    private final PlanificadorService service;

    public PlanificadorController(PlanificadorService service) {
        this.service = service;
    }

    /**
     * Inicia una planificación en segundo plano.
     *
     * Body: { "escenario": "PERIODO", "fechaInicio": "2026-01-02", "numDias": 5 }
     * Escenarios válidos: PERIODO, DIA_A_DIA, COLAPSO
     */
    @PostMapping("/iniciar")
    public ResponseEntity<Map<String, String>> iniciar(@RequestBody IniciarRequestDTO req) {
        try {
            service.iniciar(req.getEscenario(), req.getFechaInicio(), req.getNumDias());
            return ResponseEntity.accepted()
                .body(Map.of("mensaje", "Planificación iniciada", "escenario", req.getEscenario()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", ex.getMessage()));
        }
    }

    /**
     * Devuelve el estado actual de la planificación.
     * Úsalo para polling desde el frontend hasta que estado = COMPLETADO o ERROR.
     */
    @GetMapping("/estado")
    public EstadoDTO getEstado() {
        return service.getEstado();
    }

    /**
     * Devuelve las métricas de la última planificación completada.
     * Retorna 204 si aún no hay resultados disponibles.
     */
    @GetMapping("/metricas")
    public ResponseEntity<MetricasDTO> getMetricas() {
        MetricasDTO m = service.getMetricas();
        if (m == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(m);
    }
}
