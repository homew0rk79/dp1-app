package com.tasfb2b.dto;

import lombok.Data;

@Data
public class MetricasDTO {
    private int totalEnvios;
    private int enviosConRuta;
    private int enviosSinRuta;
    private int violacionesPlazo;
    private double porcentajeCumplimiento;
    private int vuelosSaturados;
    private int aeropuertosSaturados;
    private int diasAeropuertoSaturados;
    private double tiempoPromedioEntregaMinutos;
    private double escalasPromedio;
    private double costoTotal;
    private String semaforo;

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

    public MetricasDTO(int totalEnvios,
                       int enviosConRuta,
                       int enviosSinRuta,
                       int violacionesPlazo,
                       double porcentajeCumplimiento,
                       int vuelosSaturados,
                       int aeropuertosSaturados,
                       int diasAeropuertoSaturados,
                       double tiempoPromedioEntregaMinutos,
                       double escalasPromedio,
                       double costoTotal,
                       String semaforo) {
        this.totalEnvios = totalEnvios;
        this.enviosConRuta = enviosConRuta;
        this.enviosSinRuta = enviosSinRuta;
        this.violacionesPlazo = violacionesPlazo;
        this.porcentajeCumplimiento = porcentajeCumplimiento;
        this.vuelosSaturados = vuelosSaturados;
        this.aeropuertosSaturados = aeropuertosSaturados;
        this.diasAeropuertoSaturados = diasAeropuertoSaturados;
        this.tiempoPromedioEntregaMinutos = tiempoPromedioEntregaMinutos;
        this.escalasPromedio = escalasPromedio;
        this.costoTotal = costoTotal;
        this.semaforo = semaforo;
    }
}
