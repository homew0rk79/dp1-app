import { Cpu, Zap, Target } from 'lucide-react'
import GraficoBarras from '../../components/reportes/GraficoBarras/GraficoBarras'
import styles from './ReportesPage.module.css'

const MOCK_DESEMPENO = {
  DIA_A_DIA: {
    filas: [
      { escenario: 'Día a día (7 días)', tiempo: 1240, calidad: 87.3, cumplimiento: 87.2 },
    ],
    datosTiempo: [
      { etiqueta: 'Día a día', valor: 1240 },
    ],
    datosMetricas: [
      { etiqueta: 'Día a día', calidad: 87.3, cumplimiento: 87.2 },
    ],
  },
  PERIODO: {
    filas: [
      { escenario: 'Periodo — 3 días', tiempo: 3100, calidad: 88.4, cumplimiento: 88.1 },
      { escenario: 'Periodo — 5 días', tiempo: 5480, calidad: 87.1, cumplimiento: 86.9 },
      { escenario: 'Periodo — 7 días', tiempo: 7920, calidad: 85.9, cumplimiento: 85.5 },
    ],
    datosTiempo: [
      { etiqueta: '3 días', valor: 3100 },
      { etiqueta: '5 días', valor: 5480 },
      { etiqueta: '7 días', valor: 7920 },
    ],
    datosMetricas: [
      { etiqueta: '3 días', calidad: 88.4, cumplimiento: 88.1 },
      { etiqueta: '5 días', calidad: 87.1, cumplimiento: 86.9 },
      { etiqueta: '7 días', calidad: 85.9, cumplimiento: 85.5 },
    ],
  },
  COLAPSO: {
    filas: [
      { escenario: 'Hasta colapso — iter. 1', tiempo: 14800, calidad: 73.2, cumplimiento: 72.8 },
      { escenario: 'Hasta colapso — iter. 2', tiempo: 16200, calidad: 70.8, cumplimiento: 70.1 },
    ],
    datosTiempo: [
      { etiqueta: 'Iter. 1', valor: 14800 },
      { etiqueta: 'Iter. 2', valor: 16200 },
    ],
    datosMetricas: [
      { etiqueta: 'Iter. 1', calidad: 73.2, cumplimiento: 72.8 },
      { etiqueta: 'Iter. 2', calidad: 70.8, cumplimiento: 70.1 },
    ],
  },
}

const SERIES_METRICAS = [
  { clave: 'calidad',       nombre: 'Calidad de solución (%)',    color: '#2563eb' },
  { clave: 'cumplimiento',  nombre: 'Tasa de cumplimiento (%)',   color: '#22c55e' },
]

const SERIES_TIEMPO = [
  { clave: 'valor', nombre: 'Tiempo de ejecución (ms)', color: '#f59e0b' },
]

function promedio(filas, clave) {
  return (filas.reduce((s, f) => s + f[clave], 0) / filas.length).toFixed(1)
}

function MetricaCard({ icono: Icono, label, valor }) {
  return (
    <div className={styles.card}>
      <div className={styles.cardTitleRow}>
        <div className={styles.cardTitleIcon}><Icono size={16} /></div>
        <div><h3 className={styles.cardTitle}>{label}</h3></div>
      </div>
      <div style={{ fontSize: '1.55rem', fontWeight: 800, color: 'var(--color-text)' }}>
        {valor}
      </div>
    </div>
  )
}

function ReporteAlgoritmos({ escenario }) {
  const datos = MOCK_DESEMPENO[escenario] ?? MOCK_DESEMPENO.DIA_A_DIA
  const { filas } = datos

  const promCumplimiento = promedio(filas, 'cumplimiento')
  const promCalidad = promedio(filas, 'calidad')
  const promTiempo = Math.round(filas.reduce((s, f) => s + f.tiempo, 0) / filas.length)

  return (
    <div className={styles.seccion}>

      {/* Resumen de métricas */}
      <div className={styles.dos} style={{ gridTemplateColumns: 'repeat(3, 1fr)' }}>
        <MetricaCard icono={Target} label="Tasa de cumplimiento (prom.)" valor={`${promCumplimiento}%`} />
        <MetricaCard icono={Cpu}    label="Calidad de solución (prom.)"  valor={`${promCalidad}%`}      />
        <MetricaCard icono={Zap}    label="Tiempo de ejecución (prom.)"  valor={`${promTiempo.toLocaleString('es-PE')} ms`} />
      </div>

      {/* Gráficos */}
      <div className={styles.dos}>
        <div className={styles.card}>
          <div className={styles.cardTitleRow}>
            <div>
              <h3 className={styles.cardTitle}>Calidad de solución y cumplimiento</h3>
              <p className={styles.cardSubtitle}>
                Porcentajes obtenidos por el algoritmo planificador en cada iteración ejecutada
              </p>
            </div>
          </div>
          <GraficoBarras datos={datos.datosMetricas} series={SERIES_METRICAS} altura={260} />
        </div>

        <div className={styles.card}>
          <div className={styles.cardTitleRow}>
            <div>
              <h3 className={styles.cardTitle}>Tiempo de ejecución (ms)</h3>
              <p className={styles.cardSubtitle}>
                Milisegundos empleados por el algoritmo planificador en cada iteración ejecutada
              </p>
            </div>
          </div>
          <GraficoBarras datos={datos.datosTiempo} series={SERIES_TIEMPO} altura={260} />
        </div>
      </div>

      {/* Tabla de detalle */}
      <div className={styles.card}>
        <div className={styles.cardTitleRow}>
          <div>
            <h3 className={styles.cardTitle}>Detalle por iteración</h3>
            <p className={styles.cardSubtitle}>
              Métricas individuales del algoritmo planificador en cada ejecución registrada
            </p>
          </div>
        </div>
        <div className={styles.tablaWrapper}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th>Escenario</th>
                <th className="right">Tiempo (ms)</th>
                <th className="right">Calidad (%)</th>
                <th className="right">Cumplimiento (%)</th>
              </tr>
            </thead>
            <tbody>
              {filas.map((fila) => (
                <tr key={fila.escenario}>
                  <td>{fila.escenario}</td>
                  <td className="right">{fila.tiempo.toLocaleString('es-PE')}</td>
                  <td className="right">{fila.calidad.toFixed(1)}</td>
                  <td className="right">{fila.cumplimiento.toFixed(1)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}

export default ReporteAlgoritmos
