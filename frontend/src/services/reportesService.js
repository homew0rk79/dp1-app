import api from './api'

export const reportesService = {
  getDesempeno: () => api.get('/reportes/desempeno'),
  getOcupacion: () => api.get('/reportes/ocupacion'),
  getAlgoritmos: () => api.get('/reportes/algoritmos'),
  getDemorados: () => api.get('/reportes/demorados'),
}
