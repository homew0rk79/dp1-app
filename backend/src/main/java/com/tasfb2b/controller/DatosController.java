package com.tasfb2b.controller;

import com.tasfb2b.dto.AeropuertoDTO;
import com.tasfb2b.dto.VueloDTO;
import com.tasfb2b.service.PlanificadorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
}
