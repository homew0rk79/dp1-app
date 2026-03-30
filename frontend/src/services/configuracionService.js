import api from './api'

export const configuracionService = {
  getAeropuertos: () => api.get('/configuracion/aeropuertos'),
  guardarAeropuerto: (data) => api.post('/configuracion/aeropuertos', data),
  eliminarAeropuerto: (id) => api.delete(`/configuracion/aeropuertos/${id}`),

  getVuelos: () => api.get('/configuracion/vuelos'),
  guardarVuelo: (data) => api.post('/configuracion/vuelos', data),
  eliminarVuelo: (id) => api.delete(`/configuracion/vuelos/${id}`),

  getRangosSemaforo: () => api.get('/configuracion/semaforo'),
  guardarRangosSemaforo: (data) => api.put('/configuracion/semaforo', data),
}
