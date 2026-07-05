import { useState, useEffect } from 'react'
import styles from './SimulacionControles.module.css'
import useSimulacionStore from '../../store/simulacionStore'
import { formatearFechaHora, formatearDuracion, sumarMinutos } from '../../utils/tiempos'
import { TA_EJECUCION_ALGORITMO_MIN, FECHA_INICIO_SIMULACION_ALGORITMO } from '../../constants/restricciones'

const PRESETS = [
  { label: 'x30', valor: 30 },
  { label: 'x60', valor: 60 },
  { label: 'x120', valor: 120 },
  { label: 'x360', valor: 360 },
  { label: '1d/s', valor: 1440 },
]

function formatTiempo(minutos, duracionTotalMinutos, fechaInicioStr) {
  const limite = Number.isFinite(duracionTotalMinutos) && duracionTotalMinutos > 0
    ? Math.max(0, duracionTotalMinutos % 1440 === 0
      ? duracionTotalMinutos - 1
      : duracionTotalMinutos)
    : minutos
  const tiempo = Math.max(0, Math.min(Math.floor(minutos), limite))
  const dia = Math.floor(tiempo / 1440) + 1
  const hh = String(Math.floor((tiempo % 1440) / 60)).padStart(2, '0')
  const mm = String(Math.floor(tiempo % 60)).padStart(2, '0')
  const fecha = sumarMinutos(fechaInicioStr, tiempo)
  const fechaStr = fecha ? ` (${formatearFechaHora(fecha).slice(0, 16)})` : ''

  return `Dia ${dia}${fechaStr} - ${hh}:${mm}`
}

function SimulacionControles({
  manifest,
  tiempoDisplay,
  playing,
  velocidad,
  onPlay,
  onPause,
  onSeek,
  onVelocidad,
}) {
  const [duracionMin, setDuracionMin] = useState('')
  const [horaReal, setHoraReal] = useState(() => new Date())
  const tiempoSegundos = useSimulacionStore((s) => s.tiempoSegundos)
  const fechaInicioParam = useSimulacionStore((s) => s.parametros.fechaInicio)
  const escenarioActivo = useSimulacionStore((s) => s.escenarioActivo)
  const fechaInicio = manifest?.fechaInicioMinutos > 0
    ? sumarMinutos(FECHA_INICIO_SIMULACION_ALGORITMO, manifest.fechaInicioMinutos)?.toISOString()
    : fechaInicioParam

  useEffect(() => {
    const interval = setInterval(() => {
      setHoraReal(new Date())
    }, 1000)
    return () => clearInterval(interval)
  }, [])

  if (!manifest) return null

  const simTotMin = Math.max(0, Math.floor(tiempoDisplay || 0))
  const simD = Math.floor(simTotMin / 1440)
  const simH = Math.floor((simTotMin % 1440) / 60)
  const simM = simTotMin % 60
  const transcurridoSim = `${simD}d ${String(simH).padStart(2, '0')}h ${String(simM).padStart(2, '0')}m`

  /**
   * En "día a día" la velocidad queda fija en 60× (1 s real = 1 min simulado).
   * Se oculta el selector de presets y el input "completar en X min reales".
   */
  const velocidadFija = escenarioActivo === 'DIA_A_DIA'

  function handleDuracionBlur() {
    const n = parseFloat(duracionMin)
    if (!Number.isNaN(n) && n > 0 && manifest.duracionTotalMinutos > 0) {
      const v = manifest.duracionTotalMinutos / (n * 60)
      onVelocidad(Math.round(v))
    }
  }

  return (
    <div className={styles.panel}>
      {/* 4 Tiempos de Simulación / Sistema (#37, #38, #39) */}
      <div className={styles.gridRelojes}>
        <div className={styles.relojItem}>
          <span className={styles.relojLabel}>🕒 Real Actual:</span>
          <span className={styles.relojValor}>{formatearFechaHora(horaReal)}</span>
        </div>
        <div className={styles.relojItem}>
          <span className={styles.relojLabel}>⏱️ Ejecución Real:</span>
          <span className={styles.relojValor}>{formatearDuracion(tiempoSegundos || 0)}</span>
        </div>
        <div className={styles.relojItem}>
          <span className={styles.relojLabel}>📅 Momento Simulado:</span>
          <span className={styles.relojValorResaltado}>
            {formatTiempo(tiempoDisplay, manifest.duracionTotalMinutos, fechaInicio)}
          </span>
        </div>
        <div className={styles.relojItem}>
          <span className={styles.relojLabel}>⏳ Transcurrido Sim.:</span>
          <span className={styles.relojValorResaltado}>{transcurridoSim}</span>
        </div>
      </div>

      <div className={styles.fila}>
        <button
          className={styles.btnPlay}
          onClick={playing ? onPause : onPlay}
          aria-label={playing ? 'Pausar' : 'Reproducir'}
        >
          {playing ? '||' : '>'}
        </button>

        <span className={styles.tiempo}>
          {formatTiempo(tiempoDisplay, manifest.duracionTotalMinutos, fechaInicio)}
        </span>

        <input
          type="range"
          className={styles.progreso}
          min={0}
          max={manifest.duracionTotalMinutos}
          step={TA_EJECUCION_ALGORITMO_MIN}
          value={Math.floor(tiempoDisplay)}
          onChange={(e) => onSeek(Number(e.target.value))}
        />

        <span className={styles.velocidadLabel}>
          {velocidad >= 1440
            ? `${Math.round(velocidad / 1440)}d/s`
            : velocidad >= 60
            ? `${Math.round(velocidad / 60)}h/s`
            : `${velocidad}m/s`}
        </span>
      </div>

      {velocidadFija ? (
        <div className={styles.fila}>
          <span className={styles.velocidadLabel}>
            Velocidad fija: 1 s real = 1 min simulado (60x) · día a día
          </span>
        </div>
      ) : (
        <div className={styles.fila}>
          <span className={styles.velocidadLabel}>Velocidad:</span>
          <div className={styles.chips}>
            {PRESETS.map((p) => (
              <button
                key={p.valor}
                className={`${styles.chip} ${velocidad === p.valor ? styles.chipActivo : ''}`}
                onClick={() => onVelocidad(p.valor)}
                type="button"
              >
                {p.label}
              </button>
            ))}
          </div>

          <span className={styles.separador}>|</span>
          <span className={styles.velocidadLabel}>Completar en</span>
          <input
            type="number"
            className={styles.inputDuracion}
            placeholder="min"
            min={1}
            value={duracionMin}
            onChange={(e) => setDuracionMin(e.target.value)}
            onBlur={handleDuracionBlur}
            onKeyDown={(e) => e.key === 'Enter' && handleDuracionBlur()}
          />
          <span className={styles.velocidadLabel}>min reales</span>
        </div>
      )}
    </div>
  )
}

export default SimulacionControles
