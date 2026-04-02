import styles from './BarraProgreso.module.css'

/**
 * @param {{
 *   porcentaje: number,
 *   variante?: 'azul' | 'ambar' | 'rojo' | 'verde',
 *   className?: string,
 * }} props
 */
function BarraProgreso({ porcentaje, variante = 'azul', className = '' }) {
  const p = Math.min(100, Math.max(0, porcentaje))
  return (
    <div className={`${styles.track} ${className}`}>
      <div
        className={`${styles.fill} ${styles[`fill--${variante}`]}`}
        style={{ width: `${p}%` }}
      />
    </div>
  )
}

export default BarraProgreso
