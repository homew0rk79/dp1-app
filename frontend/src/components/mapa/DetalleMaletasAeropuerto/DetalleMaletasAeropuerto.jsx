import { useState } from 'react'
import useMaletasAeropuerto from '../../../hooks/useMaletasAeropuerto'
import { FECHA_INICIO_SIMULACION_ALGORITMO } from '../../../constants/restricciones'
import { obtenerDetalleRuta } from '../../../services/rutasService'
import useSeleccionStore from '../../../store/seleccionStore'
import styles from './DetalleMaletasAeropuerto.module.css'

const PAGE_SIZE = 20

// Solo PENDIENTE_SALIDA y EN_HUB: estados que ocupan almacén
const ESTADOS_ALMACEN = ['PENDIENTE_SALIDA', 'EN_HUB']

const ETIQUETA_ESTADO = {
  PENDIENTE_SALIDA: 'Origen (esperando salida)',
  EN_HUB:           'En tránsito (escala)',
  ENTREGADA:        'Entregado al cliente',
}

function formatearMinutosAbs(min) {
  if (min == null || min < 0) return '—'
  const base  = new Date(`${FECHA_INICIO_SIMULACION_ALGORITMO}T00:00:00Z`)
  const fecha = new Date(base.getTime() + min * 60000)
  const dd    = fecha.getUTCDate().toString().padStart(2, '0')
  const mo    = (fecha.getUTCMonth() + 1).toString().padStart(2, '0')
  const yyyy  = fecha.getUTCFullYear()
  const hh    = fecha.getUTCHours().toString().padStart(2, '0')
  const mm    = fecha.getUTCMinutes().toString().padStart(2, '0')
  return `${dd}/${mo}/${yyyy} ${hh}:${mm}`
}

function ItemMaleta({ m, onSelect }) {
  const esEntregada = m.estado === 'ENTREGADA'

  return (
    <button
      type="button"
      className={`${styles.item} ${styles.itemBtn} ${styles[`borde_${m.estado}`]} ${esEntregada ? styles.itemEntregado : ''}`}
      onClick={() => onSelect(m)}
      title="Seleccionar envio y enfocar su ruta en el mapa"
    >
      <div className={styles.itemHeader}>
        <div className={styles.itemHeaderIzq}>
          <span className={styles.envioId}>{m.envioId}</span>
          {m.presenteAhora && <span className={styles.badgeAhora}>ahora</span>}
        </div>
        <span className={esEntregada ? styles.cantidadEntregado : styles.cantidad}>
          {m.cantidad} mlt
        </span>
      </div>
      <div className={styles.ruta}>
        {m.ciudadOrigen} <span className={styles.flecha}>→</span> {m.ciudadDestino}
      </div>
      <div className={styles.tiempos}>
        {esEntregada
          ? <>
              Entregado al cliente el {formatearMinutosAbs(m.tiempoLlegadaAeropuerto)}
              <span className={styles.badgeNoAlmacen}>no ocupa almacén</span>
            </>
          : <>
              Llega {formatearMinutosAbs(m.tiempoLlegadaAeropuerto)}
              {' · sale '}{formatearMinutosAbs(m.tiempoSalidaAeropuerto)}
            </>
        }
      </div>
    </button>
  )
}

