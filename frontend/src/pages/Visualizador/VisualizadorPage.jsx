import MapaInteractivo from '../../components/mapa/MapaInteractivo/MapaInteractivo'
import styles from './VisualizadorPage.module.css'

function VisualizadorPage() {
  return (
    <div className={styles.page}>
      <MapaInteractivo />
    </div>
  )
}

export default VisualizadorPage
