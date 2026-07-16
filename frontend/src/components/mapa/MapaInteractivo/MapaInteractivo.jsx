import { memo, useEffect, useMemo, useRef, useState } from 'react'
import L from 'leaflet'
import { CircleMarker, MapContainer, TileLayer, Marker, Popup, Polyline, Tooltip, useMap } from 'react-leaflet'
import 'leaflet/dist/leaflet.css'

import LeyendaMapa from '../LeyendaMapa/LeyendaMapa'
import CanvasVuelos from '../CanvasVuelos/CanvasVuelos'
import DetalleMaletasAeropuerto from '../DetalleMaletasAeropuerto/DetalleMaletasAeropuerto'
import PanelDetalleAeropuerto from '../PanelDetalleAeropuerto/PanelDetalleAeropuerto'
import MapController from './MapController'
import PanelConfiguracionMapa from './PanelConfiguracionMapa'
import PanelBusquedaRuta from './PanelBusquedaRuta'
import SimulacionControles from '../../SimulacionControles/SimulacionControles'
import { getColorSemaforo, COLORES_SEMAFORO } from '../../../utils/semaforo'
import { formatearCapacidad } from '../../../utils/formatters'
import { formatearFechaHora, sumarMinutos } from '../../../utils/tiempos'
import useConfiguracionStore from '../../../store/configuracionStore'
import usePlanificadorStore from '../../../store/planificadorStore'
import useSimulacionStore from '../../../store/simulacionStore'
import useSeleccionStore from '../../../store/seleccionStore'
import useAnimacionTimeline from '../../../hooks/useAnimacionTimeline'
import { simulacionService } from '../../../services/simulacionService'
import { AEROPUERTOS_POR_CODIGO } from '../../../mocks/aeropuertos'
import {
  FECHA_INICIO_SIMULACION_ALGORITMO,
  TA_EJECUCION_ALGORITMO_MIN,
  SA_SALTO_ALGORITMO_MIN,
} from '../../../constants/restricciones'
import styles from './MapaInteractivo.module.css'

const AEROPUERTOS_EXTRA_POR_CODIGO = {
  OPKC: {
    codigo: 'OPKC',
    nombre: 'Karachi',
    ciudad: 'Karachi',
    pais: 'Pakistan',
    continente: 'Asia',
    lat: 24.9,
    lng: 67.15,
    capacidadMax: 410,
  },
  UBBB: {
    codigo: 'UBBB',
    nombre: 'Baku',
    ciudad: 'Baku',
    pais: 'Azerbaiyan',
    continente: 'Asia',
    lat: 40.4672,
    lng: 50.0467,
    capacidadMax: 400,
  },
}

const STORAGE_CONFIG_MAPA = 'tasf.configuracionMapa'
const STORAGE_ALMACENES = 'tasf.almacenesMapa'
const STORAGE_UT = 'tasf.unidadesTransporteMapa'
const STORAGE_TRAMOS = 'tasf.tramosMapa'

const CONFIG_MAPA_DEFAULT = {
  mostrarAlmacenes: true,
  mostrarUT: true,
  mostrarTramos: true,
  zoomInicial: 2,
  centroLat: 20,
  centroLng: 15,
  colorTramos: '#f97316',
  colorAlmacenes: '#22c55e',
}

function colorValido(valor, fallback) {
  return /^#[0-9a-fA-F]{6}$/.test(String(valor || '')) ? valor : fallback
}

function normalizarConfigMapa(config = {}) {
  return {
    ...CONFIG_MAPA_DEFAULT,
    ...config,
    zoomInicial: Number.isFinite(Number(config.zoomInicial)) ? Number(config.zoomInicial) : CONFIG_MAPA_DEFAULT.zoomInicial,
    centroLat: Number.isFinite(Number(config.centroLat)) ? Number(config.centroLat) : CONFIG_MAPA_DEFAULT.centroLat,
    centroLng: Number.isFinite(Number(config.centroLng)) ? Number(config.centroLng) : CONFIG_MAPA_DEFAULT.centroLng,
    colorTramos: colorValido(config.colorTramos, CONFIG_MAPA_DEFAULT.colorTramos),
    colorAlmacenes: colorValido(config.colorAlmacenes, CONFIG_MAPA_DEFAULT.colorAlmacenes),
  }
}

