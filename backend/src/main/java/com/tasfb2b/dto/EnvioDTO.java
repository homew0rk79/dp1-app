package com.tasfb2b.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class EnvioDTO {
    private String id;
    private String origen;
    private String destino;
    private LocalDateTime fechaHoraRegistro;
    private int cantidad;
    private String idCliente;
    private boolean entregado;
    private int plazoMaximoMinutos;
}
