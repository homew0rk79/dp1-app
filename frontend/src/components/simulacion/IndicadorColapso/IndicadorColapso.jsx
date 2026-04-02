import { AlertTriangle } from 'lucide-react'
import styles from './IndicadorColapso.module.css'

function IndicadorColapso({ tiempoSegundos }) {
  function formatearTiempo(seg) {
    const h = Math.floor(seg / 3600)
    const m = Math.floor((seg % 3600) / 60)
    const s = seg % 60
    return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
  }

  return (
    <div className={styles.alerta}>
      <div className={styles.icono}>
        <AlertTriangle size={28} />
      </div>
      <div className={styles.contenido}>
        <span className={styles.titulo}>Colapso operativo detectado</span>
        <p className={styles.descripcion}>
          Las operaciones han superado la capacidad de la red. Los envíos incumplen
          sistemáticamente los plazos comprometidos.
        </p>
        {tiempoSegundos !== undefined && (
          <span className={styles.tiempo}>
            Tiempo transcurrido al colapso: <strong>{formatearTiempo(tiempoSegundos)}</strong>
          </span>
        )}
      </div>
    </div>
  )
}

export default IndicadorColapso
