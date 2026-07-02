import api from './api'

export const simulacionService = {
  iniciar: (data) => api.post('/planificacion/iniciar', data),
  obtenerEstado: () => api.get('/planificacion/estado'),
  detener: () => api.post('/planificacion/detener'),
  obtenerMetricas: () => api.get('/planificacion/metricas'),
  obtenerAeropuertos: () => api.get('/aeropuertos'),
  obtenerVuelos: () => api.get('/vuelos'),
  obtenerManifestAnimacion: () => api.get('/planificacion/animacion'),
  obtenerConsumoBloques: () => api.get('/planificacion/consumo-bloques'),
  obtenerMaletasEnAeropuerto: (codigo, tiempoMin = 0) =>
    api.get(`/aeropuertos/${codigo}/maletas`, { params: { tiempoMin } }),
  obtenerOcupacionActual: (tiempoMin = 0) =>
    api.get('/aeropuertos/ocupacion-actual', { params: { tiempoMin } }),
  continuarColapso: () => api.post('/planificacion/continuar-colapso'),
  cancelarVuelo: (payload) => api.post('/replanificacion/vuelo-cancelado', payload),
  obtenerVuelosProximos: (tiempoMin = 0, limite = 20) =>
    api.get('/vuelos/proximos', { params: { tiempoMin, limite } }),
  uploadEnvios: (archivos) => {
    const form = new FormData()
    archivos.forEach((f) => form.append('archivo', f))
    return api.post('/planificacion/upload-envios', form)
  },
  limpiarEnvios: () => api.post('/planificacion/limpiar-envios'),
  registrarEnvio: (payload) => api.post('/planificacion/registrar-envio', payload),
}
