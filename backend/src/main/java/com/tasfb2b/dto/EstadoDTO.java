package com.tasfb2b.dto;

import lombok.Data;

@Data
public class EstadoDTO {
    private String estado;
    private int progreso;
    private String mensaje;
    private String escenario;

    public EstadoDTO(String estado, int progreso, String mensaje, String escenario) {
        this.estado = estado;
        this.progreso = progreso;
        this.mensaje = mensaje;
        this.escenario = escenario;
    }
}
