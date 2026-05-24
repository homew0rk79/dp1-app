package com.tasfb2b.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MetricasDTO {
    private int totalEnvios;
    private int enviosConRuta;
    private int enviosSinRuta;
    private int violacionesPlazo;
    private double porcentajeCumplimiento;
    private int vuelosSaturados;
    private int aeropuertosSaturados;
    private int diasAeropuertoSaturados;
    private double tiempoPromedioEntregaMinutos;
    private double escalasPromedio;
    private double costoTotal;
    private String semaforo;
}
