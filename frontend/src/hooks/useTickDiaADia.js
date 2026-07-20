import { useEffect, useState } from 'react'
import useSimulacionStore from '../store/simulacionStore'

/**
 * Countdown del ciclo de replanificación del escenario día a día.
 *
 * Usa el timestamp del último tick (updateManifest → ultimoTickTs) y el
 * intervalo publicado en el manifest (tickIntervaloSegundos) para estimar
 * cuánto falta para la próxima replanificación.
 *
 * @returns {{ activo: boolean, segundosRestantes: number, replanificando: boolean }}
 *   activo=false cuando no aplica (otro escenario o sin manifest).
 *   replanificando=true cuando el countdown llegó a 0 y aún no llega el snapshot.
 */
function useTickDiaADia() {
  const escenarioActivo = useSimulacionStore((s) => s.escenarioActivo)
  const manifest        = useSimulacionStore((s) => s.manifest)
  const ultimoTickTs    = useSimulacionStore((s) => s.ultimoTickTs)

  const [ahora, setAhora] = useState(Date.now())

  const intervaloSeg = manifest?.tickIntervaloSegundos > 0
    ? manifest.tickIntervaloSegundos
    : null
  const activo = escenarioActivo === 'DIA_A_DIA' && Boolean(manifest) && intervaloSeg !== null

  useEffect(() => {
    if (!activo) return
    const id = setInterval(() => setAhora(Date.now()), 1000)
    return () => clearInterval(id)
  }, [activo])

  if (!activo) {
    return { activo: false, segundosRestantes: 0, replanificando: false }
  }

  // Sin tick registrado aún (primer manifest recién cargado): contar desde ahora.
  const base = ultimoTickTs ?? ahora
  const transcurrido = Math.floor((ahora - base) / 1000)
  const segundosRestantes = Math.max(0, intervaloSeg - transcurrido)
  const replanificando = segundosRestantes === 0

  return { activo: true, segundosRestantes, replanificando }
}

export default useTickDiaADia
