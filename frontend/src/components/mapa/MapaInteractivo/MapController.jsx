import { useEffect } from 'react'
import { useMap } from 'react-leaflet'
import useSeleccionStore from '../../../store/seleccionStore'

function MapController({ aeropuertos }) {
  const map = useMap()
  const aeropuertoSeleccionado = useSeleccionStore((s) => s.aeropuertoSeleccionado)

  useEffect(() => {
    if (!aeropuertoSeleccionado) return
    const aero = aeropuertos.find((a) => a.codigo === aeropuertoSeleccionado)
    if (aero) {
      map.flyTo([aero.lat, aero.lng], Math.max(map.getZoom(), 5), { duration: 0.8 })
    }
  }, [aeropuertoSeleccionado, aeropuertos, map])

  return null
}

export default MapController