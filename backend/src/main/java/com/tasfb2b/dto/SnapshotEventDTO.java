package com.tasfb2b.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * Snapshot del estado del sistema en un momento dado:
 * ocupación de aeropuertos + carga de rutas.
 * Se envía por WebSocket cada N iteraciones del algoritmo.
 */
@Data
@AllArgsConstructor
public class SnapshotEventDTO {

    private int iteracion;
    private double costoActual;
    private List<AeropuertoItem> aeropuertos;
    private List<RutaItem> rutas;

    @Data
    @AllArgsConstructor
    public static class AeropuertoItem {
        private String codigo;
        private String ciudad;
        private String continente;
        private int ocupacion;
        private int capacidadMax;
        /** Porcentaje de ocupación redondeado a 1 decimal */
        private double porcentajeOcupacion;
        /** "VERDE" | "AMBAR" | "ROJO" */
        private String semaforo;
    }

    @Data
    @AllArgsConstructor
    public static class RutaItem {
        private String origen;
        private String destino;
        private int maletas;
    }
}
