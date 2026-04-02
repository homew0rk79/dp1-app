import { Clock, CalendarDays, Zap } from 'lucide-react'
import { ESCENARIOS, ETIQUETAS_ESCENARIO } from '../../../constants/escenarios'
import styles from './SelectorEscenario.module.css'

const META = {
  [ESCENARIOS.DIA_A_DIA]: {
    icono: <Clock size={24} />,
    descripcion: 'Ejecuta las operaciones en tiempo real, sincronizado con el reloj actual del sistema.',
    detalle: 'Ideal para monitorear operaciones en curso.',
  },
  [ESCENARIOS.PERIODO]: {
    icono: <CalendarDays size={24} />,
    descripcion: 'Simula el traslado de maletas en un período de 3, 5 o 7 días comprimido en 30–90 min.',
    detalle: 'Permite evaluar el desempeño operativo a lo largo del tiempo.',
  },
  [ESCENARIOS.COLAPSO]: {
    icono: <Zap size={24} />,
    descripcion: 'Ejecuta la simulación aumentando carga hasta que las entregas incumplan los plazos.',
    detalle: 'Detecta el punto límite de capacidad de la red.',
  },
}

function SelectorEscenario({ escenarioActivo, onChange }) {
  return (
    <div className={styles.grid}>
      {Object.values(ESCENARIOS).map((escenario) => {
        const meta = META[escenario]
        const activo = escenarioActivo === escenario

        return (
          <button
            key={escenario}
            className={`${styles.tarjeta} ${activo ? styles.tarjetaActiva : ''}`}
            onClick={() => onChange && onChange(escenario)}
            type="button"
          >
            <div className={`${styles.icono} ${activo ? styles.iconoActivo : ''}`}>
              {meta.icono}
            </div>
            <div className={styles.contenido}>
              <span className={styles.nombre}>{ETIQUETAS_ESCENARIO[escenario]}</span>
              <p className={styles.descripcion}>{meta.descripcion}</p>
              <p className={styles.detalle}>{meta.detalle}</p>
            </div>
            {activo && <span className={styles.seleccionado}>Seleccionado</span>}
          </button>
        )
      })}
    </div>
  )
}

export default SelectorEscenario
