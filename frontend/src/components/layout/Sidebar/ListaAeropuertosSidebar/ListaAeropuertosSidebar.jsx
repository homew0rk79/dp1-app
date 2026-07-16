import { useEffect, useMemo, useState, useRef } from 'react'
import { Search, ArrowUp, ArrowDown, Package } from 'lucide-react'
import Semaforo from '../../../common/Semaforo/Semaforo'
import { getColorSemaforo, COLORES_SEMAFORO } from '../../../../utils/semaforo'
import { matchPatronCampos } from '../../../../utils/filtros'
import { AEROPUERTOS_MOCK } from '../../../../mocks/aeropuertos'
import {
  proximaSalidaAeropuerto,
  proximaLlegadaAeropuerto,
} from '../../../../utils/planificacionAeropuerto'
import { TIEMPO_SIMULADO_REFERENCIA } from '../../../../constants/tiempoSimulado'
import useSeleccionStore from '../../../../store/seleccionStore'
import useSimulacionStore from '../../../../store/simulacionStore'
import DetalleMaletasAeropuerto from '../../../mapa/DetalleMaletasAeropuerto/DetalleMaletasAeropuerto'
import PlanificadosAeropuerto from './PlanificadosAeropuerto'
import styles from './ListaAeropuertosSidebar.module.css'

const TABS_DETALLE = [
  { id: 'almacen', label: 'En almacén' },
  { id: 'entrantes', label: 'Por llegar' },
  { id: 'salientes', label: 'Por salir' },
]

const CONTINENTES = ['Todos', 'América', 'Europa', 'Asia', 'Oceanía']

function horaLocalActual(gmtOffset) {
  const ahora = new Date()
  const utcMs = ahora.getTime() + ahora.getTimezoneOffset() * 60000
  const local = new Date(utcMs + gmtOffset * 3600000)
  const hh = local.getHours().toString().padStart(2, '0')
  const mm = local.getMinutes().toString().padStart(2, '0')
  const signo = gmtOffset >= 0 ? '+' : ''
  return `${hh}:${mm} (UTC${signo}${gmtOffset})`
}

function formatearProxima(fecha) {
  if (!fecha) return '—'
  const hh = fecha.getUTCHours().toString().padStart(2, '0')
  const mm = fecha.getUTCMinutes().toString().padStart(2, '0')
  const dd = fecha.getUTCDate().toString().padStart(2, '0')
  const mo = (fecha.getUTCMonth() + 1).toString().padStart(2, '0')
  return `${dd}/${mo} ${hh}:${mm}`
}

function cmpTiempo(ta, tb, dir) {
  if (ta == null && tb == null) return 0
  if (ta == null) return 1
  if (tb == null) return -1
  return (ta - tb) * dir
}

function BotonOrdenAero({ campo, label, ordenCampo, ordenDir, onOrdenar }) {
  const activo = ordenCampo === campo
  return (
    <button
      type="button"
      className={`${styles.ordenBtn} ${activo ? styles.ordenBtnActivo : ''}`}
      onClick={() => onOrdenar(campo)}
    >
      {label}
      {activo ? (
        ordenDir === 'asc' ? <ArrowUp size={11} /> : <ArrowDown size={11} />
      ) : (
        <span className={styles.sortNeutral}>↕</span>
      )}
    </button>
  )
}

