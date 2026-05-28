package com.tasfb2b.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "simulacion")
@Getter
@Setter
@NoArgsConstructor
public class Simulacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String escenario;
    private String estado;
    private String descripcion;

    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    private int numDias;
    private int duracionTotalMinutos;

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

    private Long tiempoCargaMs = 0L;
    private Long tiempoPlanificacionMs = 0L;
    private Long tiempoPersistenciaMs = 0L;
    private Long tiempoTotalMs = 0L;
    private Integer aeropuertosProcesados = 0;
    private Integer vuelosProcesados = 0;
    private Integer rutasGeneradas = 0;
    private Integer eventosProcesados = 0;
    private Integer maletasSimuladas = 0;
    private Integer vuelosUtilizados = 0;
    private Integer iteracionesEjecutadas = 0;
    private Integer replanificacionesEjecutadas = 0;
    private Double tiempoMaximoEntregaMinutos = 0.0;
    private Integer rutasInvalidas = 0;
    private Integer retrasos = 0;
    private Integer eventosFueraRangoTemporal = 0;

    public Simulacion(String escenario, LocalDate fechaInicio, int numDias) {
        this.escenario = escenario;
        this.fechaInicio = fechaInicio;
        this.numDias = numDias;
        this.estado = "CARGANDO";
        this.fechaCreacion = LocalDateTime.now();
        this.fechaActualizacion = this.fechaCreacion;
        if (fechaInicio != null && numDias > 0) {
            this.fechaFin = fechaInicio.plusDays(numDias);
        }
    }
}
