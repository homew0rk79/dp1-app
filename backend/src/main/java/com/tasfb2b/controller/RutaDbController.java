package com.tasfb2b.controller;

import com.tasfb2b.dto.RutaDTO;
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
    public List<RutaDTO> listarRutas() {
        return rutaDbService.listarTodos()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @PostMapping
    public RutaDTO crearRuta(@RequestBody RutaDTO dto) {
        return toDTO(rutaDbService.guardar(toEntity(dto)));
    }

    private RutaDTO toDTO(Ruta r) {
        return new RutaDTO(r.getId(), r.getIdEnvioOriginal(), r.getOrigen(), r.getDestino(),
                r.getCantidad() != null ? r.getCantidad() : 0,
                r.getTiempoTotalMinutos() != null ? r.getTiempoTotalMinutos() : 0,
                r.getPlazoMaximoMinutos() != null ? r.getPlazoMaximoMinutos() : 0,
                r.getCumplimiento(), r.getEstado(), r.isSinSolucion());
    }

    private Ruta toEntity(RutaDTO dto) {
        Ruta ruta = new Ruta();
        ruta.setIdEnvioOriginal(dto.getIdEnvioOriginal());
        ruta.setOrigen(dto.getOrigen());
        ruta.setDestino(dto.getDestino());
        ruta.setCantidad(dto.getCantidad());
        ruta.setTiempoTotalMinutos(dto.getTiempoTotalMinutos());
        ruta.setPlazoMaximoMinutos(dto.getPlazoMaximoMinutos());
        ruta.setCumplimiento(dto.getCumplimiento());
        ruta.setEstado(dto.getEstado());
        ruta.setSinSolucion(dto.isSinSolucion());
        return ruta;
    }
}
