package algorithm;

import model.Envio;
import model.Ruta;
import model.Vuelo;

import java.util.*;

/**
 * Estado completo del sistema: para cada envío, qué ruta tiene asignada.
 *
 * Función objetivo (minimizar):
 *   costoAcumulado   = Σ costoDe(ruta)  → tiempo de entrega + penalización por plazo
 *   penaltyCapacidad = penalización por exceso en capacidad de vuelos y aeropuertos
 *   getCostoTotal()  = costoAcumulado + penaltyCapacidad
 *
 * Restricciones modeladas como penalizaciones:
 *   PLAZO      : si tiempoEntrega > plazoMaximoMinutos del envío → penalización proporcional
 *   CAP. VUELO : si maletas asignadas a un vuelo > su capacidad → penalización proporcional
 *   CAP. AEROP.: si maletas en almacén de un aeropuerto en un día > su capacidad → penalización
 *                (se usa resolución diaria: se cuentan maletas presentes al menos parte del día)
 */
public class Solucion {

    // ── Penalizaciones ────────────────────────────────────────────────────────
    private static final double PENALIZACION_SIN_RUTA      = 100_000.0;
    private static final double PENALIZACION_PLAZO         =   1_000.0; // por minuto sobre el plazo
    private static final double PENALIZACION_VUELO         =   1_000.0; // por maleta sobre capacidad
    private static final double PENALIZACION_AEROPUERTO    =     500.0; // por maleta sobre capacidad (por día)

    // ── Asignaciones ─────────────────────────────────────────────────────────
    private final Map<String, Ruta> asignaciones;

    // ── Costos ───────────────────────────────────────────────────────────────
    // costoAcumulado: Σ costoDe(ruta) — tiempo + penalización por plazo
    private double costoAcumulado;

    // penaltyCapacidad: contribución global de vuelos y aeropuertos saturados
    private double penaltyCapacidad;

    // ── Tracking de capacidad de vuelos ──────────────────────────────────────
    // flightKey → maletas actualmente asignadas a ese vuelo
    private final Map<String, Integer> ocupacionVuelos;
    // flightKey → capacidad máxima del vuelo (poblado al primer uso)
    private final Map<String, Integer> capacidadMaxVuelos;

    // ── Tracking de capacidad de aeropuertos (resolución diaria) ─────────────
    // aeropuerto → (día → maletas presentes ese día)
    private final Map<String, Map<Integer, Integer>> ocupacionDiariaAeropuerto;
    // aeropuerto → capacidad máxima (pasado desde Main via constructor)
    private final Map<String, Integer> capacidadMaxAeropuertos;

    // ── Constructores ─────────────────────────────────────────────────────────

    public Solucion(Map<String, Integer> capacidadMaxAeropuertos) {
        this.asignaciones               = new LinkedHashMap<>();
        this.costoAcumulado             = 0.0;
        this.penaltyCapacidad           = 0.0;
        this.ocupacionVuelos            = new HashMap<>();
        this.capacidadMaxVuelos         = new HashMap<>();
        this.ocupacionDiariaAeropuerto  = new HashMap<>();
        this.capacidadMaxAeropuertos    = capacidadMaxAeropuertos;
    }

    public Solucion() {
        this(Collections.emptyMap());
    }

    /** Constructor privado para clonar() */
    private Solucion(Map<String, Ruta> asignaciones,
                     double costoAcumulado,
                     double penaltyCapacidad,
                     Map<String, Integer> ocupacionVuelos,
                     Map<String, Integer> capacidadMaxVuelos,
                     Map<String, Map<Integer, Integer>> ocupacionDiariaAeropuerto,
                     Map<String, Integer> capacidadMaxAeropuertos) {
        this.asignaciones              = asignaciones;
        this.costoAcumulado            = costoAcumulado;
        this.penaltyCapacidad          = penaltyCapacidad;
        this.ocupacionVuelos           = ocupacionVuelos;
        this.capacidadMaxVuelos        = capacidadMaxVuelos;
        this.ocupacionDiariaAeropuerto = ocupacionDiariaAeropuerto;
        this.capacidadMaxAeropuertos   = capacidadMaxAeropuertos; // referencia compartida (solo lectura)
    }

