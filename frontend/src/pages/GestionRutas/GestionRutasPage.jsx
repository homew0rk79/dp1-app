import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  Search,
  Filter,
  Route,
  Plane,
  Target,
  AlertTriangle,
  ChevronRight,
  MapPin,
  Clock3,
  X,
  RefreshCw,
  Building2,
} from 'lucide-react'

import Tabla from '../../components/common/Tabla/Tabla'
import tabStyles from '../../components/common/Tabla/Tabla.module.css'
import BarraProgreso from '../../components/common/BarraProgreso/BarraProgreso'
import Semaforo from '../../components/common/Semaforo/Semaforo'
import Badge from '../../components/common/Badge/Badge'
import { obtenerRutas, obtenerDetalleRuta } from '../../services/rutasService'
import useConfiguracionStore from '../../store/configuracionStore'
import usePlanificadorWS from '../../hooks/usePlanificadorWS'
import { getColorSemaforo } from '../../utils/semaforo'
import styles from './GestionRutasModule.module.css'

function textoEstado(estado) {
  const m = {
    pendiente: 'Pendiente',
    en_transito: 'En tránsito',
    completado: 'Completado',
  }
  return m[estado] ?? estado
}

function BadgeCumplimiento({ cumplimiento }) {
  const mapa = {
    verde: styles.riesgoVerde,
    ambar: styles.riesgoAmbar,
    rojo: styles.riesgoRojo,
  }
  return <span className={`${styles.riesgoDot} ${mapa[cumplimiento] || ''}`} title={cumplimiento} />
}

function KpiCard({ titulo, valor, subtitulo, icono, variante = 'default' }) {
  return (
    <article className={`${styles.kpiCard} ${styles[`kpiCard--${variante}`]}`}>
      <div className={styles.kpiHeader}>
        <span className={styles.kpiLabel}>{titulo}</span>
        <div className={styles.kpiIcono}>{icono}</div>
      </div>
      <div className={styles.kpiValor}>{valor}</div>
      <div className={styles.kpiSubtitulo}>{subtitulo}</div>
    </article>
  )
}

function FilaDetalle({ icono, label, value }) {
  return (
    <div className={styles.detailRow}>
      <div className={styles.detailIcon}>{icono}</div>
      <div>
        <div className={styles.detailLabel}>{label}</div>
        <div className={styles.detailValue}>{value}</div>
      </div>
    </div>
  )
}

function varianteBarra(cumplimiento) {
  if (cumplimiento === 'rojo') return 'rojo'
  if (cumplimiento === 'ambar') return 'ambar'
  return 'azul'
}

