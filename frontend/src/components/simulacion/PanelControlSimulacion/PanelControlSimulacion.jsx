import { Play, Pause, Square } from 'lucide-react'
import styles from './PanelControlSimulacion.module.css'

function formatearTiempo(segundos) {
  const h = Math.floor(segundos / 3600)
  const m = Math.floor((segundos % 3600) / 60)
  const s = segundos % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

function PanelControlSimulacion({ estado, tiempoSegundos, onIniciar, onPausar, onDetener }) {
  const esIdle = estado === 'idle' || estado === 'finalizado'
  const corriendo = estado === 'corriendo'
  const pausado = estado === 'pausado'

  return (
    <div className={styles.panel}>
      <div className={styles.tiempoBloque}>
        <span className={styles.tiempoLabel}>Tiempo simulado transcurrido</span>
        <span className={`${styles.tiempoValor} ${corriendo ? styles.tiempoActivo : ''}`}>
          {formatearTiempo(tiempoSegundos)}
        </span>
        <span className={`${styles.estadoBadge} ${styles[`estado--${estado}`]}`}>
          {estado === 'idle' && 'En espera'}
          {estado === 'corriendo' && 'En ejecución'}
          {estado === 'pausado' && 'Pausado'}
          {estado === 'finalizado' && 'Finalizado'}
        </span>
      </div>

      <div className={styles.controles}>
        {esIdle && (
          <button className={styles.botonIniciar} onClick={onIniciar} type="button">
            <Play size={20} />
            <span>Iniciar simulación</span>
          </button>
        )}

        {corriendo && (
          <>
            <button className={styles.botonPausar} onClick={onPausar} type="button">
              <Pause size={18} />
              <span>Pausar</span>
            </button>
            <button className={styles.botonDetener} onClick={onDetener} type="button">
              <Square size={18} />
              <span>Detener</span>
            </button>
          </>
        )}

        {pausado && (
          <>
            <button className={styles.botonIniciar} onClick={onIniciar} type="button">
              <Play size={20} />
              <span>Reanudar</span>
            </button>
            <button className={styles.botonDetener} onClick={onDetener} type="button">
              <Square size={18} />
              <span>Detener</span>
            </button>
          </>
        )}
      </div>
    </div>
  )
}

export default PanelControlSimulacion
