import styles from './PanelMetrica.module.css'
import Semaforo from '../Semaforo/Semaforo'
import { getColorSemaforo, COLORES_SEMAFORO } from '../../../utils/semaforo'

// Tarjeta compacta de KPI para la sidebar.
// color: 'default' | 'verde' | 'ambar' | 'rojo'
function PanelMetrica({
  icono: Icono,
  label,
  valor,
  color = 'default',
  semaforoValor = null,
  rangosSemaforo = null,
  subtexto = null,
}) {
  const colorSem = semaforoValor != null && rangosSemaforo ? getColorSemaforo(semaforoValor, rangosSemaforo) : null
  const hexSem = colorSem ? COLORES_SEMAFORO[colorSem] : null

  return (
    <div className={`${styles.panel} ${styles[color]}`}>
      <div className={styles.iconoWrapper}>
        <Icono size={18} strokeWidth={2} />
      </div>
      <div className={styles.info}>
        <div className={styles.topRow}>
          <span className={styles.valor}>{valor}</span>
          {semaforoValor != null && (
            <div className={styles.semaforoWrap} title={`Ocupación global: ${semaforoValor.toFixed(1)}%`}>
              <span className={styles.pctBadge} style={{ color: hexSem || 'inherit' }}>
                {semaforoValor.toFixed(1)}%
              </span>
              <Semaforo valor={semaforoValor} />
            </div>
          )}
        </div>
        <span className={styles.label}>{label}</span>
        {subtexto && <span className={styles.subtexto}>{subtexto}</span>}
        {semaforoValor != null && (
          <div className={styles.barraBg}>
            <div
              className={styles.barraFill}
              style={{ width: `${Math.min(100, semaforoValor)}%`, background: hexSem || '#3b82f6' }}
            />
          </div>
        )}
      </div>
    </div>
  )
}

export default PanelMetrica
