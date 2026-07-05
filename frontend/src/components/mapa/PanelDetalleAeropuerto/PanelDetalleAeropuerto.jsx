import { useMemo, useState } from 'react'
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
  const [tab, setTab] = useState('vuelos')
  const manifest = useSimulacionStore((s) => s.manifest)
  const tiempoAnimacion = useSimulacionStore((s) => s.tiempoAnimacion)
  const fechaInicio = useSimulacionStore((s) => s.parametros.fechaInicio)

  let vuelosReales = []
  let llegadasPlanificadas = []
  let salidasPlanificadas = []

  if (manifest && manifest.ocurrencias) {
    vuelosReales = manifest.ocurrencias
      .filter((o) => o.origen === codigo && o.salidaAbs >= tiempoAnimacion)
      .sort((a, b) => a.salidaAbs - b.salidaAbs)
      .slice(0, 5)

    llegadasPlanificadas = manifest.ocurrencias
      .filter((o) => o.destino === codigo && o.llegadaAbs >= tiempoAnimacion)
      .sort((a, b) => a.llegadaAbs - b.llegadaAbs)
      .slice(0, 10)
      .map((o, idx) => {
        const fechaLl = sumarMinutos(fechaInicio, o.llegadaAbs)
        return {
          id: `ENV-IN-${o.vuelo || o.codigoVuelo || 'V'}-${idx + 1}`,
          maletas: o.maletas || 0,
          producto: `Carga proyectada (${o.maletas || 0} pz / cap ${o.capacidadMax || 0})`,
          origen: o.origen,
          destino: o.destino,
          vuelo: o.vuelo || o.codigoVuelo || `UT-${o.origen}-${o.destino}`,
          horaLlegadaFmt: fechaLl ? formatearFechaHora(fechaLl).slice(0, 16) : '—',
        }
      })

    salidasPlanificadas = manifest.ocurrencias
      .filter((o) => o.origen === codigo && o.salidaAbs >= tiempoAnimacion)
      .sort((a, b) => a.salidaAbs - b.salidaAbs)
      .slice(0, 10)
      .map((o, idx) => {
        const fechaSa = sumarMinutos(fechaInicio, o.salidaAbs)
        return {
          id: `ENV-OUT-${o.vuelo || o.codigoVuelo || 'V'}-${idx + 1}`,
          maletas: o.maletas || 0,
          producto: `Despacho proyectado (${o.maletas || 0} pz / cap ${o.capacidadMax || 0})`,
          origen: o.origen,
          destino: o.destino,
          vuelo: o.vuelo || o.codigoVuelo || `UT-${o.origen}-${o.destino}`,
          horaSalidaFmt: fechaSa ? formatearFechaHora(fechaSa).slice(0, 16) : '—',
        }
      })
  }

  if (!manifest) return null

  return (
    <div className={styles.panel}>
      <div className={styles.tabsWrap}>
        <button
          type="button"
          className={`${styles.tabBtn} ${tab === 'vuelos' ? styles.tabBtnActivo : ''}`}
          onClick={() => setTab('vuelos')}
        >
          ✈️ Próximos ({vuelosReales.length})
        </button>
        <button
          type="button"
          className={`${styles.tabBtn} ${tab === 'llegadas' ? styles.tabBtnActivo : ''}`}
          onClick={() => setTab('llegadas')}
        >
          📥 Llegadas ({llegadasPlanificadas.length})
        </button>
        <button
          type="button"
          className={`${styles.tabBtn} ${tab === 'salidas' ? styles.tabBtnActivo : ''}`}
          onClick={() => setTab('salidas')}
        >
          📤 Salidas ({salidasPlanificadas.length})
        </button>
      </div>

      {tab === 'vuelos' && (
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

      {tab === 'llegadas' && (
        <section className={styles.seccion}>
          <h5 className={styles.seccionTitulo} style={{ color: '#2563eb' }}>
            <PlaneLanding size={13} />
            Llegadas Planificadas (#79, #80)
          </h5>
          <ListaEnvios items={llegadasPlanificadas} tipo="llegada" />
        </section>
      )}

      {tab === 'salidas' && (
        <section className={styles.seccion}>
          <h5 className={styles.seccionTitulo} style={{ color: '#059669' }}>
            <PlaneTakeoff size={13} />
            Salidas Programadas (#81, #82)
          </h5>
          <ListaEnvios items={salidasPlanificadas} tipo="salida" />
        </section>
      )}
    </div>
  )
}

export default PanelDetalleAeropuerto
