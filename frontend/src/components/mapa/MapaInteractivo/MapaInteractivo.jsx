import { useEffect, useRef, useState } from 'react'
import L from 'leaflet'
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet'
import 'leaflet/dist/leaflet.css'

import LeyendaMapa from '../LeyendaMapa/LeyendaMapa'
import CanvasVuelos from '../CanvasVuelos/CanvasVuelos'
import DetalleMaletasAeropuerto from '../DetalleMaletasAeropuerto/DetalleMaletasAeropuerto'
import PanelDetalleAeropuerto from '../PanelDetalleAeropuerto/PanelDetalleAeropuerto'
import MapController from './MapController'
import SimulacionControles from '../../SimulacionControles/SimulacionControles'
import { getColorSemaforo, COLORES_SEMAFORO } from '../../../utils/semaforo'
import { formatearCapacidad } from '../../../utils/formatters'
import { formatearDuracion, formatearFechaHora, sumarMinutos } from '../../../utils/tiempos'
import useConfiguracionStore from '../../../store/configuracionStore'
import usePlanificadorStore from '../../../store/planificadorStore'
import useSimulacionStore from '../../../store/simulacionStore'
import useSeleccionStore from '../../../store/seleccionStore'
import useAnimacionTimeline from '../../../hooks/useAnimacionTimeline'
import { simulacionService } from '../../../services/simulacionService'
import {
  FECHA_INICIO_SIMULACION_ALGORITMO,
  TA_EJECUCION_ALGORITMO_MIN,
  SA_SALTO_ALGORITMO_MIN,
} from '../../../constants/restricciones'
import styles from './MapaInteractivo.module.css'

