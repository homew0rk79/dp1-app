import { useEffect } from 'react'
import { useMap } from 'react-leaflet'
import useSeleccionStore from '../../../store/seleccionStore'
import CapaOverlayRuta from '../CapaOverlayRuta/CapaOverlayRuta'
import { useOverlayRuta } from '../../../context/OverlayRutaContext'

function MapController({ aeropuertos }) {
  const map = useMap()
  const aeropuertoSeleccionado = useSeleccionStore((s) => s.aeropuertoSeleccionado)
  const overlayCtx = useOverlayRuta()
  const overlay = overlayCtx?.overlay

  useEffect(() => {
    if (!aeropuertoSeleccionado) return
    const aero = aeropuertos.find((a) => a.codigo === aeropuertoSeleccionado)
    if (aero) {
      map.flyTo([aero.lat, aero.lng], Math.max(map.getZoom(), 5), { duration: 0.8 })
    }
  }, [aeropuertoSeleccionado, aeropuertos, map])

  if (!overlay?.escalas?.length) return null

  return (
    <CapaOverlayRuta
      escalas={overlay.escalas}
      variante={overlay.variante ?? 'actual'}
    />
  )
}

export default MapController
