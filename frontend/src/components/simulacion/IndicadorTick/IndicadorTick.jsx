import { useEffect, useRef, useState } from 'react'
import { RefreshCw, Clock3 } from 'lucide-react'

import useTickDiaADia from '../../../hooks/useTickDiaADia'
import useSimulacionStore from '../../../store/simulacionStore'
import styles from './IndicadorTick.module.css'

const TOAST_MS = 4000

/**
 * Chip del ciclo de replanificación del día a día: muestra el countdown a la
 * próxima replanificación, el estado "replanificando…" y un toast breve cuando
 * llega un plan actualizado (wsVersion cambia).
 * Solo visible en el escenario DIA_A_DIA con manifest cargado.
 */
function IndicadorTick() {
  const { activo, segundosRestantes, replanificando } = useTickDiaADia()
  const wsVersion = useSimulacionStore((s) => s.wsVersion)

  const [toastVisible, setToastVisible] = useState(false)
  const primeraVersionRef = useRef(null)
  const timerRef = useRef(null)

  useEffect(() => {
    if (!activo) return
    // Ignorar la primera versión (carga inicial, no un tick)
    if (primeraVersionRef.current === null) {
      primeraVersionRef.current = wsVersion
      return
    }
    if (wsVersion === primeraVersionRef.current) return
    primeraVersionRef.current = wsVersion

    setToastVisible(true)
    clearTimeout(timerRef.current)
    timerRef.current = setTimeout(() => setToastVisible(false), TOAST_MS)
    return () => clearTimeout(timerRef.current)
  }, [wsVersion, activo])

  if (!activo) return null

  return (
    <div className={styles.contenedor}>
      {toastVisible && (
        <div className={styles.toast}>
          ✓ Plan actualizado con la última replanificación
        </div>
      )}

      <div className={`${styles.chip} ${replanificando ? styles.chipReplanificando : ''}`}>
        {replanificando ? (
          <>
            <RefreshCw size={12} className={styles.iconoGirando} />
            <span>Replanificando…</span>
          </>
        ) : (
          <>
            <Clock3 size={12} />
            <span>Próxima replanificación en {segundosRestantes} s</span>
          </>
        )}
      </div>
    </div>
  )
}

export default IndicadorTick
