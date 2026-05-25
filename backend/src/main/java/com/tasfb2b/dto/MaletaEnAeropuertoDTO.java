package com.tasfb2b.dto;

/**
 * Detalle de un envío (grupo de maletas) presente en un aeropuerto en un momento concreto.
 * Usado por GET /api/aeropuertos/{codigo}/maletas?tiempoMin=N para alimentar
 * el panel desplegable del popup del visualizador.
 */
public class MaletaEnAeropuertoDTO {

    public enum Estado {
        /** El envío aún no ha salido de su aeropuerto origen. */
        PENDIENTE_SALIDA,
        /** El envío está en un hub intermedio esperando el siguiente vuelo. */
        EN_HUB,
        /** El envío ya llegó a su destino final. */
        ENTREGADA
    }

    private final String envioId;
    private final String origen;
    private final String destino;
    private final String ciudadOrigen;
    private final String ciudadDestino;
    private final int cantidad;
    private final String estado;
    /** Minuto absoluto desde el inicio del periodo en que el envío llegó (o se registró) aquí. */
    private final int tiempoLlegadaAeropuerto;
    /** Minuto absoluto en que el envío saldrá (o salió) de este aeropuerto. -1 si es el destino final. */
    private final int tiempoSalidaAeropuerto;
    private final boolean esDestinoFinal;

    public MaletaEnAeropuertoDTO(String envioId, String origen, String destino,
                                  String ciudadOrigen, String ciudadDestino,
                                  int cantidad, Estado estado,
                                  int tiempoLlegadaAeropuerto, int tiempoSalidaAeropuerto,
                                  boolean esDestinoFinal) {
        this.envioId = envioId;
        this.origen = origen;
        this.destino = destino;
        this.ciudadOrigen = ciudadOrigen;
        this.ciudadDestino = ciudadDestino;
        this.cantidad = cantidad;
        this.estado = estado.name();
        this.tiempoLlegadaAeropuerto = tiempoLlegadaAeropuerto;
        this.tiempoSalidaAeropuerto = tiempoSalidaAeropuerto;
        this.esDestinoFinal = esDestinoFinal;
    }

    public String getEnvioId()                  { return envioId; }
    public String getOrigen()                   { return origen; }
    public String getDestino()                  { return destino; }
    public String getCiudadOrigen()             { return ciudadOrigen; }
    public String getCiudadDestino()            { return ciudadDestino; }
    public int    getCantidad()                 { return cantidad; }
    public String getEstado()                   { return estado; }
    public int    getTiempoLlegadaAeropuerto()  { return tiempoLlegadaAeropuerto; }
    public int    getTiempoSalidaAeropuerto()   { return tiempoSalidaAeropuerto; }
    public boolean isEsDestinoFinal()           { return esDestinoFinal; }
}
