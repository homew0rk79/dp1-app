package algorithm;

import model.*;

import java.util.*;

public class AntAgent {

    private final int id;
    private final Random random;

    public AntAgent(int id, Random random) {
        this.id = id;
        this.random = random;
    }

    public Route buildRoute(Shipment shipment, Network network,
                            PheromoneMatrix pheromones, ACOConfig config) {

        String currentAirport = shipment.getOriginId();
        String destination = shipment.getDestinationId();
        int bags = shipment.getBagCount();

        List<Flight> selectedFlights = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        visited.add(currentAirport);

        double totalTime = 0.0;
        int maxHops = 4;

        while (!currentAirport.equals(destination) && selectedFlights.size() < maxHops) {
            double remainingTime = shipment.getDeadline() - totalTime;

            List<Flight> available = new ArrayList<>(
                    network.getAvailableFlights(currentAirport, bags)
            );

            available.removeIf(f ->
                    visited.contains(f.getDestinationId())
                            || f.getTravelTime() > remainingTime
            );

            if (available.isEmpty()) {
                return null;
            }

            Flight chosen = selectNextFlight(
                    available,
                    destination,
                    network,
                    pheromones,
                    config
            );

            selectedFlights.add(chosen);
            totalTime += chosen.getTravelTime();

            currentAirport = chosen.getDestinationId();
            visited.add(currentAirport);
        }

        if (!currentAirport.equals(destination)) return null;
        if (totalTime > shipment.getDeadline()) return null;

        double risk = calculateRisk(totalTime, shipment.getDeadline());
        double saturation = calculateSaturation(selectedFlights, network);
        double objectiveValue = calculateObjective(
                risk,
                saturation,
                selectedFlights.size(),
                totalTime,
                shipment.getDeadline(),
                config
        );

        return new Route(
                shipment,
                selectedFlights,
                totalTime,
                risk,
                saturation,
                objectiveValue
        );
    }

    private Flight selectNextFlight(List<Flight> available, String destination,
                                    Network network, PheromoneMatrix pheromones,
                                    ACOConfig config) {

        available.sort(Comparator.comparing(f -> !f.getDestinationId().equals(destination)));

        if (available.size() == 1) {
            return available.get(0);
        }

        double[] probabilities = new double[available.size()];
        double sum = 0.0;

        for (int i = 0; i < available.size(); i++) {
            Flight f = available.get(i);
            double tau = pheromones.get(f.getId());
            double eta = calculateHeuristic(f, destination, network, config);

            double value = Math.pow(tau, config.getAlpha())
                    * Math.pow(eta, config.getBeta());

            probabilities[i] = value;
            sum += value;
        }

        if (sum <= 0) {
            return available.get(random.nextInt(available.size()));
        }

        double r = random.nextDouble() * sum;
        double cumulative = 0.0;

        for (int i = 0; i < probabilities.length; i++) {
            cumulative += probabilities[i];
            if (r <= cumulative) {
                return available.get(i);
            }
        }

        return available.get(available.size() - 1);
    }

    private double calculateHeuristic(Flight flight, String destination,
                                      Network network, ACOConfig config) {

        double timeComponent = 1.0 / flight.getTravelTime();
        double capRatio = (double) flight.getAvailableCapacity() / flight.getCapacity();

        Airport destAirport = network.getAirport(flight.getDestinationId());
        double warehouseRatio = 1.0;

        if (destAirport != null) {
            warehouseRatio = (double) destAirport.getAvailableSpace()
                    / destAirport.getWarehouseCapacity();
        }

        double eta = config.getW1() * timeComponent
                + config.getW2() * capRatio
                + config.getW3() * warehouseRatio;

        if (flight.getDestinationId().equals(destination)) {
            eta *= 5.0;
        }

        return Math.max(eta, 1e-10);
    }

    private double calculateRisk(double totalTime, double deadline) {
        if (deadline <= 0) return 1.0;
        if (totalTime <= deadline) return 0.0;

        double excess = totalTime - deadline;
        return Math.min(1.0, excess / deadline);
    }

    private double calculateSaturation(List<Flight> flights, Network network) {
        if (flights.size() <= 1) return 0.0;

        double totalSaturation = 0.0;
        int intermediateCount = 0;

        for (int i = 0; i < flights.size() - 1; i++) {
            Airport intermediate = network.getAirport(flights.get(i).getDestinationId());
            if (intermediate == null) continue;

            double saturation = (double) intermediate.getCurrentStock()
                    / intermediate.getWarehouseCapacity();

            totalSaturation += saturation;
            intermediateCount++;
        }

        return intermediateCount > 0 ? totalSaturation / intermediateCount : 0.0;
    }

    private double calculateObjective(double risk, double saturation, int numFlights,
                                      double totalTime, double deadline,
                                      ACOConfig config) {

        int stops = Math.max(0, numFlights - 1);
        double normalizedStops = stops / 10.0;
        double normalizedTime = totalTime / deadline;

        return 0.70 * normalizedTime
                + config.getRiskWeight() * risk
                + config.getSaturationWeight() * saturation
                + config.getStopsWeight() * normalizedStops;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Ant-" + id;
    }
}