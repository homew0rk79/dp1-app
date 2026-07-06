import { useEffect, useState } from 'react'
import { simulacionService } from '../../../../services/simulacionService'
import styles from './PlanificadosAeropuerto.module.css'

function formatearMinutosAbs(min) {
  if (min == null || min < 0) return '—'
  const dia = Math.floor(min / 1440) + 1
  const hh = Math.floor((min % 1440) / 60).toString().padStart(2, '0')
  const mm = (min % 60).toString().padStart(2, '0')
  return `D${dia} ${hh}:${mm}`
}

/**
 * Información planificada de envíos que entran o salen de un almacén.
 * Consume GET /api/aeropuertos/{codigo}/planificados.
 *
 * @param codigo    código ICAO del aeropuerto
 * @param tiempoMin minuto simulado actual
 * @param modo      'entrantes' | 'salientes'
 */
function PlanificadosAeropuerto({ codigo, tiempoMin = 0, modo = 'entrantes' }) {
  const [datos, setDatos] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    let cancelado = false
    setDatos(null)
    setError(null)
    simulacionService.obtenerPlanificadosAeropuerto(codigo, Math.max(0, Math.floor(tiempoMin)))
      .then((res) => { if (!cancelado) setDatos(res.data) })
      .catch(() => { if (!cancelado) setError('No se pudo obtener la información planificada') })
    return () => { cancelado = true }
  }, [codigo, tiempoMin])

  if (error) return <div className={styles.estado}>{error}</div>
  if (!datos) return <div className={styles.estado}>Cargando…</div>

  const items = modo === 'entrantes' ? datos.entrantes : datos.salientes
  const totalMaletas = modo === 'entrantes' ? datos.totalMaletasEntrantes : datos.totalMaletasSalientes

  if (!items || items.length === 0) {
    return (
      <div className={styles.estado}>
        Sin envíos {modo === 'entrantes' ? 'por llegar' : 'por salir'}
      </div>
    )
  }

  return (
    <div className={styles.contenedor}>
      <div className={styles.resumen}>
        {items.length} envío(s) · {totalMaletas} maleta(s) planificada(s)
      </div>
      <ul className={styles.lista}>
        {items.map((it, i) => (
          <li key={`${it.envioId}-${it.horaAbs}-${i}`} className={styles.item}>
            <div className={styles.fila}>
              <span className={styles.envioId}>{it.envioId}</span>
              <span className={styles.cantidad}>{it.cantidad} mlt</span>
            </div>
            <div className={styles.filaSec}>
              <span className={styles.vuelo}>{it.vuelo}</span>
              <span className={styles.hora}>
                {modo === 'entrantes' ? 'Llega' : 'Sale'} {formatearMinutosAbs(it.horaAbs)}
              </span>
            </div>
          </li>
        ))}
      </ul>
    </div>
  )
}

export default PlanificadosAeropuerto
