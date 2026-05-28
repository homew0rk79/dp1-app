import useSimulacionStore from '../store/simulacionStore'
import { simulacionService } from '../services/simulacionService'

/**
 * Controla el estado y las acciones de la simulación activa.
 */
function useSimulacion() {
  const {
    escenarioActivo,
    estadoEjecucion,
    colapsoDetectado,
    parametros,
    setEscenario,
    setEstado,
    setColapso,
    setParametros,
    resetear,
  } = useSimulacionStore()

  const iniciar = async () => {
    await simulacionService.iniciar({
      escenario: escenarioActivo,
      ...parametros,
    })
    setEstado('CARGANDO')
  }

  return {
    escenarioActivo,
    estadoEjecucion,
    colapsoDetectado,
    parametros,
    setEscenario,
    setParametros,
    setColapso,
    iniciar,
    resetear,
  }
}

export default useSimulacion
