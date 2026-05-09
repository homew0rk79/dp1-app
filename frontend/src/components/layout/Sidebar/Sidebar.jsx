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
import { ESCENARIOS, ETIQUETAS_ESCENARIO } from '../../../constants/escenarios'
import { DURACIONES_PERIODO } from '../../../constants/restricciones'
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

  const {
    escenarioActivo,
    estadoEjecucion,
    colapsoDetectado,
    parametros,
    tiempoSegundos,
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

  // KPIs dinámicos desde las métricas finales
  const kpis = [
    {
      icono: Luggage,
      label: 'Maletas planificadas',
      valor: completado ? completado.totalEnvios.toLocaleString() : '—',
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
      valor: completado ? completado.vuelosSaturados.toString() : '—',
      color: completado?.vuelosSaturados > 0 ? 'rojo' : 'default',
    },
    {
      icono: AlertTriangle,
      label: 'Aeropuertos saturados',
      valor: completado ? completado.aeropuertosSaturados.toString() : '—',
      color: completado?.aeropuertosSaturados > 0 ? 'rojo' : 'default',
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
        fechaInicio: '2026-01-02',
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

          {/* Barra de progreso mientras corre el algoritmo */}
          {estadoEjecucion === 'corriendo' && progreso && (
            <section className={styles.section}>
              <h3 className={styles.sectionTitle}>Progreso</h3>
              <div style={{ fontSize: '0.72rem', color: '#94a3b8', marginBottom: 4 }}>
                {progreso.mensaje}
              </div>
              <div style={{ background: '#1e293b', borderRadius: 4, height: 6 }}>
                <div style={{
                  width: `${progreso.porcentaje}%`,
                  background: '#3b82f6',
                  height: '100%',
                  borderRadius: 4,
                  transition: 'width 0.5s ease',
                }} />
              </div>
              <div style={{ fontSize: '0.7rem', color: '#64748b', marginTop: 3, textAlign: 'right' }}>
                {progreso.porcentaje}%
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
                {aeropuertosWS.map((a) => (
                  <li key={a.codigo} className={styles.aeropuertoItem}>
                    <div className={styles.aeropuertoInfo}>
                      <span className={styles.aeropuertoNombre}>{a.ciudad}</span>
                      <span className={styles.aeropuertoPais}>{a.continente}</span>
                    </div>
                    <div className={styles.aeropuertoOcupacion}>
                      <span className={styles.ocupacionTexto}>{a.porcentajeOcupacion?.toFixed(1)}%</span>
                      <Semaforo valor={a.porcentajeOcupacion ?? 0} />
                    </div>
                  </li>
                ))}
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
