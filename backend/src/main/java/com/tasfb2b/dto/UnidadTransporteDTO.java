package com.tasfb2b.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnidadTransporteDTO {
    private Long id;
    private String codigo;
    private String tipo;
    private String ubicacionActual;
    private int capacidadMax;
    private String estado;
}
