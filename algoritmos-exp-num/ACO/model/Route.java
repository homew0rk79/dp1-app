package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Representa una solución (ruta) para un envío específico.
 * Una ruta es una secuencia de vuelos desde el origen hasta el destino del envío,
 * junto con métricas calculadas de tiempo total, riesgo y escalas.
 */
public class Route {

    private final Shipment shipment;
    private final List<Flight> flights;
    private final double totalTime;
    private final double risk;
    private final double saturation;
    private final double objectiveValue;

    /**
     * Crea una nueva ruta para un envío.
     *
     * @param shipment       envío al que pertenece esta ruta
     * @param flights        lista ordenada de vuelos que componen la ruta
     * @param totalTime      tiempo total de traslado en días
     * @param risk           riesgo de incumplimiento de plazo [0, 1]
     * @param saturation     saturación promedio de nodos intermedios [0, 1]
     * @param objectiveValue valor de la función objetivo f(S) para esta ruta
     */
    public Route(Shipment shipment, List<Flight> flights, double totalTime,
                 double risk, double saturation, double objectiveValue) {
        this.shipment = shipment;
        this.flights = new ArrayList<>(flights);
        this.totalTime = totalTime;
        this.risk = risk;
        this.saturation = saturation;
        this.objectiveValue = objectiveValue;
    }

    /** @return el envío asociado a esta ruta */
    public Shipment getShipment() { return shipment; }

    /** @return lista inmutable de vuelos que componen la ruta */
    public List<Flight> getFlights() { return Collections.unmodifiableList(flights); }

    /** @return tiempo total de traslado en días */
    public double getTotalTime() { return totalTime; }

    /** @return riesgo de incumplimiento [0, 1] */
    public double getRisk() { return risk; }

    /** @return saturación promedio de nodos intermedios [0, 1] */
    public double getSaturation() { return saturation; }

    /** @return valor de la función objetivo */
    public double getObjectiveValue() { return objectiveValue; }

    /** @return número de escalas (vuelos - 1, mínimo 0) */
    public int getStopCount() { return Math.max(0, flights.size() - 1); }

    /** @return true si la ruta cumple el plazo de entrega */
    public boolean meetsDeadline() { return totalTime <= shipment.getDeadline(); }

    /**
     * Devuelve la secuencia de aeropuertos visitados.
     *
     * @return lista de ids de aeropuertos en orden
     */
    public List<String> getAirportSequence() {
        List<String> seq = new ArrayList<>();
        if (!flights.isEmpty()) {
            seq.add(flights.get(0).getOriginId());
            for (Flight f : flights) {
                seq.add(f.getDestinationId());
            }
        }
        return seq;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Route[").append(shipment.getId()).append("]: ");
        sb.append(String.join(" -> ", getAirportSequence()));
        sb.append(String.format(" | time=%.2fd, risk=%.4f, stops=%d, f(S)=%.4f",
                totalTime, risk, getStopCount(), objectiveValue));
        if (!meetsDeadline()) {
            sb.append(" *** EXCEDE PLAZO ***");
        }
        return sb.toString();
    }
}
