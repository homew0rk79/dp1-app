package com.tasfb2b.dto;

import java.util.List;

/**
 * Monitor de envíos para el panel del visualizador:
 * planificados (aún no salen), en vuelo, y entregados en las últimas X horas.
 */
public class MonitorEnviosDTO {

    public static class ItemEnvio {
        private final String envioId;
        private final String origen;
        private final String destino;
        /** Vuelo (UT) relevante: primero para planificados, activo para en-vuelo, último para entregados. */
        private final String vuelo;
        /** Minuto absoluto relevante: salida para planificados, llegada para entregados. */
        private final int horaAbs;
        private final int cantidad;

        public ItemEnvio(String envioId, String origen, String destino,
                          String vuelo, int horaAbs, int cantidad) {
            this.envioId = envioId;
            this.origen = origen;
            this.destino = destino;
            this.vuelo = vuelo;
            this.horaAbs = horaAbs;
            this.cantidad = cantidad;
        }

        public String getEnvioId()  { return envioId; }
        public String getOrigen()   { return origen; }
        public String getDestino()  { return destino; }
        public String getVuelo()    { return vuelo; }
        public int    getHoraAbs()  { return horaAbs; }
        public int    getCantidad() { return cantidad; }
    }

    private final List<ItemEnvio> planificados;
    private final List<ItemEnvio> enVuelo;
    private final List<ItemEnvio> entregados;

    public MonitorEnviosDTO(List<ItemEnvio> planificados,
                             List<ItemEnvio> enVuelo,
                             List<ItemEnvio> entregados) {
        this.planificados = planificados;
        this.enVuelo = enVuelo;
        this.entregados = entregados;
    }

    public List<ItemEnvio> getPlanificados() { return planificados; }
    public List<ItemEnvio> getEnVuelo()      { return enVuelo; }
    public List<ItemEnvio> getEntregados()   { return entregados; }
}
