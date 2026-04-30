package algorithm;

import model.*;

import java.util.*;

public class ACSPlanner {

    private PheromoneMatrix pheromones;
    private final Random random;

    public ACSPlanner() {
        this.random = new Random(42);
    }

    public ACSPlanner(long seed) {
        this.random = new Random(seed);
    }

    public PlannerResult plan(Network network, List<Shipment> shipments, ACOConfig config) {
        validateInputs(network, shipments);

        long startTime = System.currentTimeMillis();
        pheromones = new PheromoneMatrix(network, config.getTauMin(), config.getTauMax());

        Map<String, Route> plannedRoutes = new LinkedHashMap<>();
        double totalObjective = 0.0;

        List<AntAgent> ants = createAnts(config);

        System.out.println("=== Iniciando planificación ACS ===");
        System.out.println("Configuración: " + config);
        System.out.println("Red: " + network);
        System.out.println("Envíos: " + shipments.size());

        for (Shipment shipment : shipments) {
            Route bestRoute = findBestRouteForShipment(
                    shipment,
                    network,
                    ants,
                    config
            );

            if (bestRoute != null && bestRoute.meetsDeadline()) {
                reserveRoute(bestRoute);
                plannedRoutes.put(shipment.getId(), bestRoute);
                totalObjective += bestRoute.getObjectiveValue();
            } else {
                totalObjective += 1000.0;
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;

        System.out.printf(
                "Planificación final: %d / %d envíos planificados%n",
                plannedRoutes.size(),
                shipments.size()
        );

        return new PlannerResult(
                plannedRoutes,
                totalObjective,
                config.getMaxIterations(),
                elapsed
        );
    }

    public PlannerResult replan(Network network,
                                List<Shipment> affectedShipments,
                                String cancelledFlightId,
                                ACOConfig config) {

        validateInputs(network, affectedShipments);

        Flight cancelled = network.getFlight(cancelledFlightId);
        if (cancelled != null) {
            cancelled.cancel();
        }

        if (pheromones == null) {
            pheromones = new PheromoneMatrix(network, config.getTauMin(), config.getTauMax());
        }

        pheromones.penalize(cancelledFlightId);

        long startTime = System.currentTimeMillis();

        Map<String, Route> replannedRoutes = new LinkedHashMap<>();
        double totalObjective = 0.0;

        List<AntAgent> ants = createAnts(config);

        for (Shipment shipment : affectedShipments) {
            Route bestRoute = findBestRouteForShipment(
                    shipment,
                    network,
                    ants,
                    config
            );

            if (bestRoute != null && bestRoute.meetsDeadline()) {
                reserveRoute(bestRoute);
                replannedRoutes.put(shipment.getId(), bestRoute);
                totalObjective += bestRoute.getObjectiveValue();
            } else {
                totalObjective += 1000.0;
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;

        return new PlannerResult(
                replannedRoutes,
                totalObjective,
                config.getMaxIterations(),
                elapsed
        );
    }

    private Route findBestRouteForShipment(Shipment shipment,
                                           Network network,
                                           List<AntAgent> ants,
                                           ACOConfig config) {

        Route bestRoute = null;
        double bestObjective = Double.MAX_VALUE;

        for (int iter = 1; iter <= config.getMaxIterations(); iter++) {
            for (AntAgent ant : ants) {
                Route candidate = ant.buildRoute(
                        shipment,
                        network,
                        pheromones,
                        config
                );

                if (candidate == null) continue;
                if (!candidate.meetsDeadline()) continue;

                if (candidate.getObjectiveValue() < bestObjective) {
                    bestObjective = candidate.getObjectiveValue();
                    bestRoute = candidate;
                }
            }

            if (bestRoute != null) {
                pheromones.updateGlobalBest(
                        List.of(bestRoute),
                        config.getRho(),
                        config.getQ()
                );
            }
        }

        return bestRoute;
    }

    private List<AntAgent> createAnts(ACOConfig config) {
        List<AntAgent> ants = new ArrayList<>();
        for (int i = 0; i < config.getNumAnts(); i++) {
            ants.add(new AntAgent(i, random));
        }
        return ants;
    }

    private void reserveRoute(Route route) {
        int bags = route.getShipment().getBagCount();

        for (Flight flight : route.getFlights()) {
            flight.reserveCapacity(bags);
        }
    }

    private void validateInputs(Network network, List<Shipment> shipments) {
        if (network == null || network.getAirportCount() == 0) {
            throw new IllegalArgumentException("La red de transporte está vacía o es nula");
        }

        if (shipments == null || shipments.isEmpty()) {
            throw new IllegalArgumentException("La lista de envíos está vacía o es nula");
        }

        for (Shipment s : shipments) {
            if (network.getAirport(s.getOriginId()) == null) {
                throw new IllegalArgumentException("Origen no encontrado: " + s.getOriginId());
            }

            if (network.getAirport(s.getDestinationId()) == null) {
                throw new IllegalArgumentException("Destino no encontrado: " + s.getDestinationId());
            }
        }
    }
}