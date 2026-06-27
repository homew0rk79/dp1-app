package com.tasfb2b.dto;

/**
 * Vuelo próximo a salir según la solución actual del planificador.
 * Usado por el panel del visualizador para listar vuelos ordenados por hora
 * de salida y permitir cancelarlos con un click.
 */
public class VueloProximoDTO {

    private final String origen;
    private final String destino;
    /** Minuto absoluto desde el inicio de la simulación en que el vuelo sale. */
    private final int salidaAbsMin;
    private final int llegadaAbsMin;
    /** Minuto del día (0..1439) de salida — para la clave de cancelación. */
    private final int horaSalidaMin;
    private final int diaSimulado;
    private final int maletas;
    private final int capacidadMax;
    /** "Día N · HH:MM" — listo para mostrar. */
    private final String salidaFormateada;
    /** Clave estable "origen|destino|horaSalidaMin" para identificar el vuelo. */
    private final String claveVuelo;

    public VueloProximoDTO(String origen, String destino,
                            int salidaAbsMin, int llegadaAbsMin,
                            int horaSalidaMin, int diaSimulado,
                            int maletas, int capacidadMax,
                            String salidaFormateada, String claveVuelo) {
        this.origen = origen;
        this.destino = destino;
        this.salidaAbsMin = salidaAbsMin;
        this.llegadaAbsMin = llegadaAbsMin;
        this.horaSalidaMin = horaSalidaMin;
        this.diaSimulado = diaSimulado;
        this.maletas = maletas;
        this.capacidadMax = capacidadMax;
        this.salidaFormateada = salidaFormateada;
        this.claveVuelo = claveVuelo;
    }

    public String getOrigen()             { return origen; }
    public String getDestino()            { return destino; }
    public int    getSalidaAbsMin()       { return salidaAbsMin; }
    public int    getLlegadaAbsMin()      { return llegadaAbsMin; }
    public int    getHoraSalidaMin()      { return horaSalidaMin; }
    public int    getDiaSimulado()        { return diaSimulado; }
    public int    getMaletas()            { return maletas; }
    public int    getCapacidadMax()       { return capacidadMax; }
    public String getSalidaFormateada()   { return salidaFormateada; }
    public String getClaveVuelo()         { return claveVuelo; }
}
