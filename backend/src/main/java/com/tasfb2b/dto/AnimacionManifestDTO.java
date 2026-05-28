package com.tasfb2b.dto;

import java.util.List;

public class AnimacionManifestDTO {

    private int duracionTotalMinutos;
    private int fechaInicioMinutos;
    private List<OcurrenciaVueloDTO> ocurrencias;
    private List<AeropuertoManifestDTO> aeropuertos;

    public AnimacionManifestDTO(int duracionTotalMinutos,
                                 int fechaInicioMinutos,
                                 List<OcurrenciaVueloDTO> ocurrencias,
                                 List<AeropuertoManifestDTO> aeropuertos) {
        this.duracionTotalMinutos = duracionTotalMinutos;
        this.fechaInicioMinutos   = fechaInicioMinutos;
        this.ocurrencias          = ocurrencias;
        this.aeropuertos          = aeropuertos;
    }

    public int getDuracionTotalMinutos()             { return duracionTotalMinutos; }
    public int getFechaInicioMinutos()               { return fechaInicioMinutos; }
    public List<OcurrenciaVueloDTO> getOcurrencias() { return ocurrencias; }
    public List<AeropuertoManifestDTO> getAeropuertos() { return aeropuertos; }
}