    // ── Costo de una ruta individual ──────────────────────────────────────────

    private double costoDe(Ruta r) {
        if (r.isSinSolucion()) return PENALIZACION_SIN_RUTA;
        int t = r.calcularTiempoTotal();
        if (t == Integer.MAX_VALUE) return PENALIZACION_SIN_RUTA;

        double costo = t;
        int plazo = r.getEnvio().getPlazoMaximoMinutos();
        if (plazo > 0 && t > plazo) {
            costo += PENALIZACION_PLAZO * (t - plazo);
        }
        return costo;
    }

    // ── Gestión de rutas ──────────────────────────────────────────────────────

    public void agregarRuta(Ruta nueva) {
        String clave = claveGlobal(nueva.getEnvio());
        Ruta vieja = asignaciones.put(clave, nueva);

        if (vieja != null) {
            costoAcumulado -= costoDe(vieja);
            actualizarCapacidad(vieja, -1);
        }
        costoAcumulado += costoDe(nueva);
        actualizarCapacidad(nueva, +1);
    }

    public Ruta getRuta(Envio envio) {
        return asignaciones.get(claveGlobal(envio));
    }

    private static String claveGlobal(Envio envio) {
        return envio.getOrigen() + "-" + envio.getId();
    }

    public Collection<Ruta> getRutas() {
        return asignaciones.values();
    }

    public int getTotalEnvios() {
        return asignaciones.size();
    }

    // ── Actualización incremental de capacidades ──────────────────────────────

    /**
     * Suma o resta (signo = +1 / -1) las contribuciones de capacidad de una ruta.
     * Se llama al agregar/quitar una ruta de la solución.
     */
    private void actualizarCapacidad(Ruta ruta, int signo) {
        if (ruta.isSinSolucion() || ruta.getVuelos().isEmpty()) return;

        int cantidad = ruta.getEnvio().getCantidad();

        // Capacidad de vuelos — clave incluye el día real para no acumular entre días
        int tiempoActual = ruta.getEnvio().getMinutosRegistro();
        for (Vuelo v : ruta.getVuelos()) {
            int salidaAbsoluta = GrafoVuelos.proximaSalidaAbsoluta(
                tiempoActual, v.getSalidaMinutos(), 30);
            int dia = salidaAbsoluta / 1440;
            String key = v.getClave() + "-d" + dia;

            capacidadMaxVuelos.put(key, v.getCapacidadMax());

            int ocupAntes   = ocupacionVuelos.getOrDefault(key, 0);
            int ocupDespues = ocupAntes + signo * cantidad;

            int excesAntes   = Math.max(0, ocupAntes   - v.getCapacidadMax());
            int excesDespues = Math.max(0, ocupDespues - v.getCapacidadMax());
            penaltyCapacidad += (double)(excesDespues - excesAntes) * PENALIZACION_VUELO;

            ocupacionVuelos.put(key, Math.max(0, ocupDespues));
            tiempoActual = salidaAbsoluta + v.getDuracionMinutos();
        }

        // Capacidad de aeropuertos (resolución diaria)
        Map<String, int[]> intervalos = ruta.calcularIntervalosAlmacen();
        for (Map.Entry<String, int[]> entry : intervalos.entrySet()) {
            String aeropuerto = entry.getKey();
            int t1 = entry.getValue()[0];
            int t2 = entry.getValue()[1];
            if (t2 <= t1) continue;

            int capMax = capacidadMaxAeropuertos.getOrDefault(aeropuerto, Integer.MAX_VALUE);

            int dayStart = t1 / 1440;
            int dayEnd   = (t2 - 1) / 1440;

            Map<Integer, Integer> diasAeropuerto =
                ocupacionDiariaAeropuerto.computeIfAbsent(aeropuerto, k -> new HashMap<>());

            for (int d = dayStart; d <= dayEnd; d++) {
                int ocupAntes   = diasAeropuerto.getOrDefault(d, 0);
                int ocupDespues = ocupAntes + signo * cantidad;

                if (capMax != Integer.MAX_VALUE) {
                    int excesAntes   = Math.max(0, ocupAntes   - capMax);
                    int excesDespues = Math.max(0, ocupDespues - capMax);
                    penaltyCapacidad += (double)(excesDespues - excesAntes) * PENALIZACION_AEROPUERTO;
                }

                diasAeropuerto.put(d, Math.max(0, ocupDespues));
            }
        }
    }

