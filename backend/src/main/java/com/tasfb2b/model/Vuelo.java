package com.tasfb2b.model;

public class Vuelo {

    private final String origen;          // Código ICAO origen
    private final String destino;         // Código ICAO destino
    private final int salidaMinutos;      // Hora de salida en minutos desde medianoche
    private final int llegadaMinutos;     // Hora de llegada en minutos desde medianoche
    private final int capacidadMax;       // Máximo de maletas que admite el vuelo

    // Ocupación dinámica: maletas ya asignadas a este vuelo
    private int ocupacion;

    public Vuelo(String origen, String destino, int salidaMinutos, int llegadaMinutos, int capacidadMax) {
        this.origen = origen;
        this.destino = destino;
        this.salidaMinutos = salidaMinutos;
        this.llegadaMinutos = llegadaMinutos;
        this.capacidadMax = capacidadMax;
        this.ocupacion = 0;
    }

    // Convierte "HH:MM" a minutos desde medianoche. Ej: "03:34" → 214
    public static int parsearHora(String hora) {
        String[] partes = hora.split(":");
        return Integer.parseInt(partes[0]) * 60 + Integer.parseInt(partes[1]);
    }

    // Duración del vuelo en minutos.
    // Si la llegada es al día siguiente (llegada < salida), sumamos 1440 (24h).
    public int getDuracionMinutos() {
        if (llegadaMinutos >= salidaMinutos) {
            return llegadaMinutos - salidaMinutos;
        } else {
            return (1440 - salidaMinutos) + llegadaMinutos;
        }
    }

    // Verifica si un envío que llega a 'tiempoDisponible' puede tomar este vuelo.
    // Se exige al menos 30 min de margen para el transbordo.
    public boolean esTomable(int tiempoDisponibleMinutos) {
        int margen = 30;
        int salidaEfectiva = salidaMinutos;

        // Si el vuelo ya pasó hoy, evaluar el del día siguiente
        if (tiempoDisponibleMinutos + margen > salidaEfectiva) {
            salidaEfectiva += 1440;
        }
        return true; // el tiempo de espera siempre es calculable; la restricción real es la capacidad
    }

    // Tiempo de espera (en minutos) desde 'tiempoLlegada' hasta poder abordar este vuelo.
    public int tiempoEspera(int tiempoLlegadaMinutos) {
        int margen = 30;
        int salidaEfectiva = salidaMinutos;

        if (tiempoLlegadaMinutos + margen > salidaEfectiva) {
            salidaEfectiva += 1440; // vuelo del día siguiente
        }
        return salidaEfectiva - tiempoLlegadaMinutos;
    }

    public boolean tieneCapacidad(int cantidad) {
        return ocupacion + cantidad <= capacidadMax;
    }

    public boolean asignar(int cantidad) {
        if (!tieneCapacidad(cantidad)) return false;
        ocupacion += cantidad;
        return true;
    }

    public void liberar(int cantidad) {
        ocupacion = Math.max(0, ocupacion - cantidad);
    }

    public String getOrigen()         { return origen; }
    public String getDestino()        { return destino; }
    public int getSalidaMinutos()     { return salidaMinutos; }
    public int getLlegadaMinutos()    { return llegadaMinutos; }
    public int getCapacidadMax()      { return capacidadMax; }
    public int getOcupacion()         { return ocupacion; }
    public void setOcupacion(int v)   { this.ocupacion = v; }

    // Clave única para identificar el vuelo en la lista tabú
    public String getClave() {
        return origen + "-" + destino + "-" + salidaMinutos;
    }

    @Override
    public String toString() {
        return origen + "→" + destino
             + " " + formatearMinutos(salidaMinutos) + "-" + formatearMinutos(llegadaMinutos)
             + " [" + ocupacion + "/" + capacidadMax + "]";
    }

    private String formatearMinutos(int minutos) {
        return String.format("%02d:%02d", minutos / 60, minutos % 60);
    }
}
