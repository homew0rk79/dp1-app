import { getColorSemaforo } from '../../../utils/semaforo'
import useConfiguracionStore from '../../../store/configuracionStore'
import styles from './Semaforo.module.css'

// Muestra un punto de color basado en el valor y los rangos configurados.
function Semaforo({ valor }) {
  const rangosSemaforo = useConfiguracionStore((s) => s.rangosSemaforo)
  const color = getColorSemaforo(valor, rangosSemaforo)

  return (
    <span
      className={`${styles.dot} ${styles[color]}`}
      title={`${valor}% — ${color}`}
    />
  )
}

export default Semaforo