function MapaInteractivo() {
  const rangosSemaforo = useConfiguracionStore((s) => s.rangosSemaforo)
  const fechaInicio = useSimulacionStore((s) => s.parametros.fechaInicio)
  const tiempoSegundos = useSimulacionStore((s) => s.tiempoSegundos)
  const inicioEjecucionReal = useSimulacionStore((s) => s.inicioEjecucionReal)
  const wsVersion = useSimulacionStore((s) => s.wsVersion)

  const aeropuertoSeleccionado = useSeleccionStore((s) => s.aeropuertoSeleccionado)
  const setAeropuertoSeleccionado = useSeleccionStore((s) => s.setAeropuertoSeleccionado)
  const filtrosMapa = useSeleccionStore((s) => s.filtrosMapa)

  const offsetMinutos = (() => {
    const base = new Date(FECHA_INICIO_SIMULACION_ALGORITMO)
    const inicio = new Date(fechaInicio || FECHA_INICIO_SIMULACION_ALGORITMO)
    return Math.round((inicio - base) / 60000)
  })()

  const [aeropuertos, setAeropuertos] = useState([])
  const [ahoraReal, setAhoraReal] = useState(() => new Date())
  const [consumoBloques, setConsumoBloques] = useState([])
  // Ocupación durante el algoritmo (snapshot WS), usada antes de tener manifest
  const [ocupacionWS, setOcupacionWS] = useState({})
  // Ocupación real al minuto exacto de la animación (actualizada c/hora simulada)
  const [ocupacionRealtime, setOcupacionRealtime] = useState({})
  const ultimaHoraRef = useRef(-1)
  const ultimaVersionRef = useRef(-1)

  const snapshot = usePlanificadorStore((s) => s.snapshot)
  const completado = usePlanificadorStore((s) => s.completado)
  const progreso = usePlanificadorStore((s) => s.progreso)
  const alertasCancelacion = usePlanificadorStore((s) => s.alertasCancelacion)
  const removeAlertaCancelacion = usePlanificadorStore((s) => s.removeAlertaCancelacion)

  // Ref para no registrar el mismo timer dos veces (#58)
  const alertaTimersRef = useRef({})

  useEffect(() => {
    alertasCancelacion.forEach((alerta) => {
      if (alertaTimersRef.current[alerta.id]) return
      alertaTimersRef.current[alerta.id] = setTimeout(() => {
        removeAlertaCancelacion(alerta.id)
        delete alertaTimersRef.current[alerta.id]
      }, 9000)
    })
  }, [alertasCancelacion, removeAlertaCancelacion])

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
    actualizarManifest,
  } = useAnimacionTimeline()

  const handleCancelVuelo = async (vuelo) => {
    const horaSalida = sumarMinutos(fechaInicio, vuelo.salidaAbs)
    if (!window.confirm(`¿Seguro que desea cancelar el vuelo de ${vuelo.origen} a ${vuelo.destino} (salida: ${formatearFechaHora(horaSalida)})?`)) {
      return
    }

    const wasPlaying = playing
    pause()

    try {
      await simulacionService.cancelarVuelo({
        origen: vuelo.origen,
        destino: vuelo.destino,
        horaSalidaMinutos: vuelo.horaSalidaMinutos,
      })
      const res = await simulacionService.obtenerManifestAnimacion()
      if (res.data) {
        actualizarManifest(res.data)
      }
    } catch (err) {
      alert("No se pudo cancelar el vuelo: " + (err.response?.data?.error || err.response?.data || err.message))
    } finally {
      if (wasPlaying) play()
    }
  }

  const hayDatosSimulacion = Boolean(manifest || snapshot || completado)
  const fechaSimulada = sumarMinutos(fechaInicio, tiempoDisplay)
  const segundosReales = inicioEjecucionReal
    ? Math.max(0, Math.floor((ahoraReal.getTime() - inicioEjecucionReal) / 1000))
    : tiempoSegundos

  // Cargar aeropuertos base al montar
  useEffect(() => {
    simulacionService.obtenerAeropuertos()
      .then(res => setAeropuertos(res.data))
      .catch((err) => {
        console.error('No se pudieron cargar aeropuertos:', err)
      })
  }, [])

  useEffect(() => {
    const id = setInterval(() => setAhoraReal(new Date()), 1000)
    return () => clearInterval(id)
  }, [])

  useEffect(() => {
    if (!manifest && !completado) {
      setConsumoBloques([])
      return
    }
    simulacionService.obtenerConsumoBloques()
      .then(res => setConsumoBloques(res.data ?? []))
      .catch(() => setConsumoBloques([]))
  }, [manifest, completado])

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
    if (manifest) return undefined
    if (!completado && progreso?.estado !== 'COMPLETADO') return undefined

    let cancelado = false

    simulacionService.obtenerManifestAnimacion()
      .then((res) => {
        if (!cancelado && res.data) cargarManifest(res.data)
      })
      .catch((err) => {
        if (!cancelado) {
          console.error('No se pudo cargar el manifest de animacion:', err)
        }
      })

    return () => {
      cancelado = true
    }
  }, [completado, progreso, manifest, cargarManifest])

  // Actualiza la ocupación real cada hora simulada para evitar sobrecarga de requests
  useEffect(() => {
    if (!manifest) {
      ultimaHoraRef.current = -1
      setOcupacionRealtime({})
      return
    }
    const saltoSimulado = Math.floor(tiempoDisplay / SA_SALTO_ALGORITMO_MIN)
    const versionCambio = wsVersion !== ultimaVersionRef.current
    if (!versionCambio && saltoSimulado === ultimaHoraRef.current) return
    
    ultimaHoraRef.current = saltoSimulado
    ultimaVersionRef.current = wsVersion

    const tiempoMin = Math.floor(tiempoDisplay) + offsetMinutos
    simulacionService.obtenerOcupacionActual(tiempoMin)
      .then(res => setOcupacionRealtime(res.data ?? {}))
      .catch(() => {})
  }, [tiempoDisplay, manifest, offsetMinutos, wsVersion])

  // Ocupación en tiempo real al minuto exacto de la animación
  function getOcupacion(codigo) {
    if (manifest) {
      const aero    = manifest.aeropuertos.find(a => a.codigo === codigo)
      const cap     = aero?.capacidadMax ?? 1
      const maletas = ocupacionRealtime[codigo] ?? 0
      return {
        ocupacion:        Math.round((maletas / cap) * 1000) / 10,
        ocupacionMaletas: maletas,
        capacidadMax:     cap,
      }
    }
    return ocupacionWS[codigo] ?? null
  }

  function aeropuertoPasaFiltros(aeropuerto, pctOcup) {
    const filtros = filtrosMapa?.almacenes
    if (!filtros) return true
    const hayFiltro =
      Boolean(filtros.texto?.trim()) ||
      filtros.continente !== 'Todos' ||
      filtros.semaforo !== 'todos'
    if (!hayFiltro) return true
    if (Array.isArray(filtros.visibles)) {
      return filtros.visibles.includes(aeropuerto.codigo)
    }
    if (filtros.semaforo !== 'todos' && getColorSemaforo(pctOcup, rangosSemaforo) !== filtros.semaforo) return false
    if (filtros.continente !== 'Todos' && aeropuerto.continente !== filtros.continente) return false
    const q = filtros.texto.trim().toLowerCase()
    return !q || aeropuerto.codigo.toLowerCase().includes(q) || aeropuerto.ciudad.toLowerCase().includes(q)
  }

  const maxConsumo = consumoBloques.reduce((max, item) => Math.max(max, item.maletas || 0), 0)
  const bloquesVisibles = consumoBloques.slice(0, 24)
  const scConsumoReal = bloquesVisibles[0]?.saltoMin ?? null

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

        {/* Controlador de mapa: reacciona a selección externa (flyTo) */}
        <MapController aeropuertos={aeropuertos} />

        {/* Animación canvas — solo activo cuando hay manifest */}
        {manifest && (
          <CanvasVuelos
            manifest={manifest}
            tiempoRef={tiempoRef}
            velocidadRef={velocidadRef}
            playing={playing}
            onTick={onTick}
            avanceTickMin={TA_EJECUCION_ALGORITMO_MIN}
            onCancelVuelo={handleCancelVuelo}
            filtrosUT={filtrosMapa?.ut}
          />
        )}

        {/* Aeropuertos: se muestran cuando existe ejecucion, snapshot o manifest */}
        {hayDatosSimulacion && aeropuertos.map((aeropuerto) => {
          const estado    = getOcupacion(aeropuerto.codigo)
          const pctOcup   = estado?.ocupacion ?? 0
          const color     = getColorSemaforo(pctOcup, rangosSemaforo)
          const colorHex  = COLORES_SEMAFORO[color]
          const seleccionado = aeropuertoSeleccionado === aeropuerto.codigo
          const visiblePorFiltro = aeropuertoPasaFiltros(aeropuerto, pctOcup)
          if (!visiblePorFiltro && !seleccionado) return null

          const icon = L.divIcon({
            className: styles.aeropuertoMarker,
            html: `
              <span class="${styles.aeropuertoIcono}" style="--airport-color:${colorHex};">
                <span class="${styles.aeropuertoPista}"></span>
                <span class="${styles.aeropuertoAvion}">&#9992;</span>
              </span>
            `,
            iconSize: seleccionado ? [32, 32] : [28, 28],
            iconAnchor: seleccionado ? [16, 16] : [14, 14],
          })

          return (
            <Marker
              key={aeropuerto.codigo}
              position={[aeropuerto.lat, aeropuerto.lng]}
              icon={icon}
              zIndexOffset={seleccionado ? 500 : 0}
              eventHandlers={{
                click: () => setAeropuertoSeleccionado(
                  seleccionado ? null : aeropuerto.codigo
                ),
              }}
            >
              <Popup className={styles.popupWrapper} maxWidth={400} minWidth={280}>
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

                  <PanelDetalleAeropuerto 
                    codigo={aeropuerto.codigo} 
                    onCancelVuelo={handleCancelVuelo}
                  />

                  {/* Detalle desplegable de envíos en este aeropuerto (simulación activa) */}
                  {manifest && (
                    <DetalleMaletasAeropuerto
                      codigo={aeropuerto.codigo}
                      tiempoMin={Math.floor(tiempoDisplay) + offsetMinutos}
                    />
                  )}
                </div>
              </Popup>
            </Marker>
          )
        })}
        {/* Marcadores de cancelación (#57/#58) — pulsantes, auto-desaparecen */}
        {alertasCancelacion.flatMap((alerta) =>
          [alerta.origen, alerta.destino].map((codigo) => {
            const aero = aeropuertos.find((a) => a.codigo === codigo)
            if (!aero) return null
            const icon = L.divIcon({
              className: styles.cancelacionWrapper,
              html: `<span class="${styles.cancelacionPulse}"></span>`,
              iconSize: [44, 44],
              iconAnchor: [22, 22],
            })
            return (
              <Marker
                key={`alerta-${alerta.id}-${codigo}`}
                position={[aero.lat, aero.lng]}
                icon={icon}
                interactive={false}
                zIndexOffset={1000}
              />
            )
          }).filter(Boolean)
        )}
      </MapContainer>

      {hayDatosSimulacion && <LeyendaMapa />}

      {hayDatosSimulacion && (
        <div className={styles.panelTiempos}>
          <div>
            <span>Fecha-hora simulada</span>
            <strong>{formatearFechaHora(fechaSimulada)}</strong>
          </div>
          <div>
            <span>Fecha-hora real</span>
            <strong>{formatearFechaHora(ahoraReal)}</strong>
          </div>
          <div>
            <span>Transcurrido simulado</span>
            <strong>{formatearDuracion(tiempoDisplay * 60)}</strong>
          </div>
          <div>
            <span>Transcurrido real</span>
            <strong>{formatearDuracion(segundosReales)}</strong>
          </div>
        </div>
      )}

      {(manifest || completado) && bloquesVisibles.length > 0 && (
        <div className={styles.panelConsumo}>
          <div className={styles.consumoHeader}>
            <span>Consumo por bloques</span>
            <strong>{scConsumoReal ? `Sc=${scConsumoReal} min` : 'Sc --'}</strong>
          </div>
          <div className={styles.consumoBarras}>
            {bloquesVisibles.map((item) => (
              <span
                key={item.bloque}
                className={styles.consumoBarra}
                title={`Bloque ${item.bloque}: ${item.maletas} maletas`}
                style={{ height: `${Math.max(8, ((item.maletas || 0) / (maxConsumo || 1)) * 52)}px` }}
              />
            ))}
          </div>
        </div>
      )}

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
