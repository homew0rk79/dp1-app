package com.tasfb2b.algorithm;

import com.tasfb2b.model.Envio;
import com.tasfb2b.model.Ruta;
import com.tasfb2b.model.Vuelo;

import java.util.*;

/**
 * Genera movimientos candidatos (vecindad) para el Tabu Search.
 *
 * Para cada envío evaluado se producen hasta 2 tipos de movimientos:
 *
 *  TIPO 1 — PRÓXIMO VUELO (mismo tramo, vuelo más tardío):
 *    Si la ruta actual usa el vuelo de las 07:55 en el tramo EBCI→SPIM,
 *    probamos usar el siguiente vuelo disponible en ese mismo tramo.
 *    Esto puede mejorar si el vuelo actual está saturado o si un vuelo
 *    posterior permite una mejor conexión en vuelos siguientes.
 *
 *  TIPO 2 — HUB ALTERNATIVO (ruta diferente):
 *    Si la ruta actual es directa (EBCI→SPIM), probamos añadir
 *    un aeropuerto intermedio que tenga vuelos tanto desde EBCI
 *    como hacia SPIM. Esto redistribuye carga entre vuelos.
 *    Si la ruta ya tiene escalas, probamos una escala diferente.
 *
 * Por qué muestrear en vez de evaluar todos los envíos:
 *    Con 330K envíos, evaluar la vecindad completa en cada iteración
 *    sería demasiado lento. Tomamos una muestra aleatoria de tamaño
 *    configurable (ej: 200 envíos) y exploramos sus vecinos.
 *    En cada iteración distinta se muestrea diferente, por lo que el
 *    algoritmo converge igualmente con más iteraciones.
 */
public class Vecindad {

    private final GrafoVuelos grafo;
    private final Random rng;

    // Hubs candidatos como escala: aeropuertos con muchas conexiones
    // (se calculan automáticamente del grafo en el constructor)
    private List<String> hubsPrincipales;

    public Vecindad(GrafoVuelos grafo, long semilla) {
        this.grafo = grafo;
        this.rng   = new Random(semilla);
    }

    public Vecindad(GrafoVuelos grafo) {
        this(grafo, 42L);
    }

    /**
     * Genera una lista de movimientos candidatos para la iteración actual.
     *
     * @param solucion      solución actual
     * @param todosEnvios   lista completa de envíos
     * @param tamanoMuestra cuántos envíos evaluar en esta iteración
     */
    public List<Movimiento> generarCandidatos(Solucion solucion,
                                               List<Envio> todosEnvios,
                                               int tamanoMuestra) {
        List<Movimiento> candidatos = new ArrayList<>();

        // Muestra aleatoria sin reemplazo
        List<Envio> muestra = muestrear(todosEnvios, tamanoMuestra);

        for (Envio envio : muestra) {
            Ruta rutaActual = solucion.getRuta(envio);
            if (rutaActual == null) continue;

            String claveEnvio = envio.getOrigen() + "-" + envio.getId();

            // Tipo 1: próximo vuelo en el primer tramo
            Movimiento mov1 = generarProximoVuelo(envio, rutaActual, claveEnvio);
            if (mov1 != null) candidatos.add(mov1);

            // Tipo 2: hub alternativo
            Movimiento mov2 = generarHubAlternativo(envio, rutaActual, claveEnvio);
            if (mov2 != null) candidatos.add(mov2);
        }

        return candidatos;
    }

    // -------------------------------------------------------------------------
    // Tipo 1: Próximo vuelo en el primer tramo
    // -------------------------------------------------------------------------

    /**
     * Propone usar el siguiente vuelo disponible en el primer tramo de la ruta.
     *
     * "Siguiente" significa: mismo origen→destino, pero con hora de salida
     * posterior al vuelo actual (dentro del mismo día o al día siguiente).
     */
    private Movimiento generarProximoVuelo(Envio envio, Ruta rutaActual, String claveEnvio) {
        if (rutaActual.isSinSolucion() || rutaActual.getVuelos().isEmpty()) return null;

        Vuelo primerVuelo = rutaActual.getVuelos().get(0);
        String desde = primerVuelo.getOrigen();
        String hacia = primerVuelo.getDestino();

        // Buscamos un vuelo en el mismo tramo con salida POSTERIOR al actual
        Vuelo siguiente = siguienteVueloEnTramo(
            desde, hacia,
            primerVuelo.getSalidaMinutos(),
            envio.getMinutosRegistro()
        );

        if (siguiente == null || siguiente == primerVuelo) return null;

        // Construir la nueva ruta: mismo hub sequence, primer vuelo diferente
        Ruta rutaNueva = construirRutaConPrimerVuelo(envio, rutaActual, siguiente);
        if (rutaNueva == null) return null;

        return new Movimiento(claveEnvio, rutaActual, rutaNueva);
    }

    /**
     * Busca el vuelo en el tramo [desde→hacia] cuya hora de salida
     * sea inmediatamente posterior a 'salidaActual'.
     */
    private Vuelo siguienteVueloEnTramo(String desde, String hacia,
                                         int salidaActualMin, int tiempoRegistroMin) {
        List<Vuelo> candidatos = grafo.getVuelosDesde(desde);
        Vuelo mejor = null;
        int mejorSalida = Integer.MAX_VALUE;

        for (Vuelo v : candidatos) {
            if (!v.getDestino().equals(hacia)) continue;
            if (v.getSalidaMinutos() == salidaActualMin) continue; // mismo vuelo

            // Calcular su próxima salida desde el tiempo de registro
            int salidaAbs = GrafoVuelos.proximaSalidaAbsoluta(
                tiempoRegistroMin, v.getSalidaMinutos(), 30
            );
            if (salidaAbs < mejorSalida) {
                mejorSalida = salidaAbs;
                mejor = v;
            }
        }
        return mejor;
    }

