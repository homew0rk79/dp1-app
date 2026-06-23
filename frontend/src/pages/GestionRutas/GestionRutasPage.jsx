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
  Map,
} from 'lucide-react'

import ReasignarModal from './ReasignarModal'
import Modal from '../../components/common/Modal/Modal'
import Tabla from '../../components/common/Tabla/Tabla'
import tabStyles from '../../components/common/Tabla/Tabla.module.css'
import BarraProgreso from '../../components/common/BarraProgreso/BarraProgreso'
import Semaforo from '../../components/common/Semaforo/Semaforo'
import Badge from '../../components/common/Badge/Badge'
import { obtenerRutas, obtenerDetalleRuta, cancelarVuelo, cancelarRuta } from '../../services/rutasService'
import { RUTAS_MOCK, obtenerRutaMockPorId } from '../../mocks/rutas'
import { TIEMPO_SIMULADO_REFERENCIA } from '../../constants/tiempoSimulado'
import useConfiguracionStore from '../../store/configuracionStore'
import usePlanificadorStore from '../../store/planificadorStore'
import { getColorSemaforo } from '../../utils/semaforo'
import styles from './GestionRutasModule.module.css'

function textoEstado(estado) {
  const m = {
    pendiente: 'Pendiente',
    en_transito: 'En tránsito',
    completado: 'Completado',
    sin_ruta: 'Sin ruta',
    cancelado: 'Cancelado',
  }
  return m[estado] ?? estado
}

