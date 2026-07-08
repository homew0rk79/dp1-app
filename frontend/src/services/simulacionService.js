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
  obtenerEnviosDeVuelo: (origen, destino, horaSalidaMinutos, dia = 0) =>
    api.get('/vuelos/envios', { params: { origen, destino, horaSalidaMinutos, dia } }),
  obtenerPlanificadosAeropuerto: (codigo, tiempoMin = 0, limite = 30) =>
    api.get(`/aeropuertos/${codigo}/planificados`, { params: { tiempoMin, limite } }),
  obtenerMonitorEnvios: (tiempoMin = 0, ventanaHoras = 4, limite = 50) =>
    api.get('/envios/monitor', { params: { tiempoMin, ventanaHoras, limite } }),
  uploadEnvios: (archivos) => {
    const form = new FormData()
    archivos.forEach((f) => form.append('archivo', f))
    return api.post('/planificacion/upload-envios', form)
  },
  limpiarEnvios: () => api.post('/planificacion/limpiar-envios'),
  registrarEnvio: (payload) => api.post('/planificacion/registrar-envio', payload),
  listarAlmacenesMantenimiento: () => api.get('/mantenimiento-mapa/almacenes'),
  crearAlmacenMantenimiento: (payload) => api.post('/mantenimiento-mapa/almacenes', payload),
  actualizarAlmacenMantenimiento: (id, payload) => api.put(`/mantenimiento-mapa/almacenes/${id}`, payload),
  listarUTMantenimiento: () => api.get('/mantenimiento-mapa/ut'),
  crearUTMantenimiento: (payload) => api.post('/mantenimiento-mapa/ut', payload),
  actualizarUTMantenimiento: (id, payload) => api.put(`/mantenimiento-mapa/ut/${id}`, payload),
  actualizarCapacidadUTMantenimiento: (id, payload) => api.patch(`/mantenimiento-mapa/ut/${id}/capacidad`, payload),
  listarTramosMantenimiento: () => api.get('/mantenimiento-mapa/tramos'),
  crearTramoMantenimiento: (payload) => api.post('/mantenimiento-mapa/tramos', payload),
  actualizarTramoMantenimiento: (id, payload) => api.put(`/mantenimiento-mapa/tramos/${id}`, payload),
  actualizarHorariosTramoMantenimiento: (id, payload) => api.patch(`/mantenimiento-mapa/tramos/${id}/horarios`, payload),
  obtenerConfiguracionMapa: () => api.get('/mantenimiento-mapa/configuracion'),
  guardarConfiguracionMapa: (payload) => api.put('/mantenimiento-mapa/configuracion', payload),
  obtenerRutaMaleta: (origen, idEnvio, numeroMaleta) =>
    api.get(`/envios/${encodeURIComponent(origen)}/${encodeURIComponent(idEnvio)}/maletas/${encodeURIComponent(numeroMaleta)}/ruta`),
  obtenerRutaEnvio: (origen, idEnvio) =>
    api.get(`/envios/${encodeURIComponent(origen)}/${encodeURIComponent(idEnvio)}/ruta`),
  obtenerIdsPruebaRutas: () => api.get('/rutas-busqueda/ids-prueba'),
}
