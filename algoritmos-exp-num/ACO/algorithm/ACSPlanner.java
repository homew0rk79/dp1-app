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

        Map<String, List<Shipment>> grupos = groupShipmentsByOD(shipments);

        for (List<Shipment> group : grupos.values()) {

            for (Shipment shipment : group) {
                Route bestRoute = findBestRouteForShipment(
                        shipment,
                        network,
                        ants,
                        config
                );

                if (bestRoute != null &&
                        bestRoute.meetsDeadline() &&
                        canReserveRoute(bestRoute, shipment.getBagCount(), network)) {

                    reserveRoute(bestRoute, network);
                    plannedRoutes.put(shipment.getId(), bestRoute);
                    totalObjective += bestRoute.getObjectiveValue();

                } else {
                    totalObjective += 1000.0;
                }
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;

        System.out.printf(
                "Planificación final: %d / %d envíos planificados%n",
                plannedRoutes.size(),
                shipments.size()
        );

        ValidationReport validation = validateSolution(network, plannedRoutes);

        System.out.println("Validación restricciones:");
        System.out.println("  Rutas fuera de plazo     : " + validation.rutasFueraDePlazo);
        System.out.println("  Vuelos sobrecapacidad    : " + validation.vuelosSobrecapacidad);
        System.out.println("  Almacenes sobrecapacidad : " + validation.almacenesSobrecapacidad);
        System.out.println("  Solución válida          : " + validation.isValid());

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
                    reserveRoute(bestRoute, network);
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

        int noImprovement = 0;
        int maxNoImprovement = 8;

        for (int iter = 1; iter <= config.getMaxIterations(); iter++) {
            boolean improved = false;

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
                    improved = true;
                }
            }

            if (bestRoute != null && improved) {
                pheromones.updateGlobalBest(
                        List.of(bestRoute),
                        config.getRho(),
                        config.getQ()
                );
            }

            if (improved) {
                noImprovement = 0;
            } else {
                noImprovement++;
            }

            if (noImprovement >= maxNoImprovement) {
                break;
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

    private void reserveRoute(Route route, Network network) {
        int bags = route.getShipment().getBagCount();
        String finalDestination = route.getShipment().getDestinationId();

        for (Flight flight : route.getFlights()) {
            flight.reserveCapacity(bags);

            // Solo se almacena temporalmente en escalas, no en destino final
            if (!flight.getDestinationId().equals(finalDestination)) {
                Airport dest = network.getAirport(flight.getDestinationId());
                if (dest != null) {
                    dest.setCurrentStock(dest.getCurrentStock() + bags);
                }
            }
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

    private Map<String, List<Shipment>> groupShipmentsByOD(List<Shipment> shipments) {
        Map<String, List<Shipment>> groups = new LinkedHashMap<>();

        for (Shipment s : shipments) {
            String key = s.getOriginId() + "->" + s.getDestinationId();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(s);
    }

        return groups;
    }

    private boolean canReserveRoute(Route route, int bags, Network network) {
        String finalDestination = route.getShipment().getDestinationId();

        for (Flight flight : route.getFlights()) {
            if (flight.isCancelled()) return false;
            if (flight.getAvailableCapacity() < bags) return false;

            // Solo revisar almacén si es aeropuerto intermedio
            if (!flight.getDestinationId().equals(finalDestination)) {
                Airport dest = network.getAirport(flight.getDestinationId());
                if (dest != null && dest.getAvailableSpace() < bags) return false;
            }
        }

        return true;
    }
    
    private ValidationReport validateSolution(Network network, Map<String, Route> routes) {
        ValidationReport report = new ValidationReport();

        for (Route r : routes.values()) {
            if (!r.meetsDeadline()) {
                report.rutasFueraDePlazo++;
            }
        }

        for (Flight f : network.getFlights()) {
            if (f.getUsedCapacity() > f.getCapacity()) {
                report.vuelosSobrecapacidad++;
            }
        }

        for (Airport a : network.getAirports()) {
            if (a.getCurrentStock() > a.getWarehouseCapacity()) {
                report.almacenesSobrecapacidad++;
            }
        }

        return report;
    }
}