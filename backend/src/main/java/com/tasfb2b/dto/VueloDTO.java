package com.tasfb2b.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VueloDTO {
    private String origen;
    private String destino;
    private String horaSalida;
    private String horaLlegada;
    private int capacidadMax;
    private int ocupacion;
}
