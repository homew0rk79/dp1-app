package com.tasfb2b.controller;

import com.tasfb2b.model.Envio;
import com.tasfb2b.service.EnvioDbService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/db/envios")
public class EnvioDbController {

    private final EnvioDbService envioDbService;

    public EnvioDbController(EnvioDbService envioDbService) {
        this.envioDbService = envioDbService;
    }

    @GetMapping
    public List<Envio> listarEnvios(@RequestParam(defaultValue = "100") int limite) {
        return envioDbService.listarTodos()
                .stream()
                .limit(limite)
                .toList();
    }

    @PostMapping
    public Envio crearEnvio(@RequestBody Envio envio) {
        return envioDbService.guardar(envio);
    }
}