function normalizarCodigo(valor) {
  return String(valor || '').trim().toUpperCase()
}

function leerStorage(clave, fallback) {
  try {
    const raw = window.localStorage.getItem(clave)
    return raw ? JSON.parse(raw) : fallback
  } catch {
    return fallback
  }
}

function guardarStorage(clave, valor) {
  try {
    window.localStorage.setItem(clave, JSON.stringify(valor))
  } catch {
    // El mantenimiento continua en memoria si localStorage no esta disponible.
  }
}

function ConfiguracionVistaMapa({ config }) {
  const map = useMap()

  useEffect(() => {
    const lat = Number(config.centroLat)
    const lng = Number(config.centroLng)
    const zoom = Number(config.zoomInicial)
    if (!Number.isFinite(lat) || !Number.isFinite(lng) || !Number.isFinite(zoom)) return
    map.setView([lat, lng], zoom)
  }, [config.centroLat, config.centroLng, config.zoomInicial, map])

  return null
}

function coordsTramo(tramo) {
  const origen = tramo?.aeropuertoOrigen
  const destino = tramo?.aeropuertoDestino
  if (!origen || !destino) return null
  const latOrigen = Number(origen.lat)
  const lngOrigen = Number(origen.lng)
  const latDestino = Number(destino.lat)
  const lngDestino = Number(destino.lng)
  if (![latOrigen, lngOrigen, latDestino, lngDestino].every(Number.isFinite)) return null
  return [[latOrigen, lngOrigen], [latDestino, lngDestino]]
}

function coordsRuta(ruta) {
  return (ruta?.tramos || [])
    .map(coordsTramo)
    .filter(Boolean)
}

function RutaBuscadaVistaMapa({ busqueda }) {
  const map = useMap()

  useEffect(() => {
    if (!busqueda) return
    const coords = [
      ...coordsRuta(busqueda.rutaActual).flat(),
      ...coordsRuta(busqueda.rutaAnterior).flat(),
    ]
    if (coords.length < 2) return
    map.fitBounds(coords, { padding: [64, 64], maxZoom: 6 })
  }, [map, busqueda])

  if (!busqueda) return null

  const capas = [
    {
      clave: 'actual',
      ruta: busqueda.rutaActual,
      etiqueta: 'Ruta actual',
      color: '#f97316',
      dashArray: null,
      weight: 5,
      opacity: 0.96,
    },
    {
      clave: 'anterior',
      ruta: busqueda.rutaAnterior,
      etiqueta: 'Ruta anterior',
      color: '#7c3aed',
      dashArray: '9 7',
      weight: 4,
      opacity: 0.78,
    },
  ]

  return (
    <>
      {capas.flatMap((capa) => (
        coordsRuta(capa.ruta).map((coords, index) => (
          <Polyline
            key={`ruta-buscada-${capa.clave}-${capa.ruta?.idRuta}-${index}`}
            positions={coords}
            pathOptions={{
              color: capa.color,
              weight: capa.weight,
              opacity: capa.opacity,
              dashArray: capa.dashArray,
              lineCap: 'round',
              lineJoin: 'round',
            }}
          >
            <Popup>
              <strong>{capa.etiqueta}</strong><br />
              {capa.ruta?.tramos?.[index]?.origen} - {capa.ruta?.tramos?.[index]?.destino}<br />
              {capa.ruta?.tramos?.[index]?.salida} - {capa.ruta?.tramos?.[index]?.llegada}
            </Popup>
          </Polyline>
        ))
      ))}

      {(busqueda.rutaActual?.aeropuertos || []).map((aero) => {
        const lat = Number(aero.lat)
        const lng = Number(aero.lng)
        if (!Number.isFinite(lat) || !Number.isFinite(lng)) return null
        return (
          <CircleMarker
            key={`ruta-buscada-aero-${aero.codigo}`}
            center={[lat, lng]}
            radius={7}
            pathOptions={{
              color: '#ffffff',
              weight: 2,
              fillColor: '#f97316',
              fillOpacity: 0.95,
            }}
          >
            <Tooltip direction="top" offset={[0, -6]} opacity={0.95}>
              <strong>{aero.codigo}</strong> - {aero.ciudad || aero.nombre}
            </Tooltip>
          </CircleMarker>
        )
      })}
    </>
  )
}

