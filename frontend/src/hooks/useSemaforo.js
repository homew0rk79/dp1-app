import { getColorSemaforo } from '../utils/semaforo'
import useConfiguracionStore from '../store/configuracionStore'

/**
 * Dado un valor numérico, retorna el color del semáforo
 * usando los rangos configurados en el sistema.
 *
 * @param {number} valor - Valor actual (ej: porcentaje de ocupación)
 * @returns {'verde' | 'ambar' | 'rojo'}
 */
function useSemaforo(valor) {
  const rangosSemaforo = useConfiguracionStore((s) => s.rangosSemaforo)
  return getColorSemaforo(valor, rangosSemaforo)
}

export default useSemaforo
