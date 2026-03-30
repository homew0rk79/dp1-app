import api from './api'

export const maletasService = {
  listar: () => api.get('/maletas'),
  obtenerDetalle: (id) => api.get(`/maletas/${id}`),
  registrar: (data) => api.post('/maletas', data),
  actualizarUbicacion: (id, data) => api.patch(`/maletas/${id}/ubicacion`, data),
  marcarEntregada: (id) => api.patch(`/maletas/${id}/entregar`),
}
