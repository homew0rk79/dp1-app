import { useNavigate } from 'react-router-dom'
import { Luggage, CheckCircle, PlaneTakeoff, AlertTriangle, PlayCircle } from 'lucide-react'

import PanelMetrica from '../../common/PanelMetrica/PanelMetrica'
import Semaforo from '../../common/Semaforo/Semaforo'
import Badge from '../../common/Badge/Badge'
import { AEROPUERTOS_MOCK } from '../../../mocks/aeropuertos'
import styles from './Sidebar.module.css'

const KPIS = [
  { icono: Luggage,      label: 'Maletas en tránsito', valor: '1,247', color: 'default' },
  { icono: CheckCircle,  label: 'Entregas a tiempo',   valor: '94.2%', color: 'verde'   },
  { icono: PlaneTakeoff, label: 'Vuelos activos',      valor: '18',    color: 'default' },
  { icono: AlertTriangle,label: 'Almacenes en alerta', valor: '3',     color: 'rojo'    },
]

function Sidebar() {
  const navigate = useNavigate()

  return (
    <aside className={styles.sidebar}>

      {/* Escenario activo */}
      <section className={styles.section}>
        <h3 className={styles.sectionTitle}>Escenario activo</h3>
        <Badge tipo="info">Sin simulación activa</Badge>
      </section>

      {/* KPIs */}
      <section className={styles.section}>
        <h3 className={styles.sectionTitle}>Métricas generales</h3>
        <div className={styles.kpiList}>
          {KPIS.map((kpi) => (
            <PanelMetrica key={kpi.label} {...kpi} />
          ))}
        </div>
      </section>

      {/* Lista de aeropuertos */}
      <section className={`${styles.section} ${styles.sectionScroll}`}>
        <h3 className={styles.sectionTitle}>Estado de aeropuertos</h3>
        <ul className={styles.aeropuertoList}>
          {AEROPUERTOS_MOCK.map((a) => (
            <li key={a.id} className={styles.aeropuertoItem}>
              <div className={styles.aeropuertoInfo}>
                <span className={styles.aeropuertoNombre}>{a.nombre}</span>
                <span className={styles.aeropuertoPais}>{a.continente}</span>
              </div>
              <div className={styles.aeropuertoOcupacion}>
                <span className={styles.ocupacionTexto}>{a.ocupacion}%</span>
                <Semaforo valor={a.ocupacion} />
              </div>
            </li>
          ))}
        </ul>
      </section>

      {/* Acción */}
      <section className={styles.sectionBottom}>
        <button
          className={styles.btnSimulacion}
          onClick={() => navigate('/simulacion')}
        >
          <PlayCircle size={16} strokeWidth={2} />
          Iniciar simulación
        </button>
      </section>

    </aside>
  )
}

export default Sidebar
