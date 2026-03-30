/**
 * Dado un valor numérico y los rangos configurados, retorna el color del semáforo.
 * Los rangos vienen de configuracionStore o de la API.
 *
 * @param {number} valor - Valor actual (ej: ocupación del almacén)
 * @param {{ verde: number, ambar: number }} rangos
 *   - verde: hasta este porcentaje (inclusive) → verde
 *   - ambar: hasta este porcentaje (inclusive) → ámbar
 *   - por encima de ambar → rojo
 * @returns {'verde' | 'ambar' | 'rojo'}
 */
export function getColorSemaforo(valor, rangos) {
  if (valor <= rangos.verde) return 'verde'
  if (valor <= rangos.ambar) return 'ambar'
  return 'rojo'
}

export const COLORES_SEMAFORO = {
  verde: '#4caf50',
  ambar: '#ff9800',
  rojo: '#f44336',
}
