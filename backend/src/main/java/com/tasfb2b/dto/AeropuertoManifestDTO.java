package com.tasfb2b.dto;

import java.util.Map;

public class AeropuertoManifestDTO {

    private String codigo;
    private String ciudad;
    private String pais;
    private String continente;
    private double lat;
    private double lng;
    private int capacidadMax;
    private Map<Integer, Integer> ocupacionPorDia;

    public AeropuertoManifestDTO(String codigo, String ciudad, String pais, String continente,
                                  double lat, double lng, int capacidadMax,
                                  Map<Integer, Integer> ocupacionPorDia) {
        this.codigo          = codigo;
        this.ciudad          = ciudad;
        this.pais            = pais;
        this.continente      = continente;
        this.lat             = lat;
        this.lng             = lng;
        this.capacidadMax    = capacidadMax;
        this.ocupacionPorDia = ocupacionPorDia;
    }

    public String getCodigo()                        { return codigo; }
    public String getCiudad()                        { return ciudad; }
    public String getPais()                          { return pais; }
    public String getContinente()                    { return continente; }
    public double getLat()                           { return lat; }
    public double getLng()                           { return lng; }
    public int getCapacidadMax()                     { return capacidadMax; }
    public Map<Integer, Integer> getOcupacionPorDia() { return ocupacionPorDia; }
}
