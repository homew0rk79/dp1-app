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
    setEstado('corriendo')
  }

  const pausar = async () => {
    await simulacionService.pausar()
    setEstado('pausado')
  }

  const detener = async () => {
    await simulacionService.detener()
    setEstado('finalizado')
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
    pausar,
    detener,
    resetear,
  }
}

export default useSimulacion
