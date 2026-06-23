import { useCallback, useEffect, useState } from 'react'
import { useLocation } from 'react-router-dom'
import { X } from 'lucide-react'
import MapaInteractivo from '../../components/mapa/MapaInteractivo/MapaInteractivo'
import { OverlayRutaProvider } from '../../context/OverlayRutaContext'
import useSeleccionStore from '../../store/seleccionStore'
import styles from './VisualizadorPage.module.css'

function VisualizadorPage() {
  const location = useLocation()
  const [overlay, setOverlay] = useState(() => location.state?.overlayRuta ?? null)
  const overlayStore = useSeleccionStore((s) => s.overlayRuta)
  const limpiarOverlayRuta = useSeleccionStore((s) => s.limpiarOverlayRuta)
  const overlayVisible = overlayStore ?? overlay

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
      </div>
    </OverlayRutaProvider>
  )
}

export default VisualizadorPage
