import { useCallback, useEffect, useState } from 'react'
import { PackageCheck, PlaneTakeoff, Clock3, ChevronDown, ChevronRight } from 'lucide-react'

import { simulacionService } from '../../../../services/simulacionService'
import useSimulacionStore from '../../../../store/simulacionStore'
import styles from './PanelMonitorEnvios.module.css'

const REFRESH_MS = 10000
const VENTANA_HORAS = 4

function formatearMinutosAbs(min) {
  if (min == null || min < 0) return '—'
  const dia = Math.floor(min / 1440) + 1
  const hh = Math.floor((min % 1440) / 60).toString().padStart(2, '0')
  const mm = (min % 60).toString().padStart(2, '0')
  return `D${dia} ${hh}:${mm}`
}

function SeccionEnvios({ id, titulo, icono: Icono, items, abierta, onToggle, etiquetaHora }) {
  const totalMaletas = items.reduce((s, x) => s + x.cantidad, 0)
  return (
    <section className={styles.seccion}>
      <button type="button" className={styles.seccionHeader} onClick={() => onToggle(id)}>
        {abierta ? <ChevronDown size={13} /> : <ChevronRight size={13} />}
        <Icono size={13} />
        <span className={styles.seccionTitulo}>{titulo}</span>
        <span className={styles.seccionContador}>
          {items.length} envío(s) · {totalMaletas} mlt
        </span>
      </button>

      {abierta && (
        items.length === 0 ? (
          <p className={styles.vacio}>Sin envíos en esta categoría</p>
        ) : (
          <ul className={styles.lista}>
            {items.map((it, i) => (
              <li key={`${it.envioId}-${i}`} className={styles.item}>
                <div className={styles.fila}>
                  <span className={styles.envioId}>{it.envioId}</span>
                  <span className={styles.cantidad}>{it.cantidad} mlt</span>
                </div>
                <div className={styles.filaSec}>
                  <span>{it.origen} → {it.destino}</span>
                  <span className={styles.hora}>
                    {etiquetaHora} {formatearMinutosAbs(it.horaAbs)}
                  </span>
                </div>
                <div className={styles.vuelo}>UT: {it.vuelo}</div>
              </li>
            ))}
          </ul>
        )
      )}
    </section>
  )
}

/**
 * Monitor de envíos del panel del visualizador: planificados por salir,
 * en vuelo en el instante actual, y entregados en las últimas 4 horas simuladas.
 */
function PanelMonitorEnvios() {
  const tiempoAnimacion = useSimulacionStore((s) => s.tiempoAnimacion)
  const manifest = useSimulacionStore((s) => s.manifest)

  const [datos, setDatos] = useState(null)
  const [error, setError] = useState(null)
  const [abiertas, setAbiertas] = useState({ planificados: true, enVuelo: true, entregados: false })

  const cargar = useCallback(async () => {
    if (!manifest) return
    try {
      const res = await simulacionService.obtenerMonitorEnvios(
        Math.max(0, Math.floor(tiempoAnimacion)), VENTANA_HORAS)
      setDatos(res.data)
      setError(null)
    } catch {
      setError('No se pudo obtener el monitor de envíos')
    }
  }, [manifest, tiempoAnimacion])

  useEffect(() => {
    if (!manifest) return
    cargar()
    const id = setInterval(cargar, REFRESH_MS)
    return () => clearInterval(id)
  }, [cargar, manifest])

  function toggle(id) {
    setAbiertas((prev) => ({ ...prev, [id]: !prev[id] }))
  }

  if (!manifest) {
    return <div className={styles.estado}>Esperando solución activa…</div>
  }
  if (error) return <div className={styles.estado}>{error}</div>
  if (!datos) return <div className={styles.estado}>Cargando…</div>

  return (
    <div className={styles.contenedor}>
      <SeccionEnvios
        id="planificados"
        titulo="Planificados por salir"
        icono={Clock3}
        items={datos.planificados || []}
        abierta={abiertas.planificados}
        onToggle={toggle}
        etiquetaHora="Sale"
      />
      <SeccionEnvios
        id="enVuelo"
        titulo="En vuelo"
        icono={PlaneTakeoff}
        items={datos.enVuelo || []}
        abierta={abiertas.enVuelo}
        onToggle={toggle}
        etiquetaHora="Salió"
      />
      <SeccionEnvios
        id="entregados"
        titulo={`Entregados (últimas ${VENTANA_HORAS} h)`}
        icono={PackageCheck}
        items={datos.entregados || []}
        abierta={abiertas.entregados}
        onToggle={toggle}
        etiquetaHora="Llegó"
      />
    </div>
  )
}

export default PanelMonitorEnvios
