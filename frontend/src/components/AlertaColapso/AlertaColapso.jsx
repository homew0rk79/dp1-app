import usePlanificadorStore from '../../store/planificadorStore'
import { FECHA_INICIO_SIMULACION_ALGORITMO } from '../../constants/restricciones'
import styles from './AlertaColapso.module.css'

const ETIQUETAS_TIPO = {
  AEROPUERTO: 'Aeropuerto saturado',
  VUELO:      'Vuelo saturado',
  SLA:        'Plazo incumplido',
}

function formatearMinuto(min) {
  const base  = new Date(`${FECHA_INICIO_SIMULACION_ALGORITMO.slice(0, 10)}T00:00:00Z`)
  const fecha = new Date(base.getTime() + min * 60000)
  const dd    = fecha.getUTCDate().toString().padStart(2, '0')
  const mo    = (fecha.getUTCMonth() + 1).toString().padStart(2, '0')
  const yyyy  = fecha.getUTCFullYear()
  const hh    = fecha.getUTCHours().toString().padStart(2, '0')
  const mm    = fecha.getUTCMinutes().toString().padStart(2, '0')
  return `${dd}/${mo}/${yyyy} a las ${hh}:${mm} UTC`
}

function AlertaColapso() {
  const colapso    = usePlanificadorStore((s) => s.colapso)
  const setColapso = usePlanificadorStore((s) => s.setColapso)

  if (!colapso) return null

  const tieneTimestamp = colapso.minutosColapso > 0

  return (
    <div className={styles.overlay}>
      <div className={styles.panel}>
        <div className={styles.header}>
          <span className={styles.icono}>⚡</span>
          <h2 className={styles.titulo}>Colapso detectado</h2>
        </div>

        <div className={styles.body}>
          <span className={styles.badge}>{ETIQUETAS_TIPO[colapso.tipo] ?? colapso.tipo}</span>
          <p className={styles.descripcion}>{colapso.descripcion}</p>
          {tieneTimestamp
            ? <p className={styles.timestamp}>
                Primer momento de saturación: {formatearMinuto(colapso.minutosColapso)}
              </p>
            : <p className={styles.fecha}>
                Fecha de simulación: {colapso.fechaColapso}
              </p>
          }
        </div>

        <div className={styles.acciones}>
          <button className={styles.btnCerrar} onClick={() => setColapso(null)}>
            Cerrar
          </button>
        </div>
      </div>
    </div>
  )
}

export default AlertaColapso
