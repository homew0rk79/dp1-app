import { useState, useMemo, useCallback, useRef } from 'react'
import { ArrowUpDown, Search, Filter, ChevronDown, ChevronRight, Loader2 } from 'lucide-react'
import Semaforo from '../../../common/Semaforo/Semaforo'
import { getColorSemaforo, COLORES_SEMAFORO } from '../../../../utils/semaforo'
import useConfiguracionStore from '../../../../store/configuracionStore'
import useSeleccionStore from '../../../../store/seleccionStore'
import { obtenerRutas, obtenerDetalleRuta } from '../../../../services/rutasService'
import styles from './PanelVuelos.module.css'

/** Convierte un patrón con `*` (wildcard) a una expresión regular segura. */
function patronARegex(patron) {
  try {
    const escaped = patron.replace(/[.+?^${}()|[\]\\]/g, '\\$&').replace(/\*/g, '.*')
    return new RegExp(escaped, 'i')
  } catch {
    return null
  }
}

function PanelVuelos({ ocurrencias, tiempoAnimacion }) {
  const rangosSemaforo = useConfiguracionStore((s) => s.rangosSemaforo)
  const vueloSeleccionado = useSeleccionStore((s) => s.vueloSeleccionado)
  const setVueloSeleccionado = useSeleccionStore((s) => s.setVueloSeleccionado)

  const [busqueda, setBusqueda] = useState('')
  const [patronFiltro, setPatronFiltro] = useState('')     // #67 wildcard/regex
  const [mostrarFiltroPatron, setMostrarFiltroPatron] = useState(false)
  const [ordenarPorOcupacion, setOrdenarPorOcupacion] = useState(false)

  // Drill-down: vuelo expandido para ver envíos (#61)
  const [vueloExpandido, setVueloExpandido] = useState(null) // key del vuelo
  const [enviosVuelo, setEnviosVuelo] = useState([])         // rutas coincidentes
  const [cargandoEnvios, setCargandoEnvios] = useState(false)
  const rutasCacheRef = useRef(null) // cache de todas las rutas (carga perezosa)

  // Drill-down: envío expandido para ver maletas/tramos (#62)
  const [envioExpandido, setEnvioExpandido] = useState(null) // id de la ruta
  const [detalleEnvio, setDetalleEnvio] = useState(null)
  const [cargandoDetalle, setCargandoDetalle] = useState(false)

  const vuelosActivos = useMemo(() => {
    if (!ocurrencias) return []
    return ocurrencias
      .filter((o) => o.salidaAbs <= tiempoAnimacion && tiempoAnimacion <= o.llegadaAbs)
      .map((o) => ({
        key: `${o.origen}-${o.destino}-${o.salidaAbs}`,
        origen: o.origen,
        destino: o.destino,
        maletas: o.maletas,
        capacidadMax: o.capacidadMax,
        pct: o.capacidadMax > 0 ? Math.round((o.maletas / o.capacidadMax) * 1000) / 10 : 0,
        // campo vuelo/codigoVuelo opcional del backend
        vuelo: o.vuelo ?? o.codigoVuelo ?? null,
      }))
  }, [ocurrencias, tiempoAnimacion])

  const vuelosFiltrados = useMemo(() => {
    const q = busqueda.toLowerCase().trim()
    const regex = patronFiltro.trim() ? patronARegex(patronFiltro.trim()) : null

    let lista = vuelosActivos.filter((v) => {
      // Búsqueda parcial por código (#64)
      if (q) {
        const origenDest = `${v.origen}-${v.destino}`.toLowerCase()
        const vuelo = v.vuelo?.toLowerCase() ?? ''
        const matchQ =
          v.origen.toLowerCase().includes(q) ||
          v.destino.toLowerCase().includes(q) ||
          origenDest.includes(q) ||
          vuelo.includes(q)
        if (!matchQ) return false
      }
      // Filtro por patrón wildcard/regex (#67)
      if (regex) {
        const target = v.vuelo ?? `${v.origen}-${v.destino}`
        if (!regex.test(target)) return false
      }
      return true
    })

    if (ordenarPorOcupacion) {
      lista = [...lista].sort((a, b) => b.pct - a.pct)
    }
    return lista
  }, [vuelosActivos, busqueda, patronFiltro, ordenarPorOcupacion])

  // Carga perezosa de todas las rutas (#61)
  const cargarRutas = useCallback(async () => {
    if (rutasCacheRef.current !== null) return rutasCacheRef.current
    const data = await obtenerRutas(500)
    rutasCacheRef.current = Array.isArray(data) ? data : []
    return rutasCacheRef.current
  }, [])

  const toggleVueloExpandido = useCallback(async (v) => {
    const key = v.key
    if (vueloExpandido === key) {
      setVueloExpandido(null)
      setEnviosVuelo([])
      setEnvioExpandido(null)
      setDetalleEnvio(null)
      return
    }
    setVueloExpandido(key)
    setEnvioExpandido(null)
    setDetalleEnvio(null)
    setCargandoEnvios(true)
    try {
      const rutas = await cargarRutas()
      // Filtrar rutas cuyo origen Y destino coincidan con el tramo de esta UT (#61)
      const coincidentes = rutas.filter(
        (r) => r.origen === v.origen && r.destino === v.destino,
      )
      setEnviosVuelo(coincidentes)
    } catch {
      setEnviosVuelo([])
    } finally {
      setCargandoEnvios(false)
    }
  }, [vueloExpandido, cargarRutas])

  const toggleEnvioExpandido = useCallback(async (rutaId) => {
    if (envioExpandido === rutaId) {
      setEnvioExpandido(null)
      setDetalleEnvio(null)
      return
    }
    setEnvioExpandido(rutaId)
    setDetalleEnvio(null)
    setCargandoDetalle(true)
    try {
      const detalle = await obtenerDetalleRuta(rutaId)
      setDetalleEnvio(detalle)
    } catch {
      setDetalleEnvio(null)
    } finally {
      setCargandoDetalle(false)
    }
  }, [envioExpandido])

  if (!ocurrencias) {
    return (
      <p className={styles.vacio}>Disponible al completar una simulación</p>
    )
  }

  return (
    <div className={styles.contenedor}>
      <div className={styles.controles}>
        <div className={styles.searchWrap}>
          <Search size={12} className={styles.searchIcon} />
          <input
            className={styles.searchInput}
            placeholder="Tramo, origen o destino"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
          />
        </div>
        <button
          className={`${styles.sortBtn} ${mostrarFiltroPatron ? styles.sortActivo : ''}`}
          onClick={() => setMostrarFiltroPatron((v) => !v)}
          title="Filtro por patrón (wildcard)"
        >
          <Filter size={12} />
        </button>
        <button
          className={`${styles.sortBtn} ${ordenarPorOcupacion ? styles.sortActivo : ''}`}
          onClick={() => setOrdenarPorOcupacion((v) => !v)}
          title="Ordenar por ocupación"
        >
          <ArrowUpDown size={13} />
        </button>
      </div>

      {/* Filtro por patrón wildcard/regex (#67) */}
      {mostrarFiltroPatron && (
        <div className={styles.searchWrap} style={{ marginTop: '0.25rem' }}>
          <Filter size={12} className={styles.searchIcon} />
          <input
            className={styles.searchInput}
            placeholder="Patrón: LA*, *-JFK, IB[0-9]+"
            value={patronFiltro}
            onChange={(e) => setPatronFiltro(e.target.value)}
          />
        </div>
      )}

      {vuelosFiltrados.length === 0 ? (
        <p className={styles.vacio}>
          {vuelosActivos.length === 0 ? 'Sin vuelos activos en este momento' : 'Sin resultados'}
        </p>
      ) : (
        <ul className={styles.lista}>
          {vuelosFiltrados.map((v) => {
            const color = getColorSemaforo(v.pct, rangosSemaforo)
            const hex = COLORES_SEMAFORO[color]
            const seleccionado = vueloSeleccionado === v.key
            const expandido = vueloExpandido === v.key
            return (
              <li key={v.key} className={styles.itemWrapper}>
                {/* Fila principal del vuelo (#60 + #63) */}
                <div
                  className={`${styles.item} ${seleccionado ? styles.itemSeleccionado : ''} ${expandido ? styles.itemExpandido : ''}`}
                  onClick={() => {
                    setVueloSeleccionado(seleccionado && !expandido ? null : v.key)
                    toggleVueloExpandido(v)
                  }}
                >
                  <div className={styles.itemLeft}>
                    <span className={styles.expandIcon}>
                      {expandido ? <ChevronDown size={11} /> : <ChevronRight size={11} />}
                    </span>
                    <div className={styles.tramo}>
                      <span className={styles.codigo}>{v.origen}</span>
                      <span className={styles.flecha}>→</span>
                      <span className={styles.codigo}>{v.destino}</span>
                    </div>
                  </div>
                  <div className={styles.ocupacion}>
                    <span className={styles.maletas} style={{ color: hex }}>
                      {v.maletas}/{v.capacidadMax}
                    </span>
                    <span className={styles.pct} style={{ color: hex }}>
                      {v.pct.toFixed(1)}%
                    </span>
                    <Semaforo valor={v.pct} />
                  </div>
                </div>

                {/* Barra de ocupación con semáforo (#63) */}
                <div className={styles.barraOcupacion}>
                  <div
                    className={styles.barraFill}
                    style={{ width: `${Math.min(100, v.pct)}%`, background: hex }}
                  />
                </div>

                {/* Drill-down: envíos del vuelo (#61) */}
                {expandido && (
                  <div className={styles.drillDown}>
                    {cargandoEnvios ? (
                      <div className={styles.drillLoading}>
                        <Loader2 size={11} className={styles.spinIcon} />
                        <span>Cargando envíos…</span>
                      </div>
                    ) : enviosVuelo.length === 0 ? (
                      <p className={styles.drillVacio}>
                        Sin envíos directos {v.origen}→{v.destino}
                      </p>
                    ) : (
                      <ul className={styles.drillLista}>
                        {enviosVuelo.map((r) => (
                          <li key={r.id} className={styles.drillItem}>
                            <button
                              className={`${styles.drillBtn} ${envioExpandido === r.id ? styles.drillBtnActivo : ''}`}
                              onClick={() => toggleEnvioExpandido(r.id)}
                              title="Ver tramos / maletas de este envío"
                            >
                              <span>{envioExpandido === r.id ? <ChevronDown size={10} /> : <ChevronRight size={10} />}</span>
                              <span className={styles.drillRutaId}>{r.id}</span>
                              <span className={styles.drillRutaInfo}>
                                {r.origenCiudad} → {r.destinoCiudad}
                              </span>
                            </button>

                            {/* Drill-down: tramos/maletas de ese envío (#62) */}
                            {envioExpandido === r.id && (
                              <div className={styles.tramosWrap}>
                                {cargandoDetalle ? (
                                  <div className={styles.drillLoading}>
                                    <Loader2 size={10} className={styles.spinIcon} />
                                    <span>Cargando tramos…</span>
                                  </div>
                                ) : !detalleEnvio ? (
                                  <p className={styles.drillVacio}>Error al cargar</p>
                                ) : (
                                  <>
                                    <div className={styles.tramoHeader}>
                                      <span>Vuelo</span>
                                      <span>Maletas</span>
                                      <span>Estado</span>
                                    </div>
                                    {detalleEnvio.tramos.map((t) => (
                                      <div key={t.id} className={styles.tramoFila}>
                                        <span className={styles.tramoVuelo}>{t.vuelo}</span>
                                        <span className={styles.tramoMaletas}>
                                          {t.ocupacion}/{t.capacidadMax}
                                        </span>
                                        <span className={styles.tramoEstado}>{t.estado}</span>
                                      </div>
                                    ))}
                                  </>
                                )}
                              </div>
                            )}
                          </li>
                        ))}
                      </ul>
                    )}
                  </div>
                )}
              </li>
            )
          })}
        </ul>
      )}
    </div>
  )
}

export default PanelVuelos
