import algorithm.*;
import data.DataLoader;
import model.*;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Main {

    // Cambia ESCENARIO para seleccionar el modo de ejecución:
    //   "prueba"   → 5 000 envios de SPIM, EBCI, EHAM y VIDP   (experimento DCA)
    //   "5dias"    → dataset completo, parametros intensivos     30–90 min
    //   "completo" → dataset completo, parametros balanceados    30–60 min
    //   "exp_num"  → todo el dataset mezclado (semilla 42), muestra de 430 000
    private static final String ESCENARIO = "exp_num";

    // Numero de corridas independientes para el experimento estadistico
    private static final int CORRIDAS = 15;

    public static void main(String[] args) throws Exception {

        String base             = "C:/Users/ERLIN/Documents/GitHub/dp1-app/docs/data";
        String rutaAeropuertos  = base + "/c.1inf54.26.1.v1.Aeropuerto.husos.v1.20250818__estudiantes.txt";
        String rutaVuelos       = base + "/planes_vuelo.txt";
        String directorioEnvios = base + "/_envios_preliminar_";

        separador('=');
        System.out.println("  EXPERIMENTO NUMERICO — BUSQUEDA TABU");
        System.out.println("  Escenario: " + ESCENARIO.toUpperCase() + " | Corridas: " + CORRIDAS);
        separador('=');

        // ── 1. Cargar datos ───────────────────────────────────────────────────
        System.out.println("\nCargando datos del sistema...");
        DataLoader loader = new DataLoader(rutaAeropuertos, rutaVuelos, directorioEnvios);
        Map<String, Aeropuerto> aeropuertos = loader.cargarAeropuertos();
        List<Vuelo> vuelos = loader.cargarVuelos();

        // ── 2. Seleccionar parametros segun escenario ─────────────────────────
        List<Envio> envios;
        int maxIter, tenencia, muestra;

        switch (ESCENARIO) {
            case "5dias":
                envios   = loader.cargarEnvios();
                maxIter  = 200;
                tenencia = 60;
                muestra  = 500_000;
                break;
            case "completo":
                envios   = loader.cargarEnvios();
                maxIter  = 300;
                tenencia = 50;
                muestra  = 200_000;
                break;
            case "exp_num": {
                List<Envio> enviosBase = loader.cargarEnvios(-1);
                Collections.shuffle(enviosBase, new Random(42));
                int limite = Math.min(430_000, enviosBase.size());
                envios   = new ArrayList<>(enviosBase.subList(0, limite));
                maxIter  = 300;
                tenencia = 50;
                muestra  = 5_000;   // ~1.2% del dataset; 500k > 430k volteaba el guard de muestrear()
                break;
            }
            default: // "prueba"
                envios   = loader.cargarEnvios(Arrays.asList("SPIM", "EBCI", "EHAM", "VIDP"), 5_000);
                maxIter  = 200;
                tenencia = 45;
                muestra  = 1_000;
                break;
        }

        System.out.printf("%nDatos cargados exitosamente:%n");
        System.out.printf("  Aeropuertos en la red : %,d%n", aeropuertos.size());
        System.out.printf("  Vuelos disponibles    : %,d%n", vuelos.size());
        System.out.printf("  Envios a planificar   : %,d%n", envios.size());

        // ── 3. Fijar plazo de entrega por envio ───────────────────────────────
        int sinPlazo = 0;
        for (Envio envio : envios) {
            Aeropuerto orig = aeropuertos.get(envio.getOrigen());
            Aeropuerto dest = aeropuertos.get(envio.getDestino());
            if (orig != null && dest != null) {
                boolean mismo = orig.getContinente().equals(dest.getContinente());
                envio.setPlazoMaximoMinutos(mismo ? 1440 : 2880);
            } else {
                sinPlazo++;
            }
        }
        if (sinPlazo > 0)
            System.out.printf("  Advertencia: %d envios sin aeropuerto reconocido (sin plazo asignado)%n", sinPlazo);

        // ── 4. Construir grafo y capacidades ──────────────────────────────────
        Map<String, Integer> capacidadAeropuertos = new HashMap<>();
        for (Aeropuerto a : aeropuertos.values())
            capacidadAeropuertos.put(a.getCodigo(), a.getCapacidadMax());

        GrafoVuelos grafo = new GrafoVuelos(vuelos);

        // ── 5. Solucion inicial (determinista — igual para todas las corridas) ─
        separador('-');
        System.out.println("Construyendo solucion inicial (BFS + greedy)...");
        separador('-');
        SolucionInicial generador = new SolucionInicial(grafo, capacidadAeropuertos);
        Solucion inicial = generador.construir(envios);
        System.out.println();
        inicial.imprimirResumen();

        // ── 6. Bucle de corridas ──────────────────────────────────────────────
        separador('=');
        System.out.printf("  INICIO DEL EXPERIMENTO: %d corridas independientes%n", CORRIDAS);
        separador('=');

        String csvPath = "resultados_TS_" + ESCENARIO + ".csv";
        long[] tiempos       = new long[CORRIDAS];
        double[] cumplimiento = new double[CORRIDAS];
        double[] costos       = new double[CORRIDAS];
        double[] escalas      = new double[CORRIDAS];

        try (PrintWriter csv = new PrintWriter(new FileWriter(csvPath))) {
            csv.println("corrida,semilla,tiempo_ms,pct_cumplimiento,costo_total,escalas_promedio");

            for (int c = 1; c <= CORRIDAS; c++) {
                long semilla = c;
                separador('-');
                System.out.printf("  Corrida %d de %d  (semilla aleatoria: %d)%n", c, CORRIDAS, semilla);
                separador('-');

                long t1 = System.currentTimeMillis();
                TabuSearch tabu = new TabuSearch(grafo, maxIter, tenencia, muestra, semilla);
                Solucion resultado = tabu.ejecutar(inicial, envios);
                long tMs = System.currentTimeMillis() - t1;

                double pct     = resultado.getPorcentajeCumplimientoPlazo();
                double costo   = resultado.getCostoTotal();
                double escProm = resultado.getEscalasPromedio();

                tiempos[c - 1]      = tMs;
                cumplimiento[c - 1] = pct;
                costos[c - 1]       = costo;
                escalas[c - 1]      = escProm;

                System.out.println();
                System.out.printf("  Resultados de la corrida %d:%n", c);
                System.out.printf("    Tiempo de ejecucion      : %,d ms  (%.1f s)%n", tMs, tMs / 1000.0);
                System.out.printf("    Cumplimiento de plazos   : %.2f%%%n", pct);
                System.out.printf("    Funcion objetivo (costo) : %,.0f%n", costo);
                System.out.printf("    Escalas promedio         : %.4f por envio%n", escProm);
                imprimirColapsados(resultado);

                csv.printf("%d,%d,%d,%.4f,%.0f,%.4f%n", c, semilla, tMs, pct, costo, escProm);
            }
        }

        // ── 7. Resumen del experimento ────────────────────────────────────────
        separador('=');
        System.out.printf("  RESUMEN FINAL — %d CORRIDAS — BUSQUEDA TABU%n", CORRIDAS);
        separador('=');

        long   sumTiempo  = 0;
        double sumCumpl   = 0, sumCosto = 0, sumEscalas = 0;
        long   minTiempo  = Long.MAX_VALUE,  maxTiempo  = Long.MIN_VALUE;
        double minCosto   = Double.MAX_VALUE, maxCosto   = Double.MIN_VALUE;

        for (int i = 0; i < CORRIDAS; i++) {
            sumTiempo  += tiempos[i];
            sumCumpl   += cumplimiento[i];
            sumCosto   += costos[i];
            sumEscalas += escalas[i];
            if (tiempos[i] < minTiempo) minTiempo = tiempos[i];
            if (tiempos[i] > maxTiempo) maxTiempo = tiempos[i];
            if (costos[i]  < minCosto)  minCosto  = costos[i];
            if (costos[i]  > maxCosto)  maxCosto  = costos[i];
        }

        System.out.printf("  Tiempo de ejecucion:%n");
        System.out.printf("    Promedio : %,d ms  (%.1f s)%n", sumTiempo / CORRIDAS, sumTiempo / CORRIDAS / 1000.0);
        System.out.printf("    Minimo   : %,d ms%n", minTiempo);
        System.out.printf("    Maximo   : %,d ms%n", maxTiempo);
        System.out.printf("  Cumplimiento de plazos (promedio) : %.2f%%%n", sumCumpl / CORRIDAS);
        System.out.printf("  Funcion objetivo:%n");
        System.out.printf("    Promedio : %,.0f%n", sumCosto / CORRIDAS);
        System.out.printf("    Minimo   : %,.0f%n", minCosto);
        System.out.printf("    Maximo   : %,.0f%n", maxCosto);
        System.out.printf("  Escalas promedio (promedio)       : %.4f%n", sumEscalas / CORRIDAS);

        separador('-');
        System.out.printf("  Resultados exportados a: %s%n", csvPath);
        separador('=');
    }

    private static void imprimirColapsados(Solucion s) {
        final int TOP = 10;
        List<String> vuelos = s.reporteVuelosSaturados();
        List<String> aerops = s.reporteAeropuertosSaturados();

        if (vuelos.isEmpty() && aerops.isEmpty()) {
            System.out.println("  [Sin colapsos de vuelo ni aeropuerto en esta corrida]");
            return;
        }
        if (!vuelos.isEmpty()) {
            System.out.printf("  Vuelos saturados: %,d total — top %d por exceso:%n",
                vuelos.size(), Math.min(vuelos.size(), TOP));
            vuelos.stream().limit(TOP).forEach(System.out::println);
        }
        if (!aerops.isEmpty()) {
            System.out.printf("  Aeropuertos saturados: %,d pares aeropuerto-dia — top %d por exceso:%n",
                aerops.size(), Math.min(aerops.size(), TOP));
            aerops.stream().limit(TOP).forEach(System.out::println);
        }
    }

    private static void separador(char c) {
        StringBuilder sb = new StringBuilder(60);
        for (int i = 0; i < 60; i++) sb.append(c);
        System.out.println(sb.toString());
    }
}