    /**
     * Reconstruye la ruta manteniendo la secuencia de aeropuertos pero
     * reemplazando solo el primer vuelo. Los vuelos subsiguientes se
     * re-asignan con el primer vuelo disponible desde la nueva hora de llegada.
     */
    private Ruta construirRutaConPrimerVuelo(Envio envio, Ruta original, Vuelo nuevoVuelo) {
        Ruta nueva = new Ruta(envio);
        nueva.agregarVuelo(nuevoVuelo);

        int salidaAbs = GrafoVuelos.proximaSalidaAbsoluta(
            envio.getMinutosRegistro(), nuevoVuelo.getSalidaMinutos(), 30
        );
        int tiempoActual = salidaAbs + nuevoVuelo.getDuracionMinutos();

        // Reasignar los tramos restantes (si los hay)
        List<Vuelo> vuelosOriginales = original.getVuelos();
        for (int i = 1; i < vuelosOriginales.size(); i++) {
            Vuelo segmento = vuelosOriginales.get(i);
            Vuelo reemplazo = grafo.primerVueloDisponible(
                segmento.getOrigen(), segmento.getDestino(), tiempoActual
            );
            if (reemplazo == null) return null;
            nueva.agregarVuelo(reemplazo);
            int sal = GrafoVuelos.proximaSalidaAbsoluta(
                tiempoActual, reemplazo.getSalidaMinutos(), 30
            );
            tiempoActual = sal + reemplazo.getDuracionMinutos();
        }
        return nueva;
    }

    // -------------------------------------------------------------------------
    // Tipo 2: Hub alternativo
    // -------------------------------------------------------------------------

    /**
     * Propone una ruta diferente a través de un aeropuerto de escala distinto.
     *
     * Para una ruta directa A→D: prueba A→X→D para un X aleatorio
     *   que tenga vuelos tanto desde A como hacia D.
     * Para una ruta con escala A→X→D: prueba con un X' diferente.
     */
    private Movimiento generarHubAlternativo(Envio envio, Ruta rutaActual, String claveEnvio) {
        String origen  = envio.getOrigen();
        String destino = envio.getDestino();

        // Hubs actuales (la escala que ya usa la ruta, si la tiene)
        Set<String> hubsActuales = new HashSet<>();
        List<Vuelo> vuelos = rutaActual.getVuelos();
        for (int i = 0; i < vuelos.size() - 1; i++) {
            hubsActuales.add(vuelos.get(i).getDestino());
        }

        // Candidatos a hub: aeropuertos con vuelos desde origen y hacia destino
        List<String> hubs = hubsValidos(origen, destino, hubsActuales);
        if (hubs.isEmpty()) return null;

        // Elegir uno al azar
        String hub = hubs.get(rng.nextInt(hubs.size()));
        int tiempoActual = envio.getMinutosRegistro();

        Vuelo v1 = grafo.primerVueloDisponible(origen, hub, tiempoActual);
        if (v1 == null) return null;

        int sal1 = GrafoVuelos.proximaSalidaAbsoluta(tiempoActual, v1.getSalidaMinutos(), 30);
        int llegada1 = sal1 + v1.getDuracionMinutos();

        Vuelo v2 = grafo.primerVueloDisponible(hub, destino, llegada1);
        if (v2 == null) return null;

        Ruta rutaNueva = new Ruta(envio);
        rutaNueva.agregarVuelo(v1);
        rutaNueva.agregarVuelo(v2);

        return new Movimiento(claveEnvio, rutaActual, rutaNueva);
    }

    /**
     * Devuelve aeropuertos X tales que existen vuelos ORIG→X y X→DEST,
     * excluyendo los hubs que la ruta actual ya usa.
     */
    private List<String> hubsValidos(String origen, String destino, Set<String> excluir) {
        // Destinos alcanzables desde el origen
        Set<String> desdeOrigen = new HashSet<>();
        for (Vuelo v : grafo.getVuelosDesde(origen)) {
            desdeOrigen.add(v.getDestino());
        }

        List<String> hubs = new ArrayList<>();
        for (String candidato : desdeOrigen) {
            if (candidato.equals(destino)) continue;
            if (candidato.equals(origen))  continue;
            if (excluir.contains(candidato)) continue;

            // Verificar que desde candidato hay vuelo hacia destino
            boolean tieneConexion = grafo.getVuelosDesde(candidato).stream()
                .anyMatch(v -> v.getDestino().equals(destino));
            if (tieneConexion) hubs.add(candidato);
        }
        return hubs;
    }

    // -------------------------------------------------------------------------
    // Utilidades
    // -------------------------------------------------------------------------

    /**
     * Muestra aleatoria sin reemplazo de tamaño min(n, lista.size()).
     *
     * Usa índices aleatorios en lugar de copiar+mezclar toda la lista.
     * Con 9M envíos esto pasa de O(9M) a O(n) por iteración del TS.
     */
    private List<Envio> muestrear(List<Envio> lista, int n) {
        int size = lista.size();
        if (n >= size) return lista;
        Set<Integer> vistos = new HashSet<>(n * 2);
        List<Envio> resultado = new ArrayList<>(n);
        while (resultado.size() < n) {
            int idx = rng.nextInt(size);
            if (vistos.add(idx)) resultado.add(lista.get(idx));
        }
        return resultado;
    }
}
