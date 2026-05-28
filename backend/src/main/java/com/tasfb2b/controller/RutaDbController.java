package com.tasfb2b.controller;

import com.tasfb2b.model.Ruta;
import com.tasfb2b.service.RutaDbService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/db/rutas")
public class RutaDbController {

    private final RutaDbService rutaDbService;

    public RutaDbController(RutaDbService rutaDbService) {
        this.rutaDbService = rutaDbService;
    }

    @GetMapping
    public List<Ruta> listarRutas() {
        return rutaDbService.listarTodos();
    }

    @PostMapping
    public Ruta crearRuta(@RequestBody Ruta ruta) {
        return rutaDbService.guardar(ruta);
    }
}