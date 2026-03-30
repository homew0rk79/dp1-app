import styles from './PanelMetrica.module.css'

// Tarjeta compacta de KPI para la sidebar.
// color: 'default' | 'verde' | 'ambar' | 'rojo'
function PanelMetrica({ icono: Icono, label, valor, color = 'default' }) {
  return (
    <div className={`${styles.panel} ${styles[color]}`}>
      <div className={styles.iconoWrapper}>
        <Icono size={18} strokeWidth={2} />
      </div>
      <div className={styles.info}>
        <span className={styles.valor}>{valor}</span>
        <span className={styles.label}>{label}</span>
      </div>
    </div>
  )
}

export default PanelMetrica
