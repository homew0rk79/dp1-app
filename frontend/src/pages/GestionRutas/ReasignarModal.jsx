import { useState, useEffect } from 'react'
import Modal from '../../components/common/Modal/Modal'
import { RUTAS_ALTERNATIVAS_MOCK } from '../../services/rutasService'
import styles from './GestionRutasModule.module.css'

/**
 * @param {{
 *   abierto: boolean,
 *   rutaId: string | null,
 *   onCerrar: () => void,
 *   onConfirmar: (nuevaRutaId: string) => Promise<void>,
 * }} props
 */
function ReasignarModal({ abierto, rutaId, onCerrar, onConfirmar }) {
  const [seleccion, setSeleccion] = useState(RUTAS_ALTERNATIVAS_MOCK[0]?.id ?? '')
  const [cargando, setCargando] = useState(false)

  useEffect(() => {
    if (abierto) {
      setSeleccion(RUTAS_ALTERNATIVAS_MOCK[0]?.id ?? '')
    }
  }, [abierto, rutaId])

  const confirmar = async () => {
    if (!rutaId || !seleccion) return
    setCargando(true)
    try {
      await onConfirmar(seleccion)
      onCerrar()
    } finally {
      setCargando(false)
    }
  }

  return (
    <Modal
      titulo="Reasignar ruta"
      abierto={abierto}
      onCerrar={onCerrar}
      acciones={
        <>
          <button
            type="button"
            className={styles.botonSecundario}
            onClick={onCerrar}
            disabled={cargando}
          >
            Cancelar
          </button>
          <button
            type="button"
            className={styles.botonPrimario}
            onClick={confirmar}
            disabled={cargando || !seleccion}
          >
            {cargando ? 'Confirmando…' : 'Confirmar'}
          </button>
        </>
      }
    >
      <p style={{ margin: '0 0 12px', color: '#64748b', fontSize: '0.92rem' }}>
        Selecciona un plan de ruta alternativo para el envío{' '}
        <strong style={{ color: '#0f172a' }}>{rutaId ?? '—'}</strong>. Los datos son simulados.
      </p>
      <label className={styles.detailLabel} htmlFor="plan-alternativo" style={{ display: 'block', marginBottom: 6 }}>
        Nueva ruta (mock)
      </label>
      <select
        id="plan-alternativo"
        className={styles.select}
        value={seleccion}
        onChange={(e) => setSeleccion(e.target.value)}
      >
        {RUTAS_ALTERNATIVAS_MOCK.map((r) => (
          <option key={r.id} value={r.id}>
            {r.label}
          </option>
        ))}
      </select>
    </Modal>
  )
}

export default ReasignarModal
