package model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    // Calcula el tiempo total de entrega en minutos usando tiempos absolutos.
    // tiempoActual arranca en el minuto absoluto de registro y avanza con cada vuelo.
    public int calcularTiempoTotal() {
        if (sinSolucion || vuelos.isEmpty()) return Integer.MAX_VALUE;

        int tiempoActual = envio.getMinutosRegistro(); // absoluto desde 2026-01-01
        int tiempoInicio = tiempoActual;

        for (Vuelo vuelo : vuelos) {
            int salidaAbsoluta = proximaSalidaAbsolutaLocal(
                tiempoActual, vuelo.getSalidaMinutos(), 30);
            tiempoActual = salidaAbsoluta + vuelo.getDuracionMinutos();
        }
        return tiempoActual - tiempoInicio;
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

    /**
     * Devuelve los intervalos de tiempo [entrada, salida] en minutos absolutos
     * para cada aeropuerto donde las maletas ocupan almacén:
     *   - Aeropuerto origen: desde hora de registro hasta salida del primer vuelo.
     *   - Aeropuertos de tránsito: desde llegada del vuelo anterior hasta salida del siguiente.
     *   - Aeropuerto destino: NO incluido (el cliente recoge las maletas).
     *
     * Clave del mapa: código ICAO del aeropuerto.
     * Valor: int[]{tiempoEntrada, tiempoSalida} en minutos absolutos desde día 0.
     */
    public Map<String, int[]> calcularIntervalosAlmacen() {
        Map<String, int[]> intervalos = new LinkedHashMap<>();
        if (sinSolucion || vuelos.isEmpty()) return intervalos;

        int tiempoActual = envio.getMinutosRegistro();

        for (int i = 0; i < vuelos.size(); i++) {
            Vuelo v = vuelos.get(i);
            String aeropuerto = (i == 0) ? envio.getOrigen() : vuelos.get(i - 1).getDestino();
            int entrada = tiempoActual;
            int salidaAbsoluta = proximaSalidaAbsolutaLocal(tiempoActual, v.getSalidaMinutos(), 30);
            intervalos.put(aeropuerto, new int[]{entrada, salidaAbsoluta});
            tiempoActual = salidaAbsoluta + v.getDuracionMinutos();
        }
        return intervalos;
    }

    private static int proximaSalidaAbsolutaLocal(int tiempoActual, int salidaEnDia, int margen) {
        int dia = tiempoActual / 1440;
        int minDentroDelDia = tiempoActual % 1440;
        if (minDentroDelDia + margen <= salidaEnDia) {
            return dia * 1440 + salidaEnDia;
        }
        return (dia + 1) * 1440 + salidaEnDia;
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
