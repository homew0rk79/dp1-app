import { useEffect } from 'react'
import { useLocation } from 'react-router-dom'
import { X } from 'lucide-react'
import MapaInteractivo from '../../components/mapa/MapaInteractivo/MapaInteractivo'
import { OverlayRutaProvider } from '../../context/OverlayRutaContext'
import { useState, useCallback } from 'react'
import styles from './VisualizadorPage.module.css'

function VisualizadorPage() {
  const location = useLocation()
  const [overlay, setOverlay] = useState(() => location.state?.overlayRuta ?? null)

  useEffect(() => {
    if (location.state?.overlayRuta) {
      setOverlay(location.state.overlayRuta)
    }
  }, [location.state])

  const limpiarOverlay = useCallback(() => setOverlay(null), [])

  return (
    <OverlayRutaProvider value={{ overlay, setOverlay: limpiarOverlay }}>
      <div className={styles.page}>
        {overlay?.escalas?.length >= 2 && (
          <div className={styles.overlayBar}>
            <span>
              Ruta en mapa: {overlay.escalas.join(' → ')}
              {overlay.variante === 'anterior' ? ' (anterior)' : ''}
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
