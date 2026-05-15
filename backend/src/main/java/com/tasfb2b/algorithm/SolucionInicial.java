package com.tasfb2b.algorithm;

import com.tasfb2b.model.Envio;
import com.tasfb2b.model.Ruta;
import com.tasfb2b.model.Vuelo;

import java.util.*;

/**
 * Genera la solución de partida para el algoritmo Tabu Search.
 *
 * Estrategia híbrida por envío:
 *
 *  PRIMARIO — BFS + greedy:
 *    Ruta con mínimas escalas; distribuye carga naturalmente entre vuelos.
 *    Funciona bien para el ~99% de envíos donde el vuelo directo es viable.
 *
 *  FALLBACK — Dijkstra time-aware:
 *    Se activa solo cuando BFS produce una ruta que viola el SLA del envío
 *    (ej: vuelo directo con duración ~24h dentro del mismo continente).
 *    Busca la ruta que minimiza el tiempo de llegada sin importar escalas.
 *    Al aplicarse a muy pocos envíos, no genera la concentración de capacidad
 *    que causaba problemas al usar Dijkstra para todos.
 */
public class SolucionInicial {

    private final GrafoVuelos grafo;
    private final Map<String, Integer> capacidadMaxAeropuertos;

    public SolucionInicial(GrafoVuelos grafo, Map<String, Integer> capacidadMaxAeropuertos) {
        this.grafo = grafo;
        this.capacidadMaxAeropuertos = capacidadMaxAeropuertos;
    }

    public SolucionInicial(GrafoVuelos grafo) {
        this(grafo, Collections.emptyMap());
    }

    /**
     * Construye la solución inicial para todos los envíos dados.
     * Imprime progreso cada 10.000 envíos para dar visibilidad.
     */
    public Solucion construir(List<Envio> envios) {
        Solucion solucion = new Solucion(capacidadMaxAeropuertos);
        int procesados = 0;
        int sinRuta    = 0;

        for (Envio envio : envios) {
            Ruta ruta = construirRuta(envio);
            solucion.agregarRuta(ruta);

            if (ruta.isSinSolucion()) sinRuta++;

            procesados++;
            if (procesados % 10_000 == 0) {
                System.out.printf("[SolucionInicial] %d/%d procesados (sin ruta: %d)%n",
                    procesados, envios.size(), sinRuta);
            }
        }

        System.out.printf("[SolucionInicial] Completado: %d envíos, %d sin ruta%n",
            procesados, sinRuta);
        return solucion;
    }

    /**
     * Estrategia híbrida para un único envío:
     *  1. Intenta BFS+greedy.
     *  2. Si el resultado viola el SLA, prueba Dijkstra como fallback.
     *  3. Retorna la ruta con menor tiempo de entrega.
     */
    public Ruta construirRuta(Envio envio) {
        if (envio.getOrigen().equals(envio.getDestino())) return new Ruta(envio);

        Ruta rutaBFS = construirRutaBFS(envio);

        int plazo = envio.getPlazoMaximoMinutos();
        if (plazo <= 0) return rutaBFS; // sin SLA definido, BFS es suficiente

        int tiempoBFS = rutaBFS.isSinSolucion() ? Integer.MAX_VALUE : rutaBFS.calcularTiempoTotal();

        // BFS cumple el SLA: no hace falta Dijkstra
        if (tiempoBFS <= plazo) return rutaBFS;

        // BFS viola el SLA: intentar Dijkstra
        Ruta rutaDijkstra = construirRutaDijkstra(envio);

        if (rutaDijkstra.isSinSolucion()) return rutaBFS;

        int tiempoDijkstra = rutaDijkstra.calcularTiempoTotal();
        return tiempoDijkstra < tiempoBFS ? rutaDijkstra : rutaBFS;
    }

    // -------------------------------------------------------------------------
    // BFS + greedy (estrategia primaria)
    // -------------------------------------------------------------------------

    private Ruta construirRutaBFS(Envio envio) {
        Ruta ruta = new Ruta(envio);

        List<String> secuencia = grafo.getRutaCorta(envio.getOrigen(), envio.getDestino());
        if (secuencia.isEmpty()) { ruta.setSinSolucion(true); return ruta; }

        int tiempoActual = envio.getMinutosRegistro();

        for (int i = 0; i < secuencia.size() - 1; i++) {
            String desde = secuencia.get(i);
            String hacia = secuencia.get(i + 1);

            Vuelo vuelo = grafo.primerVueloDisponible(desde, hacia, tiempoActual);
            if (vuelo == null) { ruta.setSinSolucion(true); return ruta; }

            ruta.agregarVuelo(vuelo);
            int salidaAbs = GrafoVuelos.proximaSalidaAbsoluta(tiempoActual, vuelo.getSalidaMinutos(), 30);
            tiempoActual  = salidaAbs + vuelo.getDuracionMinutos();
        }

        return ruta;
    }

    // -------------------------------------------------------------------------
    // Dijkstra time-aware (fallback para envíos que BFS no puede cumplir a tiempo)
    // -------------------------------------------------------------------------

    private Ruta construirRutaDijkstra(Envio envio) {
        Ruta ruta = new Ruta(envio);

        int tiempoInicio = envio.getMinutosRegistro();
        int plazo        = envio.getPlazoMaximoMinutos();
        int limiteTiempo = tiempoInicio + Math.max(plazo > 0 ? plazo * 2 : 0, 4320);

        PriorityQueue<Estado> pq = new PriorityQueue<>(Comparator.comparingInt(e -> e.tiempo));
        pq.add(new Estado(tiempoInicio, envio.getOrigen(), null, null));

        Map<String, Integer> mejorTiempo = new HashMap<>();
        mejorTiempo.put(envio.getOrigen(), tiempoInicio);

        while (!pq.isEmpty()) {
            Estado actual = pq.poll();

            if (actual.aeropuerto.equals(envio.getDestino())) {
                List<Vuelo> vuelos = new ArrayList<>();
                for (Estado e = actual; e.vuelo != null; e = e.padre) vuelos.add(0, e.vuelo);
                vuelos.forEach(ruta::agregarVuelo);
                return ruta;
            }

            if (actual.tiempo > mejorTiempo.getOrDefault(actual.aeropuerto, Integer.MAX_VALUE)) continue;

            for (Vuelo vuelo : grafo.getVuelosDesde(actual.aeropuerto)) {
                int salidaAbs  = GrafoVuelos.proximaSalidaAbsoluta(actual.tiempo, vuelo.getSalidaMinutos(), 30);
                int llegadaAbs = salidaAbs + vuelo.getDuracionMinutos();

                if (llegadaAbs > limiteTiempo) continue;

                if (llegadaAbs < mejorTiempo.getOrDefault(vuelo.getDestino(), Integer.MAX_VALUE)) {
                    mejorTiempo.put(vuelo.getDestino(), llegadaAbs);
                    pq.add(new Estado(llegadaAbs, vuelo.getDestino(), actual, vuelo));
                }
            }
        }

        ruta.setSinSolucion(true);
        return ruta;
    }

    private static final class Estado {
        final int    tiempo;
        final String aeropuerto;
        final Estado padre;
        final Vuelo  vuelo;

        Estado(int tiempo, String aeropuerto, Estado padre, Vuelo vuelo) {
            this.tiempo     = tiempo;
            this.aeropuerto = aeropuerto;
            this.padre      = padre;
            this.vuelo      = vuelo;
        }
    }
}
