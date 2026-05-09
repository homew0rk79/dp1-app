import { useEffect, useState } from 'react'
import { MapContainer, TileLayer, CircleMarker, Polyline, Popup } from 'react-leaflet'
import 'leaflet/dist/leaflet.css'

import LeyendaMapa from '../LeyendaMapa/LeyendaMapa'
import { getColorSemaforo, COLORES_SEMAFORO } from '../../../utils/semaforo'
import { formatearCapacidad } from '../../../utils/formatters'
import useConfiguracionStore from '../../../store/configuracionStore'
import usePlanificadorWS from '../../../hooks/usePlanificadorWS'
import { simulacionService } from '../../../services/simulacionService'
import styles from './MapaInteractivo.module.css'

function MapaInteractivo() {
  const rangosSemaforo = useConfiguracionStore((s) => s.rangosSemaforo)

  // Aeropuertos base (código, ciudad, lat, lng, capacidadMax)
  const [aeropuertos, setAeropuertos] = useState([])
  // Ocupación en tiempo real: { SKBO: { ocupacion: 380, capacidadMax: 430, semaforo: "AMBAR" }, ... }
  const [ocupacion, setOcupacion] = useState({})
  // Rutas activas: [{ origen: "SKBO", destino: "SEQM", maletas: 247 }, ...]
  const [rutas, setRutas] = useState([])

  const { snapshot } = usePlanificadorWS()

  // Cargar aeropuertos del backend al montar
  useEffect(() => {
    simulacionService.obtenerAeropuertos()
      .then(res => setAeropuertos(res.data))
      .catch(() => {}) // silencioso si el backend no respondió aún
  }, [])

  // Actualizar mapa cuando llega un snapshot del WebSocket
  useEffect(() => {
    if (!snapshot) return

    const nuevaOcupacion = {}
    snapshot.aeropuertos?.forEach(a => {
      nuevaOcupacion[a.codigo] = {
        ocupacion: a.porcentajeOcupacion,
        ocupacionMaletas: a.ocupacion,
        capacidadMax: a.capacidadMax,
        semaforo: a.semaforo,
      }
    })
    setOcupacion(nuevaOcupacion)
    setRutas(snapshot.rutas ?? [])
  }, [snapshot])

  // Construir lookup código → aeropuerto para las rutas
  const porCodigo = {}
  aeropuertos.forEach(a => { porCodigo[a.codigo] = a })

  return (
    <div className={styles.contenedor}>
      <MapContainer
        center={[20, 15]}
        zoom={2}
        minZoom={2}
        className={styles.mapa}
        zoomControl
      >
        <TileLayer
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        />

        {/* Rutas activas desde snapshot */}
        {rutas.map((ruta, i) => {
          const origen  = porCodigo[ruta.origen]
          const destino = porCodigo[ruta.destino]
          if (!origen || !destino) return null
          const grosor = Math.max(1, Math.min(6, ruta.maletas / 100))
          return (
            <Polyline
              key={i}
              positions={[
                [origen.lat,  origen.lng],
                [destino.lat, destino.lng],
              ]}
              pathOptions={{
                color: '#2563eb',
                weight: grosor,
                opacity: 0.55,
                dashArray: '6 5',
              }}
            />
          )
        })}

        {/* Aeropuertos */}
        {aeropuertos.map((aeropuerto) => {
          const estado = ocupacion[aeropuerto.codigo]
          const pctOcup = estado?.ocupacion ?? 0
          const color    = getColorSemaforo(pctOcup, rangosSemaforo)
          const colorHex = COLORES_SEMAFORO[color]

          return (
            <CircleMarker
              key={aeropuerto.codigo}
              center={[aeropuerto.lat, aeropuerto.lng]}
              radius={10}
              pathOptions={{
                fillColor:   colorHex,
                fillOpacity: 0.9,
                color:       'white',
                weight:      2,
              }}
            >
              <Popup className={styles.popupWrapper}>
                <div className={styles.popup}>
                  <h4 className={styles.popupTitulo}>
                    {aeropuerto.ciudad}
                    <span className={styles.popupPais}>{aeropuerto.pais}</span>
                  </h4>
                  <p className={styles.popupContinente}>{aeropuerto.continente}</p>
                  <div className={styles.popupFila}>
                    <span>Almacén</span>
                    <span>
                      {formatearCapacidad(
                        estado?.ocupacionMaletas ?? 0,
                        aeropuerto.capacidadMax
                      )}
                    </span>
                  </div>
                  <div className={styles.popupFila}>
                    <span>Ocupación</span>
                    <span style={{ color: colorHex, fontWeight: 700 }}>
                      {pctOcup.toFixed(1)}%
                    </span>
                  </div>
                </div>
              </Popup>
            </CircleMarker>
          )
        })}
      </MapContainer>

      <LeyendaMapa />
    </div>
  )
}

export default MapaInteractivo
