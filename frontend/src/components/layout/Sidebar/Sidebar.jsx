import { useState, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Luggage,
  CheckCircle,
  PlaneTakeoff,
  AlertTriangle,
  Play,
  Square,
  RotateCcw,
  ChevronLeft,
  ChevronRight,
  Gauge,
} from 'lucide-react'

import PanelMetrica from '../../common/PanelMetrica/PanelMetrica'
import Semaforo from '../../common/Semaforo/Semaforo'
import Badge from '../../common/Badge/Badge'
import PanelVuelos from './PanelVuelos/PanelVuelos'
import useSimulacionStore from '../../../store/simulacionStore'
import { getColorSemaforo, COLORES_SEMAFORO } from '../../../utils/semaforo'
import useConfiguracionStore from '../../../store/configuracionStore'
import useSeleccionStore from '../../../store/seleccionStore'
import { ESCENARIOS, ETIQUETAS_ESCENARIO } from '../../../constants/escenarios'
import {
  DURACIONES_PERIODO,
  FECHA_INICIO_DATOS,
  FECHA_FIN_DATOS,
  FECHA_INICIO_SIMULACION_ALGORITMO,
  TA_EJECUCION_ALGORITMO_MIN,
  SA_SALTO_ALGORITMO_MIN,
} from '../../../constants/restricciones'
import { formatearDuracion } from '../../../utils/tiempos'
import usePlanificadorStore from '../../../store/planificadorStore'
import { simulacionService } from '../../../services/simulacionService'
import styles from './Sidebar.module.css'

