import algorithm.*;
import data.DataLoader;
import model.*;

import java.io.*;
import java.util.*;

public class Main {

    // "prueba", "5dias", "completo"
    private static final String ESCENARIO = "completo";
    private static final int CORRIDAS = 15;

    public static void main(String[] args) throws Exception {

        String base = "C:/DP1/dp1-app/docs/data";
        String rutaAeropuertos = base + "/c.1inf54.26.1.v1.Aeropuerto.husos.v1.20250818__estudiantes.txt";
        String rutaVuelos = base + "/planes_vuelo.txt";
        String directorioEnvios = base + "/_envios_preliminar_";

        separador('=');
        System.out.println("  EXPERIMENTO NUMERICO — ACO");
        System.out.println("  Escenario: " + ESCENARIO.toUpperCase() + " | Corridas: " + CORRIDAS);
        separador('=');

        DataLoader loader = new DataLoader(rutaAeropuertos, rutaVuelos, directorioEnvios);

        System.out.println("\nCargando datos...");
        Map<String, Airport> aeropuertos = loader.cargarAeropuertos();
        List<Flight> vuelosBase = loader.cargarVuelos();

        List<Shipment> enviosBase;

        switch (ESCENARIO) {
            case "5dias":
                enviosBase = loader.cargarEnvios(500);
                break;

            case "completo":
                enviosBase = loader.cargarEnvios(-1);

                Collections.shuffle(enviosBase, new Random(42));

                int limite = 430_000;
                if (enviosBase.size() > limite) {
                    enviosBase = enviosBase.subList(0, limite);
                }

                break;

            default:
                enviosBase = filtrarPorOrigen(
                        loader.cargarEnvios(5000),
                        Set.of("SPIM", "EBCI", "EHAM", "VIDP")
                );
                break;
        }

        System.out.printf("%nDatos cargados:%n");
        System.out.printf("  Aeropuertos : %,d%n", aeropuertos.size());
        System.out.printf("  Vuelos      : %,d%n", vuelosBase.size());
        System.out.printf("  Envios      : %,d%n", enviosBase.size());

        String csvPath = "resultados_ACO_" + ESCENARIO + ".csv";

        long[] tiempos = new long[CORRIDAS];
        double[] cumplimiento = new double[CORRIDAS];
        double[] costos = new double[CORRIDAS];
        double[] escalas = new double[CORRIDAS];

        try (PrintWriter csv = new PrintWriter(new FileWriter(csvPath))) {
            csv.println("corrida,semilla,tiempo_ms,pct_cumplimiento,costo_total,escalas_promedio,planificados,total");

            for (int c = 1; c <= CORRIDAS; c++) {
                long semilla = c;

                separador('-');
                System.out.printf("  Corrida %d de %d | semilla=%d%n", c, CORRIDAS, semilla);
                separador('-');

                Network network = construirNetwork(aeropuertos, vuelosBase);
                List<Shipment> shipments = prepararEnvios(enviosBase, network);

                ACOConfig config = ACOConfig.defaults();
                ACSPlanner planner = new ACSPlanner(semilla);

                PlannerResult result = planner.plan(network, shipments, config);

                long tiempo = result.getExecutionTimeMs();
                int planificados = result.getRoutesByShipment().size();
                long aTiempo = result.getOnTimeCount();

                double pct = shipments.isEmpty() ? 0.0 : (100.0 * aTiempo / shipments.size());
                double costo = result.getBestObjectiveValue();
                double escProm = calcularEscalasPromedio(result);

                tiempos[c - 1] = tiempo;
                cumplimiento[c - 1] = pct;
                costos[c - 1] = costo;
                escalas[c - 1] = escProm;

                System.out.printf("  Tiempo              : %,d ms (%.2f s)%n", tiempo, tiempo / 1000.0);
                System.out.printf("  Planificados        : %,d / %,d%n", planificados, shipments.size());
                System.out.printf("  Cumplimiento plazo  : %.2f%%%n", pct);
                System.out.printf("  Funcion objetivo    : %.4f%n", costo);
                System.out.printf("  Escalas promedio    : %.4f%n", escProm);

                csv.printf(Locale.US,
                        "%d,%d,%d,%.4f,%.4f,%.4f,%d,%d%n",
                        c, semilla, tiempo, pct, costo, escProm, planificados, shipments.size()
                );
            }
        }

        separador('=');
        System.out.printf("  RESUMEN FINAL — %d CORRIDAS — ACO%n", CORRIDAS);
        separador('=');

        imprimirResumen(tiempos, cumplimiento, costos, escalas);

        separador('-');
        System.out.println("  Resultados exportados a: " + csvPath);
        separador('=');
    }

