import { Plane, Warehouse } from 'lucide-react'
import GraficoBarras from '../../components/reportes/GraficoBarras/GraficoBarras'
import styles from './ReportesPage.module.css'
import { getColorSemaforo } from '../../utils/semaforo'
import useConfiguracionStore from '../../store/configuracionStore'

const MOCK_OCUPACION = {
  DIA_A_DIA: {
    vuelos: [
      { id: 'LA801', ruta: 'LIM → BOG', tipo: 'Continental',       capacidad: 250, ocupacion: 198 },
      { id: 'LA802', ruta: 'BOG → MAD', tipo: 'Intercontinental',  capacidad: 400, ocupacion: 380 },
      { id: 'IB310', ruta: 'MAD → FRA', tipo: 'Continental',       capacidad: 250, ocupacion: 142 },
      { id: 'LH720', ruta: 'FRA → GRU', tipo: 'Intercontinental',  capacidad: 400, ocupacion: 290 },
      { id: 'AF210', ruta: 'CDG → JFK', tipo: 'Intercontinental',  capacidad: 400, ocupacion: 350 },
      { id: 'AV560', ruta: 'SAL → MIA', tipo: 'Continental',       capacidad: 250, ocupacion: 89 },
    ],
    almacenes: [
      { codigo: 'LIM', nombre: 'Lima (LIM)',          capacidad: 800, almacenadas: 423 },
      { codigo: 'BOG', nombre: 'Bogotá (BOG)',        capacidad: 600, almacenadas: 542 },
      { codigo: 'MAD', nombre: 'Madrid (MAD)',        capacidad: 800, almacenadas: 310 },
      { codigo: 'FRA', nombre: 'Frankfurt (FRA)',     capacidad: 700, almacenadas: 610 },
      { codigo: 'CDG', nombre: 'París CDG (CDG)',     capacidad: 800, almacenadas: 290 },
      { codigo: 'JFK', nombre: 'Nueva York (JFK)',    capacidad: 700, almacenadas: 430 },
    ],
  },
  PERIODO: {
    vuelos: [
      { id: 'LA801', ruta: 'LIM → BOG', tipo: 'Continental',       capacidad: 250, ocupacion: 248 },
      { id: 'LA802', ruta: 'BOG → MAD', tipo: 'Intercontinental',  capacidad: 400, ocupacion: 400 },
      { id: 'IB310', ruta: 'MAD → FRA', tipo: 'Continental',       capacidad: 250, ocupacion: 210 },
      { id: 'LH720', ruta: 'FRA → GRU', tipo: 'Intercontinental',  capacidad: 400, ocupacion: 368 },
      { id: 'AF210', ruta: 'CDG → JFK', tipo: 'Intercontinental',  capacidad: 400, ocupacion: 392 },
      { id: 'AV560', ruta: 'SAL → MIA', tipo: 'Continental',       capacidad: 250, ocupacion: 204 },
    ],
    almacenes: [
      { codigo: 'LIM', nombre: 'Lima (LIM)',          capacidad: 800, almacenadas: 680 },
      { codigo: 'BOG', nombre: 'Bogotá (BOG)',        capacidad: 600, almacenadas: 590 },
      { codigo: 'MAD', nombre: 'Madrid (MAD)',        capacidad: 800, almacenadas: 620 },
      { codigo: 'FRA', nombre: 'Frankfurt (FRA)',     capacidad: 700, almacenadas: 695 },
      { codigo: 'CDG', nombre: 'París CDG (CDG)',     capacidad: 800, almacenadas: 570 },
      { codigo: 'JFK', nombre: 'Nueva York (JFK)',    capacidad: 700, almacenadas: 580 },
    ],
  },
  COLAPSO: {
    vuelos: [
      { id: 'LA801', ruta: 'LIM → BOG', tipo: 'Continental',       capacidad: 250, ocupacion: 250 },
      { id: 'LA802', ruta: 'BOG → MAD', tipo: 'Intercontinental',  capacidad: 400, ocupacion: 400 },
      { id: 'IB310', ruta: 'MAD → FRA', tipo: 'Continental',       capacidad: 250, ocupacion: 250 },
      { id: 'LH720', ruta: 'FRA → GRU', tipo: 'Intercontinental',  capacidad: 400, ocupacion: 400 },
      { id: 'AF210', ruta: 'CDG → JFK', tipo: 'Intercontinental',  capacidad: 400, ocupacion: 400 },
      { id: 'AV560', ruta: 'SAL → MIA', tipo: 'Continental',       capacidad: 250, ocupacion: 249 },
    ],
    almacenes: [
      { codigo: 'LIM', nombre: 'Lima (LIM)',          capacidad: 800, almacenadas: 800 },
      { codigo: 'BOG', nombre: 'Bogotá (BOG)',        capacidad: 600, almacenadas: 600 },
      { codigo: 'MAD', nombre: 'Madrid (MAD)',        capacidad: 800, almacenadas: 798 },
      { codigo: 'FRA', nombre: 'Frankfurt (FRA)',     capacidad: 700, almacenadas: 700 },
      { codigo: 'CDG', nombre: 'París CDG (CDG)',     capacidad: 800, almacenadas: 795 },
      { codigo: 'JFK', nombre: 'Nueva York (JFK)',    capacidad: 700, almacenadas: 698 },
    ],
  },
}

