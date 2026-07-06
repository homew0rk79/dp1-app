package com.tasfb2b.dto;

/**
 * Envío asignado a una instancia concreta de vuelo (UT).
 * Usado por el drill-down del panel de unidades de transporte.
 */
public class EnvioEnVueloDTO {

    private final String envioId;
    private final String origenEnvio;
    private final String destinoEnvio;
    private final String ciudadOrigen;
    private final String ciudadDestino;
    /** Cantidad de maletas (productos) del envío. */
    private final int cantidad;

    public EnvioEnVueloDTO(String envioId, String origenEnvio, String destinoEnvio,
                            String ciudadOrigen, String ciudadDestino, int cantidad) {
        this.envioId = envioId;
        this.origenEnvio = origenEnvio;
        this.destinoEnvio = destinoEnvio;
        this.ciudadOrigen = ciudadOrigen;
        this.ciudadDestino = ciudadDestino;
        this.cantidad = cantidad;
    }

    public String getEnvioId()       { return envioId; }
    public String getOrigenEnvio()   { return origenEnvio; }
    public String getDestinoEnvio()  { return destinoEnvio; }
    public String getCiudadOrigen()  { return ciudadOrigen; }
    public String getCiudadDestino() { return ciudadDestino; }
    public int    getCantidad()      { return cantidad; }
}
