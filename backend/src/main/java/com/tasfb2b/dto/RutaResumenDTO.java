package com.tasfb2b.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RutaResumenDTO {
    private String id;
    private String origen;
    private String destino;
    private String origenCiudad;
    private String destinoCiudad;
    private String estado;        // "en_transito" | "sin_ruta"
    private String cumplimiento;  // "verde" | "ambar" | "rojo"
    private String tiempoEstimado;
    private String fechaIngreso;
    private String fechaLimite;
}
