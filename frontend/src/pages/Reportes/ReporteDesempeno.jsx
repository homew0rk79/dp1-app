import { useMemo } from 'react'
import {
  TrendingUp,
  PackageX,
  PlaneLanding,
  CheckCircle2,
} from 'lucide-react'
import GraficoBarras from '../../components/reportes/GraficoBarras/GraficoBarras'
import GraficoLinea from '../../components/reportes/GraficoLinea/GraficoLinea'
import styles from './ReportesPage.module.css'

const MOCK_DESEMPENO = {
  DIA_A_DIA: {
    totalEnvios: 847,
    aTiempo: 739,
    demorados: 85,
    cancelaciones: 23,
    serieBarras: [
      { etiqueta: 'Lun', aTiempo: 124, demorados: 18, cancelaciones: 4 },
      { etiqueta: 'Mar', aTiempo: 138, demorados: 12, cancelaciones: 2 },
      { etiqueta: 'Mié', aTiempo: 119, demorados: 22, cancelaciones: 6 },
      { etiqueta: 'Jue', aTiempo: 143, demorados: 14, cancelaciones: 3 },
      { etiqueta: 'Vie', aTiempo: 115, demorados: 10, cancelaciones: 5 },
      { etiqueta: 'Sáb', aTiempo: 67,  demorados: 6,  cancelaciones: 2 },
      { etiqueta: 'Dom', aTiempo: 33,  demorados: 3,  cancelaciones: 1 },
    ],
    serieCumplimiento: [
      { etiqueta: 'Lun', cumplimiento: 84.4 },
      { etiqueta: 'Mar', cumplimiento: 91.9 },
      { etiqueta: 'Mié', cumplimiento: 84.4 },
      { etiqueta: 'Jue', cumplimiento: 91.1 },
      { etiqueta: 'Vie', cumplimiento: 88.5 },
      { etiqueta: 'Sáb', cumplimiento: 91.8 },
      { etiqueta: 'Dom', cumplimiento: 91.7 },
    ],
    topDemorados: [
      { id: 'TRK-2026-003', ruta: 'FRA → GRU → EZE', exceso: '4h 20min', causa: 'Cancelación de vuelo' },
      { id: 'TRK-2026-011', ruta: 'BOG → MIA',       exceso: '2h 45min', causa: 'Capacidad excedida' },
      { id: 'TRK-2026-017', ruta: 'LIM → JFK',       exceso: '1h 10min', causa: 'Replanificación tardía' },
    ],
  },
  PERIODO: {
    totalEnvios: 4230,
    aTiempo: 3712,
    demorados: 390,
    cancelaciones: 128,
    serieBarras: [
      { etiqueta: 'Día 1', aTiempo: 620, demorados: 52, cancelaciones: 18 },
      { etiqueta: 'Día 2', aTiempo: 780, demorados: 65, cancelaciones: 22 },
      { etiqueta: 'Día 3', aTiempo: 810, demorados: 78, cancelaciones: 30 },
      { etiqueta: 'Día 4', aTiempo: 740, demorados: 89, cancelaciones: 28 },
      { etiqueta: 'Día 5', aTiempo: 762, demorados: 106, cancelaciones: 30 },
    ],
    serieCumplimiento: [
      { etiqueta: 'Día 1', cumplimiento: 91.0 },
      { etiqueta: 'Día 2', cumplimiento: 89.5 },
      { etiqueta: 'Día 3', cumplimiento: 88.1 },
      { etiqueta: 'Día 4', cumplimiento: 85.6 },
      { etiqueta: 'Día 5', cumplimiento: 84.2 },
    ],
    topDemorados: [
      { id: 'TRK-2026-089', ruta: 'CDG → GRU', exceso: '6h 50min', causa: 'Colapso de almacén' },
      { id: 'TRK-2026-104', ruta: 'MAD → EZE', exceso: '5h 15min', causa: 'Cancelación de vuelo' },
      { id: 'TRK-2026-210', ruta: 'JFK → LIM', exceso: '3h 40min', causa: 'Capacidad excedida' },
    ],
  },
  COLAPSO: {
    totalEnvios: 6841,
    aTiempo: 4309,
    demorados: 2021,
    cancelaciones: 511,
    serieBarras: [
      { etiqueta: 'Día 1', aTiempo: 820, demorados: 80,  cancelaciones: 30 },
      { etiqueta: 'Día 2', aTiempo: 790, demorados: 140, cancelaciones: 50 },
      { etiqueta: 'Día 3', aTiempo: 730, demorados: 220, cancelaciones: 80 },
      { etiqueta: 'Día 4', aTiempo: 650, demorados: 310, cancelaciones: 110 },
      { etiqueta: 'Día 5', aTiempo: 530, demorados: 420, cancelaciones: 130 },
      { etiqueta: 'Día 6', aTiempo: 420, demorados: 520, cancelaciones: 70 },
      { etiqueta: 'Día 7', aTiempo: 369, demorados: 331, cancelaciones: 41 },
    ],
    serieCumplimiento: [
      { etiqueta: 'Día 1', cumplimiento: 87.2 },
      { etiqueta: 'Día 2', cumplimiento: 82.1 },
      { etiqueta: 'Día 3', cumplimiento: 74.8 },
      { etiqueta: 'Día 4', cumplimiento: 64.3 },
      { etiqueta: 'Día 5', cumplimiento: 52.1 },
      { etiqueta: 'Día 6', cumplimiento: 43.4 },
      { etiqueta: 'Día 7', cumplimiento: 40.5 },
    ],
    topDemorados: [
      { id: 'TRK-2026-312', ruta: 'BOG → MAD',       exceso: '18h 30min', causa: 'Colapso generalizado' },
      { id: 'TRK-2026-389', ruta: 'LIM → FRA → JFK', exceso: '15h 10min', causa: 'Saturación almacén' },
      { id: 'TRK-2026-401', ruta: 'EZE → GRU → CDG', exceso: '12h 45min', causa: 'Sin vuelo disponible' },
    ],
  },
}

