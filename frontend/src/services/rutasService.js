import api from './api'

export const rutasService = {
  listar: () => api.get('/rutas'),
  obtenerDetalle: (id) => api.get(`/rutas/${id}`),
  reasignar: (id, data) => api.post(`/rutas/${id}/reasignar`, data),
  listarHistoricas: () => api.get('/rutas/historicas'),
}
