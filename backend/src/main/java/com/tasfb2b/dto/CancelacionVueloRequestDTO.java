package com.tasfb2b.dto;

import lombok.Data;

@Data
public class CancelacionVueloRequestDTO {
    private String origen;
    private String destino;
    private int horaSalidaMinutos;
}
