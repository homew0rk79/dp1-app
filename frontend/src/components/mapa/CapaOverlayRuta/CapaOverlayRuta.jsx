import { useEffect } from 'react'
import { Polyline, CircleMarker, Tooltip, useMap } from 'react-leaflet'
import { rutaACoordenadas, obtenerEscalas } from '../../../utils/rutaMapa'
import { AEROPUERTOS_POR_CODIGO } from '../../../mocks/aeropuertos'

const ESTILO_ACTUAL = {
  color: '#2563eb',
  weight: 3,
  opacity: 0.85,
}

const ESTILO_ANTERIOR = {
  color: '#94a3b8',
  weight: 2.5,
  opacity: 0.75,
  dashArray: '8 6',
}

function CapaOverlayRuta({ escalas, variante = 'actual' }) {
  const map = useMap()
  const coords = rutaACoordenadas(escalas)
  const estilo = variante === 'anterior' ? ESTILO_ANTERIOR : ESTILO_ACTUAL
  const intermedias = obtenerEscalas(escalas)

  useEffect(() => {
    if (coords.length < 2) return
    map.fitBounds(coords, { padding: [48, 48], maxZoom: 6 })
  }, [map, coords])

  if (coords.length < 2) return null

  return (
    <>
      <Polyline positions={coords} pathOptions={estilo} />
      {escalas.map((codigo, i) => {
        const aero = AEROPUERTOS_POR_CODIGO[codigo]
        if (!aero) return null
        const esEscala = intermedias.includes(codigo)
        const esOrigen = i === 0
        const esDestino = i === escalas.length - 1
        const radio = esEscala ? 7 : 9
        const fill = variante === 'anterior'
          ? '#cbd5e1'
          : esOrigen ? '#2563eb' : esDestino ? '#16a34a' : '#f59e0b'

        return (
          <CircleMarker
            key={`${codigo}-${i}`}
            center={[aero.lat, aero.lng]}
            radius={radio}
            pathOptions={{
              color: '#fff',
              weight: 2,
              fillColor: fill,
              fillOpacity: 0.95,
            }}
          >
            <Tooltip direction="top" offset={[0, -6]} opacity={0.95}>
              <strong>{codigo}</strong> — {aero.ciudad}
              {esEscala ? ' (escala)' : ''}
            </Tooltip>
          </CircleMarker>
        )
      })}
    </>
  )
}

export default CapaOverlayRuta
