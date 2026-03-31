import { Cpu, Zap, Target } from 'lucide-react'
import GraficoBarras from '../../components/reportes/GraficoBarras/GraficoBarras'
import TablaComparativaAlgoritmos from '../../components/reportes/TablaComparativaAlgoritmos/TablaComparativaAlgoritmos'
import styles from './ReportesPage.module.css'

const MOCK_ALGORITMOS = {
  DIA_A_DIA: {
    filas: [
      {
        escenario: 'Día a día (7 días)',
        alg1_tiempo: 1240,   alg1_calidad: 87.3,  alg1_cumplimiento: 87.2,
        alg2_tiempo: 980,    alg2_calidad: 84.1,  alg2_cumplimiento: 83.9,
      },
    ],
    datosComparacion: [
      { etiqueta: 'Día a día', alg1: 87.3, alg2: 84.1 },
    ],
    datosTiempo: [
      { etiqueta: 'Día a día', alg1: 1240, alg2: 980 },
    ],
  },
  PERIODO: {
    filas: [
      {
        escenario: 'Periodo — 3 días',
        alg1_tiempo: 3100,  alg1_calidad: 88.4,  alg1_cumplimiento: 88.1,
        alg2_tiempo: 2650,  alg2_calidad: 85.2,  alg2_cumplimiento: 85.6,
      },
      {
        escenario: 'Periodo — 5 días',
        alg1_tiempo: 5480,  alg1_calidad: 87.1,  alg1_cumplimiento: 86.9,
        alg2_tiempo: 4720,  alg2_calidad: 84.8,  alg2_cumplimiento: 84.2,
      },
      {
        escenario: 'Periodo — 7 días',
        alg1_tiempo: 7920,  alg1_calidad: 85.9,  alg1_cumplimiento: 85.5,
        alg2_tiempo: 6800,  alg2_calidad: 83.4,  alg2_cumplimiento: 83.1,
      },
    ],
    datosComparacion: [
      { etiqueta: '3 días', alg1: 88.4, alg2: 85.2 },
      { etiqueta: '5 días', alg1: 87.1, alg2: 84.8 },
      { etiqueta: '7 días', alg1: 85.9, alg2: 83.4 },
    ],
    datosTiempo: [
      { etiqueta: '3 días', alg1: 3100, alg2: 2650 },
      { etiqueta: '5 días', alg1: 5480, alg2: 4720 },
      { etiqueta: '7 días', alg1: 7920, alg2: 6800 },
    ],
  },
  COLAPSO: {
    filas: [
      {
        escenario: 'Hasta colapso — iteración 1',
        alg1_tiempo: 14800, alg1_calidad: 73.2,  alg1_cumplimiento: 72.8,
        alg2_tiempo: 11900, alg2_calidad: 69.4,  alg2_cumplimiento: 68.9,
      },
      {
        escenario: 'Hasta colapso — iteración 2',
        alg1_tiempo: 16200, alg1_calidad: 70.8,  alg1_cumplimiento: 70.1,
        alg2_tiempo: 13100, alg2_calidad: 66.9,  alg2_cumplimiento: 66.4,
      },
    ],
    datosComparacion: [
      { etiqueta: 'Iter. 1', alg1: 73.2, alg2: 69.4 },
      { etiqueta: 'Iter. 2', alg1: 70.8, alg2: 66.9 },
    ],
    datosTiempo: [
      { etiqueta: 'Iter. 1', alg1: 14800, alg2: 11900 },
      { etiqueta: 'Iter. 2', alg1: 16200, alg2: 13100 },
    ],
  },
}

const SERIES_CALIDAD = [
  { clave: 'alg1', nombre: 'Algoritmo 1', color: '#2563eb' },
  { clave: 'alg2', nombre: 'Algoritmo 2', color: '#22c55e' },
]

const SERIES_TIEMPO = [
  { clave: 'alg1', nombre: 'Algoritmo 1 (ms)', color: '#2563eb' },
  { clave: 'alg2', nombre: 'Algoritmo 2 (ms)', color: '#22c55e' },
]

