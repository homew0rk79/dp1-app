import { useState, useMemo } from 'react'
import { Download, TrendingUp, Plane, Cpu, Activity } from 'lucide-react'
import { ESCENARIOS, ETIQUETAS_ESCENARIO } from '../../constants/escenarios'
import ReporteDesempeno from './ReporteDesempeno'
import ReporteOcupacion from './ReporteOcupacion'
import ReporteAlgoritmos from './ReporteAlgoritmos'
import styles from './ReportesPage.module.css'

const TABS = [
  { id: 'desempeno',  label: 'Desempeño operacional', icono: TrendingUp },
  { id: 'ocupacion',  label: 'Ocupación',              icono: Plane },
  { id: 'algoritmos', label: 'Comparativa algoritmos', icono: Cpu },
]

// KPIs globales de resumen (mock — en producción vendrían de reportesService)
const MOCK_RESUMEN = {
  [ESCENARIOS.DIA_A_DIA]: {
    totalEnvios: 847,
    cumplimiento: 87.3,
    vuelosActivos: 24,
    aeropuertos: 6,
  },
  [ESCENARIOS.PERIODO]: {
    totalEnvios: 4230,
    cumplimiento: 87.8,
    vuelosActivos: 24,
    aeropuertos: 6,
  },
  [ESCENARIOS.COLAPSO]: {
    totalEnvios: 6841,
    cumplimiento: 63.0,
    vuelosActivos: 24,
    aeropuertos: 6,
  },
}

function KpiCard({ titulo, valor, sub, icono: Icono, variante = '' }) {
  return (
    <article className={`${styles.kpiCard} ${styles[`kpiCard--${variante}`]}`}>
      <div className={styles.kpiHeader}>
        <span className={styles.kpiLabel}>{titulo}</span>
        <div className={styles.kpiIcono}><Icono size={16} /></div>
      </div>
      <div className={styles.kpiValor}>{valor}</div>
      <div className={styles.kpiSub}>{sub}</div>
    </article>
  )
}

function ReportesPage() {
  const [escenario, setEscenario] = useState(ESCENARIOS.DIA_A_DIA)
  const [tabActiva, setTabActiva] = useState('desempeno')

  const resumen = useMemo(() => MOCK_RESUMEN[escenario], [escenario])
  const cumplColor = resumen.cumplimiento >= 85 ? 'verde' : resumen.cumplimiento >= 60 ? 'ambar' : 'rojo'

  return (
    <div className={styles.page}>
      {/* Header */}
      <section className={styles.header}>
        <div className={styles.headerTextos}>
          <h1 className={styles.titulo}>Reportes operacionales</h1>
          <p className={styles.subtitulo}>
            Análisis de desempeño, ocupación y comparativa de algoritmos por escenario de simulación.
          </p>
        </div>
        <div className={styles.headerAcciones}>
          <button className={styles.botonSecundario}>
            <Download size={15} />
            Exportar PDF
          </button>
        </div>
      </section>

      {/* Selector de escenario */}
      <section className={styles.escenarioBar}>
        <span className={styles.escenarioLabel}>
          <Activity size={15} style={{ display: 'inline', marginRight: 6, verticalAlign: 'middle' }} />
          Escenario:
        </span>
        <div className={styles.escenarioBtns}>
          {Object.values(ESCENARIOS).map((esc) => (
            <button
              key={esc}
              className={`${styles.btnEscenario} ${escenario === esc ? styles['btnEscenario--activo'] : ''}`}
              onClick={() => setEscenario(esc)}
            >
              {ETIQUETAS_ESCENARIO[esc]}
            </button>
          ))}
        </div>
      </section>

      {/* KPIs globales */}
      <section className={styles.kpisGrid}>
        <KpiCard
          titulo="Total de envíos"
          valor={resumen.totalEnvios.toLocaleString('es-PE')}
          sub="Envíos registrados en el escenario"
          icono={Plane}
          variante="azul"
        />
        <KpiCard
          titulo="Cumplimiento global"
          valor={`${resumen.cumplimiento}%`}
          sub="Tasa de entregas dentro del plazo"
          icono={TrendingUp}
          variante={cumplColor}
        />
        <KpiCard
          titulo="Vuelos activos"
          valor={resumen.vuelosActivos}
          sub="Operando en el período analizado"
          icono={Plane}
        />
        <KpiCard
          titulo="Aeropuertos"
          valor={resumen.aeropuertos}
          sub="Nodos de la red activos"
          icono={Cpu}
        />
      </section>

      {/* Tabs con contenido */}
      <div className={styles.tabsWrapper}>
        <nav className={styles.tabsNav}>
          <div className={styles.tabs}>
            {TABS.map(({ id, label }) => (
              <button
                key={id}
                className={`${styles.tab} ${tabActiva === id ? styles['tab--activo'] : ''}`}
                onClick={() => setTabActiva(id)}
              >
                {label}
              </button>
            ))}
          </div>
        </nav>

        <div className={styles.tabContent}>
          {tabActiva === 'desempeno'  && <ReporteDesempeno  escenario={escenario} />}
          {tabActiva === 'ocupacion'  && <ReporteOcupacion  escenario={escenario} />}
          {tabActiva === 'algoritmos' && <ReporteAlgoritmos escenario={escenario} />}
        </div>
      </div>
    </div>
  )
}

export default ReportesPage
