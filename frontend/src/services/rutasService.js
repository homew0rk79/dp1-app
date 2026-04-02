/**
 * Servicio mock de rutas — datos simulados para la pantalla de gestión sin backend.
 */

const delay = (ms = 280) => new Promise((resolve) => setTimeout(resolve, ms))

function clonar(o) {
  return JSON.parse(JSON.stringify(o))
}

/** @typedef {'pendiente' | 'en_transito' | 'completado'} EstadoRuta */
/** @typedef {'verde' | 'ambar' | 'rojo'} Cumplimiento */

/** Opciones de reasignación (mock): alternativas de planificación. */
export const RUTAS_ALTERNATIVAS_MOCK = [
  { id: 'ALT-01', label: 'LIM → BOG → MAD (vía hub Bogotá)' },
  { id: 'ALT-02', label: 'LIM → MIA → MAD (vía Miami)' },
  { id: 'ALT-03', label: 'LIM → MEX → MAD (vía Ciudad de México)' },
  { id: 'ALT-04', label: 'LIM → GRU → MAD (vía São Paulo)' },
]

const detalleInicial = {
  'RUT-2026-001': {
    id: 'RUT-2026-001',
    origen: 'LIM',
    destino: 'MAD',
    origenCiudad: 'Lima',
    destinoCiudad: 'Madrid',
    estado: 'en_transito',
    cumplimiento: 'verde',
    tiempoEstimado: '18 h 40 min',
    plazoCompromiso: '48 h (distinto continente)',
    fechaIngreso: '31/03/2026 06:00 UTC',
    fechaLimite: '02/04/2026 06:00 UTC',
    progreso: 62,
    tramos: [
      {
        id: 't1',
        vuelo: 'LA2406',
        ocupacion: 180,
        capacidadMax: 320,
        salida: '31/03/2026 14:20 UTC',
        llegada: '31/03/2026 19:05 UTC',
        estado: 'completado',
      },
      {
        id: 't2',
        vuelo: 'IB6580',
        ocupacion: 265,
        capacidadMax: 400,
        salida: '31/03/2026 22:30 UTC',
        llegada: '01/04/2026 08:15 UTC',
        estado: 'en_transito',
      },
      {
        id: 't3',
        vuelo: 'IB312',
        ocupacion: 0,
        capacidadMax: 280,
        salida: '01/04/2026 12:00 UTC',
        llegada: '01/04/2026 14:40 UTC',
        estado: 'pendiente',
      },
    ],
  },
  'RUT-2026-002': {
    id: 'RUT-2026-002',
    origen: 'BOG',
    destino: 'UIO',
    origenCiudad: 'Bogotá',
    destinoCiudad: 'Quito',
    estado: 'pendiente',
    cumplimiento: 'ambar',
    tiempoEstimado: '4 h 15 min',
    plazoCompromiso: '24 h (mismo continente)',
    fechaIngreso: '01/04/2026 09:30 UTC',
    fechaLimite: '02/04/2026 09:30 UTC',
    progreso: 15,
    tramos: [
      {
        id: 't1',
        vuelo: 'AV8372',
        ocupacion: 142,
        capacidadMax: 250,
        salida: '01/04/2026 16:00 UTC',
        llegada: '01/04/2026 17:15 UTC',
        estado: 'pendiente',
      },
    ],
  },
  'RUT-2026-003': {
    id: 'RUT-2026-003',
    origen: 'FRA',
    destino: 'EZE',
    origenCiudad: 'Frankfurt',
    destinoCiudad: 'Buenos Aires',
    estado: 'en_transito',
    cumplimiento: 'rojo',
    tiempoEstimado: '22 h 10 min',
    plazoCompromiso: '48 h (distinto continente)',
    fechaIngreso: '28/03/2026 11:00 UTC',
    fechaLimite: '30/03/2026 11:00 UTC',
    progreso: 78,
    tramos: [
      {
        id: 't1',
        vuelo: 'LH506',
        ocupacion: 310,
        capacidadMax: 400,
        salida: '30/03/2026 20:00 UTC',
        llegada: '31/03/2026 06:30 UTC',
        estado: 'completado',
      },
      {
        id: 't2',
        vuelo: 'LH502',
        ocupacion: 298,
        capacidadMax: 400,
        salida: '31/03/2026 18:45 UTC',
        llegada: '01/04/2026 05:20 UTC',
        estado: 'en_transito',
      },
    ],
  },
  'RUT-2026-004': {
    id: 'RUT-2026-004',
    origen: 'CDG',
    destino: 'JFK',
    origenCiudad: 'París',
    destinoCiudad: 'Nueva York',
    estado: 'completado',
    cumplimiento: 'verde',
    tiempoEstimado: '—',
    plazoCompromiso: '48 h (distinto continente)',
    fechaIngreso: '29/03/2026 08:00 UTC',
    fechaLimite: '31/03/2026 08:00 UTC',
    progreso: 100,
    tramos: [
      {
        id: 't1',
        vuelo: 'AF008',
        ocupacion: 220,
        capacidadMax: 350,
        salida: '30/03/2026 10:10 UTC',
        llegada: '30/03/2026 12:45 UTC',
        estado: 'completado',
      },
    ],
  },
  'RUT-2026-005': {
    id: 'RUT-2026-005',
    origen: 'SAL',
    destino: 'MIA',
    origenCiudad: 'San Salvador',
    destinoCiudad: 'Miami',
    estado: 'en_transito',
    cumplimiento: 'ambar',
    tiempoEstimado: '3 h 50 min',
    plazoCompromiso: '24 h (mismo continente)',
    fechaIngreso: '01/04/2026 02:00 UTC',
    fechaLimite: '02/04/2026 02:00 UTC',
    progreso: 48,
    tramos: [
      {
        id: 't1',
        vuelo: 'TA396',
        ocupacion: 195,
        capacidadMax: 280,
        salida: '01/04/2026 08:30 UTC',
        llegada: '01/04/2026 12:20 UTC',
        estado: 'en_transito',
      },
    ],
  },
}

