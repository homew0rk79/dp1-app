import { ETIQUETAS_ESTADO } from '../constants/estados'

/**
 * Formatea una fecha a texto legible en español.
 * Ej: "29 mar 2026, 14:30"
 */
export function formatearFecha(date) {
  return new Date(date).toLocaleString('es-PE', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    timeZone: 'UTC',
  })
}

/**
 * Retorna el texto en español para un estado interno de maleta.
 */
export function formatearEstado(estado) {
  return ETIQUETAS_ESTADO[estado] ?? estado
}

/**
 * Formatea la ocupación de un almacén o vuelo.
 * Ej: "320 / 500 maletas"
 */
export function formatearCapacidad(actual, max) {
  return `${actual} / ${max} maletas`
}

/**
 * Formatea un porcentaje con un decimal.
 * Ej: 87.3%
 */
export function formatearPorcentaje(valor) {
  return `${valor.toFixed(1)}%`
}
