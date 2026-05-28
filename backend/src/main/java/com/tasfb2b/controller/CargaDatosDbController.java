package com.tasfb2b.controller;

import com.tasfb2b.service.CargaDatosDbService;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/db/carga")
public class CargaDatosDbController {

    private final CargaDatosDbService cargaDatosDbService;

    public CargaDatosDbController(CargaDatosDbService cargaDatosDbService) {
        this.cargaDatosDbService = cargaDatosDbService;
    }

    @PostMapping("/aeropuertos")
    public String cargarAeropuertos() throws IOException {
        cargaDatosDbService.cargarAeropuertos();
        return "Aeropuertos cargados en PostgreSQL";
    }

    @PostMapping("/vuelos")
    public String cargarVuelos() throws IOException {
        cargaDatosDbService.cargarVuelos();
        return "Vuelos cargados en PostgreSQL";
    }

    @PostMapping("/envios")
    public String cargarEnvios() throws IOException {
        cargaDatosDbService.cargarEnvios();
        return "Envios cargados en PostgreSQL";
    }
}