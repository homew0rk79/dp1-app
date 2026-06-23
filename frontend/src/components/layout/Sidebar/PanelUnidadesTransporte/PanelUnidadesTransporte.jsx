import { useEffect, useMemo, useState } from 'react'
import { ArrowUp, ArrowDown, Search } from 'lucide-react'
import Semaforo from '../../../common/Semaforo/Semaforo'
import { getColorSemaforo, COLORES_SEMAFORO } from '../../../../utils/semaforo'
import useConfiguracionStore from '../../../../store/configuracionStore'
import useSeleccionStore from '../../../../store/seleccionStore'
import { VUELOS_MOCK } from '../../../../mocks/vuelos'
import { FECHA_INICIO_SIMULACION_ALGORITMO } from '../../../../constants/restricciones'
import styles from './PanelUnidadesTransporte.module.css'

const COLUMNAS = [
  { key: 'codigo', label: 'Código' },
  { key: 'origen', label: 'Origen' },
  { key: 'destino', label: 'Destino' },
  { key: 'horaSalida', label: 'Hora salida' },
  { key: 'horaLlegada', label: 'Hora llegada' },
  { key: 'ocupacion', label: 'Ocupación (%)' },
]

function minutosAbsAFecha(min) {
  const base = new Date(`${FECHA_INICIO_SIMULACION_ALGORITMO}:00Z`)
  return new Date(base.getTime() + min * 60000)
}

function formatearHora(fecha) {
  if (!fecha) return '—'
  const d = fecha instanceof Date ? fecha : new Date(fecha)
  const hh = d.getUTCHours().toString().padStart(2, '0')
  const mm = d.getUTCMinutes().toString().padStart(2, '0')
  const dd = d.getUTCDate().toString().padStart(2, '0')
  const mo = (d.getUTCMonth() + 1).toString().padStart(2, '0')
  return `${dd}/${mo} ${hh}:${mm}`
}

function ocurrenciasAUt(ocurrencias, tiempoAnimacion) {
  if (!ocurrencias) return null
  return ocurrencias
    .filter((o) => o.salidaAbs <= tiempoAnimacion && tiempoAnimacion <= o.llegadaAbs)
    .map((o) => ({
      key: `${o.origen}-${o.destino}-${o.salidaAbs}`,
      codigo: o.vuelo ?? o.codigoVuelo ?? `${o.origen}-${o.destino}`,
      origen: o.origen,
      destino: o.destino,
      horaSalida: minutosAbsAFecha(o.salidaAbs),
      horaLlegada: minutosAbsAFecha(o.llegadaAbs),
      ocupacion: o.capacidadMax > 0 ? Math.round((o.maletas / o.capacidadMax) * 1000) / 10 : 0,
      maletas: o.maletas,
      capacidadMax: o.capacidadMax,
    }))
}

function mocksAUt() {
  return VUELOS_MOCK.map((v) => ({
    key: v.id,
    codigo: v.codigo,
    origen: v.origen,
    destino: v.destino,
    horaSalida: new Date(v.horaSalida),
    horaLlegada: new Date(v.horaLlegada),
    ocupacion: v.ocupacion,
    maletas: v.maletas,
    capacidadMax: v.capacidadMax,
  }))
}

function compararFilas(a, b, key, dir) {
  const factor = dir === 'asc' ? 1 : -1
  let va = a[key]
  let vb = b[key]

  if (key === 'horaSalida' || key === 'horaLlegada') {
    va = va?.getTime?.() ?? 0
    vb = vb?.getTime?.() ?? 0
    return (va - vb) * factor
  }
  if (typeof va === 'string') return va.localeCompare(vb, 'es', { sensitivity: 'base' }) * factor
  return (va - vb) * factor
}

function EncabezadoOrdenable({ col, label, sortConfig, onSort }) {
  const activo = sortConfig.key === col
  return (
    <th
      className={`${styles.thSortable} ${activo ? styles.thActivo : ''}`}
      onClick={() => onSort(col)}
      title={`Ordenar por ${label}`}
    >
      <span>{label}</span>
      <span className={styles.sortIcon} aria-hidden>
        {activo ? (
          sortConfig.direction === 'asc' ? <ArrowUp size={12} /> : <ArrowDown size={12} />
        ) : (
          <span className={styles.sortNeutral}>↕</span>
        )}
      </span>
    </th>
  )
}

