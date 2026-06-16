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
 *  TIPO 3 — SALIDA TEMPRANA (aeropuerto saturado):
 *    Solo actúa cuando el origen del envío está sobre el umbral del 80%.
 *    Busca el vuelo saliente más temprano desde ese aeropuerto (a cualquier
 *    destino), y desde ahí la primera conexión disponible al destino final.
 *    El objetivo es sacar las maletas del almacén saturado lo antes posible
 *    sin violar el plazo de entrega (cumplePlazoMaximo sigue siendo el filtro).
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

    private List<String> hubsPrincipales;

    // Contadores acumulados entre todas las iteraciones del Tabu Search
    private int generados1 = 0;
    private int generados2 = 0;
    private int generados3 = 0;

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

        // Muestra sesgada: 60% desde aeropuertos sobrecargados, 40% aleatorio
        Set<String> saturados = solucion.getAeropuertosSobreCarga(0.80);
        List<Envio> muestra = saturados.isEmpty()
            ? muestrear(todosEnvios, tamanoMuestra)
            : muestrearConSesgo(todosEnvios, tamanoMuestra, solucion, saturados);

        for (Envio envio : muestra) {
            Ruta rutaActual = solucion.getRuta(envio);
            if (rutaActual == null) continue;

            String claveEnvio = envio.getOrigen() + "-" + envio.getId();

            // Tipo 1: próximo vuelo en el primer tramo
            Movimiento mov1 = generarProximoVuelo(envio, rutaActual, claveEnvio);
            if (mov1 != null) { candidatos.add(mov1); generados1++; }

            // Tipo 2: hub alternativo
            Movimiento mov2 = generarHubAlternativo(envio, rutaActual, claveEnvio);
            if (mov2 != null) { candidatos.add(mov2); generados2++; }

            // Tipo 3: salida temprana desde aeropuerto saturado
            if (!saturados.isEmpty()) {
                Movimiento mov3 = generarSalidaTemprana(envio, rutaActual, claveEnvio, saturados);
                if (mov3 != null) { candidatos.add(mov3); generados3++; }
            }
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
        if (!rutaNueva.cumplePlazoMaximo()) return null;

        return new Movimiento(claveEnvio, rutaActual, rutaNueva, 1);
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
        if (!rutaNueva.cumplePlazoMaximo()) return null;

        return new Movimiento(claveEnvio, rutaActual, rutaNueva, 2);
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
    // Tipo 3: Salida temprana desde aeropuerto saturado
    // -------------------------------------------------------------------------

    /**
     * Solo actúa cuando el origen del envío está saturado (≥80% capacidad).
     * Busca el vuelo saliente más temprano desde ese aeropuerto —a cualquier
     * destino— que parta antes que el vuelo actual, y desde ahí la primera
     * conexión disponible al destino final. Garantiza cumplePlazoMaximo().
     */
    private Movimiento generarSalidaTemprana(Envio envio, Ruta rutaActual,
                                              String claveEnvio, Set<String> saturados) {
        String origen = envio.getOrigen();
        if (!saturados.contains(origen)) return null;
        if (rutaActual.isSinSolucion() || rutaActual.getVuelos().isEmpty()) return null;

        String destino = envio.getDestino();
        int tiempoActual = envio.getMinutosRegistro();

        Vuelo vueloActual = rutaActual.getVuelos().get(0);
        int salidaActualAbs = GrafoVuelos.proximaSalidaAbsoluta(
            tiempoActual, vueloActual.getSalidaMinutos(), 30);

        // Vuelo saliente más temprano que parta ANTES que el vuelo actual
        Vuelo mejorPrimero = null;
        int mejorSalidaAbs = salidaActualAbs; // umbral: solo interesa algo más temprano

        for (Vuelo v : grafo.getVuelosDesde(origen)) {
            int salidaAbs = GrafoVuelos.proximaSalidaAbsoluta(tiempoActual, v.getSalidaMinutos(), 30);
            if (salidaAbs >= mejorSalidaAbs) continue;
            mejorSalidaAbs = salidaAbs;
            mejorPrimero = v;
        }

        if (mejorPrimero == null) return null;

        Ruta rutaNueva = new Ruta(envio);
        rutaNueva.agregarVuelo(mejorPrimero);
        int llegada = mejorSalidaAbs + mejorPrimero.getDuracionMinutos();

        if (!mejorPrimero.getDestino().equals(destino)) {
            Vuelo conexion = grafo.primerVueloDisponible(mejorPrimero.getDestino(), destino, llegada);
            if (conexion == null) return null;
            rutaNueva.agregarVuelo(conexion);
        }

        if (!rutaNueva.cumplePlazoMaximo()) return null;

        return new Movimiento(claveEnvio, rutaActual, rutaNueva, 3);
    }

    public String getResumenGenerados() {
        return String.format("T1: %,d  T2: %,d  T3: %,d", generados1, generados2, generados3);
    }

    // -------------------------------------------------------------------------
    // Utilidades
    // -------------------------------------------------------------------------

    /**
     * Muestra sesgada: nPrioritario envíos de aeropuertos sobrecargados + resto aleatorio.
     * Garantiza que TabuSearch atienda la redistribución de carga en cada iteración.
     */
    private List<Envio> muestrearConSesgo(List<Envio> lista, int n,
                                           Solucion solucion,
                                           Set<String> saturados) {
        int size = lista.size();
        if (n >= size) return lista;

        // Índices de envíos que pasan por aeropuertos saturados
        List<Integer> idxSaturados = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Ruta ruta = solucion.getRuta(lista.get(i));
            if (ruta == null || ruta.isSinSolucion() || ruta.getVuelos() == null) continue;
            for (Vuelo v : ruta.getVuelos()) {
                if (saturados.contains(v.getOrigen())) {
                    idxSaturados.add(i);
                    break;
                }
            }
        }

        int nPrioritario = Math.min(n * 6 / 10, idxSaturados.size());
        int nAleatorio   = n - nPrioritario;

        Set<Integer> seleccionados = new LinkedHashSet<>(n * 2);

        // Prioritarios: shuffle y tomar los primeros nPrioritario
        Collections.shuffle(idxSaturados, rng);
        for (int i = 0; i < nPrioritario; i++) {
            seleccionados.add(idxSaturados.get(i));
        }

        // Aleatorios para completar la muestra
        int intentos = 0;
        while (seleccionados.size() < n && intentos < n * 5) {
            seleccionados.add(rng.nextInt(size));
            intentos++;
        }

        List<Envio> resultado = new ArrayList<>(seleccionados.size());
        for (int idx : seleccionados) resultado.add(lista.get(idx));
        return resultado;
    }

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
