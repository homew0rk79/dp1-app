import styles from './TablaComparativaAlgoritmos.module.css'

/**
 * Tabla comparativa de los dos algoritmos metaheurísticos.
 * @param {Array} filas - [{ escenario, alg1_tiempo, alg1_calidad, alg1_cumplimiento,
 *                                  alg2_tiempo, alg2_calidad, alg2_cumplimiento }]
 */
function TablaComparativaAlgoritmos({ filas = [] }) {
  return (
    <div className={styles.wrapper}>
      <table className={styles.table}>
        <thead>
          <tr>
            <th rowSpan={2} className={styles.thEscenario}>Escenario</th>
            <th colSpan={3} className={styles.thAlg}>Algoritmo 1</th>
            <th colSpan={3} className={styles.thAlg2}>Algoritmo 2</th>
          </tr>
          <tr>
            <th>Tiempo (ms)</th>
            <th>Calidad (%)</th>
            <th>Cumplimiento (%)</th>
            <th>Tiempo (ms)</th>
            <th>Calidad (%)</th>
            <th>Cumplimiento (%)</th>
          </tr>
        </thead>
        <tbody>
          {filas.map((fila, idx) => {
            const mejorCumpl = fila.alg1_cumplimiento >= fila.alg2_cumplimiento ? 1 : 2
            const mejorCalidad = fila.alg1_calidad >= fila.alg2_calidad ? 1 : 2
            const mejorTiempo = fila.alg1_tiempo <= fila.alg2_tiempo ? 1 : 2

            return (
              <tr key={idx}>
                <td className={styles.tdEscenario}>{fila.escenario}</td>

                <td className={mejorTiempo === 1 ? styles.mejor : ''}>
                  {fila.alg1_tiempo.toLocaleString('es-PE')}
                </td>
                <td className={mejorCalidad === 1 ? styles.mejor : ''}>
                  {fila.alg1_calidad.toFixed(1)}%
                </td>
                <td className={mejorCumpl === 1 ? styles.mejor : ''}>
                  {fila.alg1_cumplimiento.toFixed(1)}%
                </td>

                <td className={mejorTiempo === 2 ? styles.mejor : ''}>
                  {fila.alg2_tiempo.toLocaleString('es-PE')}
                </td>
                <td className={mejorCalidad === 2 ? styles.mejor : ''}>
                  {fila.alg2_calidad.toFixed(1)}%
                </td>
                <td className={mejorCumpl === 2 ? styles.mejor : ''}>
                  {fila.alg2_cumplimiento.toFixed(1)}%
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}

export default TablaComparativaAlgoritmos
