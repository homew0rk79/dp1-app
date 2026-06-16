package com.tasfb2b.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ColapsoEventDTO {
    private String tipo;           // "AEROPUERTO", "VUELO", "SLA"
    private String entidad;        // código ICAO, clave de vuelo, o ID de envío
    private String descripcion;    // mensaje legible
    private String fechaColapso;   // "2026-11-05"
    private int    minutosColapso; // minuto absoluto desde 2026-01-01 del primer overflow; 0 si no aplica
}
