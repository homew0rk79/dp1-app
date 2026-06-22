import { VUELOS_MOCK } from '../mocks/vuelos'
import { TIEMPO_SIMULADO_REFERENCIA } from '../constants/tiempoSimulado'

const REF = new Date(TIEMPO_SIMULADO_REFERENCIA)

function formatearHora(iso) {
  const d = new Date(iso)
  const dd = d.getUTCDate().toString().padStart(2, '0')
  const mo = (d.getUTCMonth() + 1).toString().padStart(2, '0')
  const hh = d.getUTCHours().toString().padStart(2, '0')
  const mm = d.getUTCMinutes().toString().padStart(2, '0')
  return `${dd}/${mo} ${hh}:${mm}`
}

/**
 * Envíos planificados que llegan al aeropuerto después del tiempo simulado.
 */
export function obtenerProximasLlegadas(codigoAeropuerto, tiempoReferencia = REF) {
  const tRef = tiempoReferencia instanceof Date ? tiempoReferencia : new Date(tiempoReferencia)
  const llegadas = []

  for (const vuelo of VUELOS_MOCK) {
    if (vuelo.destino !== codigoAeropuerto) continue
    const llegada = new Date(vuelo.horaLlegada)
    if (llegada <= tRef) continue

    for (const envio of vuelo.envios) {
      llegadas.push({
        id: envio.id,
        producto: envio.producto,
        maletas: envio.maletas,
        vuelo: vuelo.codigo,
        origen: vuelo.origen,
        horaLlegada: vuelo.horaLlegada,
        horaLlegadaFmt: formatearHora(vuelo.horaLlegada),
      })
    }
  }

  return llegadas.sort(
    (a, b) => new Date(a.horaLlegada).getTime() - new Date(b.horaLlegada).getTime(),
  )
}

/**
 * Envíos planificados que salen del aeropuerto después del tiempo simulado.
 */
export function obtenerProximasSalidas(codigoAeropuerto, tiempoReferencia = REF) {
  const tRef = tiempoReferencia instanceof Date ? tiempoReferencia : new Date(tiempoReferencia)
  const salidas = []

  for (const vuelo of VUELOS_MOCK) {
    if (vuelo.origen !== codigoAeropuerto) continue
    const salida = new Date(vuelo.horaSalida)
    if (salida <= tRef) continue

    for (const envio of vuelo.envios) {
      salidas.push({
        id: envio.id,
        producto: envio.producto,
        maletas: envio.maletas,
        vuelo: vuelo.codigo,
        destino: envio.destino ?? vuelo.destino,
        horaSalida: vuelo.horaSalida,
        horaSalidaFmt: formatearHora(vuelo.horaSalida),
      })
    }
  }

  return salidas.sort(
    (a, b) => new Date(a.horaSalida).getTime() - new Date(b.horaSalida).getTime(),
  )
}

/** Próxima salida desde un aeropuerto (para ordenamiento en sidebar). */
export function proximaSalidaAeropuerto(codigo, tiempoReferencia = REF) {
  const tRef = tiempoReferencia instanceof Date ? tiempoReferencia : new Date(tiempoReferencia)
  let min = null
  for (const v of VUELOS_MOCK) {
    if (v.origen !== codigo) continue
    const salida = new Date(v.horaSalida)
    if (salida <= tRef) continue
    if (!min || salida < min) min = salida
  }
  return min
}

/** Próxima llegada a un aeropuerto (para ordenamiento en sidebar). */
export function proximaLlegadaAeropuerto(codigo, tiempoReferencia = REF) {
  const tRef = tiempoReferencia instanceof Date ? tiempoReferencia : new Date(tiempoReferencia)
  let min = null
  for (const v of VUELOS_MOCK) {
    if (v.destino !== codigo) continue
    const llegada = new Date(v.horaLlegada)
    if (llegada <= tRef) continue
    if (!min || llegada < min) min = llegada
  }
  return min
}