/** @type {Record<string, (typeof detalleInicial)[string]>} */
let detallePorId = clonar(detalleInicial)

/**
 * Listado resumido para la tabla principal.
 */
export async function obtenerRutas() {
  await delay()
  return Object.values(detallePorId).map((r) => ({
    id: r.id,
    origen: r.origen,
    destino: r.destino,
    origenCiudad: r.origenCiudad,
    destinoCiudad: r.destinoCiudad,
    estado: r.estado,
    tiempoEstimado: r.tiempoEstimado,
    cumplimiento: r.cumplimiento,
  }))
}

/**
 * Detalle completo con tramos.
 * @param {string} id
 */
export async function obtenerDetalleRuta(id) {
  await delay(180)
  const d = detallePorId[id]
  if (!d) return null
  return clonar(d)
}

/**
 * Simula reasignación a una ruta alternativa.
 * @param {string} idRuta
 * @param {{ nuevaRutaId: string }} payload
 */
export async function reasignarRuta(idRuta, { nuevaRutaId }) {
  await delay(320)
  const alt = RUTAS_ALTERNATIVAS_MOCK.find((a) => a.id === nuevaRutaId)
  if (!alt || !detallePorId[idRuta]) {
    return { ok: false, mensaje: 'No se pudo reasignar la ruta.' }
  }
  const actual = detallePorId[idRuta]
  detallePorId[idRuta] = {
    ...actual,
    cumplimiento: 'ambar',
    estado: actual.estado === 'completado' ? actual.estado : 'en_transito',
    planAlternativoId: nuevaRutaId,
    planAlternativoLabel: alt.label,
  }
  return {
    ok: true,
    mensaje: `Ruta reasignada correctamente al plan ${alt.label}.`,
  }
}

/** Para pruebas o reset del mock */
export function _resetRutasMock() {
  detallePorId = clonar(detalleInicial)
}
