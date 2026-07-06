package com.tasfb2b.dto;

import java.util.List;

/**
 * Información planificada de envíos que entran y salen de un almacén (aeropuerto).
 * Cada item indica el envío, el vuelo que lo mueve, la hora absoluta y la
 * cantidad de maletas (productos).
 */
public class PlanificadosAeropuertoDTO {

    public static class ItemPlanificado {
        private final String envioId;
        private final String vuelo;
        /** Minuto absoluto desde el inicio de la simulación (llegada o salida según lista). */
        private final int horaAbs;
        private final int cantidad;
        private final String destinoFinal;

        public ItemPlanificado(String envioId, String vuelo, int horaAbs,
                                int cantidad, String destinoFinal) {
            this.envioId = envioId;
            this.vuelo = vuelo;
            this.horaAbs = horaAbs;
            this.cantidad = cantidad;
            this.destinoFinal = destinoFinal;
        }

        public String getEnvioId()      { return envioId; }
        public String getVuelo()        { return vuelo; }
        public int    getHoraAbs()      { return horaAbs; }
        public int    getCantidad()     { return cantidad; }
        public String getDestinoFinal() { return destinoFinal; }
    }

    private final List<ItemPlanificado> entrantes;
    private final List<ItemPlanificado> salientes;
    private final int totalMaletasEntrantes;
    private final int totalMaletasSalientes;

    public PlanificadosAeropuertoDTO(List<ItemPlanificado> entrantes,
                                      List<ItemPlanificado> salientes,
                                      int totalMaletasEntrantes,
                                      int totalMaletasSalientes) {
        this.entrantes = entrantes;
        this.salientes = salientes;
        this.totalMaletasEntrantes = totalMaletasEntrantes;
        this.totalMaletasSalientes = totalMaletasSalientes;
    }

    public List<ItemPlanificado> getEntrantes() { return entrantes; }
    public List<ItemPlanificado> getSalientes() { return salientes; }
    public int getTotalMaletasEntrantes()       { return totalMaletasEntrantes; }
    public int getTotalMaletasSalientes()       { return totalMaletasSalientes; }
}
