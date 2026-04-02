import { useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { Settings2, Play } from 'lucide-react'

import SelectorEscenario from '../../components/simulacion/SelectorEscenario/SelectorEscenario'
import ConfiguradorPeriodo from '../../components/simulacion/ConfiguradorPeriodo/ConfiguradorPeriodo'
import PanelControlSimulacion from '../../components/simulacion/PanelControlSimulacion/PanelControlSimulacion'
import IndicadorColapso from '../../components/simulacion/IndicadorColapso/IndicadorColapso'

import useSimulacionStore from '../../store/simulacionStore'
import { ESCENARIOS } from '../../constants/escenarios'
import styles from './SimulacionPage.module.css'

function SimulacionPage() {
  const navigate = useNavigate()
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
      intervalRef.current = setInterval(() => {
        incrementarTiempo()
      }, 1000)
    } else {
      clearInterval(intervalRef.current)
    }
    return () => clearInterval(intervalRef.current)
  }, [estadoEjecucion, incrementarTiempo])

  function handleIniciar() {
    if (!escenarioActivo) return

    if (estadoEjecucion === 'pausado') {
      setEstado('corriendo')
      return
    }

    setEstado('corriendo')
    navigate('/visualizador')
  }

  function handlePausar() {
    setEstado('pausado')
  }

  function handleDetener() {
    clearInterval(intervalRef.current)
    setEstado('finalizado')
    setColapso(false)
  }

  function handleReset() {
    clearInterval(intervalRef.current)
    resetear()
  }

  const enCurso = estadoEjecucion === 'corriendo' || estadoEjecucion === 'pausado'
  const finalizado = estadoEjecucion === 'finalizado'

  return (
    <div className={styles.page}>
      <section className={styles.header}>
        <div className={styles.headerTextos}>
          <h1 className={styles.titulo}>Simulación de Escenarios</h1>
          <p className={styles.subtitulo}>
            Configura y ejecuta los escenarios operativos de Tasf.B2B. Al iniciar, el
            visualizador se activará en tiempo real.
          </p>
        </div>

        {(enCurso || finalizado) && (
          <button className={styles.botonReset} onClick={handleReset} type="button">
            Nueva simulación
          </button>
        )}
      </section>

      {colapsoDetectado && (
        <IndicadorColapso tiempoSegundos={tiempoSegundos} />
      )}

      <section className={styles.card}>
        <div className={styles.cardTitleRow}>
          <div className={styles.cardTitleIcon}>
            <Play size={16} />
          </div>
          <div>
            <h2 className={styles.cardTitle}>Tipo de escenario</h2>
            <p className={styles.cardSubtitle}>
              Selecciona el modo de operación que deseas ejecutar.
            </p>
          </div>
        </div>

        <SelectorEscenario
          escenarioActivo={escenarioActivo}
          onChange={enCurso ? null : setEscenario}
        />
      </section>

      {escenarioActivo && (
        <section className={styles.card}>
          <div className={styles.cardTitleRow}>
            <div className={styles.cardTitleIcon}>
              <Settings2 size={16} />
            </div>
            <div>
              <h2 className={styles.cardTitle}>Parámetros de configuración</h2>
              <p className={styles.cardSubtitle}>
                {escenarioActivo === ESCENARIOS.PERIODO
                  ? 'Define la duración del periodo y el algoritmo planificador.'
                  : 'Selecciona el algoritmo metaheurístico que resolverá la planificación.'}
              </p>
            </div>
          </div>

          <ConfiguradorPeriodo
            escenario={escenarioActivo}
            parametros={parametros}
            onChange={setParametros}
            deshabilitado={enCurso}
          />
        </section>
      )}

      {escenarioActivo && (
        <section className={styles.card}>
          <PanelControlSimulacion
            estado={estadoEjecucion}
            tiempoSegundos={tiempoSegundos ?? 0}
            onIniciar={handleIniciar}
            onPausar={handlePausar}
            onDetener={handleDetener}
          />
        </section>
      )}
    </div>
  )
}

export default SimulacionPage
