package com.tasfb2b.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProgresoEventDTO {
    private int porcentaje;
    private String mensaje;
    private String estado;
    private double costoActual;
}