function BadgeEstadoRuta({ estado }) {
  const mapa = {
    en_transito: styles.badgeInfo,
    pendiente: styles.badgeNeutral,
    completado: styles.badgeSuccess,
    sin_ruta: styles.badgeDanger,
    cancelado: styles.badgeDanger,
  }

  return (
    <span className={`${styles.badge} ${mapa[estado] || styles.badgeNeutral}`}>
      {textoEstado(estado)}
    </span>
  )
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

function escalasDeDetalle(detalle, ruta) {
  if (detalle?.escalas?.length >= 2) return detalle.escalas
  if (detalle?.tramos?.length > 0) {
    return [detalle.tramos[0].origen, ...detalle.tramos.map((t) => t.destino)]
      .filter(Boolean)
  }
  return ruta?.escalas ?? [ruta?.origen, ruta?.destino].filter(Boolean)
}

function GestionRutasPage() {
  const navigate = useNavigate()
  const rangosSemaforo = useConfiguracionStore((s) => s.rangosSemaforo)
  const snapshot = usePlanificadorStore((s) => s.snapshot)
  const completado = usePlanificadorStore((s) => s.completado)
  const [rutas, setRutas] = useState([])
  const [cargando, setCargando] = useState(true)
  const [detalle, setDetalle] = useState(null)
  const [cargandoDetalle, setCargandoDetalle] = useState(false)

  const [query, setQuery] = useState('')
  const [origen, setOrigen] = useState('Todos')
  const [destino, setDestino] = useState('Todos')
  const [filtroEntregas, setFiltroEntregas] = useState('todas')

  const [selectedId, setSelectedId] = useState(null)
  const [panelAbierto, setPanelAbierto] = useState(false)
  const [modalReasignarId, setModalReasignarId] = useState(null)
  const [modalCancelarId, setModalCancelarId] = useState(null)
  const [cancelandoRuta, setCancelandoRuta] = useState(false)

  const cargarLista = useCallback(async () => {
    setCargando(true)
    try {
      const data = await obtenerRutas()
      const apiIds = new Set(data.map((r) => r.id))
      const extras = RUTAS_MOCK.filter((m) => !apiIds.has(m.id))
      const siguienteLista = [...data, ...extras]
      setRutas(siguienteLista)
      return siguienteLista
    } catch {
      setRutas(RUTAS_MOCK)
      return RUTAS_MOCK
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
    setDetalle(null)
    setCargandoDetalle(true)
    obtenerDetalleRuta(selectedId)
      .then((d) => {
        if (!cancel) setDetalle(d ?? obtenerRutaMockPorId(selectedId))
      })
      .catch(() => {
        if (!cancel) setDetalle(obtenerRutaMockPorId(selectedId))
      })
      .finally(() => {
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
    const ref = new Date(TIEMPO_SIMULADO_REFERENCIA)
    const horasLimite = filtroEntregas === '4h' ? 4 : filtroEntregas === '12h' ? 12 : filtroEntregas === '24h' ? 24 : null

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

      if (!matchQ || !matchOrigen || !matchDest) return false

      if (horasLimite == null) return true
      if (!r.fechaEntrega) return false
      const entrega = new Date(r.fechaEntrega)
      const diffH = (ref.getTime() - entrega.getTime()) / 3600000
      return diffH >= 0 && diffH <= horasLimite
    })
  }, [rutas, query, origen, destino, filtroEntregas])

  useEffect(() => {
    if (filtered.length === 0) return
    if (!selectedId || !filtered.some((r) => r.id === selectedId)) {
      setSelectedId(filtered[0].id)
    }
  }, [filtered, selectedId])

  const selected = filtered.find((r) => r.id === selectedId) ?? filtered[0]
  const rutaEnCancelacion = rutas.find((r) => r.id === modalCancelarId) ?? null

  const limpiarFiltros = () => {
    setQuery('')
    setOrigen('Todos')
    setDestino('Todos')
    setFiltroEntregas('todas')
  }

  async function verEnMapa(ruta, e) {
    e?.stopPropagation?.()
    let escalas = ruta.escalas ?? [ruta.origen, ruta.destino]
    try {
      const detalleRuta = await obtenerDetalleRuta(ruta.id)
      escalas = escalasDeDetalle(detalleRuta, ruta)
    } catch {
      escalas = escalasDeDetalle(null, ruta)
    }
    navigate('/visualizador', {
      state: {
        overlayRuta: {
          escalas,
          variante: 'actual',
          rutaId: ruta.id,
        },
      },
    })
  }

  async function handleConfirmarReasignacion({ rutaId, origen, destino, horaSalidaMinutos }) {
    const idObjetivo = rutaId ?? selectedId
    setSelectedId(idObjetivo)
    setPanelAbierto(true)

    const resultado = await cancelarVuelo(origen, destino, horaSalidaMinutos, idObjetivo)
    const listaActualizada = await cargarLista()
    const idParaDetalle = resultado?.envioSolicitanteId || idObjetivo

    if (idParaDetalle) {
      setSelectedId(idParaDetalle)
      const rutaActualizada = listaActualizada.find((r) => r.id === idParaDetalle)
      try {
        const d = await obtenerDetalleRuta(idParaDetalle)
        setDetalle(d ?? rutaActualizada ?? obtenerRutaMockPorId(idParaDetalle))
      } catch {
        setDetalle(rutaActualizada ?? obtenerRutaMockPorId(idParaDetalle))
      }
    }
  }

  async function handleConfirmarCancelarRuta() {
    if (!modalCancelarId) return
    console.info('[cancelacion-envio] enviando id=', modalCancelarId)
    setCancelandoRuta(true)
    setSelectedId(modalCancelarId)
    setPanelAbierto(true)
    setDetalle(null)
    setCargandoDetalle(true)
    try {
      const resultado = await cancelarRuta(modalCancelarId)
      console.info('[cancelacion-envio] respuesta=', resultado)
      const listaActualizada = await cargarLista()
      const rutaActualizada = listaActualizada.find((r) => r.id === modalCancelarId)
      try {
        const d = await obtenerDetalleRuta(modalCancelarId)
        console.info('[cancelacion-envio] detalle actualizado=', d)
        setDetalle(d ?? rutaActualizada ?? obtenerRutaMockPorId(modalCancelarId))
      } catch (detalleErr) {
        console.warn('[cancelacion-envio] no se pudo refrescar detalle, usando fila actualizada', detalleErr)
        setDetalle(rutaActualizada ?? obtenerRutaMockPorId(modalCancelarId))
      }
      setSelectedId(modalCancelarId)
      setModalCancelarId(null)
    } finally {
      setCargandoDetalle(false)
      setCancelandoRuta(false)
    }
  }

  const kpisEstaticos = useMemo(() => {
    const activas = rutas.filter((r) => r.estado !== 'sin_ruta' && r.estado !== 'cancelado').length
    const riesgo  = rutas.filter((r) => r.cumplimiento === 'rojo').length
    // Usar el porcentaje global del backend (sobre todas las maletas, no solo las 300 mostradas)
    const cumplimiento = completado != null
      ? completado.porcentajeCumplimiento.toFixed(1) + '%'
      : '—'
    return { activas, cumplimiento, vuelos: rutas.length, riesgo }
  }, [rutas, completado])

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

              <select
                className={styles.select}
                value={filtroEntregas}
                onChange={(e) => setFiltroEntregas(e.target.value)}
              >
                <option value="todas">Entregados: Todas</option>
                <option value="4h">Entregados: Últimas 4 h</option>
                <option value="12h">Entregados: Últimas 12 h</option>
                <option value="24h">Entregados: Últimas 24 h</option>
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
                  <th>Estado</th>
                  <th>Ingreso</th>
                  <th>Límite entrega</th>
                  <th className={styles.thCenter}>Cumplimiento</th>
                  <th className={styles.thCenter}>Acciones</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((r) => {
                  const isSelected = selected?.id === r.id
                  const estaCancelado = r.estado === 'cancelado'
                  const puedeCancelar = r.estado === 'en_transito'
                  const puedeReasignar = r.estado === 'en_transito' || r.estado === 'sin_ruta'
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
                      <td><BadgeEstadoRuta estado={r.estado} /></td>
                      <td>{r.fechaIngreso ?? '—'}</td>
                      <td>{r.fechaLimite ?? '—'}</td>
                      <td className={styles.centerCell}>
                        <BadgeCumplimiento cumplimiento={r.cumplimiento} />
                      </td>
                      <td className={styles.centerCell}>
                        <button
                          type="button"
                          className={styles.linkAccion}
                          onClick={(e) => verEnMapa(r, e)}
                          title="Ver ruta en el mapa"
                        >
                          <Map size={13} style={{ verticalAlign: 'middle', marginRight: 2 }} />
                          Ver en mapa
                        </button>
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
                        {estaCancelado ? (
                          <span className={styles.accionEstadoCancelado}>Cancelado</span>
                        ) : (
                          <>
                            {puedeCancelar && (
                        <button
                          type="button"
                          className={styles.linkAccionPeligro}
                          onClick={(e) => {
                            e.stopPropagation()
                            setSelectedId(r.id)
                            setPanelAbierto(true)
                            setModalCancelarId(r.id)
                          }}
                          disabled={r.estado === 'cancelado'}
                          title="Cancelar ruta/envío"
                        >
                          Cancelar
                        </button>
                            )}
                            {puedeReasignar && (
                              <button
                                type="button"
                                className={styles.linkAccionPeligro}
                                onClick={(e) => {
                                  e.stopPropagation()
                                  setSelectedId(r.id)
                                  setPanelAbierto(true)
                                  setModalReasignarId(r.id)
                                }}
                                title="Reasignar ruta/envio"
                              >
                                Reasignar
                              </button>
                            )}
                          </>
                        )}
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

      <Modal
        titulo="Confirmar cancelacion"
        abierto={Boolean(modalCancelarId)}
        onCerrar={() => {
          if (!cancelandoRuta) setModalCancelarId(null)
        }}
        acciones={
          <>
            <button
              type="button"
              className={styles.botonSecundario}
              onClick={() => setModalCancelarId(null)}
              disabled={cancelandoRuta}
            >
              Volver / Cancelar accion
            </button>
            <button
              type="button"
              className={styles.botonPrimario}
              onClick={handleConfirmarCancelarRuta}
              disabled={cancelandoRuta}
            >
              {cancelandoRuta ? 'Cancelando...' : 'Confirmar cancelacion'}
            </button>
          </>
        }
      >
        <p style={{ margin: '0 0 12px', color: '#64748b', fontSize: '0.88rem' }}>
          Se cancelara la ruta/envio{' '}
          <strong style={{ color: '#0f172a' }}>{modalCancelarId ?? '-'}</strong>.
        </p>
        {rutaEnCancelacion ? (
          <p style={{ margin: 0, color: '#334155', fontSize: '0.84rem' }}>
            Ruta: <strong>{rutaEnCancelacion.origen}</strong> {'->'} <strong>{rutaEnCancelacion.destino}</strong>
          </p>
        ) : null}
      </Modal>

      <ReasignarModal
        abierto={Boolean(modalReasignarId)}
        rutaId={modalReasignarId}
        onCerrar={() => setModalReasignarId(null)}
        onConfirmar={handleConfirmarReasignacion}
      />

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
