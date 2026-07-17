import { useCallback, useEffect, useRef, useState } from 'react'
import { useLocation } from 'react-router-dom'
import { X } from 'lucide-react'
import MapaInteractivo from '../../components/mapa/MapaInteractivo/MapaInteractivo'
import { OverlayRutaProvider } from '../../context/OverlayRutaContext'
import useSeleccionStore from '../../store/seleccionStore'
import useSimulacionStore from '../../store/simulacionStore'
import { simulacionService } from '../../services/simulacionService'
import { ETIQUETAS_ESCENARIO } from '../../constants/escenarios'
import styles from './VisualizadorPage.module.css'

const ETIQUETAS_ESCENARIO_REPORTE = {
  DIA_A_DIA: 'Día a día',
  PERIODO: 'Simulación de período',
  COLAPSO: 'Simulación de colapso',
}

function ReporteFinalModal({ escenario, onCerrar }) {
  const [metricas, setMetricas] = useState(null)
  const [cargando, setCargando] = useState(true)

  useEffect(() => {
    let activo = true
    simulacionService.obtenerMetricas()
      .then(({ data }) => { if (activo) setMetricas(data) })
      .catch(() => { if (activo) setMetricas({}) })
      .finally(() => { if (activo) setCargando(false) })
    return () => { activo = false }
  }, [])

  const cumpl = metricas?.porcentajeCumplimiento ?? null
  const cumplColor = cumpl == null ? '' : cumpl >= 85 ? styles.kpiVerde : styles.kpiRojo

  return (
    <div className={styles.modalBackdrop}>
      <div className={styles.modal} role="dialog" aria-modal="true">
        <div className={styles.modalHeader}>
          <div>
            <p className={styles.modalTitulo}>Reporte de planificación estable</p>
            <p className={styles.modalSubtitulo}>
              {ETIQUETAS_ESCENARIO_REPORTE[escenario] ?? escenario} · Última ejecución completada
            </p>
          </div>
          <button type="button" className={styles.modalCerrar} onClick={onCerrar} aria-label="Cerrar">✕</button>
        </div>

        <div className={styles.modalBody}>
          {cargando ? (
            <p className={styles.modalCargando}>Cargando métricas…</p>
          ) : (
            <div className={styles.kpiGrid}>
              <div className={styles.kpiCard}>
                <div className={styles.kpiLabel}>Total envíos</div>
                <div className={styles.kpiValor}>{(metricas?.totalEnvios ?? '—').toLocaleString?.('es-PE') ?? metricas?.totalEnvios ?? '—'}</div>
                <div className={styles.kpiSub}>Envíos procesados</div>
              </div>
              <div className={`${styles.kpiCard} ${cumplColor}`}>
                <div className={styles.kpiLabel}>Cumplimiento</div>
                <div className={styles.kpiValor}>{cumpl != null ? `${Number(cumpl).toFixed(1)}%` : '—'}</div>
                <div className={styles.kpiSub}>Entregas dentro del plazo</div>
              </div>
              <div className={styles.kpiCard}>
                <div className={styles.kpiLabel}>Vuelos planificados</div>
                <div className={styles.kpiValor}>{metricas?.vuelosUtilizados ?? '—'}</div>
                <div className={styles.kpiSub}>En la solución final</div>
              </div>
              <div className={styles.kpiCard}>
                <div className={styles.kpiLabel}>Tiempo promedio entrega</div>
                <div className={styles.kpiValor}>
                  {metricas?.tiempoPromedioEntregaMinutos != null
                    ? `${(metricas.tiempoPromedioEntregaMinutos / 60).toFixed(1)} h`
                    : '—'}
                </div>
                <div className={styles.kpiSub}>Promedio por envío</div>
              </div>
            </div>
          )}
        </div>

        <div className={styles.modalFooter}>
          <button type="button" className={styles.btnCerrar} onClick={onCerrar}>
            Cerrar
          </button>
        </div>
      </div>
    </div>
  )
}

function VisualizadorPage() {
  const location = useLocation()
  const [overlay, setOverlay] = useState(() => location.state?.overlayRuta ?? null)
  const overlayStore = useSeleccionStore((s) => s.overlayRuta)
  const limpiarOverlayRuta = useSeleccionStore((s) => s.limpiarOverlayRuta)
  const overlayVisible = overlayStore ?? overlay

  const estadoEjecucion = useSimulacionStore((s) => s.estadoEjecucion)
  const escenarioActivo = useSimulacionStore((s) => s.escenarioActivo)
  const tiempoAnimacion = useSimulacionStore((s) => s.tiempoAnimacion)
  const manifest = useSimulacionStore((s) => s.manifest)
  const [mostrarReporte, setMostrarReporte] = useState(false)
  const yaSeMostro = useRef(false)

  function intentarMostrarReporte() {
    if (!yaSeMostro.current) {
      yaSeMostro.current = true
      setMostrarReporte(true)
    }
  }

  // Trigger 1: la animación llega al final del timeline
  useEffect(() => {
    if (!manifest || manifest.duracionTotalMinutos <= 0) return
    if (tiempoAnimacion >= manifest.duracionTotalMinutos) intentarMostrarReporte()
  }, [tiempoAnimacion, manifest])

  // Trigger 2: planificación completada y la animación ya avanzó ≥90%
  // Cubre el caso de fast-forward donde el slider salta el valor exacto
  useEffect(() => {
    if (!manifest || manifest.duracionTotalMinutos <= 0) return
    if (estadoEjecucion !== 'COMPLETADO') return
    const avance = tiempoAnimacion / manifest.duracionTotalMinutos
    if (avance >= 0.9) intentarMostrarReporte()
  }, [estadoEjecucion, tiempoAnimacion, manifest])

  // Resetear al iniciar nueva simulación (manifest pasa a null entre runs)
  useEffect(() => {
    if (!manifest) {
      yaSeMostro.current = false
      setMostrarReporte(false)
    }
  }, [manifest])

  useEffect(() => {
    if (location.state?.overlayRuta) {
      setOverlay(location.state.overlayRuta)
    }
  }, [location.state])

  const limpiarOverlay = useCallback(() => {
    setOverlay(null)
    limpiarOverlayRuta()
  }, [limpiarOverlayRuta])

  return (
    <OverlayRutaProvider value={{ overlay: overlayVisible, setOverlay: limpiarOverlay }}>
      <div className={styles.page}>
        {overlayVisible?.escalas?.length >= 2 && (
          <div className={styles.overlayBar}>
            <span>
              Ruta en mapa: {overlayVisible.escalas.join(' -> ')}
              {overlayVisible.variante === 'anterior' ? ' (anterior)' : ''}
            </span>
            <button type="button" className={styles.overlayBtn} onClick={limpiarOverlay}>
              <X size={14} />
              Quitar overlay
            </button>
          </div>
        )}
        <MapaInteractivo />
        {mostrarReporte && (
          <ReporteFinalModal
            escenario={escenarioActivo}
            onCerrar={() => setMostrarReporte(false)}
          />
        )}
      </div>
    </OverlayRutaProvider>
  )
}

export default VisualizadorPage
