package com.tasfb2b.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AeropuertoDTO {
    private String codigo;
    private String ciudad;
    private String pais;
    private String continente;
    private int gmt;
    private int capacidadMax;
    private int ocupacionActual;
    private double lat;
    private double lng;
}
