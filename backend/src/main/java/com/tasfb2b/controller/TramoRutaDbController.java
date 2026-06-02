package com.tasfb2b.controller;

import com.tasfb2b.dto.TramoRutaDTO;
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
    public List<TramoRutaDTO> listarTramosRuta() {
        return tramoRutaDbService.listarTodos()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @PostMapping
    public TramoRutaDTO crearTramoRuta(@RequestBody TramoRutaDTO dto) {
        return toDTO(tramoRutaDbService.guardar(toEntity(dto)));
    }

    private TramoRutaDTO toDTO(TramoRuta t) {
        return new TramoRutaDTO(t.getId(), t.getIdRuta(), t.getIdVuelo(),
                t.getOrden() != null ? t.getOrden() : 0,
                t.getHoraSalidaProgramada() != null ? t.getHoraSalidaProgramada() : 0,
                t.getHoraLlegadaProgramada() != null ? t.getHoraLlegadaProgramada() : 0,
                t.getSalidaAbs() != null ? t.getSalidaAbs() : 0,
                t.getLlegadaAbs() != null ? t.getLlegadaAbs() : 0,
                t.getCapacidadReservada() != null ? t.getCapacidadReservada() : 0,
                t.getEstado());
    }

    private TramoRuta toEntity(TramoRutaDTO dto) {
        TramoRuta t = new TramoRuta();
        t.setIdRuta(dto.getIdRuta());
        t.setIdVuelo(dto.getIdVuelo());
        t.setOrden(dto.getOrden());
        t.setHoraSalidaProgramada(dto.getHoraSalidaProgramada());
        t.setHoraLlegadaProgramada(dto.getHoraLlegadaProgramada());
        t.setSalidaAbs(dto.getSalidaAbs());
        t.setLlegadaAbs(dto.getLlegadaAbs());
        t.setCapacidadReservada(dto.getCapacidadReservada());
        t.setEstado(dto.getEstado());
        return t;
    }
}
