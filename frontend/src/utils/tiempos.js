import {
  PLAZO_MISMO_CONTINENTE,
  PLAZO_DISTINTO_CONTINENTE,
} from '../constants/restricciones'

/**
 * Retorna el plazo máximo de entrega en días según si origen y destino
 * están en el mismo continente.
 */
export function calcularPlazoMaximo(mismokontinente) {
  return mismokontinente ? PLAZO_MISMO_CONTINENTE : PLAZO_DISTINTO_CONTINENTE
}

/**
 * Retorna true si el envío aún está dentro del plazo.
 * @param {string|Date} fechaIngreso
 * @param {number} plazoEnDias
 */
export function estaEnPlazo(fechaIngreso, plazoEnDias) {
  const ingreso = new Date(fechaIngreso)
  const limite = new Date(ingreso)
  limite.setDate(limite.getDate() + plazoEnDias)
  return new Date() <= limite
}
