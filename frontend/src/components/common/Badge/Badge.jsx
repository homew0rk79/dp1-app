import styles from './Badge.module.css'

// tipo: 'info' | 'verde' | 'ambar' | 'rojo' | 'primary'
function Badge({ tipo = 'info', children }) {
  return (
    <span className={`${styles.badge} ${styles[tipo]}`}>
      {children}
    </span>
  )
}

export default Badge