function ListaAeropuertosSidebar({
  aeropuertosWS,
  getOcupacionPct,
  rangosSemaforo,
  aeropuertoSeleccionado,
  setAeropuertoSeleccionado,
  gmtMap,
  ocultarOcupacion = false,
}) {
  const [busqueda, setBusqueda] = useState('')
  const [horaActual, setHoraActual] = useState(Date.now())
  const tickRef = useRef(null)

  useEffect(() => {
    if (!gmtMap) return
    tickRef.current = setInterval(() => setHoraActual(Date.now()), 30000)
    return () => clearInterval(tickRef.current)
  }, [gmtMap])
  const [continente, setContinente] = useState('Todos')
  const [semaforo, setSemaforo] = useState('todos')
  const [ordenCampo, setOrdenCampo] = useState('ocupacion')
  const [ordenDir, setOrdenDir] = useState('desc')
  const setFiltroAlmacenes = useSeleccionStore((s) => s.setFiltroAlmacenes)

  // Drill-down: aeropuerto expandido y pestaña activa (almacén / por llegar / por salir)
  const [aeropuertoExpandido, setAeropuertoExpandido] = useState(null)
  const [tabDetalle, setTabDetalle] = useState('almacen')
  const tiempoAnimacion = useSimulacionStore((s) => s.tiempoAnimacion)
  const manifest        = useSimulacionStore((s) => s.manifest)
  const offsetMinutos   = manifest?.fechaInicioMinutos > 0 ? manifest.fechaInicioMinutos : 0
  const tiempoAbs       = Math.floor(tiempoAnimacion) + offsetMinutos

  // Vinculación mapa→panel: al seleccionar un aeropuerto (p. ej. click en el
  // marker del mapa) desplazar la lista hasta su item.
  const listaRef = useRef(null)
  useEffect(() => {
    if (!aeropuertoSeleccionado || !listaRef.current) return
    const item = listaRef.current.querySelector(`[data-codigo="${aeropuertoSeleccionado}"]`)
    if (item) item.scrollIntoView({ block: 'nearest', behavior: 'smooth' })
  }, [aeropuertoSeleccionado])

  function toggleDetalle(codigo, e) {
    e.stopPropagation()
    setAeropuertoExpandido((prev) => (prev === codigo ? null : codigo))
    setTabDetalle('almacen')
  }

  const aeropuertosBase = useMemo(() => {
    if (aeropuertosWS.length > 0) {
      return aeropuertosWS.map((a) => ({
        codigo: a.codigo,
        ciudad: a.ciudad,
        continente: a.continente,
        porcentajeOcupacion: getOcupacionPct(a.codigo, a.porcentajeOcupacion),
      }))
    }
    return AEROPUERTOS_MOCK.map((a) => ({
      codigo: a.codigo,
      ciudad: a.ciudad ?? a.nombre,
      continente: a.continente,
      porcentajeOcupacion: a.ocupacion,
    }))
  }, [aeropuertosWS, getOcupacionPct])

  const aeropuertosFiltrados = useMemo(() => {
    const q = busqueda.trim()
    let lista = aeropuertosBase.filter((a) => {
      // Coincide contra código o ciudad; admite comodín * (p. ej. "SK*")
      if (q && !matchPatronCampos(q, a.codigo, a.ciudad)) {
        return false
      }
      if (continente !== 'Todos' && a.continente !== continente) return false
      // Semáforo con categoría "vacío" (0%): los colores excluyen los vacíos
      if (semaforo === 'vacio') {
        if (a.porcentajeOcupacion > 0) return false
      } else if (semaforo !== 'todos') {
        if (a.porcentajeOcupacion === 0) return false
        if (getColorSemaforo(a.porcentajeOcupacion, rangosSemaforo) !== semaforo) return false
      }
      return true
    })

    const ref = new Date(TIEMPO_SIMULADO_REFERENCIA)
    const dir = ordenDir === 'asc' ? 1 : -1

    lista = [...lista].sort((a, b) => {
      if (ordenCampo === 'ocupacion') {
        return (a.porcentajeOcupacion - b.porcentajeOcupacion) * dir
      }
      if (ordenCampo === 'proximaSalida') {
        const sa = proximaSalidaAeropuerto(a.codigo, ref)?.getTime() ?? null
        const sb = proximaSalidaAeropuerto(b.codigo, ref)?.getTime() ?? null
        return cmpTiempo(sa, sb, dir)
      }
      if (ordenCampo === 'proximaLlegada') {
        const la = proximaLlegadaAeropuerto(a.codigo, ref)?.getTime() ?? null
        const lb = proximaLlegadaAeropuerto(b.codigo, ref)?.getTime() ?? null
        return cmpTiempo(la, lb, dir)
      }
      return 0
    })

    return lista
  }, [aeropuertosBase, busqueda, continente, semaforo, rangosSemaforo, ordenCampo, ordenDir])

  useEffect(() => {
    setFiltroAlmacenes({
      texto: busqueda,
      continente,
      semaforo,
      visibles: aeropuertosFiltrados.map((a) => a.codigo),
    })
  }, [busqueda, continente, semaforo, aeropuertosFiltrados, setFiltroAlmacenes])

  function handleOrdenar(campo) {
    if (ordenCampo === campo) {
      setOrdenDir((d) => (d === 'asc' ? 'desc' : 'asc'))
    } else {
      setOrdenCampo(campo)
      setOrdenDir(campo === 'ocupacion' ? 'desc' : 'asc')
    }
  }

  const usaMock = aeropuertosWS.length === 0
  const ref = new Date(TIEMPO_SIMULADO_REFERENCIA)

  return (
    <div className={styles.contenedor}>
      <div className={styles.controles}>
        <div className={styles.searchWrap}>
          <Search size={12} className={styles.searchIcon} />
          <input
            className={styles.searchInput}
            placeholder="Código IATA o ciudad"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
          />
        </div>
        <select
          className={styles.select}
          value={continente}
          onChange={(e) => setContinente(e.target.value)}
        >
          {CONTINENTES.map((c) => (
            <option key={c} value={c}>{c === 'Todos' ? 'Continente: todos' : c}</option>
          ))}
        </select>
        <select
          className={styles.select}
          value={semaforo}
          onChange={(e) => setSemaforo(e.target.value)}
        >
          <option value="todos">Semaforo: todos</option>
          <option value="verde">Verde</option>
          <option value="ambar">Ambar</option>
          <option value="rojo">Rojo</option>
          <option value="vacio">Vacío (0%)</option>
        </select>
      </div>

      <div className={styles.ordenRow}>
        <BotonOrdenAero
          campo="ocupacion"
          label="% Ocupación"
          ordenCampo={ordenCampo}
          ordenDir={ordenDir}
          onOrdenar={handleOrdenar}
        />
        <BotonOrdenAero
          campo="proximaSalida"
          label="Próx. salida"
          ordenCampo={ordenCampo}
          ordenDir={ordenDir}
          onOrdenar={handleOrdenar}
        />
        <BotonOrdenAero
          campo="proximaLlegada"
          label="Próx. llegada"
          ordenCampo={ordenCampo}
          ordenDir={ordenDir}
          onOrdenar={handleOrdenar}
        />
      </div>

      {usaMock && (
        <p className={styles.notaMock}>Datos mock · referencia {TIEMPO_SIMULADO_REFERENCIA.slice(0, 16).replace('T', ' ')} UTC</p>
      )}

      {aeropuertosFiltrados.length === 0 ? (
        <p className={styles.vacio}>Sin aeropuertos que coincidan</p>
      ) : (
        <ul className={styles.lista} ref={listaRef}>
          {aeropuertosFiltrados.map((a) => {
            const pct = a.porcentajeOcupacion
            const color = getColorSemaforo(pct, rangosSemaforo)
            const hex = COLORES_SEMAFORO[color]
            const selected = aeropuertoSeleccionado === a.codigo
            const proxSal = proximaSalidaAeropuerto(a.codigo, ref)
            const proxLleg = proximaLlegadaAeropuerto(a.codigo, ref)

            const expandido = aeropuertoExpandido === a.codigo
            return (
              <li
                key={a.codigo}
                data-codigo={a.codigo}
                className={`${styles.item} ${selected ? styles.itemSeleccionado : ''}`}
                onClick={() => setAeropuertoSeleccionado(selected ? null : a.codigo)}
              >
                <div className={styles.filaPrincipal}>
                  <div className={styles.info}>
                    <span className={styles.codigo}>{a.codigo}</span>
                    <span className={styles.nombre}>{a.ciudad}</span>
                    <span className={styles.continente}>{a.continente}</span>
                    {gmtMap && gmtMap[a.codigo] !== undefined && (
                      <span className={styles.horaLocal}>
                        🕐 {horaLocalActual(gmtMap[a.codigo])}
                      </span>
                    )}
                    <span className={styles.proximas}>
                      Sal: {formatearProxima(proxSal)} · Lleg: {formatearProxima(proxLleg)}
                    </span>
                  </div>
                  <div className={styles.accionesItem}>
                    {!ocultarOcupacion && (
                      <div className={styles.ocupacion}>
                        <span style={{ color: hex }}>{pct.toFixed(1)}%</span>
                        <Semaforo valor={pct} />
                      </div>
                    )}
                    <button
                      type="button"
                      className={`${styles.btnEnvios} ${expandido ? styles.btnEnviosActivo : ''}`}
                      onClick={(e) => toggleDetalle(a.codigo, e)}
                      title="Ver envíos y planificados del almacén"
                    >
                      <Package size={12} />
                    </button>
                  </div>
                </div>

                {expandido && (
                  <div className={styles.detalleAlmacen} onClick={(e) => e.stopPropagation()}>
                    <div className={styles.tabsDetalle}>
                      {TABS_DETALLE.map((t) => (
                        <button
                          key={t.id}
                          type="button"
                          className={`${styles.tabDetalle} ${tabDetalle === t.id ? styles.tabDetalleActivo : ''}`}
                          onClick={() => setTabDetalle(t.id)}
                        >
                          {t.label}
                        </button>
                      ))}
                    </div>
                    {tabDetalle === 'almacen' && (
                      <DetalleMaletasAeropuerto
                        codigo={a.codigo}
                        tiempoMin={tiempoAbs}
                      />
                    )}
                    {tabDetalle === 'entrantes' && (
                      <PlanificadosAeropuerto
                        codigo={a.codigo}
                        tiempoMin={tiempoAbs}
                        modo="entrantes"
                      />
                    )}
                    {tabDetalle === 'salientes' && (
                      <PlanificadosAeropuerto
                        codigo={a.codigo}
                        tiempoMin={tiempoAbs}
                        modo="salientes"
                      />
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

export default ListaAeropuertosSidebar
