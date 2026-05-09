import api from './api'

export const simulacionService = {
  iniciar: (data) => api.post('/planificacion/iniciar', data),
  obtenerEstado: () => api.get('/planificacion/estado'),
  obtenerMetricas: () => api.get('/planificacion/metricas'),
  obtenerAeropuertos: () => api.get('/aeropuertos'),
  obtenerVuelos: () => api.get('/vuelos'),
  obtenerManifestAnimacion: () => api.get('/planificacion/animacion'),
}
