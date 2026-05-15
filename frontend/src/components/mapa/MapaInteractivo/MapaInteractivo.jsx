import { useEffect, useState } from 'react'
import { MapContainer, TileLayer, CircleMarker, Popup } from 'react-leaflet'
import 'leaflet/dist/leaflet.css'

import LeyendaMapa from '../LeyendaMapa/LeyendaMapa'
import CanvasVuelos from '../CanvasVuelos/CanvasVuelos'
import SimulacionControles from '../../SimulacionControles/SimulacionControles'
import { getColorSemaforo, COLORES_SEMAFORO } from '../../../utils/semaforo'
import { formatearCapacidad } from '../../../utils/formatters'
import useConfiguracionStore from '../../../store/configuracionStore'
import usePlanificadorWS from '../../../hooks/usePlanificadorWS'
import useAnimacionTimeline from '../../../hooks/useAnimacionTimeline'
import { simulacionService } from '../../../services/simulacionService'
import styles from './MapaInteractivo.module.css'

function MapaInteractivo() {
  const rangosSemaforo = useConfiguracionStore((s) => s.rangosSemaforo)

  const [aeropuertos, setAeropuertos] = useState([])
  // Ocupación durante el algoritmo (snapshot WS), usada antes de tener manifest
  const [ocupacionWS, setOcupacionWS] = useState({})

  const { snapshot, completado } = usePlanificadorWS()

  const {
    manifest,
    cargarManifest,
    playing,
    velocidad,
    velocidadRef,
    tiempoRef,
    tiempoDisplay,
    play,
    pause,
    seekTo,
    setVelocidad,
    onTick,
  } = useAnimacionTimeline()

  // Cargar aeropuertos base al montar
  useEffect(() => {
    simulacionService.obtenerAeropuertos()
      .then(res => setAeropuertos(res.data))
      .catch(() => {})
  }, [])

  // Ocupación en tiempo real durante la ejecución del algoritmo
  useEffect(() => {
    if (!snapshot) { setOcupacionWS({}); return }
    const nuevaOcupacion = {}
    snapshot.aeropuertos?.forEach(a => {
      nuevaOcupacion[a.codigo] = {
        ocupacion:       a.porcentajeOcupacion,
        ocupacionMaletas: a.ocupacion,
        capacidadMax:    a.capacidadMax,
        semaforo:        a.semaforo,
      }
    })
    setOcupacionWS(nuevaOcupacion)
  }, [snapshot])

  // Al completar la planificación, cargar el manifest de animación
  useEffect(() => {
    if (!completado) return
    simulacionService.obtenerManifestAnimacion()
      .then(res => {
        if (res.data) cargarManifest(res.data)
      })
      .catch(() => {})
  }, [completado, cargarManifest])

  // Ocupación de aeropuertos: interpola linealmente entre días para suavizar la transición
  function getOcupacion(codigo) {
    if (manifest) {
      const aero = manifest.aeropuertos.find(a => a.codigo === codigo)
      const cap  = aero?.capacidadMax ?? 1
      const diaActual  = Math.floor(tiempoDisplay / 1440)
      const fraccion   = (tiempoDisplay % 1440) / 1440
      const m0 = aero?.ocupacionPorDia?.[diaActual]   ?? aero?.ocupacionPorDia?.[String(diaActual)]   ?? 0
      const m1 = aero?.ocupacionPorDia?.[diaActual+1] ?? aero?.ocupacionPorDia?.[String(diaActual+1)] ?? m0
      const maletas = m0 + (m1 - m0) * fraccion
      return {
        ocupacion:        Math.round((maletas / cap) * 1000) / 10,
        ocupacionMaletas: Math.round(maletas),
        capacidadMax:     cap,
      }
    }
    return ocupacionWS[codigo] ?? null
  }

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

        {/* Animación canvas — solo activo cuando hay manifest */}
        {manifest && (
          <CanvasVuelos
            manifest={manifest}
            tiempoRef={tiempoRef}
            velocidadRef={velocidadRef}
            playing={playing}
            onTick={onTick}
          />
        )}

        {/* Aeropuertos */}
        {aeropuertos.map((aeropuerto) => {
          const estado    = getOcupacion(aeropuerto.codigo)
          const pctOcup   = estado?.ocupacion ?? 0
          const color     = getColorSemaforo(pctOcup, rangosSemaforo)
          const colorHex  = COLORES_SEMAFORO[color]

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
                        aeropuerto.capacidadMax,
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

      {/* Controles de animación — aparecen solo cuando hay manifest */}
      <SimulacionControles
        manifest={manifest}
        tiempoDisplay={tiempoDisplay}
        playing={playing}
        velocidad={velocidad}
        onPlay={play}
        onPause={pause}
        onSeek={seekTo}
        onVelocidad={setVelocidad}
      />
    </div>
  )
}

export default MapaInteractivo
