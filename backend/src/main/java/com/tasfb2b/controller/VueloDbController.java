package com.tasfb2b.controller;

import com.tasfb2b.dto.VueloDTO;
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
    public List<VueloDTO> listarVuelos() {
        return vueloDbService.listarTodos()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @PostMapping
    public VueloDTO crearVuelo(@RequestBody VueloDTO dto) {
        return toDTO(vueloDbService.guardar(toEntity(dto)));
    }

    private VueloDTO toDTO(Vuelo v) {
        return new VueloDTO(v.getOrigen(), v.getDestino(),
                minutosAHora(v.getSalidaMinutos()), minutosAHora(v.getLlegadaMinutos()),
                v.getCapacidadMax(), v.getOcupacion());
    }

    private Vuelo toEntity(VueloDTO dto) {
        return new Vuelo(dto.getOrigen(), dto.getDestino(),
                Vuelo.parsearHora(dto.getHoraSalida()), Vuelo.parsearHora(dto.getHoraLlegada()),
                dto.getCapacidadMax());
    }

    private String minutosAHora(int minutos) {
        return String.format("%02d:%02d", minutos / 60, minutos % 60);
    }
}
