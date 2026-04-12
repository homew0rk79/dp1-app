package algorithm;

import model.Envio;
import model.Ruta;

import java.util.*;

/**
 * Estado completo del sistema: para cada envío, qué ruta tiene asignada.
 *
 * Esta clase es el objeto central que el algoritmo Tabu Search modifica
 * en cada iteración. Para explorar vecinos sin destruir la solución actual,
 * se usa clonar() antes de aplicar cada movimiento.
 *
 * Función objetivo: minimizar la suma total de tiempos de entrega.
 * Los envíos sin ruta viable reciben una penalización alta para que
 * el algoritmo los trate como la peor situación posible.
 */
public class Solucion {

    private static final int PENALIZACION_SIN_RUTA = 100_000; // minutos equivalentes

    // Asignación principal: id del envío → ruta asignada
    private final Map<String, Ruta> asignaciones;

    // Costo cacheado; se invalida con marcarDirty()
    private double costoTotal;
    private boolean dirty; // true = hay que recalcular el costo

    public Solucion() {
        this.asignaciones = new LinkedHashMap<>();
        this.costoTotal   = Double.MAX_VALUE;
        this.dirty        = true;
    }

    /** Constructor de copia (usado en clonar()) */
    private Solucion(Map<String, Ruta> asignaciones) {
        this.asignaciones = new LinkedHashMap<>(asignaciones);
        this.dirty        = true;
    }

    // -------------------------------------------------------------------------
    // Gestión de rutas
    // -------------------------------------------------------------------------

    public void agregarRuta(Ruta ruta) {
        asignaciones.put(claveGlobal(ruta.getEnvio()), ruta);
        dirty = true;
    }

    public Ruta getRuta(Envio envio) {
        return asignaciones.get(claveGlobal(envio));
    }

    /**
     * Los IDs de envío son únicos dentro de cada archivo de origen,
     * pero se repiten entre aeropuertos (todos empiezan en 000000001).
     * La clave global combina origen + id para garantizar unicidad.
     */
    private static String claveGlobal(Envio envio) {
        return envio.getOrigen() + "-" + envio.getId();
    }

    public Collection<Ruta> getRutas() {
        return asignaciones.values();
    }

    public int getTotalEnvios() {
        return asignaciones.size();
    }

    // -------------------------------------------------------------------------
    // Función objetivo
    // -------------------------------------------------------------------------

    /**
     * Calcula (o devuelve el cacheado) el costo total de la solución.
     *
     * Costo = suma de tiempos de entrega de envíos con ruta
     *       + PENALIZACION_SIN_RUTA × cantidad de envíos sin ruta viable.
     *
     * Un costo menor es mejor.
     */
    public double getCostoTotal() {
        if (dirty) recalcularCosto();
        return costoTotal;
    }

    private void recalcularCosto() {
        long suma = 0;
        int sinRuta = 0;

        for (Ruta ruta : asignaciones.values()) {
            if (ruta.isSinSolucion()) {
                sinRuta++;
            } else {
                int t = ruta.calcularTiempoTotal();
                suma += (t == Integer.MAX_VALUE) ? PENALIZACION_SIN_RUTA : t;
            }
        }

        costoTotal = suma + (long) sinRuta * PENALIZACION_SIN_RUTA;
        dirty = false;
    }

    public void marcarDirty() {
        dirty = true;
    }

    // -------------------------------------------------------------------------
    // Estadísticas
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Clon
    // -------------------------------------------------------------------------

    /**
     * Crea una copia profunda de la solución.
     * Cada Ruta se clona (constructor de copia) para que las modificaciones
     * en el vecino no afecten a la solución original.
     */
    public Solucion clonar() {
        Map<String, Ruta> copiaAsignaciones = new LinkedHashMap<>();
        for (Map.Entry<String, Ruta> entrada : asignaciones.entrySet()) {
            copiaAsignaciones.put(entrada.getKey(), new Ruta(entrada.getValue()));
        }
        return new Solucion(copiaAsignaciones);
    }

    // -------------------------------------------------------------------------
    // Resumen
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        return String.format(
            "Solucion { envios=%d, conRuta=%d, sinRuta=%d, costoTotal=%.0f, promedio=%.1f min }",
            getTotalEnvios(),
            contarEnviosConRuta(),
            contarEnviosSinRuta(),
            getCostoTotal(),
            getTiempoPromedioEntrega()
        );
    }

    public void imprimirResumen() {
        System.out.println("=== RESUMEN DE SOLUCIÓN ===");
        System.out.printf("  Envíos totales    : %d%n", getTotalEnvios());
        System.out.printf("  Con ruta          : %d%n", contarEnviosConRuta());
        System.out.printf("  Sin ruta viable   : %d%n", contarEnviosSinRuta());
        System.out.printf("  Costo total       : %.0f min%n", getCostoTotal());
        System.out.printf("  Tiempo promedio   : %.1f min%n", getTiempoPromedioEntrega());
    }
}
