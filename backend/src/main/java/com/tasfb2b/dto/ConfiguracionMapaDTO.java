package com.tasfb2b.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionMapaDTO {
    private boolean mostrarAlmacenes;
    private boolean mostrarUT;
    private boolean mostrarTramos;
    private int zoomInicial;
    private double centroLat;
    private double centroLng;
    private String colorTramos;
    private String colorUT;
    private String colorAlmacenes;
}
