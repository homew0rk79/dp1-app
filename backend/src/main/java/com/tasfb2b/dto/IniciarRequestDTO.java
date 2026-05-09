package com.tasfb2b.dto;

import lombok.Data;

@Data
public class IniciarRequestDTO {
    /** "PERIODO", "DIA_A_DIA", "COLAPSO" */
    private String escenario = "PERIODO";
    /** Fecha de inicio (yyyy-MM-dd), requerida para PERIODO y DIA_A_DIA */
    private String fechaInicio;
    /** Cantidad de días a simular (solo para PERIODO) */
    private int numDias = 5;
}
