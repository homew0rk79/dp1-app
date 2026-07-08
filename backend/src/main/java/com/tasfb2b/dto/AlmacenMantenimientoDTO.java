package com.tasfb2b.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlmacenMantenimientoDTO {
    private Long id;
    private String codigo;
    private String nombre;
    private String ciudad;
    private String pais;
    private String continente;
    private double lat;
    private double lng;
    private int capacidadMax;
    private int ocupacionActual;
}