function endpointMantenimientoNoDisponible(err) {
  return err?.response?.status === 404 || err?.response?.status === 405
}

const TRAMOS_PATH_OPTIONS_BASE = { weight: 3.5, opacity: 0.95, dashArray: '8 7', lineCap: 'round', lineJoin: 'round' }

const TramosLayer = memo(function TramosLayer({ tramosVisibles, colorTramos }) {
  const pathOptions = useMemo(() => ({ ...TRAMOS_PATH_OPTIONS_BASE, color: colorTramos }), [colorTramos])
  return tramosVisibles.map((tramo) => (
    <Polyline key={tramo.renderKey} positions={[tramo.coordsOrigen, tramo.coordsDestino]} pathOptions={pathOptions}>
      <Popup>
        <strong>{tramo.utAsignada}</strong><br />
        {tramo.origen} - {tramo.destino}<br />
        {tramo.estado}
      </Popup>
    </Polyline>
  ))
})

function MapaInteractivo() {
  const rangosSemaforo = useConfiguracionStore((s) => s.rangosSemaforo)
  const fechaInicio = useSimulacionStore((s) => s.parametros.fechaInicio)
  const wsVersion = useSimulacionStore((s) => s.wsVersion)
  const escenarioActivo = useSimulacionStore((s) => s.escenarioActivo)
  const estadoEjecucion = useSimulacionStore((s) => s.estadoEjecucion)

  const aeropuertoSeleccionado = useSeleccionStore((s) => s.aeropuertoSeleccionado)
  const setAeropuertoSeleccionado = useSeleccionStore((s) => s.setAeropuertoSeleccionado)
  const filtrosMapa = useSeleccionStore((s) => s.filtrosMapa)

  const [aeropuertos, setAeropuertos] = useState([])
  const [panelConfigAbierto, setPanelConfigAbierto] = useState(false)
  const [panelRutaAbierto, setPanelRutaAbierto] = useState(false)
  const [rutaBuscada, setRutaBuscada] = useState(null)
  const [rutaBuscadaError, setRutaBuscadaError] = useState('')
  const [rutaBuscadaCargando, setRutaBuscadaCargando] = useState(false)
  const [idsPruebaRutas, setIdsPruebaRutas] = useState({ origenes: [], ejemplos: [], idsEnvio: [], idsMaleta: [], mensaje: '' })
  const [configMapa, setConfigMapa] = useState(() => normalizarConfigMapa(leerStorage(STORAGE_CONFIG_MAPA, CONFIG_MAPA_DEFAULT)))
  const [unidadesTransporte, setUnidadesTransporte] = useState(() => leerStorage(STORAGE_UT, []))
  const [tramosMantenimiento, setTramosMantenimiento] = useState(() => leerStorage(STORAGE_TRAMOS, []))
  // Ocupación durante el algoritmo (snapshot WS), usada antes de tener manifest
  const [ocupacionWS, setOcupacionWS] = useState({})
  // Ocupación real al minuto exacto de la animación (actualizada c/hora simulada)
  const [ocupacionRealtime, setOcupacionRealtime] = useState({})
  const ultimaHoraRef = useRef(-1)
  const ultimaVersionRef = useRef(-1)

  const snapshot = usePlanificadorStore((s) => s.snapshot)
  const completado = usePlanificadorStore((s) => s.completado)
  const colapso = usePlanificadorStore((s) => s.colapso)
  const progreso = usePlanificadorStore((s) => s.progreso)
  const alertasCancelacion = usePlanificadorStore((s) => s.alertasCancelacion)
  const removeAlertaCancelacion = usePlanificadorStore((s) => s.removeAlertaCancelacion)
  const vuelosCancelados = usePlanificadorStore((s) => s.vuelosCancelados)
  const addVueloCancelado = usePlanificadorStore((s) => s.addVueloCancelado)

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

  const offsetMinutos = (() => {
    if (manifest?.fechaInicioMinutos > 0) return manifest.fechaInicioMinutos
    const base = new Date(FECHA_INICIO_SIMULACION_ALGORITMO)
    const inicio = new Date(fechaInicio || FECHA_INICIO_SIMULACION_ALGORITMO)
    return Math.round((inicio - base) / 60000)
  })()

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
      addVueloCancelado({
        origen: vuelo.origen,
        destino: vuelo.destino,
        salidaAbs: vuelo.salidaAbs,
        llegadaAbs: vuelo.llegadaAbs,
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
  // Cargar datos de mantenimiento al montar. Backend es la fuente principal;
  // localStorage queda como respaldo si el backend no esta disponible.
  useEffect(() => {
    simulacionService.obtenerConfiguracionMapa()
      .then(res => setConfigMapa(normalizarConfigMapa(res.data ?? {})))
      .catch(() => setConfigMapa(normalizarConfigMapa(leerStorage(STORAGE_CONFIG_MAPA, CONFIG_MAPA_DEFAULT))))

    simulacionService.listarAlmacenesMantenimiento()
      .then(res => {
        setAeropuertos(res.data ?? [])
      })
      .catch((err) => {
        console.error('No se pudieron cargar aeropuertos:', err)
        setAeropuertos(leerStorage(STORAGE_ALMACENES, []))
      })

    simulacionService.listarUTMantenimiento()
      .then(res => setUnidadesTransporte(res.data ?? []))
      .catch(() => setUnidadesTransporte(leerStorage(STORAGE_UT, [])))

    simulacionService.listarTramosMantenimiento()
      .then(res => setTramosMantenimiento(res.data ?? []))
      .catch(() => setTramosMantenimiento(leerStorage(STORAGE_TRAMOS, [])))

    simulacionService.obtenerIdsPruebaRutas()
      .then(res => setIdsPruebaRutas(res.data ?? { origenes: [], ejemplos: [], idsEnvio: [], idsMaleta: [], mensaje: '' }))
      .catch((err) => {
        console.warn('[rutas-id] No se pudieron cargar IDs reales de prueba:', err)
        setIdsPruebaRutas({ origenes: [], ejemplos: [], idsEnvio: [], idsMaleta: [], mensaje: 'No se pudieron cargar IDs reales de prueba.' })
      })
  }, [])

  useEffect(() => {
    guardarStorage(STORAGE_CONFIG_MAPA, configMapa)
  }, [configMapa])

  useEffect(() => {
    if (aeropuertos.length > 0) guardarStorage(STORAGE_ALMACENES, aeropuertos)
  }, [aeropuertos])

  useEffect(() => {
    guardarStorage(STORAGE_UT, unidadesTransporte)
  }, [unidadesTransporte])

  useEffect(() => {
    guardarStorage(STORAGE_TRAMOS, tramosMantenimiento)
  }, [tramosMantenimiento])

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

  // Al completar la planificación, cargar el manifest de animación.
  // En día a día el estado se queda en PLANIFICANDO (el scheduler sigue
  // replanificando), así que la señal de "listo" es porcentaje 100.
  useEffect(() => {
    if (manifest) return undefined
    const diaADiaListo = escenarioActivo === 'DIA_A_DIA' &&
      progreso?.estado === 'PLANIFICANDO' && progreso?.porcentaje === 100
    if (!completado && progreso?.estado !== 'COMPLETADO' && !diaADiaListo) return undefined

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

  // Día a día: cada replanificación (tick de 5 min o cancelación) publica un
  // snapshot; refrescamos el manifest sin tocar el tiempo ni el play/pause.
  useEffect(() => {
    if (!snapshot || escenarioActivo !== 'DIA_A_DIA') return
    if (!useSimulacionStore.getState().manifest) return
    simulacionService.obtenerManifestAnimacion()
      .then((res) => { if (res.data) actualizarManifest(res.data) })
      .catch(() => {})
  }, [snapshot, escenarioActivo, actualizarManifest])

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

  const aeropuertosPorCodigo = useMemo(() => ({
    ...AEROPUERTOS_POR_CODIGO,
    ...AEROPUERTOS_EXTRA_POR_CODIGO,
    ...Object.fromEntries(
      aeropuertos
        .filter((a) => a?.codigo && Number.isFinite(Number(a.lat)) && Number.isFinite(Number(a.lng)))
        .map((a) => [normalizarCodigo(a.codigo), { ...a, lat: Number(a.lat), lng: Number(a.lng) }]),
    ),
  }), [aeropuertos])

  const tramosVisibles = useMemo(() =>
    tramosMantenimiento
      .map((tramo, index) => {
        const origen = normalizarCodigo(tramo.origen)
        const destino = normalizarCodigo(tramo.destino)
        const origenAero = aeropuertosPorCodigo[origen]
        const destinoAero = aeropuertosPorCodigo[destino]
        const coordsOrigen = origenAero ? [origenAero.lat, origenAero.lng] : null
        const coordsDestino = destinoAero ? [destinoAero.lat, destinoAero.lng] : null
        return {
          ...tramo,
          renderKey: tramo.id ?? `${origen}-${destino}-${index}`,
          origen, destino, origenAero, destinoAero, coordsOrigen, coordsDestino,
          dibujable: Boolean(coordsOrigen && coordsDestino),
        }
      })
      .filter((t) => t.dibujable)
  , [tramosMantenimiento, aeropuertosPorCodigo])

  async function guardarConfigMapa(siguiente) {
    try {
      const res = await simulacionService.guardarConfiguracionMapa(siguiente)
      setConfigMapa(normalizarConfigMapa(res.data ?? siguiente))
    } catch (err) {
      console.error('No se pudo guardar configuracion del mapa:', err)
      guardarStorage(STORAGE_CONFIG_MAPA, siguiente)
    }
  }

  async function guardarAlmacen(almacen) {
    if (!almacen.id) throw new Error('Solo se permite editar almacenes existentes.')
    const res = await simulacionService.actualizarAlmacenMantenimiento(almacen.id, almacen)
    const guardado = res.data
    setAeropuertos((actuales) => {
      return actuales.map((a) => a.id === guardado.id ? guardado : a)
    })
  }

  async function guardarUnidad(unidad) {
    if (!unidad.id) throw new Error('Solo se permite editar UT existentes.')
    let res
    try {
      res = await simulacionService.actualizarCapacidadUTMantenimiento(unidad.id, {
        capacidadMax: unidad.capacidadMax,
      })
    } catch (err) {
      if (!endpointMantenimientoNoDisponible(err)) throw err
      res = await simulacionService.actualizarUTMantenimiento(unidad.id, unidad)
    }
    const guardado = res.data
    setUnidadesTransporte((actuales) => {
      return actuales.map((ut) => ut.id === guardado.id ? guardado : ut)
    })
  }

  async function guardarTramo(tramo) {
    if (!tramo.id) throw new Error('Solo se permite editar tramos existentes.')
    const res = await simulacionService.actualizarTramoMantenimiento(tramo.id, {
      utAsignada: tramo.utAsignada,
      origen: (tramo.origen || '').trim().toUpperCase(),
      destino: (tramo.destino || '').trim().toUpperCase(),
      horaSalida: tramo.horaSalida,
      horaLlegada: tramo.horaLlegada,
    })
    const guardado = res.data
    setTramosMantenimiento((actuales) => {
      return actuales.map((t) => t.id === guardado.id ? guardado : t)
    })
  }

  function mensajeBusquedaRuta(err, fallback) {
    const data = err?.response?.data
    if (typeof data === 'string' && data.trim()) return data
    if (data?.error) return data.error
    return fallback
  }

  async function buscarRutaMaleta({ origen, idEnvio, numeroMaleta }) {
    setRutaBuscadaCargando(true)
    setRutaBuscadaError('')
    const endpoint = `/api/envios/${encodeURIComponent(origen)}/${encodeURIComponent(idEnvio)}/maletas/${encodeURIComponent(numeroMaleta)}/ruta`
    console.log('[rutas-id] ID ingresado:', { origen, idEnvio, numeroMaleta })
    console.log('[rutas-id] endpoint llamado:', endpoint)
    try {
      const res = await simulacionService.obtenerRutaMaleta(origen, idEnvio, numeroMaleta)
      console.log('[rutas-id] respuesta backend:', res.data)
      setRutaBuscada(res.data)
    } catch (err) {
      console.log('[rutas-id] respuesta backend:', err?.response?.data ?? err)
      setRutaBuscada(null)
      setRutaBuscadaError(mensajeBusquedaRuta(err, 'No se encontró una maleta con ese ID.'))
    } finally {
      setRutaBuscadaCargando(false)
    }
  }

  async function buscarRutaEnvio({ origen, idEnvio }) {
    setRutaBuscadaCargando(true)
    setRutaBuscadaError('')
    const endpoint = `/api/envios/${encodeURIComponent(origen)}/${encodeURIComponent(idEnvio)}/ruta`
    console.log('[rutas-id] ID ingresado:', { origen, idEnvio })
    console.log('[rutas-id] endpoint llamado:', endpoint)
    try {
      const res = await simulacionService.obtenerRutaEnvio(origen, idEnvio)
      console.log('[rutas-id] respuesta backend:', res.data)
      setRutaBuscada(res.data)
    } catch (err) {
      console.log('[rutas-id] respuesta backend:', err?.response?.data ?? err)
      setRutaBuscada(null)
      setRutaBuscadaError(mensajeBusquedaRuta(err, 'No se encontró un envío con ese ID.'))
    } finally {
      setRutaBuscadaCargando(false)
    }
  }

  function limpiarRutaBuscada() {
    setRutaBuscada(null)
    setRutaBuscadaError('')
  }

  return (
    <div className={styles.contenedor}>
      <MapContainer
        center={[configMapa.centroLat, configMapa.centroLng]}
        zoom={configMapa.zoomInicial}
        minZoom={0}
        className={styles.mapa}
        zoomControl
      >
        <TileLayer
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
        />

        {/* Controlador de mapa: reacciona a selección externa (flyTo) */}
        <MapController aeropuertos={aeropuertos} mostrarTramos={configMapa.mostrarTramos} />
        <ConfiguracionVistaMapa config={configMapa} />
        <RutaBuscadaVistaMapa busqueda={rutaBuscada} />

        {configMapa.mostrarTramos && (
          <TramosLayer tramosVisibles={tramosVisibles} colorTramos={configMapa.colorTramos} />
        )}

        {/* Animación canvas — solo activo cuando hay manifest */}
        {manifest && configMapa.mostrarUT && (
          <CanvasVuelos
            manifest={manifest}
            tiempoRef={tiempoRef}
            velocidadRef={velocidadRef}
            playing={playing}
            onTick={onTick}
            avanceTickMin={TA_EJECUCION_ALGORITMO_MIN}
            onCancelVuelo={handleCancelVuelo}
            filtrosUT={filtrosMapa?.ut}
            vuelosCancelados={vuelosCancelados}
          />
        )}

        {/* Aeropuertos: se muestran cuando existe ejecucion, snapshot o manifest */}
        {configMapa.mostrarAlmacenes && aeropuertos.map((aeropuerto) => {
          const estado    = getOcupacion(aeropuerto.codigo)
          const pctOcup   = estado?.ocupacion ?? 0
          const color     = getColorSemaforo(pctOcup, rangosSemaforo)
          const colorHex  = (estadoEjecucion === 'PLANIFICANDO' && escenarioActivo !== 'DIA_A_DIA') ? '#94a3b8' : (color === 'verde' ? (configMapa.colorAlmacenes || COLORES_SEMAFORO.verde) : COLORES_SEMAFORO[color])
          const seleccionado = aeropuertoSeleccionado === aeropuerto.codigo
          const visiblePorFiltro = aeropuertoPasaFiltros(aeropuerto, pctOcup)
          if (hayDatosSimulacion && !visiblePorFiltro && !seleccionado) return null

          const icon = L.divIcon({
            className: styles.aeropuertoMarker,
            html: `
              <span class="${styles.aeropuertoIcono}${seleccionado ? ' ' + styles.aeropuertoIconoSeleccionado : ''}" style="--airport-color:${colorHex};">
                ${seleccionado ? `<span class="${styles.aeropuertoPulse}"></span>` : ''}
                <span class="${styles.aeropuertoPista}"></span>
                <span class="${styles.aeropuertoAvion}">&#9992;</span>
              </span>
            `,
            iconSize: seleccionado ? [36, 36] : [28, 28],
            iconAnchor: seleccionado ? [18, 18] : [14, 14],
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

      <button
        type="button"
        className={styles.btnConfiguracion}
        onClick={() => setPanelConfigAbierto(true)}
      >
        Configuración
      </button>

      <button
        type="button"
        className={styles.btnBuscarRuta}
        onClick={() => setPanelRutaAbierto(true)}
      >
        Rutas por ID
      </button>

      <PanelConfiguracionMapa
        abierto={panelConfigAbierto}
        onCerrar={() => setPanelConfigAbierto(false)}
        configMapa={configMapa}
        onConfigMapaChange={setConfigMapa}
        onGuardarConfigMapa={guardarConfigMapa}
        almacenes={aeropuertos}
        onGuardarAlmacen={guardarAlmacen}
        unidades={unidadesTransporte}
        onGuardarUnidad={guardarUnidad}
        tramos={tramosMantenimiento}
        onGuardarTramo={guardarTramo}
        velocidad={velocidad}
        onVelocidad={setVelocidad}
        styles={styles}
      />

      <PanelBusquedaRuta
        abierto={panelRutaAbierto}
        onCerrar={() => setPanelRutaAbierto(false)}
        onBuscarMaleta={buscarRutaMaleta}
        onBuscarEnvio={buscarRutaEnvio}
        onLimpiar={limpiarRutaBuscada}
        resultado={rutaBuscada}
        cargando={rutaBuscadaCargando}
        error={rutaBuscadaError}
        idsPrueba={idsPruebaRutas}
        tiempoActualAbs={Math.floor(tiempoDisplay) + offsetMinutos}
        styles={styles}
      />

      {hayDatosSimulacion && <LeyendaMapa />}

      {/* Botón ir al colapso */}
      {colapso && manifest && colapso.minutosColapso > 0 && (
        <button
          className={styles.btnColapso}
          onClick={() => {
            const tiempoRel = colapso.minutosColapso - (manifest.fechaInicioMinutos || 0)
            seekTo(Math.max(0, Math.min(tiempoRel, manifest.duracionTotalMinutos)))
          }}
        >
          ⚡ Ir al momento del colapso
        </button>
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
