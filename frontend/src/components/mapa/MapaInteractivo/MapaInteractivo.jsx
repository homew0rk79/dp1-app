import { MapContainer, TileLayer, CircleMarker, Polyline, Popup } from 'react-leaflet'
import 'leaflet/dist/leaflet.css'

import { AEROPUERTOS_MOCK } from '../../../mocks/aeropuertos'
import { RUTAS_MOCK } from '../../../mocks/rutas'
import LeyendaMapa from '../LeyendaMapa/LeyendaMapa'
import { getColorSemaforo, COLORES_SEMAFORO } from '../../../utils/semaforo'
import { formatearCapacidad } from '../../../utils/formatters'
import useConfiguracionStore from '../../../store/configuracionStore'
import styles from './MapaInteractivo.module.css'

// Nota: el cacheo offline de tiles se implementa con leaflet.offline
// una vez que el mapa haya sido cargado al menos una vez con internet.

function MapaInteractivo() {
  const rangosSemaforo = useConfiguracionStore((s) => s.rangosSemaforo)

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

        {/* Rutas activas */}
        {RUTAS_MOCK.map((ruta) => {
          const origen  = AEROPUERTOS_MOCK.find((a) => a.id === ruta.origen)
          const destino = AEROPUERTOS_MOCK.find((a) => a.id === ruta.destino)
          if (!origen || !destino) return null
          return (
            <Polyline
              key={ruta.id}
              positions={[
                [origen.lat,  origen.lng],
                [destino.lat, destino.lng],
              ]}
              pathOptions={{
                color: '#2563eb',
                weight: 1.8,
                opacity: 0.55,
                dashArray: '6 5',
              }}
            />
          )
        })}

        {/* Aeropuertos */}
        {AEROPUERTOS_MOCK.map((aeropuerto) => {
          const color    = getColorSemaforo(aeropuerto.ocupacion, rangosSemaforo)
          const colorHex = COLORES_SEMAFORO[color]

          return (
            <CircleMarker
              key={aeropuerto.id}
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
                    {aeropuerto.nombre}
                    <span className={styles.popupPais}>{aeropuerto.pais}</span>
                  </h4>
                  <p className={styles.popupContinente}>{aeropuerto.continente}</p>
                  <div className={styles.popupFila}>
                    <span>Almacén</span>
                    <span>{formatearCapacidad(aeropuerto.maletasActuales, aeropuerto.capacidad)}</span>
                  </div>
                  <div className={styles.popupFila}>
                    <span>Ocupación</span>
                    <span style={{ color: colorHex, fontWeight: 700 }}>
                      {aeropuerto.ocupacion}%
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
