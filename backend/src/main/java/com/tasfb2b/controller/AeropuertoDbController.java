package com.tasfb2b.controller;

import com.tasfb2b.dto.AeropuertoDTO;
import com.tasfb2b.model.Aeropuerto;
import com.tasfb2b.service.AeropuertoDbService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/db/aeropuertos")
public class AeropuertoDbController {

    private final AeropuertoDbService aeropuertoDbService;

    public AeropuertoDbController(AeropuertoDbService aeropuertoDbService) {
        this.aeropuertoDbService = aeropuertoDbService;
    }

    @GetMapping
    public List<AeropuertoDTO> listarAeropuertos() {
        return aeropuertoDbService.listarTodos()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @PostMapping
    public AeropuertoDTO crearAeropuerto(@RequestBody AeropuertoDTO dto) {
        return toDTO(aeropuertoDbService.guardar(toEntity(dto)));
    }

    private AeropuertoDTO toDTO(Aeropuerto a) {
        return new AeropuertoDTO(a.getCodigo(), a.getCiudad(), a.getPais(), a.getContinente(),
                a.getGmt(), a.getCapacidadMax(), a.getOcupacionActual(), a.getLat(), a.getLng());
    }

    private Aeropuerto toEntity(AeropuertoDTO dto) {
        Aeropuerto a = new Aeropuerto(dto.getCodigo(), dto.getCiudad(), dto.getPais(),
                dto.getGmt(), dto.getCapacidadMax(), dto.getContinente(), dto.getLat(), dto.getLng());
        a.setOcupacionActual(dto.getOcupacionActual());
        return a;
    }
}
