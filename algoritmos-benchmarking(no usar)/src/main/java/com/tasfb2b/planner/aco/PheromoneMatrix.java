package com.tasfb2b.planner.aco;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Matriz de feromonas τ(i,j) para el algoritmo ACS.
 * Gestiona la inicialización, consulta, evaporación y actualización global-best
 * de los niveles de feromona en cada arco (vuelo) de la red.
 */
public class PheromoneMatrix {

    /** Niveles de feromona indexados por id de vuelo */
    private final Map<String, Double> pheromones;
    private final double tauMin;
    private final double tauMax;
    private final double initialTau;

    /**
     * Crea la matriz de feromonas e inicializa todos los arcos.
     *
     * @param network red de transporte
     * @param tauMin  valor mínimo de feromona
     * @param tauMax  valor máximo de feromona
     */
    public PheromoneMatrix(Network network, double tauMin, double tauMax) {
        this.tauMin = tauMin;
        this.tauMax = tauMax;
        this.initialTau = (tauMin + tauMax) / 2.0;
        this.pheromones = new HashMap<>();
        for (Flight f : network.getFlights()) {
            pheromones.put(f.getId(), initialTau);
        }
    }

    /**
     * Obtiene el nivel de feromona de un vuelo.
     *
     * @param flightId id del vuelo
     * @return nivel de feromona τ(i,j)
     */
    public double get(String flightId) {
        return pheromones.getOrDefault(flightId, tauMin);
    }

    /**
     * Establece directamente el nivel de feromona de un vuelo (acotado a [τ_min, τ_max]).
     *
     * @param flightId id del vuelo
     * @param value    nuevo valor de feromona
     */
    public void set(String flightId, double value) {
        pheromones.put(flightId, clamp(value));
    }

    /**
     * Aplica la evaporación global de feromona a todos los arcos.
     * τ(i,j) ← (1 − ρ) · τ(i,j)
     *
     * @param rho tasa de evaporación ρ ∈ (0, 1)
     */
    public void evaporate(double rho) {
        for (Map.Entry<String, Double> entry : pheromones.entrySet()) {
            double newVal = (1.0 - rho) * entry.getValue();
            entry.setValue(clamp(newVal));
        }
    }

    /**
     * Aplica la actualización global-best de feromona del ACS.
     * Para cada arco en la mejor solución: τ(i,j) ← (1 − ρ)·τ(i,j) + ρ·Δτ
     * donde Δτ = Q / f(S*)
     *
     * @param bestRoutes mejores rutas encontradas (global-best)
     * @param rho        tasa de evaporación
     * @param q          constante Q de depósito
     */
    public void updateGlobalBest(List<Route> bestRoutes, double rho, double q) {
        // Primero evaporar todos los arcos
        evaporate(rho);

        // Luego depositar feromona solo en arcos de la mejor solución
        for (Route route : bestRoutes) {
            double objectiveValue = route.getObjectiveValue();
            if (objectiveValue <= 0) continue;

            double deltaTau = q / objectiveValue;
            for (Flight flight : route.getFlights()) {
                double current = pheromones.getOrDefault(flight.getId(), tauMin);
                double newVal = current + rho * deltaTau;
                pheromones.put(flight.getId(), clamp(newVal));
            }
        }
    }

    /**
     * Penaliza un vuelo cancelado reduciendo su feromona a τ_min.
     * Esto redirige las hormigas a rutas alternativas en la siguiente iteración.
     *
     * @param flightId id del vuelo cancelado
     */
    public void penalize(String flightId) {
        pheromones.put(flightId, tauMin);
    }

    /**
     * Reinicia todas las feromonas al valor inicial.
     */
    public void reset() {
        for (String key : pheromones.keySet()) {
            pheromones.put(key, initialTau);
        }
    }

    /**
     * Acota un valor al rango [τ_min, τ_max].
     */
    private double clamp(double value) {
        return Math.max(tauMin, Math.min(tauMax, value));
    }

    /** @return valor mínimo de feromona */
    public double getTauMin() { return tauMin; }

    /** @return valor máximo de feromona */
    public double getTauMax() { return tauMax; }

    @Override
    public String toString() {
        return "PheromoneMatrix{arcs=" + pheromones.size()
                + ", τ∈[" + tauMin + ", " + tauMax + "]}";
    }
}
