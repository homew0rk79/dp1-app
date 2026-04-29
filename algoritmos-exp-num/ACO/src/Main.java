import algorithm.ACSPlanner;
import algorithm.PlannerResult;
import config.ACOConfig;
import config.Shipment;
import model.Airport;
import model.Flight;
import model.Network;
import model.Route;

import java.util.ArrayList;
import java.util.List;

/**
 * Clase principal de prueba para verificar el funcionamiento del planificador ACS.
 * Crea una red hardcodeada con 5 aeropuertos en 2 continentes (Europa y Sudamérica),
 * define envíos de prueba y ejecuta tanto la planificación como la replanificación.
 */
public class Main {

    /**
     * Punto de entrada principal.
     *
     * @param args argumentos de línea de comandos (no utilizados)
     */
    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║         Tasf.B2B — Planificador ACS de Equipajes        ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println();

        // --- Paso 1: Construir la red de transporte ---
        Network network = buildTestNetwork();
        System.out.println("Red de transporte creada: " + network);
        System.out.println("Aeropuertos:");
        for (Airport a : network.getAirports()) {
            System.out.println("  " + a);
        }
        System.out.println("Vuelos:");
        for (Flight f : network.getFlights()) {
            System.out.println("  " + f);
        }
        System.out.println();

        // --- Paso 2: Definir envíos de prueba ---
        List<Shipment> shipments = buildTestShipments();
        System.out.println("Envíos a planificar:");
        for (Shipment s : shipments) {
            System.out.println("  " + s);
        }
        System.out.println();

        // --- Paso 3: Configurar y ejecutar el planificador ---
        ACOConfig config = ACOConfig.defaults();
        ACSPlanner planner = new ACSPlanner(42);

        System.out.println("--- FASE 1: PLANIFICACIÓN INICIAL ---\n");
        PlannerResult result = planner.plan(network, shipments, config);
        System.out.println();
        System.out.println(result.summary());

        // --- Paso 4: Simular cancelación y replanificación ---
        System.out.println("--- FASE 2: REPLANIFICACIÓN ANTE CANCELACIÓN ---");
        System.out.println("Simulando cancelación del vuelo MAD-BOG-1 (Madrid → Bogotá)...\n");

        // Los envíos afectados son los que usaban ese vuelo
        List<Shipment> affected = new ArrayList<>();
        for (Shipment s : shipments) {
            Route route = result.getRoutesByShipment().get(s.getId());
            if (route != null && route.getFlights().stream()
                    .anyMatch(f -> f.getId().equals("MAD-BOG-1"))) {
                affected.add(s);
            }
        }

        // Si ninguno usaba ese vuelo, replanificar todos como ejemplo
        if (affected.isEmpty()) {
            System.out.println("Ningún envío usaba directamente MAD-BOG-1. Replanificando todos como ejemplo.");
            affected = shipments;
        } else {
            System.out.println("Envíos afectados: " + affected.size());
        }

        PlannerResult replanResult = planner.replan(network, affected, "MAD-BOG-1", config);
        System.out.println();
        System.out.println(replanResult.summary());

        System.out.println("=== Ejecución completada exitosamente ===");
    }

    /**
     * Construye una red de prueba con 5 aeropuertos en 2 continentes.
     * <pre>
     * Europa (EU):
     *   MAD - Madrid Barajas     (cap almacén: 700)
     *   CDG - Paris Charles de Gaulle (cap almacén: 750)
     *   FCO - Roma Fiumicino     (cap almacén: 600)
     *
     * Sudamérica (SA):
     *   BOG - Bogotá El Dorado   (cap almacén: 500)
     *   LIM - Lima Jorge Chávez  (cap almacén: 550)
     * </pre>
     */
    private static Network buildTestNetwork() {
        Network network = new Network();

        // Aeropuertos Europa
        network.addAirport(new Airport("MAD", "Madrid Barajas", "EU", 700, 200));
        network.addAirport(new Airport("CDG", "Paris CDG", "EU", 750, 150));
        network.addAirport(new Airport("FCO", "Roma Fiumicino", "EU", 600, 100));

        // Aeropuertos Sudamérica
        network.addAirport(new Airport("BOG", "Bogota El Dorado", "SA", 500, 80));
        network.addAirport(new Airport("LIM", "Lima Jorge Chavez", "SA", 550, 120));

        // --- Vuelos intra-Europa (mismo continente: cap 150-250, tiempo 0.5d) ---
        network.addFlight(new Flight("MAD-CDG-1", "MAD", "CDG", 200, 0.5, 3));
        network.addFlight(new Flight("CDG-MAD-1", "CDG", "MAD", 200, 0.5, 3));
        network.addFlight(new Flight("MAD-FCO-1", "MAD", "FCO", 180, 0.5, 2));
        network.addFlight(new Flight("FCO-MAD-1", "FCO", "MAD", 180, 0.5, 2));
        network.addFlight(new Flight("CDG-FCO-1", "CDG", "FCO", 150, 0.5, 2));
        network.addFlight(new Flight("FCO-CDG-1", "FCO", "CDG", 150, 0.5, 2));

        // --- Vuelos intra-Sudamérica (mismo continente: cap 150-250, tiempo 0.5d) ---
        network.addFlight(new Flight("BOG-LIM-1", "BOG", "LIM", 200, 0.5, 2));
        network.addFlight(new Flight("LIM-BOG-1", "LIM", "BOG", 200, 0.5, 2));

        // --- Vuelos intercontinentales (distinto continente: cap 150-400, tiempo 1.0d) ---
        network.addFlight(new Flight("MAD-BOG-1", "MAD", "BOG", 300, 1.0, 1));
        network.addFlight(new Flight("BOG-MAD-1", "BOG", "MAD", 300, 1.0, 1));
        network.addFlight(new Flight("MAD-LIM-1", "MAD", "LIM", 250, 1.0, 1));
        network.addFlight(new Flight("LIM-MAD-1", "LIM", "MAD", 250, 1.0, 1));
        network.addFlight(new Flight("CDG-BOG-1", "CDG", "BOG", 250, 1.0, 1));
        network.addFlight(new Flight("BOG-CDG-1", "BOG", "CDG", 250, 1.0, 1));

        return network;
    }

    /**
     * Crea envíos de prueba variados.
     */
    private static List<Shipment> buildTestShipments() {
        List<Shipment> shipments = new ArrayList<>();

        // Envío 1: Madrid → Lima (intercontinental, plazo 2 días)
        shipments.add(new Shipment("SHP-001", "MAD", "LIM", 100, 2.0));

        // Envío 2: Paris → Bogotá (intercontinental, plazo 2 días)
        shipments.add(new Shipment("SHP-002", "CDG", "BOG", 80, 2.0));

        // Envío 3: Madrid → Paris (mismo continente, plazo 1 día)
        shipments.add(new Shipment("SHP-003", "MAD", "CDG", 120, 1.0));

        // Envío 4: Bogotá → Roma (intercontinental con escala, plazo 2 días)
        shipments.add(new Shipment("SHP-004", "BOG", "FCO", 60, 2.0));

        // Envío 5: Lima → Paris (intercontinental con posibles escalas, plazo 2 días)
        shipments.add(new Shipment("SHP-005", "LIM", "CDG", 90, 2.0));

        return shipments;
    }
}
