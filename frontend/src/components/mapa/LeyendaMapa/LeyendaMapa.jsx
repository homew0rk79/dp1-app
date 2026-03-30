import { COLORES_SEMAFORO } from '../../../utils/semaforo'
import useConfiguracionStore from '../../../store/configuracionStore'
import styles from './LeyendaMapa.module.css'

function LeyendaMapa() {
  const { rangosSemaforo } = useConfiguracionStore()

  const items = [
    { color: 'verde', label: `Normal (≤ ${rangosSemaforo.verde}%)` },
    { color: 'ambar', label: `Alerta (${rangosSemaforo.verde + 1}–${rangosSemaforo.ambar}%)` },
    { color: 'rojo',  label: `Crítico (> ${rangosSemaforo.ambar}%)` },
  ]

  return (
    <div className={styles.leyenda}>
      <p className={styles.titulo}>Estado almacenes</p>
      {items.map(({ color, label }) => (
        <div key={color} className={styles.item}>
          <span
            className={styles.dot}
            style={{ background: COLORES_SEMAFORO[color] }}
          />
          <span>{label}</span>
        </div>
      ))}
      <div className={styles.divider} />
      <div className={styles.item}>
        <span className={styles.linea} />
        <span>Ruta activa</span>
      </div>
    </div>
  )
}

export default LeyendaMapa
