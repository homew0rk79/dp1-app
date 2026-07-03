package com.tasfb2b.controller;

import com.tasfb2b.dto.*;
import com.tasfb2b.service.PlanificadorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @GetMapping("/animacion")
    public ResponseEntity<AnimacionManifestDTO> getAnimacionManifest() {
        AnimacionManifestDTO manifest = service.getAnimacionManifest();
        if (manifest == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(manifest);
    }

    @PostMapping("/detener")
    public ResponseEntity<Map<String, String>> detener() {
        service.detener();
        return ResponseEntity.ok(Map.of("mensaje", "Planificación detenida"));
    }

    @GetMapping("/consumo-bloques")
    public ResponseEntity<java.util.List<Map<String, Object>>> getConsumoBloques() {
        return ResponseEntity.ok(service.getConsumoBloques());
    }

    @PostMapping("/upload-envios")
    public ResponseEntity<Map<String, Object>> uploadEnvios(
            @RequestParam("archivo") MultipartFile[] archivos) {
        Map<String, Object> resultado = service.importarEnviosTxt(archivos);
        return ResponseEntity.ok(resultado);
    }

    @PostMapping("/limpiar-envios")
    public ResponseEntity<Map<String, String>> limpiarEnvios() {
        service.limpiarEnvios();
        return ResponseEntity.ok(Map.of("mensaje", "Tabla de envíos limpiada"));
    }

    @PostMapping("/registrar-envio")
    public ResponseEntity<Map<String, Object>> registrarEnvio(
            @RequestBody com.tasfb2b.dto.RegistrarEnvioRequestDTO req) {
        if (req.getOrigen() == null || req.getDestino() == null || req.getCantidad() < 1) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Campos requeridos: origen, destino, cantidad (≥1)"));
        }
        Map<String, Object> resultado = service.registrarEnvio(req);
        return ResponseEntity.ok(resultado);
    }

}