function Sidebar() {
  const navigate = useNavigate()
  const [collapsed, setCollapsed] = useState(false)
  const [tabActivo, setTabActivo] = useState('aeropuertos')
  const intervalRef    = useRef(null)
  const ultimaHoraRef  = useRef(-1)
  const rangosSemaforo = useConfiguracionStore((s) => s.rangosSemaforo)
  const [ocupacionRealtime, setOcupacionRealtime] = useState({})
  const [scConsumoReal, setScConsumoReal] = useState(null)

  const aeropuertoSeleccionado = useSeleccionStore((s) => s.aeropuertoSeleccionado)
  const setAeropuertoSeleccionado = useSeleccionStore((s) => s.setAeropuertoSeleccionado)

  const {
    escenarioActivo,
    estadoEjecucion,
    colapsoDetectado,
    parametros,
    tiempoSegundos,
    manifest,
    tiempoAnimacion,
    playingAnimacion,
    setEscenario,
    setEstado,
    setColapso,
    setParametros,
    resetear,
    incrementarTiempo,
    clearManifest,
    setTiempoAnimacion,
    setPlayingAnimacion,
    setInicioEjecucionReal,
  } = useSimulacionStore()

  const progreso = usePlanificadorStore((s) => s.progreso)
  const snapshot = usePlanificadorStore((s) => s.snapshot)
  const completado = usePlanificadorStore((s) => s.completado)
  const resetPlanificador = usePlanificadorStore((s) => s.resetPlanificador)

  const offsetMinutos = (() => {
    const base   = new Date(FECHA_INICIO_SIMULACION_ALGORITMO)
    const inicio = new Date(parametros.fechaInicio || FECHA_INICIO_SIMULACION_ALGORITMO)
    return Math.round((inicio - base) / 60000)
  })()

  // Sincronizar estado local con eventos del backend
  useEffect(() => {
    if (!progreso) return
    if (progreso.estado) setEstado(progreso.estado)
  }, [progreso, setEstado])

  // Cuando el mapa selecciona un aeropuerto, cambia al tab de aeropuertos
  useEffect(() => {
    if (aeropuertoSeleccionado) setTabActivo('aeropuertos')
  }, [aeropuertoSeleccionado])

  // Ocupación en tiempo real — se actualiza una vez por hora simulada
  useEffect(() => {
    if (!manifest) {
      ultimaHoraRef.current = -1
      setOcupacionRealtime({})
      return
    }
    const saltoSimulado = Math.floor(tiempoAnimacion / SA_SALTO_ALGORITMO_MIN)
    if (saltoSimulado === ultimaHoraRef.current) return
    ultimaHoraRef.current = saltoSimulado

    const tiempoMin = Math.floor(tiempoAnimacion) + offsetMinutos
    simulacionService.obtenerOcupacionActual(tiempoMin)
      .then(res => setOcupacionRealtime(res.data ?? {}))
      .catch(() => {})
  }, [tiempoAnimacion, manifest, offsetMinutos])

  useEffect(() => {
    if (!manifest && !completado) {
      setScConsumoReal(null)
      return
    }
    simulacionService.obtenerConsumoBloques()
      .then(res => setScConsumoReal(res.data?.[0]?.saltoMin ?? null))
      .catch(() => setScConsumoReal(null))
  }, [manifest, completado])

  // Aeropuertos en tiempo real (desde snapshot WS) o lista vacía
  const aeropuertosWS = snapshot?.aeropuertos ?? []

  // Lookup manifest para calcular ocupaciones animadas por aeropuerto
  const manifestLookup = manifest
    ? Object.fromEntries(manifest.aeropuertos.map((a) => [a.codigo, a]))
    : null

  function getOcupacionPct(codigo, fallbackPct) {
    if (!manifestLookup) return fallbackPct ?? 0
    const aero = manifestLookup[codigo]
    if (!aero) return fallbackPct ?? 0
    const cap     = aero.capacidadMax || 1
    const maletas = ocupacionRealtime[codigo] ?? 0
    return Math.round((maletas / cap) * 1000) / 10
  }

  // KPIs animados desde el manifest (cuando hay animación activa)
  const kpisAnimados = (() => {
    if (!manifest) return null
    const T = tiempoAnimacion
    let maletasEnTransito = 0
    let vuelosSaturados   = 0
    for (const o of manifest.ocurrencias) {
      if (o.salidaAbs <= T && T <= o.llegadaAbs) {
        maletasEnTransito += o.maletas
        if (o.maletas / (o.capacidadMax || 1) > 0.9) vuelosSaturados++
      }
    }
    let aeropuertosSaturados = 0
    for (const a of manifest.aeropuertos) {
      const maletas = ocupacionRealtime[a.codigo] ?? 0
      if (maletas / (a.capacidadMax || 1) > rangosSemaforo.ambar / 100) aeropuertosSaturados++
    }
    return { maletasEnTransito, vuelosSaturados, aeropuertosSaturados }
  })()

  const kpis = [
    {
      icono: Luggage,
      label: kpisAnimados ? 'Maletas en tránsito' : 'Maletas planificadas',
      valor: kpisAnimados
        ? kpisAnimados.maletasEnTransito.toLocaleString()
        : completado ? completado.totalEnvios.toLocaleString() : '—',
      color: 'default',
    },
    {
      icono: CheckCircle,
      label: 'Entregas a tiempo',
      valor: completado ? `${completado.porcentajeCumplimiento.toFixed(1)}%` : '—',
      color: completado?.semaforo === 'VERDE' ? 'verde' : completado?.semaforo === 'AMBAR' ? 'ambar' : 'default',
    },
    {
      icono: PlaneTakeoff,
      label: 'Vuelos saturados',
      valor: kpisAnimados
        ? kpisAnimados.vuelosSaturados.toString()
        : completado ? completado.vuelosSaturados.toString() : '—',
      color: (kpisAnimados?.vuelosSaturados ?? completado?.vuelosSaturados ?? 0) > 0 ? 'rojo' : 'default',
    },
    {
      icono: AlertTriangle,
      label: 'Aeropuertos saturados',
      valor: kpisAnimados
        ? kpisAnimados.aeropuertosSaturados.toString()
        : completado ? completado.aeropuertosSaturados.toString() : '—',
      color: (kpisAnimados?.aeropuertosSaturados ?? completado?.aeropuertosSaturados ?? 0) > 0 ? 'rojo' : 'default',
    },
  ]

  useEffect(() => {
    if (estadoEjecucion === 'CARGANDO' || estadoEjecucion === 'PLANIFICANDO') {
      intervalRef.current = setInterval(incrementarTiempo, 1000)
    } else {
      clearInterval(intervalRef.current)
    }
    return () => clearInterval(intervalRef.current)
  }, [estadoEjecucion, incrementarTiempo])

  async function handleIniciar() {
    if (!escenarioActivo) return
    if (manifest) {
      if (tiempoAnimacion >= manifest.duracionTotalMinutos) {
        setTiempoAnimacion(0)
      }
      setPlayingAnimacion(true)
      navigate('/visualizador')
      return
    }

    try {
      clearInterval(intervalRef.current)
      resetPlanificador()
      clearManifest()
      setColapso(false)
      setEstado('CARGANDO')
      setInicioEjecucionReal(Date.now())
      await simulacionService.iniciar({
        escenario: escenarioActivo,
        fechaInicio: parametros.fechaInicio,
        numDias: parametros.duracionPeriodo,
      })
      navigate('/visualizador')
    } catch (err) {
      try {
        const estadoRes = await simulacionService.obtenerEstado()
        setEstado(estadoRes.data?.estado ?? 'ERROR')
      } catch (estadoErr) {
        console.error('No se pudo sincronizar el estado del backend:', estadoErr)
        setEstado('ERROR')
      }
      console.error('Error al iniciar simulación:', err)
    }
  }

  async function handleDetener() {
    clearInterval(intervalRef.current)
    try {
      const estadoRes = await simulacionService.obtenerEstado()
      setEstado(estadoRes.data?.estado ?? 'ERROR')
    } catch (err) {
      console.error('No se pudo sincronizar el estado del backend:', err)
      setEstado('ERROR')
    }
  }

  function handleNuevaSimulacion() {
    clearInterval(intervalRef.current)
    resetPlanificador()
    resetear()
  }

  function handleSeleccionEscenario(valor) {
    if (enCurso) return
    resetPlanificador()
    clearManifest()
    setColapso(false)
    setEstado('IDLE')
    setEscenario(valor || null)
  }

  const cargando = estadoEjecucion === 'CARGANDO'
  const planificando = estadoEjecucion === 'PLANIFICANDO'
  const completadoEstado = estadoEjecucion === 'COMPLETADO'
  const errorEstado = estadoEjecucion === 'ERROR'
  const puedeIniciar = estadoEjecucion === 'IDLE' || errorEstado || completadoEstado || Boolean(manifest)
  const enCurso = cargando || planificando
  const simulacionEnCurso = Boolean(manifest && playingAnimacion)

  const badgeEscenario = enCurso ? 'verde' : errorEstado ? 'rojo' : 'info'

  return (
    <aside className={`${styles.sidebar} ${collapsed ? styles.collapsed : ''}`}>
      <button
        className={styles.toggleBtn}
        onClick={() => setCollapsed((c) => !c)}
        title={collapsed ? 'Expandir panel' : 'Ocultar panel'}
      >
        {collapsed ? <ChevronRight size={15} /> : <ChevronLeft size={15} />}
      </button>

      {!collapsed && (
        <>
          {/* Escenario activo */}
          <section className={styles.section}>
            <h3 className={styles.sectionTitle}>Escenario activo</h3>
            <Badge tipo={badgeEscenario}>
              {escenarioActivo ? ETIQUETAS_ESCENARIO[escenarioActivo] : 'Sin simulación activa'}
            </Badge>
          </section>

          {/* KPIs */}
          <section className={styles.section}>
            <h3 className={styles.sectionTitle}>Métricas generales</h3>
            <div className={styles.kpiList}>
              {kpis.map((kpi) => (
                <PanelMetrica key={kpi.label} {...kpi} />
              ))}
            </div>
          </section>

          {/* Barra de progreso mientras corre el algoritmo */}
          {enCurso && progreso && (
            <section className={styles.section}>
              <h3 className={styles.sectionTitle}>Procesando</h3>
              <div className={styles.loadingPanel}>
                <div className={styles.loadingDots}>
                  <span className={styles.dot} />
                  <span className={styles.dot} />
                  <span className={styles.dot} />
                </div>
                <p className={styles.loadingMensaje}>{progreso.mensaje}</p>
                <div className={styles.loadingBarra}>
                  <div
                    className={styles.loadingBarraFill}
                    style={{ width: `${progreso.porcentaje}%` }}
                  />
                </div>
                <span className={styles.loadingPct}>{progreso.porcentaje}%</span>
              </div>
            </section>
          )}

          {/* Panel con tabs: Aeropuertos / Vuelos */}
          <section className={`${styles.section} ${styles.sectionScroll}`}>
            <div className={styles.tabs}>
              <button
                className={`${styles.tab} ${tabActivo === 'aeropuertos' ? styles.tabActivo : ''}`}
                onClick={() => setTabActivo('aeropuertos')}
              >
                Aeropuertos
              </button>
              <button
                className={`${styles.tab} ${tabActivo === 'vuelos' ? styles.tabActivo : ''}`}
                onClick={() => setTabActivo('vuelos')}
              >
                Vuelos
              </button>
            </div>

            {tabActivo === 'aeropuertos' && (
              aeropuertosWS.length === 0 ? (
                <p style={{ fontSize: '0.72rem', color: '#64748b' }}>
                  Disponible al iniciar una simulación
                </p>
              ) : (
                <ul className={styles.aeropuertoList}>
                  {aeropuertosWS.map((a) => {
                    const pct      = getOcupacionPct(a.codigo, a.porcentajeOcupacion)
                    const color    = getColorSemaforo(pct, rangosSemaforo)
                    const hex      = COLORES_SEMAFORO[color]
                    const selected = aeropuertoSeleccionado === a.codigo
                    return (
                      <li
                        key={a.codigo}
                        className={`${styles.aeropuertoItem} ${selected ? styles.aeropuertoSeleccionado : ''}`}
                        onClick={() => setAeropuertoSeleccionado(selected ? null : a.codigo)}
                      >
                        <div className={styles.aeropuertoInfo}>
                          <span className={styles.aeropuertoNombre}>{a.ciudad}</span>
                          <span className={styles.aeropuertoPais}>{a.continente}</span>
                        </div>
                        <div className={styles.aeropuertoOcupacion}>
                          <span className={styles.ocupacionTexto} style={{ color: hex }}>
                            {pct.toFixed(1)}%
                          </span>
                          <Semaforo valor={pct} />
                        </div>
                      </li>
                    )
                  })}
                </ul>
              )
            )}

            {tabActivo === 'vuelos' && (
              <PanelVuelos
                ocurrencias={manifest?.ocurrencias ?? null}
                tiempoAnimacion={tiempoAnimacion}
              />
            )}
          </section>

          {/* Panel de simulación */}
          <section className={styles.sectionBottom}>
            <h3 className={styles.sectionTitle}>Simulación</h3>
            <div className={styles.simPanel}>

              {/* Selector de escenario */}
              <div className={styles.simField}>
                <label className={styles.simLabel}>Escenario</label>
                <select
                  className={styles.simSelect}
                  value={escenarioActivo ?? ''}
                  onChange={(e) => handleSeleccionEscenario(e.target.value)}
                  disabled={enCurso}
                >
                  <option value="">— Seleccionar —</option>
                  {Object.values(ESCENARIOS).map((esc) => (
                    <option key={esc} value={esc}>{ETIQUETAS_ESCENARIO[esc]}</option>
                  ))}
                </select>
              </div>

              {/* Fecha de inicio — disponible para todos los escenarios */}
              {escenarioActivo !== null && (
                <div className={styles.simField}>
                  <label className={styles.simLabel}>Fecha y hora de inicio</label>
                  <input
                    type="datetime-local"
                    className={styles.simSelect}
                    min={FECHA_INICIO_DATOS}
                    max={`${FECHA_FIN_DATOS}T23:59`}
                    step={60}
                    value={parametros.fechaInicio}
                    onChange={(e) => setParametros({ fechaInicio: e.target.value })}
                    disabled={enCurso}
                  />
                </div>
              )}

              {/* Duración (solo PERIODO) */}
              {escenarioActivo === ESCENARIOS.PERIODO && (
                <div className={styles.simField}>
                  <label className={styles.simLabel}>Duración</label>
                  <select
                    className={styles.simSelect}
                    value={parametros.duracionPeriodo}
                    onChange={(e) => setParametros({ duracionPeriodo: Number(e.target.value) })}
                    disabled={enCurso}
                  >
                    {DURACIONES_PERIODO.map((d) => (
                      <option key={d} value={d}>{d} días</option>
                    ))}
                  </select>
                </div>
              )}

              {/* Estado y tiempo */}
              <div className={styles.simEstado}>
                <span className={`${styles.simEstadoBadge} ${styles[`simEstado--${estadoEjecucion}`]}`}>
                  {estadoEjecucion === 'IDLE' && 'En espera'}
                  {estadoEjecucion === 'CARGANDO' && 'Cargando'}
                  {estadoEjecucion === 'PLANIFICANDO' && 'En ejecucion'}
                  {estadoEjecucion === 'COMPLETADO' && 'Completado'}
                  {estadoEjecucion === 'ERROR' && 'Error'}
                </span>
                {estadoEjecucion !== 'IDLE' && (
                  <span className={styles.simTiempo}>{formatearDuracion(tiempoSegundos ?? 0)}</span>
                )}
              </div>

              <div className={styles.parametrosAlgoritmo}>
                <div className={styles.paramHeader}>
                  <Gauge size={13} />
                  <span>Tiempos del algoritmo</span>
                </div>
                <div className={styles.paramGrid}>
                  <span>Ta: avance por tick</span><strong>{TA_EJECUCION_ALGORITMO_MIN} min</strong>
                  <span>Sa: salto algoritmo</span><strong>{SA_SALTO_ALGORITMO_MIN} min</strong>
                  <span>Sc: bloque consumo</span><strong>{scConsumoReal ? `${scConsumoReal} min` : '--'}</strong>
                </div>
              </div>

              {simulacionEnCurso && (
                <div className={styles.simEnCurso}>
                  Simulación en curso...
                </div>
              )}

              {/* Controles */}
              <div className={styles.simControles}>
                {puedeIniciar && (
                  <button
                    className={styles.simBtnIniciar}
                    onClick={handleIniciar}
                    disabled={!escenarioActivo || simulacionEnCurso}
                    type="button"
                  >
                    <Play size={13} strokeWidth={2.5} /> Iniciar
                  </button>
                )}
                {enCurso && (
                  <button className={styles.simBtnPeligro} onClick={handleDetener} type="button">
                    <Square size={13} /> Detener
                  </button>
                )}
                {(completadoEstado || errorEstado) && (
                  <button className={styles.simBtnSecundario} onClick={handleNuevaSimulacion} type="button">
                    <RotateCcw size={13} /> Nueva simulación
                  </button>
                )}
              </div>

              {/* Alerta de colapso */}
              {colapsoDetectado && (
                <div className={styles.simColapso}>
                  <AlertTriangle size={13} />
                  <span>Colapso operativo detectado</span>
                </div>
              )}
            </div>
          </section>
        </>
      )}
    </aside>
  )
}

export default Sidebar
