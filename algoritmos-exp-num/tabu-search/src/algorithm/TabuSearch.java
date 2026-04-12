package algorithm;

import model.Envio;
import model.Ruta;

import java.util.LinkedList;
import java.util.List;

/**
 * Implementación del algoritmo Tabu Search para el problema de
 * enrutamiento de envíos entre aeropuertos.
 *
 * ── ESTRUCTURA GENERAL ──────────────────────────────────────────────────────
 *
 *  1. Partir de la solución inicial (generada por BFS + greedy).
 *  2. En cada iteración:
 *     a. Generar candidatos (muestra aleatoria de envíos × 2 tipos de movimiento).
 *     b. Elegir el mejor movimiento que NO esté en la lista tabú,
 *        O que esté tabú pero supere la mejor solución global (aspiración).
 *     c. Aplicar el movimiento a la solución actual.
 *     d. Registrar el movimiento en la lista tabú (FIFO, tamaño fijo).
 *     e. Actualizar la mejor solución global si corresponde.
 *  3. Devolver la mejor solución global encontrada.
 *
 * ── LISTA TABÚ ───────────────────────────────────────────────────────────────
 *  Almacena claves String de la forma "claveEnvio:firmaRutaAnterior".
 *  Impide que un movimiento que deshace un cambio reciente sea elegido.
 *  Tamaño fijo (tenencia): cuando se supera, el movimiento más antiguo expira.
 *  Tamaño recomendado: 20–50 para este problema.
 *
 * ── CRITERIO DE ASPIRACIÓN ───────────────────────────────────────────────────
 *  Si un movimiento tabú produce una solución mejor que la mejor global,
 *  se acepta de todas formas. Evita perderse la solución óptima global.
 *
 * ── PARÁMETROS CONFIGURABLES ─────────────────────────────────────────────────
 *  maxIteraciones      : cuántas iteraciones hace el algoritmo (ej: 200)
 *  tenenciaTabu        : cuántos movimientos guarda la lista tabú (ej: 30)
 *  tamanoMuestra       : envíos evaluados por iteración (ej: 200)
 *                        trade-off: más muestra = mejor calidad, más tiempo
 */
public class TabuSearch {

    // ── Parámetros ────────────────────────────────────────────────────────────
    private final int maxIteraciones;
    private final int tenenciaTabu;
    private final int tamanoMuestra;

    // ── Componentes ───────────────────────────────────────────────────────────
    private final GrafoVuelos grafo;
    private final Vecindad    vecindad;

    // ── Estado interno ────────────────────────────────────────────────────────
    // Cola FIFO: cuando supera 'tenenciaTabu', el primero (más viejo) se descarta
    private final LinkedList<String> listaTabu;

    public TabuSearch(GrafoVuelos grafo, int maxIteraciones, int tenenciaTabu, int tamanoMuestra) {
        this.grafo           = grafo;
        this.maxIteraciones  = maxIteraciones;
        this.tenenciaTabu    = tenenciaTabu;
        this.tamanoMuestra   = tamanoMuestra;
        this.vecindad        = new Vecindad(grafo);
        this.listaTabu       = new LinkedList<>();
    }

    /** Constructor con parámetros por defecto razonables para este problema */
    public TabuSearch(GrafoVuelos grafo) {
        this(grafo, 200, 30, 200);
    }

    // =========================================================================
    // ALGORITMO PRINCIPAL
    // =========================================================================