    // ── Función objetivo ──────────────────────────────────────────────────────

    /** Costo total = tiempos de entrega + penalizaciones por plazo + penalizaciones por capacidad */
    public double getCostoTotal() {
        return costoAcumulado + penaltyCapacidad;
    }

    /** No-op: el costo se mantiene incrementalmente. Existe para compatibilidad con TabuSearch. */
    public void marcarDirty() { }

    // ── Estadísticas ──────────────────────────────────────────────────────────

    public int contarEnviosSinRuta() {
        return (int) asignaciones.values().stream().filter(Ruta::isSinSolucion).count();
    }

    public int contarEnviosConRuta() {
        return getTotalEnvios() - contarEnviosSinRuta();
    }

    public double getTiempoPromedioEntrega() {
        return asignaciones.values().stream()
            .filter(r -> !r.isSinSolucion())
            .mapToInt(Ruta::calcularTiempoTotal)
            .filter(t -> t != Integer.MAX_VALUE)
            .average()
            .orElse(0.0);
    }

    public int contarViolacionesPlazo() {
        return (int) asignaciones.values().stream()
            .filter(r -> !r.isSinSolucion())
            .filter(r -> {
                int t = r.calcularTiempoTotal();
                int p = r.getEnvio().getPlazoMaximoMinutos();
                return p > 0 && t != Integer.MAX_VALUE && t > p;
            })
            .count();
    }

    public int contarVuelosSaturados() {
        return (int) ocupacionVuelos.entrySet().stream()
            .filter(e -> {
                Integer cap = capacidadMaxVuelos.get(e.getKey());
                return cap != null && e.getValue() > cap;
            })
            .count();
    }

    public double getPenaltyCapacidad() {
        return penaltyCapacidad;
    }

    /** Numero de aeropuertos distintos con al menos un dia-aeropuerto sobre capacidad */
    public int contarAeropuertosSaturados() {
        return (int) ocupacionDiariaAeropuerto.entrySet().stream()
            .filter(e -> {
                int cap = capacidadMaxAeropuertos.getOrDefault(e.getKey(), Integer.MAX_VALUE);
                if (cap == Integer.MAX_VALUE) return false;
                return e.getValue().values().stream().anyMatch(ocup -> ocup > cap);
            })
            .count();
    }

    /** Total de pares (aeropuerto, dia) donde la ocupacion supera la capacidad */
    public int contarDiasAeropuertoSaturados() {
        return ocupacionDiariaAeropuerto.entrySet().stream()
            .mapToInt(e -> {
                int cap = capacidadMaxAeropuertos.getOrDefault(e.getKey(), Integer.MAX_VALUE);
                if (cap == Integer.MAX_VALUE) return 0;
                return (int) e.getValue().values().stream().filter(ocup -> ocup > cap).count();
            })
            .sum();
    }

    /** Exceso total de maletas sobre capacidad en todos los aeropuertos y dias */
    public int calcularExcesoTotalAeropuertos() {
        return ocupacionDiariaAeropuerto.entrySet().stream()
            .mapToInt(e -> {
                int cap = capacidadMaxAeropuertos.getOrDefault(e.getKey(), Integer.MAX_VALUE);
                if (cap == Integer.MAX_VALUE) return 0;
                return e.getValue().values().stream().mapToInt(ocup -> Math.max(0, ocup - cap)).sum();
            })
            .sum();
    }