function MetricaResumen({ icono: Icono, label, alg1, alg2, mayor = 'alg1', unidad = '' }) {
  return (
    <div className={styles.card}>
      <div className={styles.cardTitleRow}>
        <div className={styles.cardTitleIcon}><Icono size={16} /></div>
        <div>
          <h3 className={styles.cardTitle}>{label}</h3>
        </div>
      </div>
      <div style={{ display: 'flex', gap: 24 }}>
        <div>
          <div style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', marginBottom: 4 }}>Algoritmo 1</div>
          <div style={{ fontSize: '1.4rem', fontWeight: 800, color: mayor === 'alg1' ? '#15803d' : '#0f172a' }}>
            {typeof alg1 === 'number' ? alg1.toLocaleString('es-PE') : alg1}{unidad}
          </div>
        </div>
        <div>
          <div style={{ fontSize: '0.75rem', color: 'var(--color-text-muted)', marginBottom: 4 }}>Algoritmo 2</div>
          <div style={{ fontSize: '1.4rem', fontWeight: 800, color: mayor === 'alg2' ? '#15803d' : '#0f172a' }}>
            {typeof alg2 === 'number' ? alg2.toLocaleString('es-PE') : alg2}{unidad}
          </div>
        </div>
      </div>
    </div>
  )
}

function ReporteAlgoritmos({ escenario }) {
  const datos = MOCK_ALGORITMOS[escenario] ?? MOCK_ALGORITMOS.DIA_A_DIA

  const ultimaFila = datos.filas[datos.filas.length - 1]
  const mejorCumpl = ultimaFila.alg1_cumplimiento >= ultimaFila.alg2_cumplimiento ? 'alg1' : 'alg2'
  const mejorTiempo = ultimaFila.alg1_tiempo <= ultimaFila.alg2_tiempo ? 'alg1' : 'alg2'
  const mejorCalidad = ultimaFila.alg1_calidad >= ultimaFila.alg2_calidad ? 'alg1' : 'alg2'

  return (
    <div className={styles.seccion}>
      <div className={styles.dos} style={{ gridTemplateColumns: 'repeat(3, 1fr)' }}>
        <MetricaResumen
          icono={Target}
          label="Tasa de cumplimiento"
          alg1={`${ultimaFila.alg1_cumplimiento.toFixed(1)}`}
          alg2={`${ultimaFila.alg2_cumplimiento.toFixed(1)}`}
          mayor={mejorCumpl}
          unidad="%"
        />
        <MetricaResumen
          icono={Cpu}
          label="Calidad de solución"
          alg1={`${ultimaFila.alg1_calidad.toFixed(1)}`}
          alg2={`${ultimaFila.alg2_calidad.toFixed(1)}`}
          mayor={mejorCalidad}
          unidad="%"
        />
        <MetricaResumen
          icono={Zap}
          label="Tiempo de ejecución"
          alg1={ultimaFila.alg1_tiempo}
          alg2={ultimaFila.alg2_tiempo}
          mayor={mejorTiempo}
          unidad=" ms"
        />
      </div>

      <div className={styles.dos}>
        <div className={styles.card}>
          <div className={styles.cardTitleRow}>
            <div>
              <h3 className={styles.cardTitle}>Calidad de solución por iteración</h3>
              <p className={styles.cardSubtitle}>Porcentaje de calidad de solución obtenido por cada algoritmo</p>
            </div>
          </div>
          <GraficoBarras datos={datos.datosComparacion} series={SERIES_CALIDAD} altura={260} />
        </div>

        <div className={styles.card}>
          <div className={styles.cardTitleRow}>
            <div>
              <h3 className={styles.cardTitle}>Tiempo de ejecución (ms)</h3>
              <p className={styles.cardSubtitle}>Milisegundos empleados por cada algoritmo por iteración</p>
            </div>
          </div>
          <GraficoBarras datos={datos.datosTiempo} series={SERIES_TIEMPO} altura={260} />
        </div>
      </div>

      <div className={styles.card}>
        <div className={styles.cardTitleRow}>
          <div>
            <h3 className={styles.cardTitle}>Tabla comparativa detallada</h3>
            <p className={styles.cardSubtitle}>
              Los valores resaltados en verde indican el mejor resultado en cada métrica.
              El tiempo menor es mejor; calidad y cumplimiento mayores son mejores.
            </p>
          </div>
        </div>
        <TablaComparativaAlgoritmos filas={datos.filas} />
      </div>
    </div>
  )
}

export default ReporteAlgoritmos
