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
import { AEROPUERTOS_MOCK } from '../../../mocks/aeropuertos'
import useSimulacionStore from '../../../store/simulacionStore'
import { ESCENARIOS, ETIQUETAS_ESCENARIO } from '../../../constants/escenarios'
import { DURACIONES_PERIODO } from '../../../constants/restricciones'
import styles from './Sidebar.module.css'

const KPIS = [
  { icono: Luggage,       label: 'Maletas en tránsito', valor: '1,247', color: 'default' },
  { icono: CheckCircle,   label: 'Entregas a tiempo',   valor: '94.2%', color: 'verde'   },
  { icono: PlaneTakeoff,  label: 'Vuelos activos',      valor: '18',    color: 'default' },
  { icono: AlertTriangle, label: 'Almacenes en alerta', valor: '3',     color: 'rojo'    },
]

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

  useEffect(() => {
    if (estadoEjecucion === 'corriendo') {
      intervalRef.current = setInterval(incrementarTiempo, 1000)
    } else {
      clearInterval(intervalRef.current)
    }
    return () => clearInterval(intervalRef.current)
  }, [estadoEjecucion, incrementarTiempo])

  function handleIniciar() {
    if (!escenarioActivo) return
    setEstado('corriendo')
    navigate('/visualizador')
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
              {KPIS.map((kpi) => (
                <PanelMetrica key={kpi.label} {...kpi} />
              ))}
            </div>
          </section>

          {/* Lista de aeropuertos */}
          <section className={`${styles.section} ${styles.sectionScroll}`}>
            <h3 className={styles.sectionTitle}>Estado de aeropuertos</h3>
            <ul className={styles.aeropuertoList}>
              {AEROPUERTOS_MOCK.map((a) => (
                <li key={a.id} className={styles.aeropuertoItem}>
                  <div className={styles.aeropuertoInfo}>
                    <span className={styles.aeropuertoNombre}>{a.nombre}</span>
                    <span className={styles.aeropuertoPais}>{a.continente}</span>
                  </div>
                  <div className={styles.aeropuertoOcupacion}>
                    <span className={styles.ocupacionTexto}>{a.ocupacion}%</span>
                    <Semaforo valor={a.ocupacion} />
                  </div>
                </li>
              ))}
            </ul>
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