function pct(actual, max) {
  return Math.round((actual / max) * 100)
}

function BarraOcupacion({ pct: p, color }) {
  return (
    <div className={styles.barraOcupacion}>
      <div className={styles.barraTrack}>
        <div
          className={`${styles.barraFill} ${styles[`barraFill--${color}`]}`}
          style={{ width: `${p}%` }}
        />
      </div>
      <span className={styles.barraLabel}>{p}%</span>
    </div>
  )
}

function ReporteOcupacion({ escenario }) {
  const rangosSemaforo = useConfiguracionStore((s) => s.rangosSemaforo)
  const datos = MOCK_OCUPACION[escenario] ?? MOCK_OCUPACION.DIA_A_DIA

  const datosGraficoVuelos = datos.vuelos.map((v) => ({
    etiqueta: v.id,
    ocupacion: pct(v.ocupacion, v.capacidad),
  }))

  const datosGraficoAlmacenes = datos.almacenes.map((a) => ({
    etiqueta: a.codigo,
    ocupacion: pct(a.almacenadas, a.capacidad),
  }))

  const seriesOcupacion = [{ clave: 'ocupacion', nombre: 'Ocupación (%)', color: '#2563eb' }]

  return (
    <div className={styles.seccion}>
      {/* Vuelos */}
      <div className={styles.card}>
        <div className={styles.cardTitleRow}>
          <div className={styles.cardTitleIcon}><Plane size={16} /></div>
          <div>
            <h3 className={styles.cardTitle}>Ocupación de vuelos</h3>
            <p className={styles.cardSubtitle}>Capacidad utilizada por vuelo según los rangos del semáforo configurados</p>
          </div>
        </div>

        <div className={styles.dos}>
          <div className={styles.tablaWrapper}>
            <table className={styles.table}>
              <thead>
                <tr>
                  <th>Vuelo</th>
                  <th>Ruta</th>
                  <th>Tipo</th>
                  <th className="right">Ocup. / Cap.</th>
                  <th>Estado</th>
                </tr>
              </thead>
              <tbody>
                {datos.vuelos.map((v) => {
                  const p = pct(v.ocupacion, v.capacidad)
                  const color = getColorSemaforo(p, rangosSemaforo)
                  return (
                    <tr key={v.id}>
                      <td><span className={`${styles.badge} ${styles['badge--info']}`}>{v.id}</span></td>
                      <td>{v.ruta}</td>
                      <td><span className={`${styles.badge} ${color === 'verde' ? styles['badge--verde'] : color === 'ambar' ? styles['badge--ambar'] : styles['badge--rojo']}`}>{v.tipo}</span></td>
                      <td className="right">{v.ocupacion} / {v.capacidad}</td>
                      <td>
                        <div className={styles.semaforoCell}>
                          <span className={`${styles.dot} ${styles[`dot--${color}`]}`} />
                          <BarraOcupacion pct={p} color={color} />
                        </div>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>

          <GraficoBarras
            titulo="% de ocupación por vuelo"
            datos={datosGraficoVuelos}
            series={seriesOcupacion}
            altura={260}
          />
        </div>
      </div>

      {/* Almacenes */}
      <div className={styles.card}>
        <div className={styles.cardTitleRow}>
          <div className={styles.cardTitleIcon}><Warehouse size={16} /></div>
          <div>
            <h3 className={styles.cardTitle}>Ocupación de almacenes</h3>
            <p className={styles.cardSubtitle}>Maletas almacenadas vs. capacidad máxima por aeropuerto</p>
          </div>
        </div>

        <div className={styles.dos}>
          <div className={styles.tablaWrapper}>
            <table className={styles.table}>
              <thead>
                <tr>
                  <th>Aeropuerto</th>
                  <th className="right">Almacenadas / Cap.</th>
                  <th>Ocupación</th>
                </tr>
              </thead>
              <tbody>
                {datos.almacenes.map((a) => {
                  const p = pct(a.almacenadas, a.capacidad)
                  const color = getColorSemaforo(p, rangosSemaforo)
                  return (
                    <tr key={a.codigo}>
                      <td>{a.nombre}</td>
                      <td className="right">{a.almacenadas} / {a.capacidad}</td>
                      <td>
                        <div className={styles.semaforoCell}>
                          <span className={`${styles.dot} ${styles[`dot--${color}`]}`} />
                          <BarraOcupacion pct={p} color={color} />
                        </div>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>

          <GraficoBarras
            titulo="% de ocupación por aeropuerto"
            datos={datosGraficoAlmacenes}
            series={seriesOcupacion}
            altura={260}
          />
        </div>
      </div>
    </div>
  )
}

export default ReporteOcupacion
