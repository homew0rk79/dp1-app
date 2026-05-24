package algorithm;

import config.ACOConfig;
import config.Shipment;
import model.Flight;
import model.Network;
import model.Route;

import java.util.*;

/**
 * Planificador principal basado en Ant Colony System (ACS).
 * Orquesta la colonia de hormigas, gestiona las iteraciones y aplica
 * la actualización global-best de feromonas.
 * <p>
 * Interfaz pública:
 * <ul>
 *   <li>{@link #plan(Network, List, ACOConfig)} — planificación desde cero</li>
 *   <li>{@link #replan(Network, List, String, ACOConfig)} — replanificación ante cancelación</li>
 * </ul>
 */
public class ACSPlanner {

    private PheromoneMatrix pheromones;
    private final Random random;

    /**
     * Crea un planificador ACS.
     */
    public ACSPlanner() {
        this.random = new Random(42); // Seed fija para reproducibilidad
    }

    /**
     * Crea un planificador ACS con semilla específica.
     *
     * @param seed semilla para el generador aleatorio
     */
    public ACSPlanner(long seed) {
        this.random = new Random(seed);
    }

    /**
     * Planifica rutas para un conjunto de envíos usando el algoritmo ACS.
     *
     * @param network   red de transporte
     * @param shipments lista de envíos a planificar
     * @param config    configuración del algoritmo
     * @return resultado con las mejores rutas encontradas
     * @throws IllegalArgumentException si la red o los envíos son inválidos
     */
    public PlannerResult plan(Network network, List<Shipment> shipments, ACOConfig config) {
        validateInputs(network, shipments);

        long startTime = System.currentTimeMillis();
        pheromones = new PheromoneMatrix(network, config.getTauMin(), config.getTauMax());

        // Crear colonia de hormigas
        List<AntAgent> ants = new ArrayList<>();
        for (int i = 0; i < config.getNumAnts(); i++) {
            ants.add(new AntAgent(i, random));
        }

        // Mejores rutas globales por envío
        Map<String, Route> globalBestRoutes = new HashMap<>();
        double globalBestObjective = Double.MAX_VALUE;

        System.out.println("=== Iniciando planificación ACS ===");
        System.out.println("Configuración: " + config);
        System.out.println("Red: " + network);
        System.out.println("Envíos: " + shipments.size());
        System.out.println();

        for (int iter = 1; iter <= config.getMaxIterations(); iter++) {
            // Mejor solución de esta iteración
            Map<String, Route> iterBestRoutes = new HashMap<>();
            double iterBestObjective = Double.MAX_VALUE;

            // Cada hormiga construye rutas para todos los envíos
            for (AntAgent ant : ants) {
                Map<String, Route> antRoutes = new HashMap<>();
                double antObjective = 0.0;
                boolean allFound = true;

                // Reiniciar capacidades para cada hormiga
                network.resetAllFlightCapacities();

                for (Shipment shipment : shipments) {
                    Route route = ant.buildRoute(shipment, network, pheromones, config);
                    if (route != null) {
                        antRoutes.put(shipment.getId(), route);
                        antObjective += route.getObjectiveValue();
                    } else {
                        allFound = false;
                    }
                }

                // Actualizar mejor de la iteración
                if (allFound && antObjective < iterBestObjective) {
                    iterBestObjective = antObjective;
                    iterBestRoutes = new HashMap<>(antRoutes);
                }
            }

            // Actualizar mejor global
            if (iterBestObjective < globalBestObjective && !iterBestRoutes.isEmpty()) {
                globalBestObjective = iterBestObjective;
                globalBestRoutes = new HashMap<>(iterBestRoutes);

                // Log de mejora
                if (iter <= 10 || iter % 25 == 0 || iter == config.getMaxIterations()) {
                    logIteration(iter, globalBestObjective, globalBestRoutes);
                }
            } else if (iter <= 5 || iter % 50 == 0) {
                logIteration(iter, globalBestObjective, globalBestRoutes);
            }

            // Actualización global-best de feromonas
            if (!globalBestRoutes.isEmpty()) {
                pheromones.updateGlobalBest(
                        new ArrayList<>(globalBestRoutes.values()),
                        config.getRho(),
                        config.getQ()
                );
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;

        // Si algún envío no tiene ruta, intentar rellenar con la última ruta encontrada por hormiga
        for (Shipment s : shipments) {
            if (!globalBestRoutes.containsKey(s.getId())) {
                System.out.println("ADVERTENCIA: No se encontró ruta para envío " + s.getId());
            }
        }

        return new PlannerResult(globalBestRoutes, globalBestObjective,
                config.getMaxIterations(), elapsed);
    }

    /**
     * Replanifica rutas afectadas por la cancelación de un vuelo.
     * Reduce la feromona del vuelo cancelado a τ_min y re-ejecuta el algoritmo
     * solo para los envíos afectados, sin reiniciar las feromonas existentes.
     *
     * @param network            red de transporte
     * @param affectedShipments  envíos afectados por la cancelación
     * @param cancelledFlightId  id del vuelo cancelado
     * @param config             configuración del algoritmo
     * @return resultado con las nuevas rutas para los envíos afectados
     */
    public PlannerResult replan(Network network, List<Shipment> affectedShipments,
                                String cancelledFlightId, ACOConfig config) {
        System.out.println("\n=== REPLANIFICACIÓN ===");
        System.out.println("Vuelo cancelado: " + cancelledFlightId);
        System.out.println("Envíos afectados: " + affectedShipments.size());

        // Marcar vuelo como cancelado
        Flight cancelled = network.getFlight(cancelledFlightId);
        if (cancelled != null) {
            cancelled.cancel();
            System.out.println("Vuelo cancelado: " + cancelled);
        } else {
            System.out.println("ADVERTENCIA: Vuelo " + cancelledFlightId + " no encontrado en la red");
        }

        // Penalizar feromona del vuelo cancelado
        if (pheromones != null) {
            pheromones.penalize(cancelledFlightId);
        } else {
            // Si no hay feromona previa, crear nueva
            pheromones = new PheromoneMatrix(network, config.getTauMin(), config.getTauMax());
            pheromones.penalize(cancelledFlightId);
        }

        // Re-ejecutar planificación solo para envíos afectados
        // Usar las feromonas existentes (no reiniciar)
        long startTime = System.currentTimeMillis();

        List<AntAgent> ants = new ArrayList<>();
        for (int i = 0; i < config.getNumAnts(); i++) {
            ants.add(new AntAgent(i, random));
        }

        Map<String, Route> bestRoutes = new HashMap<>();
        double bestObjective = Double.MAX_VALUE;

        for (int iter = 1; iter <= config.getMaxIterations(); iter++) {
            Map<String, Route> iterBestRoutes = new HashMap<>();
            double iterBestObjective = Double.MAX_VALUE;

            for (AntAgent ant : ants) {
                Map<String, Route> antRoutes = new HashMap<>();
                double antObjective = 0.0;
                boolean allFound = true;

                network.resetAllFlightCapacities();

                for (Shipment shipment : affectedShipments) {
                    Route route = ant.buildRoute(shipment, network, pheromones, config);
                    if (route != null) {
                        antRoutes.put(shipment.getId(), route);
                        antObjective += route.getObjectiveValue();
                    } else {
                        allFound = false;
                    }
                }

                if (allFound && antObjective < iterBestObjective) {
                    iterBestObjective = antObjective;
                    iterBestRoutes = new HashMap<>(antRoutes);
                }
            }

            if (iterBestObjective < bestObjective && !iterBestRoutes.isEmpty()) {
                bestObjective = iterBestObjective;
                bestRoutes = new HashMap<>(iterBestRoutes);
            }

            if (!bestRoutes.isEmpty()) {
                pheromones.updateGlobalBest(
                        new ArrayList<>(bestRoutes.values()),
                        config.getRho(), config.getQ()
                );
            }

            if (iter <= 5 || iter % 50 == 0 || iter == config.getMaxIterations()) {
                logIteration(iter, bestObjective, bestRoutes);
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        return new PlannerResult(bestRoutes, bestObjective, config.getMaxIterations(), elapsed);
    }

    /**
     * Valida los datos de entrada antes de planificar.
     */
    private void validateInputs(Network network, List<Shipment> shipments) {
        if (network == null || network.getAirportCount() == 0) {
            throw new IllegalArgumentException("La red de transporte está vacía o es nula");
        }
        if (shipments == null || shipments.isEmpty()) {
            throw new IllegalArgumentException("La lista de envíos está vacía o es nula");
        }
        for (Shipment s : shipments) {
            if (network.getAirport(s.getOriginId()) == null) {
                throw new IllegalArgumentException(
                        "Aeropuerto de origen no encontrado para envío " + s.getId() + ": " + s.getOriginId());
            }
            if (network.getAirport(s.getDestinationId()) == null) {
                throw new IllegalArgumentException(
                        "Aeropuerto de destino no encontrado para envío " + s.getId() + ": " + s.getDestinationId());
            }
        }
    }

    /**
     * Registra por consola el estado de una iteración.
     */
    private void logIteration(int iter, double bestObjective, Map<String, Route> bestRoutes) {
        System.out.printf("[Iter %4d] Mejor f(S*)=%.6f", iter, bestObjective);
        if (!bestRoutes.isEmpty()) {
            System.out.println();
            for (Route route : bestRoutes.values()) {
                System.out.println("           " + route);
            }
        } else {
            System.out.println(" (sin solución factible)");
        }
    }
}