    private static Network construirNetwork(Map<String, Airport> aeropuertos, List<Flight> vuelosBase) {
        Network network = new Network();

        for (Airport a : aeropuertos.values()) {
            network.addAirport(new Airport(
                    a.getId(),
                    a.getName(),
                    a.getContinent(),
                    a.getWarehouseCapacity(),
                    0
            ));
        }

        for (Flight f : vuelosBase) {
            if (network.getAirport(f.getOriginId()) == null ||
                network.getAirport(f.getDestinationId()) == null) {
                continue;
            }

            double tiempo = network.sameContinentCheck(
                    f.getOriginId(),
                    f.getDestinationId()
            ) ? 0.5 : 1.0;

            network.addFlight(new Flight(
                    f.getId(),
                    f.getOriginId(),
                    f.getDestinationId(),
                    f.getCapacity(),
                    tiempo,
                    f.getDailyFrequency()
            ));
        }

        return network;
    }

    private static List<Shipment> prepararEnvios(List<Shipment> enviosBase, Network network) {
        List<Shipment> shipments = new ArrayList<>();

        for (Shipment s : enviosBase) {
            if (network.getAirport(s.getOriginId()) == null ||
                network.getAirport(s.getDestinationId()) == null) {
                continue;
            }

            double plazo = network.sameContinentCheck(
                    s.getOriginId(),
                    s.getDestinationId()
            ) ? 1.0 : 2.0;

            shipments.add(new Shipment(
                    s.getId(),
                    s.getOriginId(),
                    s.getDestinationId(),
                    s.getBagCount(),
                    plazo
            ));
        }

        return shipments;
    }

    private static List<Shipment> filtrarPorOrigen(List<Shipment> envios, Set<String> origenes) {
        List<Shipment> filtrados = new ArrayList<>();

        for (Shipment s : envios) {
            if (origenes.contains(s.getOriginId())) {
                filtrados.add(s);
            }
        }

        return filtrados;
    }

    private static double calcularEscalasPromedio(PlannerResult result) {
        List<Route> routes = result.getAllRoutes();
        if (routes.isEmpty()) return 0.0;

        double total = 0.0;
        for (Route r : routes) {
            total += r.getStopCount();
        }

        return total / routes.size();
    }

    private static void imprimirResumen(long[] tiempos, double[] cumplimiento,
                                        double[] costos, double[] escalas) {
        long sumTiempo = 0;
        long minTiempo = Long.MAX_VALUE;
        long maxTiempo = Long.MIN_VALUE;

        double sumCumpl = 0;
        double sumCosto = 0;
        double sumEscalas = 0;

        double minCosto = Double.MAX_VALUE;
        double maxCosto = Double.MIN_VALUE;

        for (int i = 0; i < tiempos.length; i++) {
            sumTiempo += tiempos[i];
            sumCumpl += cumplimiento[i];
            sumCosto += costos[i];
            sumEscalas += escalas[i];

            minTiempo = Math.min(minTiempo, tiempos[i]);
            maxTiempo = Math.max(maxTiempo, tiempos[i]);
            minCosto = Math.min(minCosto, costos[i]);
            maxCosto = Math.max(maxCosto, costos[i]);
        }

        int n = tiempos.length;

        System.out.printf("  Tiempo promedio      : %,d ms (%.2f s)%n", sumTiempo / n, sumTiempo / n / 1000.0);
        System.out.printf("  Tiempo minimo        : %,d ms%n", minTiempo);
        System.out.printf("  Tiempo maximo        : %,d ms%n", maxTiempo);
        System.out.printf("  Cumplimiento promedio: %.2f%%%n", sumCumpl / n);
        System.out.printf("  Costo promedio       : %.4f%n", sumCosto / n);
        System.out.printf("  Costo minimo         : %.4f%n", minCosto);
        System.out.printf("  Costo maximo         : %.4f%n", maxCosto);
        System.out.printf("  Escalas promedio     : %.4f%n", sumEscalas / n);
    }

    private static void separador(char c) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 60; i++) sb.append(c);
        System.out.println(sb);
    }
}