import { useState, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Luggage,
  CheckCircle,
  PlaneTakeoff,
  AlertTriangle,
  Play,
  Pause,
  Square,
  RotateCcw,
  ChevronLeft,
  ChevronRight,
} from 'lucide-react'

import PanelMetrica from '../../common/PanelMetrica/PanelMetrica'
import Semaforo from '../../common/Semaforo/Semaforo'
import Badge from '../../common/Badge/Badge'
import useSimulacionStore from '../../../store/simulacionStore'
import { getColorSemaforo, COLORES_SEMAFORO } from '../../../utils/semaforo'
import useConfiguracionStore from '../../../store/configuracionStore'
import { ESCENARIOS, ETIQUETAS_ESCENARIO } from '../../../constants/escenarios'
import { DURACIONES_PERIODO, FECHA_INICIO_DATOS, FECHA_FIN_DATOS } from '../../../constants/restricciones'
import usePlanificadorWS from '../../../hooks/usePlanificadorWS'
import { simulacionService } from '../../../services/simulacionService'
import styles from './Sidebar.module.css'

function formatearTiempo(segundos) {
  const h = Math.floor(segundos / 3600)
  const m = Math.floor((segundos % 3600) / 60)
  const s = segundos % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

function Sidebar() {
  const navigate = useNavigate()
  const [collapsed, setCollapsed] = useState(false)
  const intervalRef = useRef(null)
  const rangosSemaforo = useConfiguracionStore((s) => s.rangosSemaforo)

  const {
    escenarioActivo,
    estadoEjecucion,
    colapsoDetectado,
    parametros,
    tiempoSegundos,
    manifest,
    tiempoAnimacion,
    setEscenario,
    setEstado,
    setColapso,
    setParametros,
    resetear,
    incrementarTiempo,
  } = useSimulacionStore()

  const { progreso, snapshot, completado } = usePlanificadorWS()

  // Sincronizar estado local con eventos del backend
  useEffect(() => {
    if (!progreso) return
    if (progreso.estado === 'COMPLETADO') setEstado('finalizado')
    if (progreso.estado === 'ERROR')      setEstado('finalizado')
  }, [progreso, setEstado])

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
    const cap        = aero.capacidadMax || 1
    const diaActual  = Math.floor(tiempoAnimacion / 1440)
    const fraccion   = (tiempoAnimacion % 1440) / 1440
    const m0 = aero.ocupacionPorDia?.[diaActual]   ?? aero.ocupacionPorDia?.[String(diaActual)]   ?? 0
    const m1 = aero.ocupacionPorDia?.[diaActual+1] ?? aero.ocupacionPorDia?.[String(diaActual+1)] ?? m0
    return Math.round((m0 + (m1 - m0) * fraccion) / cap * 1000) / 10
  }

  // KPIs animados desde el manifest (cuando hay animación activa)
  const kpisAnimados = (() => {
    if (!manifest) return null
    const T   = tiempoAnimacion
    const dia = Math.floor(T / 1440)
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
      const maletas = a.ocupacionPorDia?.[dia] ?? a.ocupacionPorDia?.[String(dia)] ?? 0
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
    if (estadoEjecucion === 'corriendo') {
      intervalRef.current = setInterval(incrementarTiempo, 1000)
    } else {
      clearInterval(intervalRef.current)
    }
    return () => clearInterval(intervalRef.current)
  }, [estadoEjecucion, incrementarTiempo])

  async function handleIniciar() {
    if (!escenarioActivo) return
    try {
      await simulacionService.iniciar({
        escenario: escenarioActivo,
        fechaInicio: parametros.fechaInicio,
        numDias: parametros.duracionPeriodo,
      })
      setEstado('corriendo')
      navigate('/visualizador')
    } catch (err) {
      console.error('Error al iniciar simulación:', err)
    }
  }

  function handleDetener() {
    clearInterval(intervalRef.current)
    setEstado('finalizado')
    setColapso(false)
  }

  const esIdle    = estadoEjecucion === 'idle' || estadoEjecucion === 'finalizado'
  const corriendo = estadoEjecucion === 'corriendo'
  const pausado   = estadoEjecucion === 'pausado'
  const enCurso   = corriendo || pausado

  const badgeEscenario = enCurso ? 'verde' : escenarioActivo ? 'info' : 'info'

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

          {/* Indicador de carga mientras corre el algoritmo */}
          {estadoEjecucion === 'corriendo' && progreso && (
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

          {/* Lista de aeropuertos (real-time desde WS, vacío si no hay snapshot) */}
          <section className={`${styles.section} ${styles.sectionScroll}`}>
            <h3 className={styles.sectionTitle}>Estado de aeropuertos</h3>
            {aeropuertosWS.length === 0 ? (
              <p style={{ fontSize: '0.72rem', color: '#64748b' }}>
                Disponible al iniciar una simulación
              </p>
            ) : (
              <ul className={styles.aeropuertoList}>
                {aeropuertosWS.map((a) => {
                  const pct   = getOcupacionPct(a.codigo, a.porcentajeOcupacion)
                  const color = getColorSemaforo(pct, rangosSemaforo)
                  const hex   = COLORES_SEMAFORO[color]
                  return (
                    <li key={a.codigo} className={styles.aeropuertoItem}>
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
                  onChange={(e) => !enCurso && setEscenario(e.target.value || null)}
                  disabled={enCurso}
                >
                  <option value="">— Seleccionar —</option>
                  {Object.values(ESCENARIOS).map((esc) => (
                    <option key={esc} value={esc}>{ETIQUETAS_ESCENARIO[esc]}</option>
                  ))}
                </select>
              </div>

              {/* Fecha de inicio (PERIODO y DIA_A_DIA) */}
              {escenarioActivo !== ESCENARIOS.COLAPSO && escenarioActivo !== null && (
                <div className={styles.simField}>
                  <label className={styles.simLabel}>Fecha de inicio</label>
                  <input
                    type="date"
                    className={styles.simSelect}
                    min={FECHA_INICIO_DATOS}
                    max={FECHA_FIN_DATOS}
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
                  {estadoEjecucion === 'idle'       && 'En espera'}
                  {estadoEjecucion === 'corriendo'  && 'En ejecución'}
                  {estadoEjecucion === 'pausado'    && 'Pausado'}
                  {estadoEjecucion === 'finalizado' && 'Finalizado'}
                </span>
                {estadoEjecucion !== 'idle' && (
                  <span className={styles.simTiempo}>{formatearTiempo(tiempoSegundos ?? 0)}</span>
                )}
              </div>

              {/* Controles */}
              <div className={styles.simControles}>
                {esIdle && (
                  <button
                    className={styles.simBtnIniciar}
                    onClick={handleIniciar}
                    disabled={!escenarioActivo}
                    type="button"
                  >
                    <Play size={13} strokeWidth={2.5} /> Iniciar
                  </button>
                )}
                {corriendo && (
                  <>
                    <button className={styles.simBtnSecundario} onClick={() => setEstado('pausado')} type="button">
                      <Pause size={13} /> Pausar
                    </button>
                    <button className={styles.simBtnPeligro} onClick={handleDetener} type="button">
                      <Square size={13} /> Detener
                    </button>
                  </>
                )}
                {pausado && (
                  <>
                    <button className={styles.simBtnIniciar} onClick={() => setEstado('corriendo')} type="button">
                      <Play size={13} strokeWidth={2.5} /> Reanudar
                    </button>
                    <button className={styles.simBtnPeligro} onClick={handleDetener} type="button">
                      <Square size={13} /> Detener
                    </button>
                  </>
                )}
                {estadoEjecucion === 'finalizado' && (
                  <button className={styles.simBtnSecundario} onClick={resetear} type="button">
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
