import { useCallback, useEffect, useRef, useState } from 'react'
import { PlaneTakeoff, X, AlertTriangle } from 'lucide-react'

import { simulacionService } from '../../../services/simulacionService'
import useSimulacionStore from '../../../store/simulacionStore'
import styles from './PanelVuelosProximos.module.css'

const REFRESH_MS = 5000

/**
 * Panel desplegable que lista los próximos vuelos a salir según la solución
 * actual del planificador y permite cancelar uno con un click para disparar
 * la replanificación. Usado en el sidebar del visualizador, especialmente
 * durante el escenario día a día.
 */
function PanelVuelosProximos() {
  const tiempoAnimacion = useSimulacionStore((s) => s.tiempoAnimacion)
  const manifest        = useSimulacionStore((s) => s.manifest)

  const [vuelos, setVuelos]               = useState([])
  const [cargando, setCargando]           = useState(false)
  const [error, setError]                 = useState(null)
  const [confirmando, setConfirmando]     = useState(null) // vuelo a confirmar
  const [cancelando, setCancelando]       = useState(false)
  const [resultado, setResultado]         = useState(null)
  const ultimoTiempoRef                   = useRef(0)

  const cargarVuelos = useCallback(async () => {
    if (!manifest) return
    const tiempoMin = Math.max(0, Math.floor(tiempoAnimacion))
    setCargando(true)
    try {
      const res = await simulacionService.obtenerVuelosProximos(tiempoMin, 20)
      setVuelos(res.data || [])
      setError(null)
      ultimoTiempoRef.current = tiempoMin
    } catch (e) {
      setError('No se pudo obtener la lista de vuelos próximos')
    } finally {
      setCargando(false)
    }
  }, [manifest, tiempoAnimacion])

  // Polling cada 5 s mientras haya manifest.
  useEffect(() => {
    if (!manifest) return
    cargarVuelos()
    const id = setInterval(cargarVuelos, REFRESH_MS)
    return () => clearInterval(id)
  }, [cargarVuelos, manifest])

  async function confirmarCancelacion(vuelo) {
    setCancelando(true)
    setResultado(null)
    try {
      const res = await simulacionService.cancelarVuelo({
        origen: vuelo.origen,
        destino: vuelo.destino,
        horaSalidaMinutos: vuelo.horaSalidaMin,
        idEnvio: null,
      })
      setResultado({
        ok: true,
        mensaje: res.data?.mensaje
          || `Replanificado: ${res.data?.reasignados ?? 0} envío(s)`,
      })
      setConfirmando(null)
      // Refresca la lista tras la replanificación
      cargarVuelos()
    } catch (e) {
      setResultado({
        ok: false,
        mensaje: e?.response?.data || 'No se pudo cancelar el vuelo',
      })
    } finally {
      setCancelando(false)
    }
  }

  if (!manifest) {
    return (
      <div className={styles.estado}>
        Esperando solución activa para listar vuelos próximos…
      </div>
    )
  }

  return (
    <div className={styles.contenedor}>
      <header className={styles.header}>
        <PlaneTakeoff size={14} />
        <span>Próximos vuelos a salir</span>
        <span className={styles.contador}>
          {vuelos.length}{cargando ? ' …' : ''}
        </span>
      </header>

      {error && <div className={styles.alerta}>{error}</div>}

      {resultado && (
        <div className={resultado.ok ? styles.toastOk : styles.toastError}>
          {resultado.mensaje}
        </div>
      )}

      {vuelos.length === 0 && !cargando && !error && (
        <div className={styles.estado}>Sin vuelos próximos en este momento</div>
      )}

      <ul className={styles.lista}>
        {vuelos.map((v) => {
          const pct = v.capacidadMax > 0
            ? Math.round((v.maletas / v.capacidadMax) * 100)
            : 0
          const color = pct >= 90 ? styles.lleno : pct >= 70 ? styles.medio : styles.bajo
          return (
            <li key={v.claveVuelo} className={styles.item}>
              <div className={styles.itemHeader}>
                <span className={styles.ruta}>
                  {v.origen} <span className={styles.flecha}>→</span> {v.destino}
                </span>
                <span className={styles.hora}>{v.salidaFormateada}</span>
              </div>
              <div className={styles.itemFooter}>
                <span className={`${styles.ocup} ${color}`}>
                  {v.maletas} / {v.capacidadMax} maletas
                </span>
                <button
                  type="button"
                  className={styles.btnCancelar}
                  onClick={() => setConfirmando(v)}
                  disabled={cancelando}
                >
                  <X size={11} /> Cancelar vuelo
                </button>
              </div>
            </li>
          )
        })}
      </ul>

      {confirmando && (
        <div className={styles.modalBackdrop} onClick={() => !cancelando && setConfirmando(null)}>
          <div className={styles.modalCard} onClick={(e) => e.stopPropagation()}>
            <div className={styles.modalHeader}>
              <AlertTriangle size={16} className={styles.iconoAlerta} />
              <span>Confirmar cancelación</span>
            </div>
            <div className={styles.modalBody}>
              <p>¿Cancelar el vuelo:</p>
              <p className={styles.modalRuta}>
                <strong>{confirmando.origen} → {confirmando.destino}</strong> ({confirmando.salidaFormateada})
              </p>
              <p className={styles.modalNota}>
                Los <strong>{confirmando.maletas}</strong> envíos asignados serán replanificados automáticamente.
              </p>
            </div>
            <div className={styles.modalAcciones}>
              <button
                type="button"
                className={styles.btnSecundario}
                onClick={() => setConfirmando(null)}
                disabled={cancelando}
              >
                Volver
              </button>
              <button
                type="button"
                className={styles.btnPrimario}
                onClick={() => confirmarCancelacion(confirmando)}
                disabled={cancelando}
              >
                {cancelando ? 'Replanificando…' : 'Cancelar y replanificar'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default PanelVuelosProximos
