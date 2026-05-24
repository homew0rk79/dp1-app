package algorithm;

import config.ACOConfig;
import config.Shipment;
import model.Airport;
import model.Flight;
import model.Network;
import model.Route;

import java.util.*;

/**
 * Representa una hormiga artificial que construye una ruta para un envío
 * utilizando la regla de transición probabilística del ACS.
 * <p>
 * La hormiga selecciona el siguiente vuelo basándose en la combinación de
 * feromona τ(i,j) y la función heurística compuesta η(i,j).
 */
public class AntAgent {

    private final int id;
    private final Random random;

    /**
     * Crea un agente hormiga.
     *
     * @param id     identificador de la hormiga
     * @param random generador de números aleatorios
     */
    public AntAgent(int id, Random random) {
        this.id = id;
        this.random = random;
    }

    /**
     * Construye una ruta para el envío dado utilizando la regla probabilística ACS.
     * La hormiga parte del aeropuerto de origen y avanza paso a paso seleccionando
     * vuelos según P(i→j) = [τ^α · η^β] / Σ[τ^α · η^β].
     *
     * @param shipment   envío para el cual construir la ruta
     * @param network    red de transporte
     * @param pheromones matriz de feromonas
     * @param config     configuración ACO
     * @return ruta construida, o null si no se encontró un camino válido
     */
    public Route buildRoute(Shipment shipment, Network network,
                            PheromoneMatrix pheromones, ACOConfig config) {
        String currentAirport = shipment.getOriginId();
        String destination = shipment.getDestinationId();
        int bags = shipment.getBagCount();

        List<Flight> selectedFlights = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        visited.add(currentAirport);
        double totalTime = 0.0;

        // Límite de saltos para evitar ciclos infinitos
        int maxHops = network.getAirportCount();

        while (!currentAirport.equals(destination) && selectedFlights.size() < maxHops) {
            List<Flight> available = network.getAvailableFlights(currentAirport, bags);

            // Filtrar aeropuertos ya visitados para evitar ciclos
            available = available.stream()
                    .filter(f -> !visited.contains(f.getDestinationId()))
                    .toList();

            if (available.isEmpty()) {
                return null; // Sin camino posible
            }

            Flight chosen = selectNextFlight(available, destination, network, pheromones, config);
            selectedFlights.add(chosen);
            totalTime += chosen.getTravelTime();
            currentAirport = chosen.getDestinationId();
            visited.add(currentAirport);
        }

        if (!currentAirport.equals(destination)) {
            return null; // No se alcanzó el destino
        }

        // Calcular métricas de la ruta
        double risk = calculateRisk(totalTime, shipment.getDeadline());
        double saturation = calculateSaturation(selectedFlights, network);
        double objectiveValue = calculateObjective(risk, saturation, selectedFlights.size(), config);

        return new Route(shipment, selectedFlights, totalTime, risk, saturation, objectiveValue);
    }

    /**
     * Selecciona el siguiente vuelo usando la regla probabilística ACS.
     * P(i→j) = [τ(i,j)^α · η(i,j)^β] / Σ_l [τ(i,l)^α · η(i,l)^β]
     */
    private Flight selectNextFlight(List<Flight> available, String destination,
                                    Network network, PheromoneMatrix pheromones,
                                    ACOConfig config) {
        if (available.size() == 1) {
            return available.get(0);
        }

        double[] probabilities = new double[available.size()];
        double sum = 0.0;

        for (int i = 0; i < available.size(); i++) {
            Flight f = available.get(i);
            double tau = pheromones.get(f.getId());
            double eta = calculateHeuristic(f, destination, network, config);

            double value = Math.pow(tau, config.getAlpha()) * Math.pow(eta, config.getBeta());
            probabilities[i] = value;
            sum += value;
        }

        if (sum <= 0) {
            // Fallback: selección uniforme
            return available.get(random.nextInt(available.size()));
        }

        // Normalizar y seleccionar por ruleta
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

    /**
     * Calcula la función heurística compuesta η(i,j).
     * η(i,j) = w1·(1/tiempo) + w2·(cap_disp_vuelo/cap_max) + w3·(almacen_disp/cap_max_almacen)
     * Se añade un bonus si el vuelo va directo al destino.
     */
    private double calculateHeuristic(Flight flight, String destination,
                                      Network network, ACOConfig config) {
        // Componente 1: inversa del tiempo de traslado
        double timeComponent = 1.0 / flight.getTravelTime();

        // Componente 2: ratio de capacidad disponible del vuelo
        double capRatio = (double) flight.getAvailableCapacity() / flight.getCapacity();

        // Componente 3: ratio de espacio disponible en almacén destino
        Airport destAirport = network.getAirport(flight.getDestinationId());
        double warehouseRatio = (double) destAirport.getAvailableSpace() / destAirport.getWarehouseCapacity();

        double eta = config.getW1() * timeComponent
                   + config.getW2() * capRatio
                   + config.getW3() * warehouseRatio;

        // Bonus por vuelo directo al destino final
        if (flight.getDestinationId().equals(destination)) {
            eta *= 1.5;
        }

        return Math.max(eta, 1e-10); // Evitar cero
    }

    /**
     * Calcula el riesgo de incumplimiento de plazo.
     * Riesgo = max(0, (tiempoTotal / deadline) - umbral) normalizado a [0, 1].
     *
     * @param totalTime tiempo total de la ruta
     * @param deadline  plazo máximo
     * @return riesgo ∈ [0, 1]
     */
    private double calculateRisk(double totalTime, double deadline) {
        if (deadline <= 0) return 1.0;
        double ratio = totalTime / deadline;
        if (ratio <= 0.5) return 0.0;
        if (ratio >= 1.0) return 1.0;
        // Escalar linealmente entre 0.5 y 1.0 del ratio
        return (ratio - 0.5) / 0.5;
    }

    /**
     * Calcula la saturación promedio de los nodos intermedios.
     *
     * @param flights vuelos de la ruta
     * @param network red de transporte
     * @return saturación promedio ∈ [0, 1]
     */
    private double calculateSaturation(List<Flight> flights, Network network) {
        if (flights.size() <= 1) return 0.0;

        double totalSaturation = 0.0;
        int intermediateCount = 0;

        // Nodos intermedios: destinos de todos los vuelos excepto el último
        for (int i = 0; i < flights.size() - 1; i++) {
            Airport intermediate = network.getAirport(flights.get(i).getDestinationId());
            double saturation = (double) intermediate.getCurrentStock() / intermediate.getWarehouseCapacity();
            totalSaturation += saturation;
            intermediateCount++;
        }

        return intermediateCount > 0 ? totalSaturation / intermediateCount : 0.0;
    }

    /**
     * Calcula la función objetivo f(S) combinando riesgo, saturación y escalas.
     * f(S) = riskWeight·risk + saturationWeight·saturation + stopsWeight·(stops/maxStops)
     */
    private double calculateObjective(double risk, double saturation, int numFlights,
                                      ACOConfig config) {
        int stops = Math.max(0, numFlights - 1);
        double normalizedStops = stops / 10.0; // Normalizar asumiendo máximo ~10 escalas

        return config.getRiskWeight() * risk
             + config.getSaturationWeight() * saturation
             + config.getStopsWeight() * normalizedStops;
    }

    /** @return identificador de la hormiga */
    public int getId() { return id; }

    @Override
    public String toString() {
        return "Ant-" + id;
    }
}
