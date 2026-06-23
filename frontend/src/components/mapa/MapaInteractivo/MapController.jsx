import { useEffect } from 'react'
import { useMap } from 'react-leaflet'
import useSeleccionStore from '../../../store/seleccionStore'
import CapaOverlayRuta from '../CapaOverlayRuta/CapaOverlayRuta'
import { useOverlayRuta } from '../../../context/OverlayRutaContext'

function MapController({ aeropuertos }) {
  const map = useMap()
  const aeropuertoSeleccionado = useSeleccionStore((s) => s.aeropuertoSeleccionado)
  const vueloSeleccionado = useSeleccionStore((s) => s.vueloSeleccionado)
  const overlayStore = useSeleccionStore((s) => s.overlayRuta)
  const overlayCtx = useOverlayRuta()
  const overlay = overlayStore ?? overlayCtx?.overlay

  useEffect(() => {
    if (!aeropuertoSeleccionado) return
    const aero = aeropuertos.find((a) => a.codigo === aeropuertoSeleccionado)
    if (aero) {
      map.flyTo([aero.lat, aero.lng], Math.max(map.getZoom(), 5), { duration: 0.8 })
    }
  }, [aeropuertoSeleccionado, aeropuertos, map])

  useEffect(() => {
    if (!vueloSeleccionado) return
    const [origen, destino] = vueloSeleccionado.split('-')
    const a = aeropuertos.find((item) => item.codigo === origen)
    const b = aeropuertos.find((item) => item.codigo === destino)
    if (a && b) {
      map.fitBounds(
        [
          [a.lat, a.lng],
          [b.lat, b.lng],
        ],
        { padding: [64, 64], maxZoom: 5 },
      )
    }
  }, [vueloSeleccionado, aeropuertos, map])

  if (!overlay?.escalas?.length) return null

  return (
    <CapaOverlayRuta
      escalas={overlay.escalas}
      variante={overlay.variante ?? 'actual'}
      aeropuertos={aeropuertos}
    />
  )
}

export default MapController
