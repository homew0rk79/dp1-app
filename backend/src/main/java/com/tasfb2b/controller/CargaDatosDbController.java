package com.tasfb2b.controller;

import com.tasfb2b.service.CargaDatosDbService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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

    @PostMapping(value = "/aeropuertos/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> subirAeropuertos(@RequestParam("archivo") MultipartFile archivo) throws IOException {
        return cargaDatosDbService.cargarAeropuertosDesdeArchivo(archivo);
    }

    @PostMapping(value = "/vuelos/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> subirVuelos(@RequestParam("archivo") MultipartFile archivo) throws IOException {
        return cargaDatosDbService.cargarVuelosDesdeArchivo(archivo);
    }

    @PostMapping(value = "/envios/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> subirEnvios(@RequestParam("archivos") MultipartFile[] archivos) throws IOException {
        List<MultipartFile> listaArchivos = Arrays.asList(archivos);
        return cargaDatosDbService.cargarEnviosDesdeArchivos(listaArchivos);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> manejarArchivoInvalido(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(Map.of("error", ex.getMessage()));
    }
}
