import api from './api'

export const simulacionService = {
  iniciar: (data) => api.post('/planificacion/iniciar', data),
  obtenerEstado: () => api.get('/planificacion/estado'),
  obtenerMetricas: () => api.get('/planificacion/metricas'),
  obtenerAeropuertos: () => api.get('/aeropuertos'),
  obtenerVuelos: () => api.get('/vuelos'),
  obtenerManifestAnimacion: () => api.get('/planificacion/animacion'),
  obtenerMaletasEnAeropuerto: (codigo, tiempoMin = 0) =>
    api.get(`/aeropuertos/${codigo}/maletas`, { params: { tiempoMin } }),
  obtenerOcupacionActual: (tiempoMin = 0) =>
    api.get('/aeropuertos/ocupacion-actual', { params: { tiempoMin } }),
  continuarColapso: () => api.post('/planificacion/continuar-colapso'),
}
