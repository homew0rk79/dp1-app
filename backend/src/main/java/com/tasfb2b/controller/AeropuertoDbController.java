package com.tasfb2b.controller;

import com.tasfb2b.model.Aeropuerto;
import com.tasfb2b.service.AeropuertoDbService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@RestController
@RequestMapping("/api/db/aeropuertos")
public class AeropuertoDbController {

    private final AeropuertoDbService aeropuertoDbService;

    public AeropuertoDbController(AeropuertoDbService aeropuertoDbService) {
        this.aeropuertoDbService = aeropuertoDbService;
    }

    @GetMapping
    public List<Aeropuerto> listarAeropuertos() {
        return aeropuertoDbService.listarTodos();
    }

    @PostMapping
    public Aeropuerto crearAeropuerto(@RequestBody Aeropuerto aeropuerto) {
        return aeropuertoDbService.guardar(aeropuerto);
    }
}