function DetalleMaletasAeropuerto({ codigo, tiempoMin = 0 }) {
  const [abierto, setAbierto] = useState(false)
  const [visibles, setVisibles] = useState(PAGE_SIZE)
  const [filtro, setFiltro]     = useState('ALMACEN_HOY')
  const setEnvioSeleccionado = useSeleccionStore((s) => s.setEnvioSeleccionado)

  const { maletas, error } = useMaletasAeropuerto(codigo, tiempoMin, abierto)

  async function seleccionarEnvio(m) {
    try {
      const detalle = await obtenerDetalleRuta(m.envioId)
      const escalas = detalle?.tramos?.length
        ? [detalle.tramos[0].origen, ...detalle.tramos.map((t) => t.destino)]
        : [m.origen, m.destino]
      setEnvioSeleccionado(m.envioId, {
        escalas,
        variante: 'actual',
        rutaId: m.envioId,
      })
    } catch {
      setEnvioSeleccionado(m.envioId, {
        escalas: [m.origen, m.destino],
        variante: 'actual',
        rutaId: m.envioId,
      })
    }
  }

  function handleToggle(e) {
    const nuevoEstado = e.target.open
    setAbierto(nuevoEstado)
    if (!nuevoEstado) {
      setVisibles(PAGE_SIZE)
      setFiltro('ALMACEN_HOY')
    }
  }

  function renderContenido() {
    if (error)            return <div className={styles.estado}>{error}</div>
    if (maletas === null) return <div className={styles.estado}>Cargando…</div>
    if (maletas.length === 0) return <div className={styles.estado}>Sin envíos registrados en este momento</div>

    const enAlmacenHoy  = maletas.filter(m => ESTADOS_ALMACEN.includes(m.estado))
    const presentesAhora = enAlmacenHoy.filter(m => m.presenteAhora)
    const entregadas     = maletas.filter(m => m.estado === 'ENTREGADA')

    const maletasAlmacenHoy = enAlmacenHoy.reduce((s, m) => s + m.cantidad, 0)

    let lista
    if (filtro === 'ALMACEN_HOY') {
      lista = enAlmacenHoy
    } else if (filtro === 'AHORA') {
      lista = presentesAhora
    } else {
      lista = entregadas
    }

    // Orden: presentes ahora primero, luego los demás por tiempo de llegada
    const ordenada = [...lista].sort((a, b) => {
      if (filtro === 'ALMACEN_HOY') {
        if (a.presenteAhora !== b.presenteAhora) return a.presenteAhora ? -1 : 1
      }
      return a.tiempoLlegadaAeropuerto - b.tiempoLlegadaAeropuerto
    })

    const mostrar = ordenada.slice(0, visibles)

    return (
      <div className={styles.contenedor}>
        {/* Chips de filtro */}
        <div className={styles.filtros}>
          <button
            className={`${styles.chip} ${filtro === 'ALMACEN_HOY' ? styles.chipActivo : ''}`}
            onClick={() => { setFiltro('ALMACEN_HOY'); setVisibles(PAGE_SIZE) }}
          >
            En almacén hoy ({enAlmacenHoy.length})
          </button>
          <button
            className={`${styles.chip} ${styles.chip_AHORA} ${filtro === 'AHORA' ? styles.chipActivo : ''}`}
            onClick={() => { setFiltro('AHORA'); setVisibles(PAGE_SIZE) }}
          >
            Presentes ahora ({presentesAhora.length})
          </button>
          {entregadas.length > 0 && (
            <button
              className={`${styles.chip} ${styles.chip_ENTREGADA} ${filtro === 'ENTREGADA' ? styles.chipActivo : ''}`}
              onClick={() => { setFiltro('ENTREGADA'); setVisibles(PAGE_SIZE) }}
            >
              Entregados ({entregadas.length})
            </button>
          )}
        </div>

        {/* Resumen de totales */}
        <div className={styles.totales}>
          <span>
            {enAlmacenHoy.length} envíos · {maletasAlmacenHoy} maletas en almacén hoy
          </span>
          <span className={styles.totalesAhora}>
            {' '}({presentesAhora.length} presentes en este momento)
          </span>
          {entregadas.length > 0 && (
            <span className={styles.totalesEntregadas}>
              {' '}· {entregadas.length} entregados al cliente
            </span>
          )}
        </div>

        {/* Lista */}
        <div className={styles.lista}>
          {mostrar.map((m) => <ItemMaleta key={m.envioId} m={m} onSelect={seleccionarEnvio} />)}
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

  return (
    <details className={styles.detalleDesplegable} onToggle={handleToggle}>
      <summary className={styles.detalleSummary}>
        Ver detalle de maletas
      </summary>
      {abierto && renderContenido()}
    </details>
  )
}

export default DetalleMaletasAeropuerto
