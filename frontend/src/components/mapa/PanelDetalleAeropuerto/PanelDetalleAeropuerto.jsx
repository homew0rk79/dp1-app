import { useMemo } from 'react'
import { PlaneLanding, PlaneTakeoff } from 'lucide-react'
import { obtenerProximasLlegadas, obtenerProximasSalidas } from '../../../utils/planificacionAeropuerto'
import { TIEMPO_SIMULADO_REFERENCIA } from '../../../constants/tiempoSimulado'
import styles from './PanelDetalleAeropuerto.module.css'

function ListaEnvios({ items, tipo }) {
  if (items.length === 0) {
    return (
      <p className={styles.vacio}>
        Sin {tipo === 'llegada' ? 'llegadas' : 'salidas'} planificadas
      </p>
    )
  }

  return (
    <ul className={styles.lista}>
      {items.map((item) => (
        <li key={item.id} className={styles.item}>
          <div className={styles.itemHeader}>
            <span className={styles.envioId}>{item.id}</span>
            <span className={styles.maletas}>{item.maletas} mlt</span>
          </div>
          <div className={styles.producto}>{item.producto}</div>
          <div className={styles.meta}>
            {tipo === 'llegada' ? (
              <>
                Desde {item.origen} · {item.vuelo} · llega {item.horaLlegadaFmt} UTC
              </>
            ) : (
              <>
                Hacia {item.destino} · {item.vuelo} · sale {item.horaSalidaFmt} UTC
              </>
            )}
          </div>
        </li>
      ))}
    </ul>
  )
}

function PanelDetalleAeropuerto({ codigo, tiempoReferencia = TIEMPO_SIMULADO_REFERENCIA }) {
  const llegadas = useMemo(
    () => obtenerProximasLlegadas(codigo, tiempoReferencia),
    [codigo, tiempoReferencia],
  )
  const salidas = useMemo(
    () => obtenerProximasSalidas(codigo, tiempoReferencia),
    [codigo, tiempoReferencia],
  )

  return (
    <div className={styles.panel}>
      <section className={styles.seccion}>
        <h5 className={styles.seccionTitulo}>
          <PlaneLanding size={13} />
          Próximas llegadas
        </h5>
        <ListaEnvios items={llegadas} tipo="llegada" />
      </section>

      <section className={styles.seccion}>
        <h5 className={styles.seccionTitulo}>
          <PlaneTakeoff size={13} />
          Próximas salidas
        </h5>
        <ListaEnvios items={salidas} tipo="salida" />
      </section>
    </div>
  )
}

export default PanelDetalleAeropuerto
