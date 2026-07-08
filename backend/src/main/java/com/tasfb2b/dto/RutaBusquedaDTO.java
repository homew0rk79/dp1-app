package com.tasfb2b.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RutaBusquedaDTO {
    private String tipo;
    private String idBuscado;
    private String idMaleta;
    private String idEnvio;
    private String origen;
    private String destino;
    private String idCliente;
    private int cantidadMaletas;
    private RutaMapaDTO rutaActual;
    private RutaMapaDTO rutaAnterior;

    @Data
    @AllArgsConstructor
    public static class RutaMapaDTO {
        private Long idRuta;
        private String origen;
        private String destino;
        private String estado;
        private String cumplimiento;
        private int cantidadMaletas;
        private List<String> escalas;
        private List<AeropuertoRutaDTO> aeropuertos;
        private List<TramoMapaDTO> tramos;
    }

    @Data
    @AllArgsConstructor
    public static class TramoMapaDTO {
        private Long id;
        private int orden;
        private String origen;
        private String destino;
        private AeropuertoRutaDTO aeropuertoOrigen;
        private AeropuertoRutaDTO aeropuertoDestino;
        private String salida;
        private String llegada;
        private Integer salidaAbs;
        private Integer llegadaAbs;
        private String estado;
    }

    @Data
    @AllArgsConstructor
    public static class AeropuertoRutaDTO {
        private String codigo;
        private String nombre;
        private String ciudad;
        private String pais;
        private Double lat;
        private Double lng;
    }
}
