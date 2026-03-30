import api from './api'

export const simulacionService = {
  iniciar: (data) => api.post('/simulacion/iniciar', data),
  pausar: () => api.post('/simulacion/pausar'),
  detener: () => api.post('/simulacion/detener'),
  obtenerEstado: () => api.get('/simulacion/estado'),
  obtenerResultados: () => api.get('/simulacion/resultados'),
}
