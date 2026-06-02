import usePlanificadorStore from '../../store/planificadorStore'
import { simulacionService } from '../../services/simulacionService'
import styles from './AlertaColapso.module.css'

const ETIQUETAS_TIPO = {
  AEROPUERTO: 'Aeropuerto saturado',
  VUELO:      'Vuelo saturado',
  SLA:        'Plazo incumplido',
}

function AlertaColapso() {
  const colapso    = usePlanificadorStore((s) => s.colapso)
  const setColapso = usePlanificadorStore((s) => s.setColapso)

  if (!colapso) return null

  async function handleContinuar() {
    try {
      await simulacionService.continuarColapso()
      setColapso(null)
    } catch (err) {
      console.error('Error al continuar simulación:', err)
    }
  }

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
          <p className={styles.fecha}>Fecha de simulación: {colapso.fechaColapso}</p>
        </div>

        <div className={styles.acciones}>
          <button className={styles.btnContinuar} onClick={handleContinuar}>
            Continuar simulando
          </button>
          <button className={styles.btnCerrar} onClick={() => setColapso(null)}>
            Cerrar
          </button>
        </div>
      </div>
    </div>
  )
}

export default AlertaColapso