function GestionRutasPage() {
  const navigate = useNavigate()
  const rangosSemaforo = useConfiguracionStore((s) => s.rangosSemaforo)
  const { snapshot } = usePlanificadorWS()
  const [rutas, setRutas] = useState([])
  const [cargando, setCargando] = useState(true)
  const [detalle, setDetalle] = useState(null)
  const [cargandoDetalle, setCargandoDetalle] = useState(false)

  const [query, setQuery] = useState('')
  const [origen, setOrigen] = useState('Todos')
  const [destino, setDestino] = useState('Todos')

  const [selectedId, setSelectedId] = useState(null)
  const [panelAbierto, setPanelAbierto] = useState(false)

  const cargarLista = useCallback(async () => {
    setCargando(true)
    try {
      const data = await obtenerRutas()
      setRutas(data)
    } finally {
      setCargando(false)
    }
  }, [])

  useEffect(() => {
    cargarLista()
  }, [cargarLista])

  useEffect(() => {
    if (!selectedId) {
      setDetalle(null)
      return
    }
    let cancel = false
    setCargandoDetalle(true)
    obtenerDetalleRuta(selectedId).then((d) => {
      if (!cancel) setDetalle(d)
      if (!cancel) setCargandoDetalle(false)
    })
    return () => {
      cancel = true
    }
  }, [selectedId])

  const origenes = useMemo(() => {
    const u = new Set(rutas.map((r) => r.origen))
    return ['Todos', ...[...u].sort()]
  }, [rutas])

  const destinos = useMemo(() => {
    const u = new Set(rutas.map((r) => r.destino))
    return ['Todos', ...[...u].sort()]
  }, [rutas])

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase()
    return rutas.filter((r) => {
      const matchQ =
        !q ||
        r.id.toLowerCase().includes(q) ||
        r.origen.toLowerCase().includes(q) ||
        r.destino.toLowerCase().includes(q) ||
        r.origenCiudad.toLowerCase().includes(q) ||
        r.destinoCiudad.toLowerCase().includes(q)

      const matchOrigen = origen === 'Todos' || r.origen === origen
      const matchDest = destino === 'Todos' || r.destino === destino

      return matchQ && matchOrigen && matchDest
    })
  }, [rutas, query, origen, destino])

  useEffect(() => {
    if (filtered.length === 0) return
    if (!selectedId || !filtered.some((r) => r.id === selectedId)) {
      setSelectedId(filtered[0].id)
    }
  }, [filtered, selectedId])

  const selected = filtered.find((r) => r.id === selectedId) ?? filtered[0]

  const limpiarFiltros = () => {
    setQuery('')
    setOrigen('Todos')
    setDestino('Todos')
  }

  const kpisEstaticos = useMemo(() => {
    const activas = rutas.filter((r) => r.estado !== 'sin_ruta').length
    const verdes = rutas.filter((r) => r.cumplimiento === 'verde').length
    const riesgo = rutas.filter((r) => r.cumplimiento === 'rojo').length
    const cumplimiento =
      rutas.length > 0 ? (verdes / rutas.length * 100).toFixed(1) + '%' : '—'
    return { activas, cumplimiento, vuelos: rutas.length, riesgo }
  }, [rutas])

  const aeropuertosVista = useMemo(() => {
    if (!snapshot?.aeropuertos?.length) return []
    return snapshot.aeropuertos.slice(0, 6).map((a) => ({
      id: a.codigo,
      nombre: a.ciudad,
      continente: a.continente,
      ocupacion: a.porcentajeOcupacion ?? 0,
    }))
  }, [snapshot])

  return (
    <div className={styles.page}>
      <div className={styles.layout}>
        <div className={styles.mainColumn}>
          <section className={styles.header}>
            <div className={styles.headerTextos}>
              <h1 className={styles.titulo}>Gestión de Rutas</h1>
              <p className={styles.subtitulo}>
                Seguimiento de rutas asignadas, cumplimiento de plazos y reasignación ante cambios
                operativos en Tasf.B2B.
              </p>
            </div>

            <div className={styles.headerAcciones}>
              <button
                type="button"
                className={styles.botonSecundario}
                onClick={() => cargarLista()}
                disabled={cargando}
              >
                <RefreshCw size={16} />
                Actualizar
              </button>
            </div>
          </section>

          <section className={styles.kpisGrid}>
            <KpiCard
              titulo="Rutas activas"
              valor={kpisEstaticos.activas}
              subtitulo="Operaciones en curso en la red"
              icono={<Route size={18} />}
              variante="azul"
            />
            <KpiCard
              titulo="Tasa de cumplimiento"
              valor={kpisEstaticos.cumplimiento}
              subtitulo="Entregas dentro del plazo comprometido"
              icono={<Target size={18} />}
            />
            <KpiCard
              titulo="Vuelos asignados"
              valor={kpisEstaticos.vuelos}
              subtitulo="Tramos con capacidad reservada"
              icono={<Plane size={18} />}
              variante="oscuro"
            />
            <KpiCard
              titulo="Rutas en riesgo"
              valor={kpisEstaticos.riesgo}
              subtitulo="Requieren seguimiento o reasignación"
              icono={<AlertTriangle size={18} />}
              variante="coral"
            />
          </section>

          <section className={styles.aeropuertosCard}>
            <div className={styles.cardTitleRow}>
              <div className={styles.cardTitleIcon}>
                <Building2 size={16} />
              </div>
              <div>
                <h2 className={styles.cardTitle}>Estado de aeropuertos</h2>
                <p className={styles.cardSubtitle}>
                  Ocupación de almacén por nodo (referencia visual — mismos datos que el panel lateral).
                </p>
              </div>
            </div>
            {aeropuertosVista.map((a) => {
              const colorBarra = getColorSemaforo(a.ocupacion, rangosSemaforo)
              return (
                <div key={a.id} className={styles.aeropuertoRow}>
                  <div>
                    <div className={styles.aeropuertoNombre}>{a.nombre}</div>
                    <div className={styles.aeropuertoContinente}>{a.continente}</div>
                  </div>
                  <div className={styles.ocupacionBar}>
                    <div className={styles.ocupacionMeta}>
                      <span>Ocupación</span>
                      <span>{a.ocupacion}%</span>
                    </div>
                    <BarraProgreso porcentaje={a.ocupacion} variante={colorBarra} />
                  </div>
                  <Semaforo valor={a.ocupacion} />
                </div>
              )
            })}
          </section>

          <section className={styles.filtrosCard}>
            <div className={styles.cardTitleRow}>
              <div className={styles.cardTitleIcon}>
                <Filter size={16} />
              </div>
              <div>
                <h2 className={styles.cardTitle}>Filtros</h2>
                <p className={styles.cardSubtitle}>Filtra por identificador, origen, destino y estado.</p>
              </div>
            </div>

            <div className={styles.filtrosGrid}>
              <div className={styles.searchBox}>
                <Search size={18} className={styles.searchIcon} />
                <input
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                  placeholder="ID, origen o destino"
                  className={styles.input}
                />
              </div>

              <select className={styles.select} value={origen} onChange={(e) => setOrigen(e.target.value)}>
                {origenes.map((o) => (
                  <option key={o} value={o}>
                    {o === 'Todos' ? 'Origen: todos' : `Origen: ${o}`}
                  </option>
                ))}
              </select>

              <select className={styles.select} value={destino} onChange={(e) => setDestino(e.target.value)}>
                {destinos.map((d) => (
                  <option key={d} value={d}>
                    {d === 'Todos' ? 'Destino: todos' : `Destino: ${d}`}
                  </option>
                ))}
              </select>

              <button type="button" className={styles.botonLimpiar} onClick={limpiarFiltros}>
                Limpiar
              </button>
            </div>
          </section>

          <section className={styles.tablaCard}>
            <div className={styles.tablaHeader}>
              <div>
                <h2 className={styles.cardTitle}>Rutas</h2>
                <p className={styles.cardSubtitle}>
                  {cargando ? 'Cargando…' : `${filtered.length} resultado(s)`}
                </p>
              </div>

              <button
                type="button"
                className={styles.botonPanelMobile}
                onClick={() => setPanelAbierto(true)}
                disabled={!selected}
              >
                Ver detalle
              </button>
            </div>

            <Tabla minWidth={1080}>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Origen → Destino</th>
                  <th>Tiempo estimado</th>
                  <th>Ingreso</th>
                  <th>Límite entrega</th>
                  <th className={styles.thCenter}>Cumplimiento</th>
                  <th className={styles.thCenter}>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((r) => {
                  const isSelected = selected?.id === r.id
                  return (
                    <tr
                      key={r.id}
                      className={isSelected ? tabStyles.rowSelected : ''}
                      onClick={() => {
                        setSelectedId(r.id)
                        setPanelAbierto(true)
                      }}
                    >
                      <td>
                        <div className={styles.idCell}>
                          <span className={styles.idPrimary}>{r.id}</span>
                          <span className={styles.idSecondary}>
                            {r.origenCiudad} — {r.destinoCiudad}
                          </span>
                        </div>
                      </td>
                      <td>
                        <div className={styles.rutaCell}>
                          <span className={styles.rutaPill}>{r.origen}</span>
                          <ChevronRight size={14} />
                          <span className={styles.rutaPill}>{r.destino}</span>
                        </div>
                      </td>
                      <td>{r.tiempoEstimado}</td>
                      <td>{r.fechaIngreso ?? '—'}</td>
                      <td>{r.fechaLimite ?? '—'}</td>
                      <td className={styles.centerCell}>
                        <BadgeCumplimiento cumplimiento={r.cumplimiento} />
                      </td>
                      <td className={styles.centerCell}>
                        <button
                          type="button"
                          className={styles.linkAccion}
                          onClick={(e) => {
                            e.stopPropagation()
                            navigate(`/gestion-rutas/${r.id}`)
                          }}
                        >
                          Ver detalle
                        </button>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </Tabla>

            {!cargando && filtered.length === 0 && (
              <div className={styles.emptyState}>
                <Search size={28} />
                <h3>No hay rutas que coincidan</h3>
                <p>Ajusta los filtros o limpia la búsqueda.</p>
              </div>
            )}
          </section>
        </div>

        <aside className={styles.sidePanel}>
          {cargandoDetalle || !detalle ? (
            <div className={styles.emptyPanel}>
              <Route size={26} />
              <h3>{cargandoDetalle ? 'Cargando detalle…' : 'Selecciona una ruta'}</h3>
              <p>El panel muestra el detalle operativo de la fila seleccionada.</p>
            </div>
          ) : (
            <>
              <div className={styles.sideHeader}>
                <div>
                  <p className={styles.sideEyebrow}>Vista rápida</p>
                  <h2 className={styles.sideTitle}>{detalle.id}</h2>
                </div>
                <button type="button" className={styles.iconButton} onClick={() => setSelectedId(null)}>
                  <X size={18} />
                </button>
              </div>

              <div className={styles.sideBody}>
                <section className={styles.progresoCard}>
                  <div className={styles.progresoTop}>
                    <span>Progreso hacia destino</span>
                    <strong>{detalle.progreso}%</strong>
                  </div>
                  <BarraProgreso porcentaje={detalle.progreso} variante={varianteBarra(detalle.cumplimiento)} />
                  <div className={styles.progresoBottom}>
                    <span>{detalle.plazoCompromiso}</span>
                    <Badge tipo={detalle.cumplimiento === 'rojo' ? 'rojo' : detalle.cumplimiento === 'ambar' ? 'ambar' : 'verde'}>
                      Cumplimiento
                    </Badge>
                  </div>
                </section>

                <div className={styles.detailGrid}>
                  <FilaDetalle
                    icono={<MapPin size={16} />}
                    label="Ruta"
                    value={`${detalle.origen} (${detalle.origenCiudad}) → ${detalle.destino} (${detalle.destinoCiudad})`}
                  />
                  <FilaDetalle
                    icono={<Clock3 size={16} />}
                    label="Tiempos"
                    value={`Estimado: ${detalle.tiempoEstimado} · Límite: ${detalle.fechaLimite}`}
                  />
                  <FilaDetalle icono={<Plane size={16} />} label="Estado" value={textoEstado(detalle.estado)} />
                </div>

                <Link className={styles.botonPlan} to={`/gestion-rutas/${detalle.id}`} style={{ textAlign: 'center', textDecoration: 'none' }}>
                  Abrir detalle completo
                </Link>
              </div>
            </>
          )}
        </aside>
      </div>

      {panelAbierto && detalle && (
        <div className={styles.mobileOverlay} onClick={() => setPanelAbierto(false)}>
          <div className={styles.mobilePanel} onClick={(e) => e.stopPropagation()}>
            <div className={styles.sideHeader}>
              <div>
                <p className={styles.sideEyebrow}>Vista rápida</p>
                <h2 className={styles.sideTitle}>{detalle.id}</h2>
              </div>
              <button type="button" className={styles.iconButton} onClick={() => setPanelAbierto(false)}>
                <X size={18} />
              </button>
            </div>
            <div className={styles.sideBody}>
              <section className={styles.progresoCard}>
                <div className={styles.progresoTop}>
                  <span>Progreso hacia destino</span>
                  <strong>{detalle.progreso}%</strong>
                </div>
                <BarraProgreso porcentaje={detalle.progreso} variante={varianteBarra(detalle.cumplimiento)} />
              </section>
              <Link
                className={styles.botonPlan}
                to={`/gestion-rutas/${detalle.id}`}
                style={{ textAlign: 'center', textDecoration: 'none' }}
                onClick={() => setPanelAbierto(false)}
              >
                Abrir detalle completo
              </Link>
            </div>
          </div>
        </div>
      )}

    </div>
  )
}

export default GestionRutasPage
