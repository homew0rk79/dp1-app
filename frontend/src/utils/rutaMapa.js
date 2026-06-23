import { AEROPUERTOS_POR_CODIGO } from '../mocks/aeropuertos'

/**
 * Convierte una secuencia de códigos IATA a coordenadas [lat, lng] para Leaflet.
 * Omite códigos desconocidos.
 */
export function rutaACoordenadas(codigos) {
  if (!Array.isArray(codigos)) return []
  return codigos
    .map((codigo) => {
      const aero = AEROPUERTOS_POR_CODIGO[codigo]
      return aero ? [aero.lat, aero.lng] : null
    })
    .filter(Boolean)
}

/**
 * Devuelve puntos intermedios (escalas) excluyendo origen y destino.
 */
export function obtenerEscalas(codigos) {
  if (!Array.isArray(codigos) || codigos.length <= 2) return []
  return codigos.slice(1, -1)
}
