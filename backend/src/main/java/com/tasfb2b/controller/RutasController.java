package com.tasfb2b.controller;

import com.tasfb2b.dto.CancelacionVueloRequestDTO;
import com.tasfb2b.dto.ReplanificacionResultDTO;
import com.tasfb2b.dto.RutaDetalleDTO;
import com.tasfb2b.dto.RutaResumenDTO;
import com.tasfb2b.service.PlanificadorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class RutasController {

    private final PlanificadorService service;

    public RutasController(PlanificadorService service) {
        this.service = service;
    }

    @GetMapping("/rutas")
    public List<RutaResumenDTO> getRutas(
            @RequestParam(defaultValue = "300") int limite) {
        return service.getRutasResumen(limite);
    }

    @GetMapping("/rutas/{id}")
    public ResponseEntity<RutaDetalleDTO> getDetalle(@PathVariable String id) {
        RutaDetalleDTO detalle = service.getRutaDetalle(id);
        if (detalle == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(detalle);
    }

    @PostMapping("/replanificacion/vuelo-cancelado")
    public ResponseEntity<?> replanificar(@RequestBody CancelacionVueloRequestDTO req) {
        try {
            ReplanificacionResultDTO result = service.replanificarPorVueloCancelado(
                req.getOrigen(), req.getDestino(), req.getHoraSalidaMinutos());
            return ResponseEntity.ok(result);
        } catch (IllegalStateException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}
