package com.tasfb2b.algorithm;

import com.tasfb2b.model.Ruta;
import com.tasfb2b.model.Vuelo;

import java.util.stream.Collectors;

/**
 * Representa un movimiento en el espacio de soluciones del Tabu Search.
 *
 * Un movimiento = asignar una nueva ruta a un envío específico.
 *
 * Atributos:
 *  - claveEnvio    : identifica el envío afectado (origen + "-" + id)
 *  - rutaAnterior  : ruta que el envío tenía antes (para calcular delta y comparar)
 *  - rutaNueva     : ruta propuesta por el movimiento
 *  - deltaCosto    : cambio en la función objetivo (negativo = mejora)
 *  - claveTabu     : lo que entra a la lista tabú; impide volver a la ruta anterior
 *                    durante el período de tenencia
 *
 * La claveTabu usa la FIRMA DE LA RUTA ANTERIOR (secuencia de aeropuertos).
 * Así, si aplicamos EBCI→SKBO→SPIM y esta queda tabú, no se puede volver a
 * asignarla al mismo envío hasta que expire. Pero sí se pueden probar otras
 * rutas alternativas para ese mismo envío.
 */
public class Movimiento {

    private final String claveEnvio;
    private final Ruta   rutaAnterior;
    private final Ruta   rutaNueva;
    private final double deltaCosto;
    private final String claveTabu;

    public Movimiento(String claveEnvio, Ruta rutaAnterior, Ruta rutaNueva) {
        this.claveEnvio    = claveEnvio;
        this.rutaAnterior  = rutaAnterior;
        this.rutaNueva     = rutaNueva;
        this.deltaCosto    = calcularDelta();
        this.claveTabu     = claveEnvio + ":" + firmaRuta(rutaAnterior);
    }

    /**
     * Diferencia de costo entre la ruta nueva y la anterior.
     * Incluye penalización por plazo de entrega (igual que Solucion.costoDe).
     * No incluye cambios en capacidad de vuelos/aeropuertos (dependen del estado
     * global de la solución y se reflejan en Solucion.getCostoTotal()).
     * Negativo significa mejora.
     */
    private double calcularDelta() {
        return costoDe(rutaNueva) - costoDe(rutaAnterior);
    }

    private static double costoDe(Ruta r) {
        if (r.isSinSolucion()) return 100_000.0;
        int t = r.calcularTiempoTotal();
        if (t == Integer.MAX_VALUE) return 100_000.0;
        double costo = t;
        int plazo = r.getEnvio().getPlazoMaximoMinutos();
        if (plazo > 0 && t > plazo) {
            costo += 1_000.0 * (t - plazo);
        }
        return costo;
    }

    /**
     * Firma de una ruta: secuencia de aeropuertos separados por guiones.
     * Ejemplos: "EBCI-SPIM" (directo), "EBCI-SKBO-SPIM" (1 escala).
     * Se usa como parte de la clave tabú.
     */
    public static String firmaRuta(Ruta ruta) {
        if (ruta == null || ruta.isSinSolucion() || ruta.getVuelos().isEmpty()) {
            return "SIN_RUTA";
        }
        String first = ruta.getVuelos().get(0).getOrigen();
        String hops  = ruta.getVuelos().stream()
            .map(Vuelo::getDestino)
            .collect(Collectors.joining("-"));
        return first + "-" + hops;
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public String  getClaveEnvio()   { return claveEnvio; }
    public Ruta    getRutaAnterior() { return rutaAnterior; }
    public Ruta    getRutaNueva()    { return rutaNueva; }
    public double  getDeltaCosto()   { return deltaCosto; }
    public String  getClaveTabu()    { return claveTabu; }

    @Override
    public String toString() {
        return String.format("Mov[%s] %s -> %s (delta=%.0f)",
            claveEnvio,
            firmaRuta(rutaAnterior),
            firmaRuta(rutaNueva),
            deltaCosto);
    }
}
