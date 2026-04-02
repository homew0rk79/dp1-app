import { DURACIONES_PERIODO, ALGORITMOS } from '../../../constants/restricciones'
import { ESCENARIOS } from '../../../constants/escenarios'
import styles from './ConfiguradorPeriodo.module.css'

const ETIQUETAS_ALGORITMO = {
  [ALGORITMOS.ALGORITMO_1]: 'Algoritmo 1',
  [ALGORITMOS.ALGORITMO_2]: 'Algoritmo 2',
}

function ConfiguradorPeriodo({ escenario, parametros, onChange, deshabilitado }) {
  const mostrarDuracion = escenario === ESCENARIOS.PERIODO

  return (
    <div className={styles.contenedor}>
      {mostrarDuracion && (
        <div className={styles.grupo}>
          <label className={styles.label}>Duración del periodo simulado</label>
          <p className={styles.hint}>
            La ejecución completa tomará entre 30 y 90 minutos en tiempo real.
          </p>
          <div className={styles.duracionOpciones}>
            {DURACIONES_PERIODO.map((dias) => (
              <button
                key={dias}
                type="button"
                className={`${styles.duracionBtn} ${parametros.duracionPeriodo === dias ? styles.duracionBtnActivo : ''}`}
                onClick={() => onChange({ duracionPeriodo: dias })}
                disabled={deshabilitado}
              >
                <span className={styles.duracionNumero}>{dias}</span>
                <span className={styles.duracionTexto}>día{dias !== 1 ? 's' : ''}</span>
              </button>
            ))}
          </div>
        </div>
      )}

      <div className={styles.grupo}>
        <label className={styles.label}>Algoritmo metaheurístico</label>
        <p className={styles.hint}>
          Selecciona el algoritmo que resolverá la planificación de rutas.
        </p>
        <div className={styles.algoritmoOpciones}>
          {Object.values(ALGORITMOS).map((alg) => (
            <button
              key={alg}
              type="button"
              className={`${styles.algoritmoBtn} ${parametros.algoritmo === alg ? styles.algoritmoBtnActivo : ''}`}
              onClick={() => onChange({ algoritmo: alg })}
              disabled={deshabilitado}
            >
              <div className={`${styles.algoritmoDot} ${parametros.algoritmo === alg ? styles.algoritmoDotActivo : ''}`} />
              <div className={styles.algoritmoTextos}>
                <span className={styles.algoritmoNombre}>{ETIQUETAS_ALGORITMO[alg]}</span>
                <span className={styles.algoritmoDesc}>
                  {alg === ALGORITMOS.ALGORITMO_1
                    ? 'Primera solución metaheurística implementada.'
                    : 'Segunda solución metaheurística alternativa.'}
                </span>
              </div>
            </button>
          ))}
        </div>
      </div>
    </div>
  )
}

export default ConfiguradorPeriodo
