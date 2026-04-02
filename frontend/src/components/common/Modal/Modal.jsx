import { useEffect } from 'react'
import { X } from 'lucide-react'
import styles from './Modal.module.css'

/**
 * @param {{
 *   titulo: string,
 *   abierto: boolean,
 *   onCerrar: () => void,
 *   children: import('react').ReactNode,
 *   acciones?: import('react').ReactNode,
 * }} props
 */
function Modal({ titulo, abierto, onCerrar, children, acciones }) {
  useEffect(() => {
    if (!abierto) return
    const prev = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      document.body.style.overflow = prev
    }
  }, [abierto])

  if (!abierto) return null

  return (
    <div
      className={styles.overlay}
      role="presentation"
      onClick={onCerrar}
    >
      <div
        className={styles.panel}
        role="dialog"
        aria-modal="true"
        aria-labelledby="modal-titulo"
        onClick={(e) => e.stopPropagation()}
      >
        <div className={styles.header}>
          <h2 id="modal-titulo" className={styles.titulo}>
            {titulo}
          </h2>
          <button type="button" className={styles.cerrar} onClick={onCerrar} aria-label="Cerrar">
            <X size={18} />
          </button>
        </div>
        <div className={styles.body}>{children}</div>
        {acciones ? <div className={styles.footer}>{acciones}</div> : null}
      </div>
    </div>
  )
}

export default Modal
