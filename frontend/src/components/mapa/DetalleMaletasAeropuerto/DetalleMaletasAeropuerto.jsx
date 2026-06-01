import { useEffect, useState } from 'react'
import { simulacionService } from '../../../services/simulacionService'
import styles from './DetalleMaletasAeropuerto.module.css'

const PAGE_SIZE = 15

const ETIQUETA_ESTADO = {
  PENDIENTE_SALIDA: 'Pendiente de salida',
  EN_HUB:           'En tránsito (escala)',
  ENTREGADA:        'Entregada',
}

const ORDEN_ESTADO = ['PENDIENTE_SALIDA', 'EN_HUB', 'ENTREGADA']

function formatearMinutosAbs(min) {
  if (min == null || min < 0) return '—'
  const dia  = Math.floor(min / 1440) + 1
  const hh   = Math.floor((min % 1440) / 60).toString().padStart(2, '0')
  const mm   = (min % 60).toString().padStart(2, '0')
  return `D${dia} ${hh}:${mm}`
}

function DetalleMaletasAeropuerto({ codigo, tiempoMin = 0 }) {
  const [maletas, setMaletas]   = useState(null)
  const [error, setError]       = useState(null)
  const [visibles, setVisibles] = useState(PAGE_SIZE)
  const [filtro, setFiltro]     = useState('TODOS')

  useEffect(() => {
    let cancelado = false
    setMaletas(null)
    setError(null)
    setVisibles(PAGE_SIZE)

    simulacionService.obtenerMaletasEnAeropuerto(codigo, tiempoMin)
      .then((res) => { if (!cancelado) setMaletas(res.data || []) })
      .catch(() => { if (!cancelado) setError('No se pudo obtener el detalle') })

    return () => { cancelado = true }
  }, [codigo, tiempoMin])

  if (error) return <div className={styles.estado}>{error}</div>
  if (maletas === null) return <div className={styles.estado}>Cargando…</div>
  if (maletas.length === 0) return <div className={styles.estado}>Sin envíos registrados en este momento</div>

  // Conteo por estado
  const conteos = maletas.reduce((acc, m) => {
    acc[m.estado] = (acc[m.estado] || 0) + 1
    return acc
  }, {})

  const lista = filtro === 'TODOS'
    ? maletas
    : maletas.filter((m) => m.estado === filtro)

  const ordenada = [...lista].sort((a, b) => {
    const pa = ORDEN_ESTADO.indexOf(a.estado)
    const pb = ORDEN_ESTADO.indexOf(b.estado)
    if (pa !== pb) return pa - pb
    return b.cantidad - a.cantidad
  })

  const mostrar = ordenada.slice(0, visibles)
  const totalMaletas = lista.reduce((s, m) => s + m.cantidad, 0)

  return (
    <div className={styles.contenedor}>
      <div className={styles.filtros}>
        <button
          className={`${styles.chip} ${filtro === 'TODOS' ? styles.chipActivo : ''}`}
          onClick={() => { setFiltro('TODOS'); setVisibles(PAGE_SIZE) }}
        >
          Todos ({maletas.length})
        </button>
        {ORDEN_ESTADO.filter((e) => conteos[e]).map((estado) => (
          <button
            key={estado}
            className={`${styles.chip} ${filtro === estado ? styles.chipActivo : ''} ${styles[`chip_${estado}`]}`}
            onClick={() => { setFiltro(estado); setVisibles(PAGE_SIZE) }}
          >
            {ETIQUETA_ESTADO[estado]} ({conteos[estado]})
          </button>
        ))}
      </div>

      <div className={styles.totales}>
        {lista.length} envío{lista.length !== 1 ? 's' : ''} · {totalMaletas} maleta{totalMaletas !== 1 ? 's' : ''}
      </div>

      <div className={styles.lista}>
        {mostrar.map((m) => (
          <div key={m.envioId} className={`${styles.item} ${styles[`borde_${m.estado}`]}`}>
            <div className={styles.itemHeader}>
              <span className={styles.envioId}>{m.envioId}</span>
              <span className={styles.cantidad}>{m.cantidad} mlt</span>
            </div>
            <div className={styles.ruta}>
              {m.ciudadOrigen} <span className={styles.flecha}>→</span> {m.ciudadDestino}
            </div>
            <div className={styles.tiempos}>
              {m.estado === 'ENTREGADA'
                ? <>Hora de llegada: {formatearMinutosAbs(m.tiempoLlegadaAeropuerto)}</>
                : <>
                    Hora de llegada: {formatearMinutosAbs(m.tiempoLlegadaAeropuerto)}
                    {' · Hora de salida: '}{formatearMinutosAbs(m.tiempoSalidaAeropuerto)}
                  </>
              }
            </div>
          </div>
        ))}
      </div>

      {visibles < ordenada.length && (
        <button
          className={styles.verMas}
          onClick={() => setVisibles((v) => v + PAGE_SIZE)}
        >
          Ver más ({ordenada.length - visibles} restantes)
        </button>
      )}
    </div>
  )
}

export default DetalleMaletasAeropuerto
