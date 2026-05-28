import usePlanificadorStore from '../store/planificadorStore'

function usePlanificadorWS() {
  return {
    conectado: usePlanificadorStore((s) => s.conectado),
    progreso: usePlanificadorStore((s) => s.progreso),
    snapshot: usePlanificadorStore((s) => s.snapshot),
    completado: usePlanificadorStore((s) => s.completado),
    error: usePlanificadorStore((s) => s.error),
  }
}

export default usePlanificadorWS
