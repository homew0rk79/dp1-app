package com.tasfb2b.dto;

import lombok.Data;

@Data
public class IniciarRequestDTO {
    /** "PERIODO", "DIA_A_DIA", "COLAPSO" */
    private String escenario = "PERIODO";
    /** Fecha-hora de inicio (yyyy-MM-ddTHH:mm). Tambien acepta yyyy-MM-dd por compatibilidad. */
    private String fechaInicio;
    /** Cantidad de días a simular (solo para PERIODO) */
    private int numDias = 5;
}
