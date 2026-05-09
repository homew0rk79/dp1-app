package com.tasfb2b.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReplanificacionResultDTO {
    private int enviosAfectados;
    private int enviosReasignados;
    private int enviosSinRuta;
    private String mensaje;
}
