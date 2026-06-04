import api from './api'

function postTxtUpload(url, fieldName, files) {
  const formData = new FormData()
  const lista = Array.isArray(files) ? files : [files]
  lista.forEach((file) => formData.append(fieldName, file))

  return api.post(url, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
}

export const configuracionService = {
  getAeropuertos: () => api.get('/configuracion/aeropuertos'),
  guardarAeropuerto: (data) => api.post('/configuracion/aeropuertos', data),
  eliminarAeropuerto: (id) => api.delete(`/configuracion/aeropuertos/${id}`),

  getVuelos: () => api.get('/configuracion/vuelos'),
  guardarVuelo: (data) => api.post('/configuracion/vuelos', data),
  eliminarVuelo: (id) => api.delete(`/configuracion/vuelos/${id}`),

  getRangosSemaforo: () => api.get('/configuracion/semaforo'),
  guardarRangosSemaforo: (data) => api.put('/configuracion/semaforo', data),

  subirAeropuertos: (file) => postTxtUpload('/db/carga/aeropuertos/upload', 'archivo', file),
  subirVuelos: (file) => postTxtUpload('/db/carga/vuelos/upload', 'archivo', file),
  subirEnvios: (files) => postTxtUpload('/db/carga/envios/upload', 'archivos', files),
}
