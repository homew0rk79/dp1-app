package com.tasfb2b.controller;

import com.tasfb2b.model.Vuelo;
import com.tasfb2b.service.VueloDbService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/db/vuelos")
public class VueloDbController {

    private final VueloDbService vueloDbService;

    public VueloDbController(VueloDbService vueloDbService) {
        this.vueloDbService = vueloDbService;
    }

    @GetMapping
    public List<Vuelo> listarVuelos() {
        return vueloDbService.listarTodos();
    }

    @PostMapping
    public Vuelo crearVuelo(@RequestBody Vuelo vuelo) {
        return vueloDbService.guardar(vuelo);
    }
}