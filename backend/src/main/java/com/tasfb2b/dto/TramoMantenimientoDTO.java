package com.tasfb2b.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TramoMantenimientoDTO {
    private Long id;
    private String utAsignada;
    private String origen;
    private String destino;
    private LocalDateTime horaSalida;
    private LocalDateTime horaLlegada;
    private String estado;
}
