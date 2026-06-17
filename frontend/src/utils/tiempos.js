import {
  PLAZO_MISMO_CONTINENTE,
  PLAZO_DISTINTO_CONTINENTE,
} from '../constants/restricciones'

/**
 * Retorna el plazo máximo de entrega en días según si origen y destino
 * están en el mismo continente.
 */
export function calcularPlazoMaximo(mismokontinente) {
  return mismokontinente ? PLAZO_MISMO_CONTINENTE : PLAZO_DISTINTO_CONTINENTE
}

/**
 * Retorna true si el envío aún está dentro del plazo.
 * @param {string|Date} fechaIngreso
 * @param {number} plazoEnDias
 */
export function estaEnPlazo(fechaIngreso, plazoEnDias) {
  const ingreso = new Date(fechaIngreso)
  const limite = new Date(ingreso)
  limite.setDate(limite.getDate() + plazoEnDias)
  return new Date() <= limite
}

export function parseFechaLocal(fechaStr) {
  if (!fechaStr) return null
  const normalizada = fechaStr.length === 10 ? `${fechaStr}T00:00` : fechaStr
  const fecha = new Date(normalizada)
  return Number.isNaN(fecha.getTime()) ? null : fecha
}

export function formatearFechaHora(fecha) {
  if (!fecha || Number.isNaN(fecha.getTime())) return '--/--/---- --:--:--'
  const dd = String(fecha.getDate()).padStart(2, '0')
  const mm = String(fecha.getMonth() + 1).padStart(2, '0')
  const yyyy = fecha.getFullYear()
  const hh = String(fecha.getHours()).padStart(2, '0')
  const min = String(fecha.getMinutes()).padStart(2, '0')
  const ss = String(fecha.getSeconds()).padStart(2, '0')
  return `${dd}/${mm}/${yyyy} ${hh}:${min}:${ss}`
}

export function formatearDuracion(segundos) {
  const total = Math.max(0, Math.floor(segundos || 0))
  const h = Math.floor(total / 3600)
  const m = Math.floor((total % 3600) / 60)
  const s = total % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

export function sumarMinutos(fechaStr, minutos) {
  const fecha = parseFechaLocal(fechaStr)
  if (!fecha) return null
  return new Date(fecha.getTime() + Math.floor(minutos || 0) * 60000)
}
