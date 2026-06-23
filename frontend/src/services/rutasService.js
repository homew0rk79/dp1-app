import api from './api'

export async function obtenerRutas(limite = 300) {
  const resp = await api.get('/rutas', { params: { limite } })
  return resp.data
}

export async function obtenerDetalleRuta(id) {
  const resp = await api.get(`/rutas/${id}`)
  return resp.data
}

export async function cancelarVuelo(origen, destino, horaSalidaMinutos, idEnvio) {
  const resp = await api.post('/replanificacion/vuelo-cancelado', {
    idEnvio, origen, destino, horaSalidaMinutos,
  })
  return resp.data
}

export async function cancelarRuta(idEnvio) {
  const resp = await api.post(`/rutas/${idEnvio}/cancelar`)
  return resp.data
}
