package model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Resultado del planificador ACS.
 * Contiene las rutas asignadas a cada envío, métricas globales y
 * tiempo de ejecución del algoritmo.
 */
public class PlannerResult {

    private final Map<String, Route> routesByShipment;
    private final double bestObjectiveValue;
    private final int totalIterations;
    private final long executionTimeMs;

    /**
     * Crea un resultado del planificador.
     *
     * @param routesByShipment  mapa de shipmentId → mejor ruta encontrada
     * @param bestObjectiveValue mejor valor global de función objetivo
     * @param totalIterations    iteraciones ejecutadas
     * @param executionTimeMs    tiempo de ejecución en milisegundos
     */
    public PlannerResult(Map<String, Route> routesByShipment, double bestObjectiveValue,
                         int totalIterations, long executionTimeMs) {
        this.routesByShipment = routesByShipment;
        this.bestObjectiveValue = bestObjectiveValue;
        this.totalIterations = totalIterations;
        this.executionTimeMs = executionTimeMs;
    }

    /** @return mapa inmutable de rutas por envío */
    public Map<String, Route> getRoutesByShipment() {
        return Collections.unmodifiableMap(routesByShipment);
    }

    /** @return mejor valor de función objetivo encontrado */
    public double getBestObjectiveValue() { return bestObjectiveValue; }

    /** @return total de iteraciones ejecutadas */
    public int getTotalIterations() { return totalIterations; }

    /** @return tiempo de ejecución en milisegundos */
    public long getExecutionTimeMs() { return executionTimeMs; }

    /** @return lista de todas las rutas */
    public List<Route> getAllRoutes() {
        return List.copyOf(routesByShipment.values());
    }

    /** @return número de envíos que cumplen el plazo */
    public long getOnTimeCount() {
        return routesByShipment.values().stream().filter(Route::meetsDeadline).count();
    }

    /** @return número total de envíos planificados */
    public int getTotalShipments() { return routesByShipment.size(); }

    /**
     * Genera un resumen textual del resultado.
     *
     * @return resumen formateado
     */
    public String summary() {
        StringBuilder sb = new StringBuilder();
        sb.append("========== RESULTADO DEL PLANIFICADOR ACS ==========\n");
        sb.append(String.format("Iteraciones: %d\n", totalIterations));
        sb.append(String.format("Tiempo de ejecución: %d ms (%.2f s)\n",
                executionTimeMs, executionTimeMs / 1000.0));
        sb.append(String.format("Mejor f(S*) global: %.6f\n", bestObjectiveValue));
        sb.append(String.format("Envíos planificados: %d / %d a tiempo\n",
                getOnTimeCount(), getTotalShipments()));
        sb.append("----------------------------------------------------\n");
        for (Route route : routesByShipment.values()) {
            sb.append(route).append("\n"); //Comentar para ver el resumen del planificador ACS
        }
        sb.append("====================================================\n");
        return sb.toString();
    }

    @Override
    public String toString() {
        return summary();
    }
}
