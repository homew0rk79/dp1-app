package com.tasfb2b.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TramoRutaDTO {
    private Long id;
    private Long idRuta;
    private Long idVuelo;
    private int orden;
    private int horaSalidaProgramada;
    private int horaLlegadaProgramada;
    private int salidaAbs;
    private int llegadaAbs;
    private int capacidadReservada;
    private String estado;
}
