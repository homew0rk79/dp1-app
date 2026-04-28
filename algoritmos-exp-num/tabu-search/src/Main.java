import algorithm.*;
import data.DataLoader;
import model.*;

import java.util.List;
import java.util.Map;

public class Main {

    public static void main(String[] args) throws Exception {

        String base             = "C:/Users/ERLIN/Documents/GitHub/dp1-app/docs/data";
        String rutaAeropuertos  = base + "/c.1inf54.26.1.v1.Aeropuerto.husos.v1.20250818__estudiantes.txt";
        String rutaVuelos       = base + "/planes_vuelo.txt";
        String directorioEnvios = base + "/_envios_preliminar_";

        // ── 1. Cargar datos ────────────────────────────────────────────────────
        DataLoader loader = new DataLoader(rutaAeropuertos, rutaVuelos, directorioEnvios);
        loader.cargarAeropuertos();                 // 30 aeropuertos
        List<Vuelo> vuelos = loader.cargarVuelos(); // 2866 vuelos

        // 50 envíos por archivo = 1500 totales. Quitar el argumento para los 330K reales.
        List<Envio> envios = loader.cargarEnvios(50);

        // ── 2. Construir grafo ─────────────────────────────────────────────────
        GrafoVuelos grafo = new GrafoVuelos(vuelos);

        // ── 3. Solución inicial (BFS + greedy) ────────────────────────────────
        System.out.println("\n=== SOLUCIÓN INICIAL ===");
        SolucionInicial generador = new SolucionInicial(grafo);
        Solucion inicial = generador.construir(envios);
        inicial.imprimirResumen();

        // ── 4. Tabu Search ────────────────────────────────────────────────────
        //   maxIteraciones = 100
        //   tenenciaTabu   = 30  (movimientos que permanecen tabú)
        //   tamanoMuestra  = 200 (envíos evaluados por iteración)
        TabuSearch tabu = new TabuSearch(grafo, 100, 30, 200);
        Solucion mejor = tabu.ejecutar(inicial, envios);

        // ── 5. Resultados finales ─────────────────────────────────────────────
        System.out.println("\n=== SOLUCIÓN FINAL ===");
        mejor.imprimirResumen();

        System.out.println("\n=== MUESTRA DE RUTAS PLANIFICADAS (primeras 10 con ruta válida) ===");
        mejor.getRutas().stream()
            .filter(r -> !r.isSinSolucion())
            .limit(10)
            .forEach(r -> {
                Envio e = r.getEnvio();
                System.out.printf("  [%s] %s → %s | %d maletas | %d escala(s) | %d min%n",
                    e.getId(), e.getOrigen(), e.getDestino(),
                    e.getCantidad(), r.getNumEscalas(), r.calcularTiempoTotal());
                r.getVuelos().forEach(v -> System.out.println("    " + v));
            });
    }
}
