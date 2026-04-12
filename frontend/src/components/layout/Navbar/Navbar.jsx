import { NavLink } from 'react-router-dom'
import {
  Map, Luggage, Route,
  BarChart2, Settings, PlaneTakeoff,
} from 'lucide-react'
import styles from './Navbar.module.css'

const NAV_ITEMS = [
  { to: '/visualizador', label: 'Visualizador', icon: Map },
  { to: '/maletas',      label: 'Maletas',       icon: Luggage },
  { to: '/rutas',        label: 'Rutas',         icon: Route },
  { to: '/reportes',     label: 'Reportes',      icon: BarChart2 },
  { to: '/configuracion',label: 'Configuración', icon: Settings },
]

function Navbar() {
  return (
    <nav className={styles.navbar}>
      {/* Logo */}
      <div className={styles.logo}>
        <PlaneTakeoff size={20} strokeWidth={2.2} />
        <span>Tasf.B2B</span>
      </div>

      {/* Links */}
      <ul className={styles.navLinks}>
        {NAV_ITEMS.map(({ to, label, icon: Icon }) => (
          <li key={to}>
            <NavLink
              to={to}
              className={({ isActive }) =>
                `${styles.navLink} ${isActive ? styles.active : ''}`
              }
            >
              <Icon size={15} strokeWidth={2} />
              <span>{label}</span>
            </NavLink>
          </li>
        ))}
      </ul>

      {/* Estado de conexión */}
      <div className={styles.conexion}>
        <span className={styles.dot} />
        <span>Conectado</span>
      </div>
    </nav>
  )
}

export default Navbar
