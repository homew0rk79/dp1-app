import { useState, useEffect } from 'react'
import { AlertTriangle } from 'lucide-react'
import Modal from '../../components/common/Modal/Modal'
import { obtenerDetalleRuta } from '../../services/rutasService'
import styles from './GestionRutasModule.module.css'

function ReasignarModal({ abierto, rutaId, onCerrar, onConfirmar }) {
  const [tramos, setTramos] = useState([])
  const [seleccion, setSeleccion] = useState(null)
  const [cargando, setCargando] = useState(false)
  const [cargandoDetalle, setCargandoDetalle] = useState(false)

  useEffect(() => {
    if (!abierto || !rutaId) return
    setCargandoDetalle(true)
    obtenerDetalleRuta(rutaId)
      .then((d) => {
        const t = d?.tramos ?? []
        setTramos(t)
        setSeleccion(t.length > 0 ? 0 : null)
      })
      .catch(() => setTramos([]))
      .finally(() => setCargandoDetalle(false))
  }, [abierto, rutaId])

  const confirmar = async () => {
    if (seleccion === null || !tramos[seleccion]) return
    const tramo = tramos[seleccion]
    setCargando(true)
    try {
      await onConfirmar({
        origen: tramo.origen,
        destino: tramo.destino,
        horaSalidaMinutos: tramo.horaSalidaMinutos,
      })
      onCerrar()
    } finally {
      setCargando(false)
    }
  }

  return (
    <Modal
      titulo="Replanificar por vuelo cancelado"
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
            disabled={cargando || seleccion === null || tramos.length === 0}
          >
            {cargando ? 'Replanificando…' : 'Confirmar cancelación'}
          </button>
        </>
      }
    >
      <p style={{ margin: '0 0 14px', color: '#64748b', fontSize: '0.88rem' }}>
        Selecciona el tramo de vuelo cancelado para el envío{' '}
        <strong style={{ color: '#0f172a' }}>{rutaId ?? '—'}</strong>. El sistema
        buscará rutas alternativas para todos los envíos afectados por ese vuelo.
      </p>

      {cargandoDetalle ? (
        <p style={{ color: '#64748b', fontSize: '0.85rem' }}>Cargando tramos…</p>
      ) : tramos.length === 0 ? (
        <p style={{ color: '#94a3b8', fontSize: '0.85rem' }}>
          No hay tramos disponibles para este envío.
        </p>
      ) : (
        <>
          <label
            className={styles.detailLabel}
            htmlFor="tramo-cancelado"
            style={{ display: 'block', marginBottom: 6 }}
          >
            Vuelo a cancelar
          </label>
          <select
            id="tramo-cancelado"
            className={styles.select}
            value={seleccion ?? ''}
            onChange={(e) => setSeleccion(Number(e.target.value))}
          >
            {tramos.map((t, i) => (
              <option key={t.id} value={i}>
                {t.vuelo}
              </option>
            ))}
          </select>

          <div
            style={{
              marginTop: 14,
              padding: '10px 12px',
              background: '#fef3c7',
              borderRadius: 6,
              display: 'flex',
              gap: 8,
              alignItems: 'flex-start',
              fontSize: '0.82rem',
              color: '#92400e',
            }}
          >
            <AlertTriangle size={14} style={{ flexShrink: 0, marginTop: 1 }} />
            <span>
              Esta acción cancelará el vuelo seleccionado y replanificará todos los
              envíos que lo usen, no solo este.
            </span>
          </div>
        </>
      )}
    </Modal>
  )
}

export default ReasignarModal