    public double getPorcentajeCumplimientoPlazo() {
        int conRuta = contarEnviosConRuta();
        if (conRuta == 0) return 0.0;
        return 100.0 * (conRuta - contarViolacionesPlazo()) / conRuta;
    }

    public double getEscalasPromedio() {
        return asignaciones.values().stream()
            .filter(r -> !r.isSinSolucion())
            .mapToInt(Ruta::getNumEscalas)
            .average()
            .orElse(0.0);
    }

    // ── Clon ──────────────────────────────────────────────────────────────────

    /**
     * Copia profunda de los mapas de ocupación y referencias compartidas
     * para los datos de solo lectura (capacidades máximas).
     */
    public Solucion clonar() {
        // Deep copy de ocupacionDiariaAeropuerto
        Map<String, Map<Integer, Integer>> cloneOcupDiaria = new HashMap<>();
        for (Map.Entry<String, Map<Integer, Integer>> e : ocupacionDiariaAeropuerto.entrySet()) {
            cloneOcupDiaria.put(e.getKey(), new HashMap<>(e.getValue()));
        }

        return new Solucion(
            new LinkedHashMap<>(asignaciones),
            costoAcumulado,
            penaltyCapacidad,
            new HashMap<>(ocupacionVuelos),
            new HashMap<>(capacidadMaxVuelos),
            cloneOcupDiaria,
            capacidadMaxAeropuertos   // compartida: es solo lectura
        );
    }

    // ── Resumen ───────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return String.format(
            "Solucion { envios=%d, conRuta=%d, sinRuta=%d, costoTotal=%.0f, promedio=%.1f min }",
            getTotalEnvios(), contarEnviosConRuta(), contarEnviosSinRuta(),
            getCostoTotal(), getTiempoPromedioEntrega()
        );
    }

    public void imprimirResumen() {
        int total     = getTotalEnvios();
        int conRuta   = contarEnviosConRuta();
        int sinRuta   = contarEnviosSinRuta();
        int cumplen   = conRuta - contarViolacionesPlazo();
        double pctRuta = total > 0 ? 100.0 * conRuta / total : 0;
        double pctPlazo = conRuta > 0 ? 100.0 * cumplen / conRuta : 0;

        System.out.println("  Envios planificados          : " + String.format("%,d", total));
        System.out.printf( "  Con ruta asignada            : %,d  (%.1f%% del total)%n", conRuta, pctRuta);
        System.out.printf( "  Sin ruta viable              : %,d  (%.1f%% del total)%n", sinRuta, 100 - pctRuta);
        System.out.printf( "  Cumplen el plazo de entrega  : %,d  (%.1f%% de los enrutados)%n", cumplen, pctPlazo);
        System.out.printf( "  Violan el plazo              : %,d%n", contarViolacionesPlazo());
        System.out.printf( "  Vuelos con sobrecarga        : %,d%n", contarVuelosSaturados());
        System.out.printf( "  Aeropuertos saturados        : %,d  (%,d dias-aeropuerto, %,d maletas en exceso)%n",
            contarAeropuertosSaturados(), contarDiasAeropuertoSaturados(), calcularExcesoTotalAeropuertos());
        System.out.printf( "  Escalas promedio por envio   : %.2f%n", getEscalasPromedio());
        System.out.printf( "  Tiempo promedio de entrega   : %.1f min%n", getTiempoPromedioEntrega());
        System.out.printf( "  Costo base (tiempo)          : %,.0f%n", costoAcumulado);
        System.out.printf( "  Penalizacion por capacidad   : %,.0f%n", penaltyCapacidad);
        System.out.printf( "  Funcion objetivo total       : %,.0f%n", getCostoTotal());
    }
}
