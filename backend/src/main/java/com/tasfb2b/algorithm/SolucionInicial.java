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
 *  PRIMARIO — BFS con conciencia de capacidad:
 *    Dijkstra sobre aeropuertos donde el costo = saltos + penalización por carga.
 *    Penaliza aeropuertos intermedios con >60% de ocupación, prefiriendo rutas
 *    por aeropuertos menos cargados. Al superar ~110% el desvío vale la pena.
 *
 *  FALLBACK — Dijkstra time-aware:
 *    Se activa solo cuando el BFS produce una ruta que viola el SLA del envío.
 *    Busca la ruta que minimiza el tiempo de llegada sin importar escalas.
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
     * Cada envío se rutea con conciencia de la ocupación acumulada hasta ese momento.
     */
    public Solucion construir(List<Envio> envios) {
        Solucion solucion = new Solucion(capacidadMaxAeropuertos);
        int procesados = 0;
        int sinRuta    = 0;

        for (Envio envio : envios) {
            Map<String, Integer> ocupacion = solucion.getOcupacionMaximaPorAeropuerto();
            Ruta ruta = construirRutaConOcupacion(envio, ocupacion);
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
     * API pública — BFS sin conciencia de capacidad.
     * Mantiene compatibilidad con TabuSearch y otros usos externos.
     */
    public Ruta construirRuta(Envio envio) {
        if (envio.getOrigen().equals(envio.getDestino())) return new Ruta(envio);

        Ruta rutaBFS = construirRutaBFS(envio);

        int plazo = envio.getPlazoMaximoMinutos();
        if (plazo <= 0) return rutaBFS;

        int tiempoBFS = rutaBFS.isSinSolucion() ? Integer.MAX_VALUE : rutaBFS.calcularTiempoTotal();
        if (tiempoBFS <= plazo) return rutaBFS;

        Ruta rutaDijkstra = construirRutaDijkstra(envio);
        if (rutaDijkstra.isSinSolucion()) return rutaBFS;

        int tiempoDijkstra = rutaDijkstra.calcularTiempoTotal();
        return tiempoDijkstra < tiempoBFS ? rutaDijkstra : rutaBFS;
    }

    // -------------------------------------------------------------------------
    // Construcción con conciencia de capacidad (uso interno en construir())
    // -------------------------------------------------------------------------

    private Ruta construirRutaConOcupacion(Envio envio, Map<String, Integer> ocupacion) {
        if (envio.getOrigen().equals(envio.getDestino())) return new Ruta(envio);

        Ruta rutaBFS = construirRutaBFSConCapacidad(envio, ocupacion);

        int plazo = envio.getPlazoMaximoMinutos();
        if (plazo <= 0) return rutaBFS;

        int tiempoBFS = rutaBFS.isSinSolucion() ? Integer.MAX_VALUE : rutaBFS.calcularTiempoTotal();
        if (tiempoBFS <= plazo) return rutaBFS;

        Ruta rutaDijkstra = construirRutaDijkstra(envio);
        if (rutaDijkstra.isSinSolucion()) return rutaBFS;

        int tiempoDijkstra = rutaDijkstra.calcularTiempoTotal();
        return tiempoDijkstra < tiempoBFS ? rutaDijkstra : rutaBFS;
    }

    private Ruta construirRutaBFSConCapacidad(Envio envio, Map<String, Integer> ocupacion) {
        Ruta ruta = new Ruta(envio);

        List<String> secuencia = getRutaCapacidadAware(
                envio.getOrigen(), envio.getDestino(), ocupacion);
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

    /**
     * Dijkstra sobre el grafo de aeropuertos con penalización por carga.
     * Costo por arco = 1000 (salto base) + penalidad(aeropuerto destino).
     * Penalidad crece linealmente desde 60% de ocupación:
     *   - Al 110% equivale a un salto extra → el desvío empieza a valer la pena.
     *   - Al 160% equivale a dos saltos → aeropuerto prácticamente evitado.
     */
    private List<String> getRutaCapacidadAware(String origen, String destino,
                                                Map<String, Integer> ocupacion) {
        PriorityQueue<NodoRuta> pq = new PriorityQueue<>(Comparator.comparingDouble(n -> n.costo));
        Map<String, Double> visitado = new HashMap<>();

        pq.add(new NodoRuta(0.0, origen, null));
        visitado.put(origen, 0.0);

        while (!pq.isEmpty()) {
            NodoRuta actual = pq.poll();

            if (actual.aeropuerto.equals(destino)) {
                List<String> ruta = new ArrayList<>();
                for (NodoRuta n = actual; n != null; n = n.padre) ruta.add(0, n.aeropuerto);
                return ruta;
            }

            if (actual.costo > visitado.getOrDefault(actual.aeropuerto, Double.MAX_VALUE)) continue;

            for (Vuelo v : grafo.getVuelosDesde(actual.aeropuerto)) {
                String siguiente  = v.getDestino();
                double penalidad  = calcularPenalidadCapacidad(siguiente, ocupacion);
                double nuevoCosto = actual.costo + 1000.0 + penalidad;

                if (nuevoCosto < visitado.getOrDefault(siguiente, Double.MAX_VALUE)) {
                    visitado.put(siguiente, nuevoCosto);
                    pq.add(new NodoRuta(nuevoCosto, siguiente, actual));
                }
            }
        }

        return Collections.emptyList();
    }

    private double calcularPenalidadCapacidad(String aeropuerto, Map<String, Integer> ocupacion) {
        int capMax = capacidadMaxAeropuertos.getOrDefault(aeropuerto, Integer.MAX_VALUE);
        if (capMax == Integer.MAX_VALUE || capMax == 0) return 0.0;
        int ocup = ocupacion.getOrDefault(aeropuerto, 0);
        double ratio = (double) ocup / capMax;
        if (ratio < 0.60) return 0.0;
        return (ratio - 0.60) * 2000.0;
    }

    // -------------------------------------------------------------------------
    // BFS básico (sin capacidad — para API pública / TabuSearch)
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

    // -------------------------------------------------------------------------
    // Clases internas
    // -------------------------------------------------------------------------

    private static final class NodoRuta {
        final double   costo;
        final String   aeropuerto;
        final NodoRuta padre;

        NodoRuta(double costo, String aeropuerto, NodoRuta padre) {
            this.costo      = costo;
            this.aeropuerto = aeropuerto;
            this.padre      = padre;
        }
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
