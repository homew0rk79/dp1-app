import { Play, Square } from 'lucide-react'
import styles from './PanelControlSimulacion.module.css'

function formatearTiempo(segundos) {
  const h = Math.floor(segundos / 3600)
  const m = Math.floor((segundos % 3600) / 60)
  const s = segundos % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

function PanelControlSimulacion({ estado, tiempoSegundos, onIniciar, onDetener }) {
  const esIdle = estado === 'IDLE' || estado === 'COMPLETADO' || estado === 'ERROR'
  const enCurso = estado === 'CARGANDO' || estado === 'PLANIFICANDO'

  return (
    <div className={styles.panel}>
      <div className={styles.tiempoBloque}>
        <span className={styles.tiempoLabel}>Tiempo simulado transcurrido</span>
        <span className={`${styles.tiempoValor} ${enCurso ? styles.tiempoActivo : ''}`}>
          {formatearTiempo(tiempoSegundos)}
        </span>
        <span className={`${styles.estadoBadge} ${styles[`estado--${estado}`]}`}>
          {estado === 'IDLE' && 'En espera'}
          {estado === 'CARGANDO' && 'Cargando'}
          {estado === 'PLANIFICANDO' && 'En ejecucion'}
          {estado === 'COMPLETADO' && 'Completado'}
          {estado === 'ERROR' && 'Error'}
        </span>
      </div>

      <div className={styles.controles}>
        {esIdle && (
          <button className={styles.botonIniciar} onClick={onIniciar} type="button">
            <Play size={20} />
            <span>Iniciar simulación</span>
          </button>
        )}

        {enCurso && (
          <button className={styles.botonDetener} onClick={onDetener} type="button">
            <Square size={18} />
            <span>Detener</span>
          </button>
        )}
      </div>
    </div>
  )
}

export default PanelControlSimulacion
