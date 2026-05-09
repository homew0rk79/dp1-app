package com.tasfb2b.algorithm;

import com.tasfb2b.model.Vuelo;

import java.util.*;

/**
 * Grafo donde los nodos son aeropuertos y las aristas son vuelos.
 *
 * Responsabilidades:
 *  1. Indexar vuelos por aeropuerto origen para búsqueda O(1).
 *  2. Precomputar con BFS la secuencia de aeropuertos de menor número
 *     de escalas entre cada par (origen, destino).
 *
 * La precomputación se hace una sola vez en el constructor sobre los
 * 30 aeropuertos del sistema (≈ 870 pares). Luego SolucionInicial la
 * consulta para cada envío sin repetir BFS.
 */
public class GrafoVuelos {

    // vuelos disponibles que parten desde cada aeropuerto
    private final Map<String, List<Vuelo>> vuelosPorOrigen;

    // rutas precomputadas: rutaCorta[origen][destino] = [A, B, C, destino]
    // incluye el origen al inicio y el destino al final
    private final Map<String, Map<String, List<String>>> rutaCorta;

    public GrafoVuelos(List<Vuelo> vuelos) {
        vuelosPorOrigen = new HashMap<>();
        for (Vuelo v : vuelos) {
            vuelosPorOrigen
                .computeIfAbsent(v.getOrigen(), k -> new ArrayList<>())
                .add(v);
        }

        // Obtener el conjunto de aeropuertos que aparecen en los vuelos
        Set<String> aeropuertos = new HashSet<>(vuelosPorOrigen.keySet());
        for (List<Vuelo> lista : vuelosPorOrigen.values()) {
            for (Vuelo v : lista) aeropuertos.add(v.getDestino());
        }

        rutaCorta = new HashMap<>();
        for (String origen : aeropuertos) {
            rutaCorta.put(origen, bfs(origen, aeropuertos));
        }

        System.out.println("[GrafoVuelos] Aeropuertos en el grafo: " + aeropuertos.size());
        System.out.println("[GrafoVuelos] Rutas precomputadas: " + contarRutas());
    }

    /**
     * BFS desde 'origen' hacia todos los demás aeropuertos.
     * Retorna un mapa: destino → secuencia completa de aeropuertos
     * [origen, escala1, ..., destino].
     *
     * Usamos BFS (no Dijkstra) porque aquí solo nos importa minimizar
     * el número de escalas, no el tiempo. El tiempo se optimiza después
     * en SolucionInicial al elegir los vuelos concretos.
     */
    private Map<String, List<String>> bfs(String origen, Set<String> todosAeropuertos) {
        Map<String, List<String>> caminos = new HashMap<>();

        // Cada entrada de la cola es el camino completo hasta ese nodo
        Queue<List<String>> cola = new LinkedList<>();
        cola.add(Collections.singletonList(origen));
        Set<String> visitados = new HashSet<>();
        visitados.add(origen);

        while (!cola.isEmpty()) {
            List<String> caminoActual = cola.poll();
            String ultimo = caminoActual.get(caminoActual.size() - 1);

            // Registrar el camino hacia 'ultimo' (si no lo teníamos ya)
            if (!ultimo.equals(origen)) {
                caminos.put(ultimo, caminoActual);
            }

            // Explorar vecinos (aeropuertos alcanzables desde 'ultimo')
            List<Vuelo> salidas = vuelosPorOrigen.getOrDefault(ultimo, Collections.emptyList());
            for (Vuelo v : salidas) {
                String siguiente = v.getDestino();
                if (!visitados.contains(siguiente)) {
                    visitados.add(siguiente);
                    List<String> nuevoCamino = new ArrayList<>(caminoActual);
                    nuevoCamino.add(siguiente);
                    cola.add(nuevoCamino);
                }
            }
        }

        return caminos;
    }

    /**
     * Devuelve la secuencia de aeropuertos (incluyendo origen y destino)
     * con menor número de escalas entre 'origen' y 'destino'.
     *
     * Retorna lista vacía si no existe ningún camino.
     */
    public List<String> getRutaCorta(String origen, String destino) {
        if (origen.equals(destino)) return Collections.singletonList(origen);
        Map<String, List<String>> desdeorigen = rutaCorta.get(origen);
        if (desdeorigen == null) return Collections.emptyList();
        return desdeorigen.getOrDefault(destino, Collections.emptyList());
    }

    /**
     * Devuelve todos los vuelos que parten desde un aeropuerto dado,
     * ordenados por hora de salida (ascendente).
     */
    public List<Vuelo> getVuelosDesde(String aeropuerto) {
        List<Vuelo> lista = vuelosPorOrigen.getOrDefault(aeropuerto, Collections.emptyList());
        // Ordenar por hora de salida para facilitar la búsqueda del primero disponible
        lista.sort(Comparator.comparingInt(Vuelo::getSalidaMinutos));
        return lista;
    }

    /**
     * Dado el tiempo actual (minutos absolutos desde medianoche del día 0)
     * y un aeropuerto de conexión, encuentra el vuelo directo hacia 'destino'
     * que sale lo antes posible respetando el margen de transbordo (30 min).
     *
     * Como los vuelos son diarios, si hoy no hay más salidas viables,
     * busca el mismo vuelo al día siguiente.
     *
     * Retorna null si no existe vuelo directo entre esos dos aeropuertos.
     */
    public Vuelo primerVueloDisponible(String desde, String hacia, int tiempoActualMinutos) {
        List<Vuelo> candidatos = vuelosPorOrigen.getOrDefault(desde, Collections.emptyList());

        int margen = 30;
        Vuelo mejor = null;
        int mejorSalida = Integer.MAX_VALUE;

        for (Vuelo v : candidatos) {
            if (!v.getDestino().equals(hacia)) continue;

            // Calcular la próxima salida posible de este vuelo (puede ser hoy o mañana)
            int salidaAbsoluta = proximaSalidaAbsoluta(tiempoActualMinutos, v.getSalidaMinutos(), margen);

            if (salidaAbsoluta < mejorSalida) {
                mejorSalida = salidaAbsoluta;
                mejor = v;
            }
        }

        return mejor;
    }

    /**
     * Calcula en qué minuto absoluto sale la próxima ocurrencia de un vuelo
     * cuya hora de salida dentro del día es 'salidaEnDia'.
     *
     * 'tiempoActual' es en minutos absolutos (puede ser > 1440 si ya pasó un día).
     * Se requiere un margen mínimo de 'margen' minutos antes de abordar.
     */
    public static int proximaSalidaAbsoluta(int tiempoActual, int salidaEnDia, int margen) {
        int dia = tiempoActual / 1440;
        int minutosDentroDelDia = tiempoActual % 1440;

        if (minutosDentroDelDia + margen <= salidaEnDia) {
            // El vuelo sale hoy y hay suficiente margen
            return dia * 1440 + salidaEnDia;
        } else {
            // El vuelo ya pasó hoy (o no hay margen): tomar el de mañana
            return (dia + 1) * 1440 + salidaEnDia;
        }
    }

    private int contarRutas() {
        return rutaCorta.values().stream().mapToInt(Map::size).sum();
    }
}
