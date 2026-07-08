import { useEffect } from 'react'
import { useMap } from 'react-leaflet'
import useSeleccionStore from '../../../store/seleccionStore'
import CapaOverlayRuta from '../CapaOverlayRuta/CapaOverlayRuta'
import { useOverlayRuta } from '../../../context/OverlayRutaContext'

function MapController({ aeropuertos, mostrarTramos = true }) {
  const map = useMap()
  const aeropuertoSeleccionado = useSeleccionStore((s) => s.aeropuertoSeleccionado)
  const vueloSeleccionado = useSeleccionStore((s) => s.vueloSeleccionado)
  const overlayStore = useSeleccionStore((s) => s.overlayRuta)
  const overlayCtx = useOverlayRuta()
  const overlay = overlayStore ?? overlayCtx?.overlay

  // Recalcular el tamaño del mapa cuando su contenedor cambia (p. ej. al
  // colapsar/expandir el sidebar). Sin esto Leaflet deja tiles sin renderizar.
  useEffect(() => {
    const contenedor = map.getContainer()?.parentElement
    if (!contenedor || typeof ResizeObserver === 'undefined') return

    let timer = null
    const observer = new ResizeObserver(() => {
      // Debounce ligeramente mayor que la transición de 0.22s del sidebar
      clearTimeout(timer)
      timer = setTimeout(() => map.invalidateSize(), 260)
    })
    observer.observe(contenedor)
    return () => {
      clearTimeout(timer)
      observer.disconnect()
    }
  }, [map])

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

  if (!mostrarTramos || !overlay?.escalas?.length) return null

  return (
    <CapaOverlayRuta
      escalas={overlay.escalas}
      variante={overlay.variante ?? 'actual'}
      aeropuertos={aeropuertos}
    />
  )
}

export default MapController
