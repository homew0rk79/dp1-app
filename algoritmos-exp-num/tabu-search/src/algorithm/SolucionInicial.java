package algorithm;

import model.Envio;
import model.Ruta;
import model.Vuelo;

import java.util.List;

/**
 * Genera la solución de partida para el algoritmo Tabu Search.
 *
 * Estrategia en dos fases por envío:
 *
 *  FASE 1 — Secuencia de aeropuertos (BFS precomputado en GrafoVuelos):
 *    Obtiene la ruta con menor número de escalas entre origen y destino.
 *    Ejemplo: EBCI → SKBO → SPIM  (2 tramos, 1 escala)
 *
 *  FASE 2 — Asignación de vuelos concretos (greedy por tiempo):
 *    Para cada tramo [A → B] de la secuencia, elige el vuelo que sale
 *    lo antes posible después de la llegada al aeropuerto A,
 *    respetando un margen mínimo de 30 min para el transbordo.
 *    Como los vuelos son diarios, si hoy no hay más salidas viables
 *    usa la del día siguiente.
 *
 * Por qué esta estrategia:
 *  - Con 330K+ envíos, un Dijkstra completo por envío sería muy lento.
 *  - La precomputación BFS (una sola vez para 870 pares) amortiza el costo.
 *  - La solución inicial no necesita ser óptima, solo válida y razonable.
 *    El Tabu Search se encargará de mejorarla.
 */
public class SolucionInicial {

    private final GrafoVuelos grafo;

    public SolucionInicial(GrafoVuelos grafo) {
        this.grafo = grafo;
    }

    /**
     * Construye la solución inicial para todos los envíos dados.
     * Imprime progreso cada 10.000 envíos para dar visibilidad.
     */
    public Solucion construir(List<Envio> envios) {
        Solucion solucion = new Solucion();
        int procesados = 0;
        int sinRuta = 0;

        for (Envio envio : envios) {
            Ruta ruta = construirRuta(envio);
            solucion.agregarRuta(ruta);

            if (ruta.isSinSolucion()) sinRuta++;

            procesados++;
            if (procesados % 10_000 == 0) {
                System.out.printf("[SolucionInicial] %d/%d envíos procesados (sin ruta: %d)%n",
                    procesados, envios.size(), sinRuta);
            }
        }

        System.out.printf("[SolucionInicial] Completado: %d envíos, %d sin ruta%n",
            procesados, sinRuta);
        return solucion;
    }

    /**
     * Construye la ruta para un único envío.
     *
     * Pasos:
     *  1. Consultar la secuencia de aeropuertos (BFS precomputado).
     *  2. Para cada tramo, pedir a GrafoVuelos el primer vuelo disponible.
     *  3. Acumular el tiempo de llegada para el siguiente tramo.
     */
    public Ruta construirRuta(Envio envio) {
        Ruta ruta = new Ruta(envio);

        // Caso trivial: origen = destino
        if (envio.getOrigen().equals(envio.getDestino())) {
            return ruta;
        }

        // FASE 1: secuencia de aeropuertos con menor número de escalas
        List<String> secuencia = grafo.getRutaCorta(envio.getOrigen(), envio.getDestino());

        if (secuencia.isEmpty()) {
            // No existe ningún camino en el grafo entre estos aeropuertos
            ruta.setSinSolucion(true);
            return ruta;
        }

        // FASE 2: asignar el primer vuelo disponible en cada tramo
        // Tiempo inicial = minutos desde medianoche del día de registro
        int tiempoActual = envio.getMinutosRegistro();

        for (int i = 0; i < secuencia.size() - 1; i++) {
            String desde = secuencia.get(i);
            String hacia = secuencia.get(i + 1);

            Vuelo vuelo = grafo.primerVueloDisponible(desde, hacia, tiempoActual);

            if (vuelo == null) {
                // No hay vuelo directo en este tramo (no debería pasar si BFS
                // fue correcto, pero lo manejamos defensivamente)
                ruta.setSinSolucion(true);
                return ruta;
            }

            ruta.agregarVuelo(vuelo);

            // El tiempo de llegada al siguiente aeropuerto se calcula así:
            //   salidaAbsoluta = primer momento en que el vuelo sale después de tiempoActual+30min
            //   llegadaAbsoluta = salidaAbsoluta + duracion del vuelo
            int salidaAbsoluta = GrafoVuelos.proximaSalidaAbsoluta(
                tiempoActual, vuelo.getSalidaMinutos(), 30
            );
            tiempoActual = salidaAbsoluta + vuelo.getDuracionMinutos();
        }

        return ruta;
    }
}
