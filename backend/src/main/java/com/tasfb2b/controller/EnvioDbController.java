package com.tasfb2b.controller;

import com.tasfb2b.dto.EnvioDTO;
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
    public List<EnvioDTO> listarEnvios(@RequestParam(defaultValue = "100") int limite) {
        return envioDbService.listarTodos()
                .stream()
                .limit(limite)
                .map(this::toDTO)
                .toList();
    }

    @PostMapping
    public EnvioDTO crearEnvio(@RequestBody EnvioDTO dto) {
        return toDTO(envioDbService.guardar(toEntity(dto)));
    }

    private EnvioDTO toDTO(Envio e) {
        return new EnvioDTO(e.getId(), e.getOrigen(), e.getDestino(),
                e.getFechaHoraRegistro(), e.getCantidad(), e.getIdCliente(),
                e.isEntregado(), e.getPlazoMaximoMinutos());
    }

    private Envio toEntity(EnvioDTO dto) {
        Envio envio = new Envio();
        envio.setId(dto.getId());
        envio.setOrigen(dto.getOrigen());
        envio.setDestino(dto.getDestino());
        envio.setFechaHoraRegistro(dto.getFechaHoraRegistro());
        envio.setCantidad(dto.getCantidad());
        envio.setIdCliente(dto.getIdCliente());
        envio.setEntregado(dto.isEntregado());
        envio.setPlazoMaximoMinutos(dto.getPlazoMaximoMinutos());
        return envio;
    }
}
