package com.tasfb2b.planner.aco;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Representa la red de transporte aéreo como un grafo dirigido.
 * Los nodos son aeropuertos y los arcos son vuelos.
 * Provee métodos para consultar conectividad y vuelos disponibles.
 */
public class Network {

    private final Map<String, Airport> airports;
    private final Map<String, Flight> flights;
    /** Mapa de aeropuerto origen → lista de vuelos salientes */
    private final Map<String, List<Flight>> adjacency;

    /**
     * Crea una red vacía.
     */
    public Network() {
        this.airports = new LinkedHashMap<>();
        this.flights = new LinkedHashMap<>();
        this.adjacency = new HashMap<>();
    }

    /**
     * Añade un aeropuerto a la red.
     *
     * @param airport aeropuerto a añadir
     * @throws IllegalArgumentException si el id ya existe
     */
    public void addAirport(Airport airport) {
        if (airports.containsKey(airport.getId())) {
            throw new IllegalArgumentException("Aeropuerto duplicado: " + airport.getId());
        }
        airports.put(airport.getId(), airport);
        adjacency.putIfAbsent(airport.getId(), new ArrayList<>());
    }

    /**
     * Añade un vuelo a la red.
     *
     * @param flight vuelo a añadir
     * @throws IllegalArgumentException si el origen o destino no existen, o el id está duplicado
     */
    public void addFlight(Flight flight) {
        if (!airports.containsKey(flight.getOriginId())) {
            throw new IllegalArgumentException("Aeropuerto de origen no encontrado: " + flight.getOriginId());
        }
        if (!airports.containsKey(flight.getDestinationId())) {
            throw new IllegalArgumentException("Aeropuerto de destino no encontrado: " + flight.getDestinationId());
        }
        if (flights.containsKey(flight.getId())) {
            throw new IllegalArgumentException("Vuelo duplicado: " + flight.getId());
        }
        flights.put(flight.getId(), flight);
        adjacency.computeIfAbsent(flight.getOriginId(), k -> new ArrayList<>()).add(flight);
    }

    /**
     * Obtiene un aeropuerto por su id.
     *
     * @param id identificador del aeropuerto
     * @return aeropuerto o null si no existe
     */
    public Airport getAirport(String id) {
        return airports.get(id);
    }

    /**
     * Obtiene un vuelo por su id.
     *
     * @param id identificador del vuelo
     * @return vuelo o null si no existe
     */
    public Flight getFlight(String id) {
        return flights.get(id);
    }

    /** @return colección de todos los aeropuertos */
    public Collection<Airport> getAirports() {
        return Collections.unmodifiableCollection(airports.values());
    }

    /** @return colección de todos los vuelos */
    public Collection<Flight> getFlights() {
        return Collections.unmodifiableCollection(flights.values());
    }

    /** @return número de aeropuertos */
    public int getAirportCount() { return airports.size(); }

    /** @return número de vuelos */
    public int getFlightCount() { return flights.size(); }

    /**
     * Obtiene los vuelos disponibles (no cancelados y con capacidad) desde un aeropuerto.
     *
     * @param airportId id del aeropuerto de origen
     * @param requiredCapacity capacidad mínima necesaria
     * @return lista de vuelos disponibles
     */
    public List<Flight> getAvailableFlights(String airportId, int requiredCapacity) {
        List<Flight> outgoing = adjacency.getOrDefault(airportId, Collections.emptyList());
        return outgoing.stream()
                .filter(f -> !f.isCancelled())
                .filter(f -> f.getAvailableCapacity() >= requiredCapacity)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene todos los vuelos salientes desde un aeropuerto (incluidos cancelados).
     *
     * @param airportId id del aeropuerto de origen
     * @return lista de vuelos
     */
    public List<Flight> getOutgoingFlights(String airportId) {
        return Collections.unmodifiableList(adjacency.getOrDefault(airportId, Collections.emptyList()));
    }

    /**
     * Verifica si dos aeropuertos están en el mismo continente.
     *
     * @param airportId1 primer aeropuerto
     * @param airportId2 segundo aeropuerto
     * @return true si están en el mismo continente
     */
    public boolean sameContinentCheck(String airportId1, String airportId2) {
        Airport a1 = airports.get(airportId1);
        Airport a2 = airports.get(airportId2);
        if (a1 == null || a2 == null) return false;
        return a1.getContinent().equals(a2.getContinent());
    }

    /**
     * Reinicia la capacidad utilizada de todos los vuelos.
     */
    public void resetAllFlightCapacities() {
        flights.values().forEach(Flight::resetUsedCapacity);
    }

    /**
     * Obtiene la lista de ids de todos los aeropuertos.
     *
     * @return lista de ids ordenada
     */
    public List<String> getAirportIds() {
        return new ArrayList<>(airports.keySet());
    }

    @Override
    public String toString() {
        return "Network{airports=" + airports.size() + ", flights=" + flights.size() + "}";
    }
}
