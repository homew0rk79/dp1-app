package com.tasfb2b.controller;

import com.tasfb2b.model.TramoRuta;
import com.tasfb2b.service.TramoRutaDbService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/db/tramos-ruta")
public class TramoRutaDbController {

    private final TramoRutaDbService tramoRutaDbService;

    public TramoRutaDbController(TramoRutaDbService tramoRutaDbService) {
        this.tramoRutaDbService = tramoRutaDbService;
    }

    @GetMapping
    public List<TramoRuta> listarTramosRuta() {
        return tramoRutaDbService.listarTodos();
    }

    @PostMapping
    public TramoRuta crearTramoRuta(@RequestBody TramoRuta tramoRuta) {
        return tramoRutaDbService.guardar(tramoRuta);
    }
}