function PanelUnidadesTransporte({ ocurrencias, tiempoAnimacion }) {
  const rangosSemaforo = useConfiguracionStore((s) => s.rangosSemaforo)
  const vueloSeleccionado = useSeleccionStore((s) => s.vueloSeleccionado)
  const setVueloSeleccionado = useSeleccionStore((s) => s.setVueloSeleccionado)
  const setFiltroUT = useSeleccionStore((s) => s.setFiltroUT)

  const [filtroDestino, setFiltroDestino] = useState('')
  const [filtroSemaforo, setFiltroSemaforo] = useState('todos')
  const [sortConfig, setSortConfig] = useState({ key: 'horaSalida', direction: 'asc' })

  const listaBase = useMemo(() => {
    const desdeSim = ocurrenciasAUt(ocurrencias, tiempoAnimacion)
    if (desdeSim && desdeSim.length > 0) return desdeSim
    return mocksAUt()
  }, [ocurrencias, tiempoAnimacion])

  const listaVisible = useMemo(() => {
    const q = filtroDestino.trim().toLowerCase()
    let lista = listaBase
    if (q) {
      lista = lista.filter((ut) =>
        ut.destino.toLowerCase().includes(q) ||
        ut.origen.toLowerCase().includes(q) ||
        ut.codigo.toLowerCase().includes(q),
      )
    }
    if (filtroSemaforo !== 'todos') {
      lista = lista.filter((ut) => getColorSemaforo(ut.ocupacion, rangosSemaforo) === filtroSemaforo)
    }
    return [...lista].sort((a, b) =>
      compararFilas(a, b, sortConfig.key, sortConfig.direction),
    )
  }, [listaBase, filtroDestino, filtroSemaforo, rangosSemaforo, sortConfig])

  useEffect(() => {
    setFiltroUT({
      texto: filtroDestino,
      semaforo: filtroSemaforo,
      visibles: listaVisible.map((ut) => ut.key),
    })
  }, [filtroDestino, filtroSemaforo, listaVisible, setFiltroUT])

  function handleSort(key) {
    setSortConfig((prev) => ({
      key,
      direction: prev.key === key && prev.direction === 'asc' ? 'desc' : 'asc',
    }))
  }

  const usaMock = !ocurrencias || ocurrenciasAUt(ocurrencias, tiempoAnimacion)?.length === 0

  return (
    <div className={styles.contenedor}>
      {usaMock && (
        <p className={styles.notaMock}>Datos mock · referencia UTC</p>
      )}

      <div className={styles.filtroWrap}>
        <Search size={13} className={styles.filtroIcon} />
        <input
          type="text"
          className={styles.filtroInput}
          placeholder="Filtrar por destino"
          value={filtroDestino}
          onChange={(e) => setFiltroDestino(e.target.value)}
        />
      </div>
      <select
        className={styles.filtroSelect}
        value={filtroSemaforo}
        onChange={(e) => setFiltroSemaforo(e.target.value)}
      >
        <option value="todos">Semaforo: todos</option>
        <option value="verde">Verde</option>
        <option value="ambar">Ambar</option>
        <option value="rojo">Rojo</option>
      </select>

      <div className={styles.tablaWrap}>
        <table className={styles.tabla}>
          <thead>
            <tr>
              {COLUMNAS.map((c) => (
                <EncabezadoOrdenable
                  key={c.key}
                  col={c.key}
                  label={c.label}
                  sortConfig={sortConfig}
                  onSort={handleSort}
                />
              ))}
            </tr>
          </thead>
          <tbody>
            {listaVisible.length === 0 ? (
              <tr>
                <td colSpan={COLUMNAS.length} className={styles.sinResultados}>
                  Sin UT que coincidan con el filtro
                </td>
              </tr>
            ) : (
              listaVisible.map((ut) => {
                const color = getColorSemaforo(ut.ocupacion, rangosSemaforo)
                const hex = COLORES_SEMAFORO[color]
                const seleccionado = vueloSeleccionado === ut.key
                return (
                  <tr
                    key={ut.key}
                    className={seleccionado ? styles.filaSeleccionada : ''}
                    onClick={() => setVueloSeleccionado(seleccionado ? null : ut.key)}
                  >
                    <td className={styles.mono}>{ut.codigo}</td>
                    <td className={styles.mono}>{ut.origen}</td>
                    <td className={styles.mono}>{ut.destino}</td>
                    <td>{formatearHora(ut.horaSalida)}</td>
                    <td>{formatearHora(ut.horaLlegada)}</td>
                    <td className={styles.celdaOcupacion}>
                      <span className={styles.ocupacion} style={{ color: hex }}>
                        {ut.ocupacion.toFixed(1)}%
                      </span>
                      <Semaforo valor={ut.ocupacion} />
                    </td>
                  </tr>
                )
              })
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}

export default PanelUnidadesTransporte
