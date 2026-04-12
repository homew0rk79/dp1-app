package model;

import java.util.ArrayList;
import java.util.List;

public class Ruta {

    /*
     * Una Ruta vincula un Envio con la secuencia de Vuelos que lo llevará
     * desde su aeropuerto origen hasta el destino.
     *
     * Ejemplo para EBCI → SPIM con escala en SKBO:
     *   vuelos = [ EBCI→SKBO (salida 10:00, llegada 18:30),
     *              SKBO→SPIM (salida 22:45, llegada 01:01) ]
     *
     * tiempoTotal incluye tanto los vuelos como las esperas en escalas.
     */

    private final Envio envio;
    private final List<Vuelo> vuelos;

    // true si no se encontró ningún camino viable para este envío
    private boolean sinSolucion;

    public Ruta(Envio envio) {
        this.envio = envio;
        this.vuelos = new ArrayList<>();
        this.sinSolucion = false;
    }

    // Constructor de copia (necesario para clonar soluciones en Tabu Search)
    public Ruta(Ruta otra) {
        this.envio = otra.envio;
        this.vuelos = new ArrayList<>(otra.vuelos);
        this.sinSolucion = otra.sinSolucion;
    }

    public void agregarVuelo(Vuelo v) {
        vuelos.add(v);
    }

    // Calcula el tiempo total de entrega en minutos.
    // Suma: espera inicial + duración vuelo1 + espera escala + duración vuelo2 + ...
    public int calcularTiempoTotal() {
        if (sinSolucion || vuelos.isEmpty()) return Integer.MAX_VALUE;

        int tiempoActual = envio.getMinutosRegistro();
        int tiempoTotal  = 0;

        for (Vuelo vuelo : vuelos) {
            int espera = vuelo.tiempoEspera(tiempoActual);
            tiempoTotal  += espera + vuelo.getDuracionMinutos();
            tiempoActual  = vuelo.getLlegadaMinutos();
        }
        return tiempoTotal;
    }

    // Verifica que la secuencia de vuelos sea coherente:
    // - cada vuelo parte del destino del anterior
    // - el primer vuelo parte del origen del envío
    // - el último vuelo llega al destino del envío
    public boolean esValida() {
        if (sinSolucion) return false;
        if (vuelos.isEmpty()) return false;

        if (!vuelos.get(0).getOrigen().equals(envio.getOrigen())) return false;
        if (!vuelos.get(vuelos.size() - 1).getDestino().equals(envio.getDestino())) return false;

        for (int i = 0; i < vuelos.size() - 1; i++) {
            if (!vuelos.get(i).getDestino().equals(vuelos.get(i + 1).getOrigen())) return false;
        }
        return true;
    }

    // Número de escalas intermedias (0 = vuelo directo)
    public int getNumEscalas() {
        return Math.max(0, vuelos.size() - 1);
    }

    public Envio getEnvio()         { return envio; }
    public List<Vuelo> getVuelos()  { return vuelos; }
    public boolean isSinSolucion()  { return sinSolucion; }
    public void setSinSolucion(boolean sinSolucion) { this.sinSolucion = sinSolucion; }

    @Override
    public String toString() {
        if (sinSolucion) return "[" + envio.getId() + "] SIN RUTA VIABLE";

        StringBuilder sb = new StringBuilder();
        sb.append("[").append(envio.getId()).append("] ");
        for (int i = 0; i < vuelos.size(); i++) {
            if (i > 0) sb.append(" → escala → ");
            sb.append(vuelos.get(i));
        }
        sb.append(" | Tiempo total: ").append(calcularTiempoTotal()).append(" min");
        return sb.toString();
    }
}
