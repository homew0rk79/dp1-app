package com.tasfb2b.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RutaDTO {
    private Long id;
    private String idEnvioOriginal;
    private String origen;
    private String destino;
    private int cantidad;
    private int tiempoTotalMinutos;
    private int plazoMaximoMinutos;
    private String cumplimiento;
    private String estado;
    private boolean sinSolucion;
}
