import { useState, useMemo } from 'react'
import { ArrowUpDown, Search } from 'lucide-react'
import Semaforo from '../../../common/Semaforo/Semaforo'
import { getColorSemaforo, COLORES_SEMAFORO } from '../../../../utils/semaforo'
import useConfiguracionStore from '../../../../store/configuracionStore'
import useSeleccionStore from '../../../../store/seleccionStore'
import styles from './PanelVuelos.module.css'

function PanelVuelos({ ocurrencias, tiempoAnimacion }) {
  const rangosSemaforo = useConfiguracionStore((s) => s.rangosSemaforo)
  const vueloSeleccionado = useSeleccionStore((s) => s.vueloSeleccionado)
  const setVueloSeleccionado = useSeleccionStore((s) => s.setVueloSeleccionado)

  const [busqueda, setBusqueda] = useState('')
  const [ordenarPorOcupacion, setOrdenarPorOcupacion] = useState(false)

  const vuelosActivos = useMemo(() => {
    if (!ocurrencias) return []
    return ocurrencias
      .filter((o) => o.salidaAbs <= tiempoAnimacion && tiempoAnimacion <= o.llegadaAbs)
      .map((o) => ({
        key: `${o.origen}-${o.destino}-${o.salidaAbs}`,
        origen: o.origen,
        destino: o.destino,
        maletas: o.maletas,
        capacidadMax: o.capacidadMax,
        pct: o.capacidadMax > 0 ? Math.round((o.maletas / o.capacidadMax) * 1000) / 10 : 0,
      }))
  }, [ocurrencias, tiempoAnimacion])

  const vuelosFiltrados = useMemo(() => {
    const q = busqueda.toLowerCase().trim()
    let lista = q
      ? vuelosActivos.filter(
          (v) =>
            v.origen.toLowerCase().includes(q) ||
            v.destino.toLowerCase().includes(q) ||
            `${v.origen}-${v.destino}`.toLowerCase().includes(q),
        )
      : vuelosActivos

    if (ordenarPorOcupacion) {
      lista = [...lista].sort((a, b) => b.pct - a.pct)
    }
    return lista
  }, [vuelosActivos, busqueda, ordenarPorOcupacion])

  if (!ocurrencias) {
    return (
      <p className={styles.vacio}>Disponible al completar una simulación</p>
    )
  }

  return (
    <div className={styles.contenedor}>
      <div className={styles.controles}>
        <div className={styles.searchWrap}>
          <Search size={12} className={styles.searchIcon} />
          <input
            className={styles.searchInput}
            placeholder="Tramo, origen o destino"
            value={busqueda}
            onChange={(e) => setBusqueda(e.target.value)}
          />
        </div>
        <button
          className={`${styles.sortBtn} ${ordenarPorOcupacion ? styles.sortActivo : ''}`}
          onClick={() => setOrdenarPorOcupacion((v) => !v)}
          title="Ordenar por ocupación"
        >
          <ArrowUpDown size={13} />
        </button>
      </div>

      {vuelosFiltrados.length === 0 ? (
        <p className={styles.vacio}>
          {vuelosActivos.length === 0 ? 'Sin vuelos activos en este momento' : 'Sin resultados'}
        </p>
      ) : (
        <ul className={styles.lista}>
          {vuelosFiltrados.map((v) => {
            const color = getColorSemaforo(v.pct, rangosSemaforo)
            const hex = COLORES_SEMAFORO[color]
            const seleccionado = vueloSeleccionado === v.key
            return (
              <li
                key={v.key}
                className={`${styles.item} ${seleccionado ? styles.itemSeleccionado : ''}`}
                onClick={() => setVueloSeleccionado(seleccionado ? null : v.key)}
              >
                <div className={styles.tramo}>
                  <span className={styles.codigo}>{v.origen}</span>
                  <span className={styles.flecha}>→</span>
                  <span className={styles.codigo}>{v.destino}</span>
                </div>
                <div className={styles.ocupacion}>
                  <span className={styles.maletas} style={{ color: hex }}>
                    {v.maletas}/{v.capacidadMax}
                  </span>
                  <span className={styles.pct} style={{ color: hex }}>
                    {v.pct.toFixed(1)}%
                  </span>
                  <Semaforo valor={v.pct} />
                </div>
              </li>
            )
          })}
        </ul>
      )}
    </div>
  )
}

export default PanelVuelos
