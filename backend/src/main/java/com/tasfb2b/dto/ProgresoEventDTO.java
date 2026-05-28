package com.tasfb2b.dto;

import lombok.Data;

@Data
public class ProgresoEventDTO {
    private int porcentaje;
    private String mensaje;
    private String estado;
    private double costoActual;

    private long tiempoCargaMs;
    private long tiempoPlanificacionMs;
    private long tiempoPersistenciaMs;
    private long tiempoTotalMs;
    private int aeropuertosProcesados;
    private int vuelosProcesados;
    private int rutasGeneradas;
    private int eventosProcesados;
    private int maletasSimuladas;
    private int vuelosUtilizados;
    private int iteracionesEjecutadas;
    private int replanificacionesEjecutadas;
    private double tiempoMaximoEntregaMinutos;
    private int rutasInvalidas;
    private int retrasos;
    private int eventosFueraRangoTemporal;

    public ProgresoEventDTO(int porcentaje, String mensaje, String estado, double costoActual) {
        this.porcentaje = porcentaje;
        this.mensaje = mensaje;
        this.estado = estado;
        this.costoActual = costoActual;
    }
}
