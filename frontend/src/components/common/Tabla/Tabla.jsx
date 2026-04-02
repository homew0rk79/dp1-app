import styles from './Tabla.module.css'

/**
 * Contenedor de tabla con scroll horizontal y estilo alineado al panel de gestión.
 * @param {{ children: import('react').ReactNode, className?: string, minWidth?: string | number }} props
 */
function Tabla({ children, className = '', minWidth = 1020 }) {
  return (
    <div className={`${styles.wrapper} ${className}`}>
      <table className={styles.table} style={{ minWidth }}>
        {children}
      </table>
    </div>
  )
}

export default Tabla
