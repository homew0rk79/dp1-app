package com.tasfb2b.dto;

public class OcurrenciaVueloDTO {

    private String origen;
    private String destino;
    private int salidaAbs;
    private int llegadaAbs;
    private int maletas;
    private int capacidadMax;

    public OcurrenciaVueloDTO(String origen, String destino,
                               int salidaAbs, int llegadaAbs,
                               int maletas, int capacidadMax) {
        this.origen      = origen;
        this.destino     = destino;
        this.salidaAbs   = salidaAbs;
        this.llegadaAbs  = llegadaAbs;
        this.maletas     = maletas;
        this.capacidadMax = capacidadMax;
    }

    public String getOrigen()       { return origen; }
    public String getDestino()      { return destino; }
    public int getSalidaAbs()       { return salidaAbs; }
    public int getLlegadaAbs()      { return llegadaAbs; }
    public int getMaletas()         { return maletas; }
    public int getCapacidadMax()    { return capacidadMax; }
}
