package com.tasfb2b.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "configuracion_mapa")
@Getter
@Setter
@NoArgsConstructor
public class ConfiguracionMapa {

    @Id
    private Long id = 1L;

    private boolean mostrarAlmacenes = true;
    private boolean mostrarUT = true;
    private boolean mostrarTramos = true;
    private int zoomInicial = 2;
    private double centroLat = 20;
    private double centroLng = 15;
    private String colorTramos = "#2563eb";
    private String colorUT = "#2563eb";
    private String colorAlmacenes = "#22c55e";
}
