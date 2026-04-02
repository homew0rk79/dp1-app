import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import Layout from './components/layout/Layout/Layout'
import VisualizadorPage from './pages/Visualizador/VisualizadorPage'
import GestionMaletasPage from './pages/GestionMaletas/GestionMaletasPage'
import GestionRutasPage from './pages/GestionRutas/GestionRutasPage'
import DetalleRuta from './pages/GestionRutas/DetalleRuta'
import SimulacionPage from './pages/Simulacion/SimulacionPage'
import ReportesPage from './pages/Reportes/ReportesPage'
import ConfiguracionPage from './pages/Configuracion/ConfiguracionPage'

function App() {
  return (
    <BrowserRouter>
      <Layout>
        <Routes>
          <Route path="/" element={<Navigate to="/visualizador" replace />} />
          <Route path="/visualizador"  element={<VisualizadorPage />} />
          <Route path="/maletas"       element={<GestionMaletasPage />} />
          <Route path="/gestion-rutas"   element={<GestionRutasPage />} />
          <Route path="/gestion-rutas/:id" element={<DetalleRuta />} />
          <Route path="/rutas"           element={<GestionRutasPage />} />
          <Route path="/simulacion"    element={<SimulacionPage />} />
          <Route path="/reportes"      element={<ReportesPage />} />
          <Route path="/configuracion" element={<ConfiguracionPage />} />
        </Routes>
      </Layout>
    </BrowserRouter>
  )
}

export default App