    /**
     * Ejecuta el Tabu Search a partir de la solución inicial dada.
     *
     * @param solucionInicial solución de partida (generada por BFS+greedy)
     * @param envios          lista completa de envíos del problema
     * @return la mejor solución encontrada durante toda la búsqueda
     */
    public Solucion ejecutar(Solucion solucionInicial, List<Envio> envios) {

        Solucion actual       = solucionInicial.clonar();
        Solucion mejorGlobal  = solucionInicial.clonar();

        System.out.println("\n[TabuSearch] Iniciando...");
        System.out.printf("[TabuSearch] Costo inicial: %.0f | Iter: %d | Muestra: %d | Tenencia: %d%n",
            actual.getCostoTotal(), maxIteraciones, tamanoMuestra, tenenciaTabu);

        for (int iter = 1; iter <= maxIteraciones; iter++) {

            // ─── a. Generar candidatos ────────────────────────────────────────
            List<Movimiento> candidatos = vecindad.generarCandidatos(actual, envios, tamanoMuestra);

            if (candidatos.isEmpty()) {
                System.out.println("[TabuSearch] Sin candidatos en iter " + iter + ". Terminando.");
                break;
            }

            // ─── b. Elegir el mejor movimiento permitido ──────────────────────
            Movimiento elegido     = null;
            double     mejorDelta  = Double.MAX_VALUE;

            for (Movimiento mov : candidatos) {
                boolean esTabu     = listaTabu.contains(mov.getClaveTabu());
                double  costoNuevo = actual.getCostoTotal() + mov.getDeltaCosto();

                // Criterio de aspiración: tabú pero mejor que el mejor global
                boolean superaAspiracion = costoNuevo < mejorGlobal.getCostoTotal();

                if (!esTabu || superaAspiracion) {
                    if (mov.getDeltaCosto() < mejorDelta) {
                        mejorDelta = mov.getDeltaCosto();
                        elegido    = mov;
                    }
                }
            }

            if (elegido == null) {
                // Todos los candidatos están tabú y ninguno supera la aspiración.
                // Forzamos el menos malo de todos (sin restricción tabú) para no estancarnos.
                elegido = candidatos.stream()
                    .min((a, b) -> Double.compare(a.getDeltaCosto(), b.getDeltaCosto()))
                    .orElse(null);
            }

            if (elegido == null) continue;

            // ─── c. Aplicar el movimiento ─────────────────────────────────────
            aplicarMovimiento(actual, elegido);

            // ─── d. Actualizar la lista tabú ──────────────────────────────────
            listaTabu.addLast(elegido.getClaveTabu());
            if (listaTabu.size() > tenenciaTabu) {
                listaTabu.removeFirst(); // expira el movimiento más antiguo
            }

            // ─── e. Actualizar mejor global ───────────────────────────────────
            if (actual.getCostoTotal() < mejorGlobal.getCostoTotal()) {
                mejorGlobal = actual.clonar();
            }

            // ─── Progreso cada 10 iteraciones ─────────────────────────────────
            if (iter % 10 == 0 || iter == 1) {
                System.out.printf(
                    "[TabuSearch] Iter %3d | CostoActual: %9.0f | MejorGlobal: %9.0f | Tabú: %d/%d | Delta: %+.0f%n",
                    iter,
                    actual.getCostoTotal(),
                    mejorGlobal.getCostoTotal(),
                    listaTabu.size(),
                    tenenciaTabu,
                    elegido.getDeltaCosto()
                );
            }
        }

        System.out.printf("%n[TabuSearch] Finalizado.%n");
        System.out.printf("[TabuSearch] Costo inicial : %.0f min%n", solucionInicial.getCostoTotal());
        System.out.printf("[TabuSearch] Mejor global  : %.0f min%n", mejorGlobal.getCostoTotal());
        System.out.printf("[TabuSearch] Mejora total  : %.0f min (%.2f%%)%n",
            solucionInicial.getCostoTotal() - mejorGlobal.getCostoTotal(),
            (1.0 - mejorGlobal.getCostoTotal() / solucionInicial.getCostoTotal()) * 100
        );

        return mejorGlobal;
    }

    // =========================================================================
    // APLICAR MOVIMIENTO
    // =========================================================================

    /**
     * Aplica un movimiento a la solución: reemplaza la ruta del envío afectado
     * por la nueva ruta propuesta por el movimiento.
     *
     * Modifica 'solucion' en el lugar (no crea una copia).
     * El costo se invalida para que se recalcule en el próximo getCostoTotal().
     */
    private void aplicarMovimiento(Solucion solucion, Movimiento movimiento) {
        Ruta rutaNueva = movimiento.getRutaNueva();
        solucion.agregarRuta(rutaNueva);  // sobreescribe la ruta anterior del mismo envío
        solucion.marcarDirty();
    }
}