const SERIES_BARRAS = [
  { clave: 'aTiempo',      nombre: 'A tiempo',      color: '#22c55e' },
  { clave: 'demorados',    nombre: 'Demorados',     color: '#f59e0b' },
  { clave: 'cancelaciones',nombre: 'Cancelaciones', color: '#ef4444' },
]

const SERIES_LINEA = [
  { clave: 'cumplimiento', nombre: 'Tasa de cumplimiento (%)', color: '#2563eb' },
]

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

function ReporteDesempeno({ escenario }) {
  const datos = MOCK_DESEMPENO[escenario] ?? MOCK_DESEMPENO.DIA_A_DIA
  const tasa = useMemo(
    () => ((datos.aTiempo / datos.totalEnvios) * 100).toFixed(1),
    [datos]
  )

  return (
    <div className={styles.seccion}>
      <div className={styles.kpisGrid}>
        <KpiCard
          titulo="Tasa de cumplimiento"
          valor={`${tasa}%`}
          sub="Entregas dentro del plazo"
          icono={TrendingUp}
          variante="verde"
        />
        <KpiCard
          titulo="Envíos a tiempo"
          valor={datos.aTiempo.toLocaleString('es-PE')}
          sub={`de ${datos.totalEnvios.toLocaleString('es-PE')} envíos totales`}
          icono={CheckCircle2}
          variante="azul"
        />
        <KpiCard
          titulo="Maletas demoradas"
          valor={datos.demorados.toLocaleString('es-PE')}
          sub="Excedieron el plazo comprometido"
          icono={PackageX}
          variante="ambar"
        />
        <KpiCard
          titulo="Vuelos cancelados"
          valor={datos.cancelaciones}
          sub="Tramos con cancelación registrada"
          icono={PlaneLanding}
          variante="rojo"
        />
      </div>

      <div className={styles.dos}>
        <div className={styles.card}>
          <div className={styles.cardTitleRow}>
            <div>
              <h3 className={styles.cardTitle}>Entregas por período</h3>
              <p className={styles.cardSubtitle}>Distribución diaria de envíos a tiempo, demorados y cancelaciones</p>
            </div>
          </div>
          <GraficoBarras datos={datos.serieBarras} series={SERIES_BARRAS} altura={260} />
        </div>

        <div className={styles.card}>
          <div className={styles.cardTitleRow}>
            <div>
              <h3 className={styles.cardTitle}>Evolución del cumplimiento</h3>
              <p className={styles.cardSubtitle}>Porcentaje de entregas a tiempo a lo largo del escenario</p>
            </div>
          </div>
          <GraficoLinea datos={datos.serieCumplimiento} series={SERIES_LINEA} altura={260} />
        </div>
      </div>

      <div className={styles.card}>
        <div className={styles.cardTitleRow}>
          <div>
            <h3 className={styles.cardTitle}>Envíos demorados destacados</h3>
            <p className={styles.cardSubtitle}>Casos con mayor exceso sobre el plazo comprometido</p>
          </div>
        </div>
        <div className={styles.tablaWrapper}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th>ID Envío</th>
                <th>Ruta</th>
                <th className="right">Exceso de tiempo</th>
                <th>Causa registrada</th>
              </tr>
            </thead>
            <tbody>
              {datos.topDemorados.map((item) => (
                <tr key={item.id}>
                  <td><span className={styles.badge + ' ' + styles['badge--info']}>{item.id}</span></td>
                  <td>{item.ruta}</td>
                  <td className="right"><span className={styles.badge + ' ' + styles['badge--rojo']}>{item.exceso}</span></td>
                  <td>{item.causa}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}

export default ReporteDesempeno
