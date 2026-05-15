import { useState } from 'react'
import styles from './SimulacionControles.module.css'
import { FECHA_INICIO_SIMULACION_ALGORITMO } from '../../constants/restricciones'

const PRESETS = [
  { label: '×30',  valor: 30 },
  { label: '×60',  valor: 60 },
  { label: '×120', valor: 120 },
  { label: '×360', valor: 360 },
  { label: '1d/s', valor: 1440 },
]

const [ay, am, ad] = FECHA_INICIO_SIMULACION_ALGORITMO.split('-').map(Number)
const BASE_ALGORITMO = new Date(ay, am - 1, ad)

function formatTiempo(minutos) {
  const dt = new Date(BASE_ALGORITMO.getTime() + minutos * 60000)
  const dd = String(dt.getDate()).padStart(2, '0')
  const mm = String(dt.getMonth() + 1).padStart(2, '0')
  const hh = String(dt.getHours()).padStart(2, '0')
  const mi = String(dt.getMinutes()).padStart(2, '0')
  return `${dd}/${mm}/${dt.getFullYear()} · ${hh}:${mi}`
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

  if (!manifest) return null

  const progreso = manifest.duracionTotalMinutos > 0
    ? tiempoDisplay / manifest.duracionTotalMinutos
    : 0

  function handleDuracionBlur() {
    const n = parseFloat(duracionMin)
    if (!isNaN(n) && n > 0 && manifest.duracionTotalMinutos > 0) {
      const v = manifest.duracionTotalMinutos / (n * 60)
      onVelocidad(Math.round(v))
    }
  }

  return (
    <div className={styles.panel}>
      {/* Fila superior: play + tiempo + barra */}
      <div className={styles.fila}>
        <button
          className={styles.btnPlay}
          onClick={playing ? onPause : onPlay}
          aria-label={playing ? 'Pausar' : 'Reproducir'}
        >
          {playing ? '⏸' : '▶'}
        </button>

        <span className={styles.tiempo}>{formatTiempo(tiempoDisplay)}</span>

        <input
          type="range"
          className={styles.progreso}
          min={0}
          max={manifest.duracionTotalMinutos}
          step={1}
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

      {/* Fila inferior: presets de velocidad + campo duración */}
      <div className={styles.fila}>
        <span className={styles.velocidadLabel}>Velocidad:</span>
        <div className={styles.chips}>
          {PRESETS.map((p) => (
            <button
              key={p.valor}
              className={`${styles.chip} ${velocidad === p.valor ? styles.chipActivo : ''}`}
              onClick={() => onVelocidad(p.valor)}
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
    </div>
  )
}

export default SimulacionControles
