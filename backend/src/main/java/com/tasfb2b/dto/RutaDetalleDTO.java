package com.tasfb2b.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RutaDetalleDTO {
    private String id;
    private String origen;
    private String destino;
    private String origenCiudad;
    private String destinoCiudad;
    private String estado;
    private String cumplimiento;
    private String tiempoEstimado;
    private int progreso;
    private String plazoCompromiso;
    private String fechaIngreso;
    private String fechaLimite;
    private List<TramoDTO> tramos;

    @Data
    @AllArgsConstructor
    public static class TramoDTO {
        private String id;
        private String vuelo;
        private String origen;
        private String destino;
        private int horaSalidaMinutos;
        private int ocupacion;
        private int capacidadMax;
        private String salida;
        private String llegada;
        private String estado;
    }
}
