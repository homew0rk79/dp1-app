import { useMemo } from 'react'
import { PlaneLanding, PlaneTakeoff, XCircle } from 'lucide-react'
import { TIEMPO_SIMULADO_REFERENCIA } from '../../../constants/tiempoSimulado'
import useSimulacionStore from '../../../store/simulacionStore'
import { sumarMinutos, formatearFechaHora } from '../../../utils/tiempos'
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

function PanelDetalleAeropuerto({ codigo, onCancelVuelo }) {

  const manifest = useSimulacionStore((s) => s.manifest)
  const tiempoAnimacion = useSimulacionStore((s) => s.tiempoAnimacion)
  const fechaInicio = useSimulacionStore((s) => s.parametros.fechaInicio)

  let vuelosReales = []
  if (manifest) {
    vuelosReales = manifest.ocurrencias
      .filter((o) => o.origen === codigo && o.salidaAbs >= tiempoAnimacion)
      .sort((a, b) => a.salidaAbs - b.salidaAbs)
      .slice(0, 5)
  }

  return (
    <div className={styles.panel}>
      {manifest && (
        <section className={styles.seccion}>
          <h5 className={styles.seccionTitulo} style={{ color: '#ef4444' }}>
            <PlaneTakeoff size={13} />
            Próximos Vuelos (Tiempo Real)
          </h5>
          {vuelosReales.length === 0 ? (
            <p className={styles.vacio}>Sin vuelos próximos</p>
          ) : (
            <ul className={styles.lista}>
              {vuelosReales.map((v) => (
                <li key={`${v.destino}-${v.salidaAbs}`} className={styles.item} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div>
                    <div className={styles.itemHeader}>
                      <span className={styles.envioId}>Hacia {v.destino}</span>
                      <span className={styles.maletas}>{v.maletas} mlt</span>
                    </div>
                    <div className={styles.meta}>
                      Sale: {formatearFechaHora(sumarMinutos(fechaInicio, v.salidaAbs))}
                    </div>
                  </div>
                  {onCancelVuelo && (
                    <button
                      onClick={() => onCancelVuelo(v)}
                      style={{ padding: '4px 6px', fontSize: '11px', background: '#dc2626', color: 'white', border: 'none', borderRadius: '4px', cursor: 'pointer', display: 'flex', alignItems: 'center', gap: '4px' }}
                      title="Cancelar este vuelo"
                    >
                      <XCircle size={12} />
                      Cancelar
                    </button>
                  )}
                </li>
              ))}
            </ul>
          )}
        </section>
      )}

    </div>
  )
}

export default PanelDetalleAeropuerto
