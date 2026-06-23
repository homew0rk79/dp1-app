import { TIEMPO_SIMULADO_REFERENCIA } from '../constants/tiempoSimulado'

const REF = new Date(TIEMPO_SIMULADO_REFERENCIA)

function haceHoras(h) {
  return new Date(REF.getTime() - h * 3600000).toISOString()
}

function enHoras(h) {
  return new Date(REF.getTime() + h * 3600000).toISOString()
}

/** Rutas mock para gestión de rutas y overlay en mapa. */
export const RUTAS_MOCK = [
  {
    id: 'RUT-001',
    origen: 'LIM',
    destino: 'MAD',
    origenCiudad: 'Lima',
    destinoCiudad: 'Madrid',
    escalas: ['LIM', 'MAD'],
    estado: 'en_transito',
    cumplimiento: 'verde',
    tiempoEstimado: '18 h',
    fechaIngreso: '2026-03-31T08:00:00Z',
    fechaLimite: '2026-04-02T08:00:00Z',
    fechaEntrega: haceHoras(2),
    progreso: 65,
    plazoCompromiso: 'Entrega mismo continente (1 día)',
    tramos: [
      { id: 'T1', vuelo: 'LA-2401', origen: 'LIM', destino: 'MAD', ocupacion: 180, capacidadMax: 250, salida: '01/04 14:30', llegada: '02/04 06:00', estado: 'en_transito' },
    ],
    rutaAnterior: null,
  },
  {
    id: 'RUT-002',
    origen: 'NRT',
    destino: 'LHR',
    origenCiudad: 'Tokio',
    destinoCiudad: 'Londres',
    escalas: ['NRT', 'LHR'],
    estado: 'en_transito',
    cumplimiento: 'ambar',
    tiempoEstimado: '8 h',
    fechaIngreso: '2026-03-30T12:00:00Z',
    fechaLimite: '2026-04-01T12:00:00Z',
    fechaEntrega: haceHoras(8),
    progreso: 40,
    plazoCompromiso: 'Entrega distinto continente (2 días)',
    tramos: [
      { id: 'T1', vuelo: 'JL-8801', origen: 'NRT', destino: 'LHR', ocupacion: 352, capacidadMax: 400, salida: '01/04 16:00', llegada: '01/04 22:30', estado: 'en_transito' },
    ],
    rutaAnterior: ['NRT', 'DXB', 'LHR'],
  },
  {
    id: 'RUT-003',
    origen: 'JFK',
    destino: 'CDG',
    origenCiudad: 'Nueva York',
    destinoCiudad: 'París',
    escalas: ['JFK', 'CDG'],
    estado: 'completado',
    cumplimiento: 'verde',
    tiempoEstimado: '—',
    fechaIngreso: '2026-03-28T10:00:00Z',
    fechaLimite: '2026-03-30T10:00:00Z',
    fechaEntrega: haceHoras(20),
    progreso: 100,
    plazoCompromiso: 'Entrega distinto continente (2 días)',
    tramos: [
      { id: 'T1', vuelo: 'AA-100', origen: 'JFK', destino: 'CDG', ocupacion: 220, capacidadMax: 400, salida: '28/03 18:45', llegada: '29/03 07:15', estado: 'completado' },
    ],
    rutaAnterior: null,
  },
  {
    id: 'RUT-004',
    origen: 'GRU',
    destino: 'DXB',
    origenCiudad: 'São Paulo',
    destinoCiudad: 'Dubái',
    escalas: ['GRU', 'DXB'],
    estado: 'en_transito',
    cumplimiento: 'rojo',
    tiempoEstimado: '22 h',
    fechaIngreso: '2026-03-29T06:00:00Z',
    fechaLimite: '2026-03-31T06:00:00Z',
    fechaEntrega: enHoras(6),
    progreso: 25,
    plazoCompromiso: 'Entrega distinto continente (2 días)',
    tramos: [
      { id: 'T1', vuelo: 'EK-261', origen: 'GRU', destino: 'DXB', ocupacion: 380, capacidadMax: 400, salida: '01/04 20:00', llegada: '02/04 14:00', estado: 'en_transito' },
    ],
    rutaAnterior: ['GRU', 'LIS', 'DXB'],
  },
  {
    id: 'RUT-005',
    origen: 'MAD',
    destino: 'PVG',
    origenCiudad: 'Madrid',
    destinoCiudad: 'Shanghái',
    escalas: ['MAD', 'DXB', 'PVG'],
    estado: 'pendiente',
    cumplimiento: 'verde',
    tiempoEstimado: '20 h',
    fechaIngreso: '2026-04-01T06:00:00Z',
    fechaLimite: '2026-04-03T06:00:00Z',
    fechaEntrega: null,
    progreso: 10,
    plazoCompromiso: 'Entrega distinto continente (2 días)',
    tramos: [
      { id: 'T1', vuelo: 'IB-6842', origen: 'MAD', destino: 'DXB', ocupacion: 120, capacidadMax: 400, salida: '01/04 22:15', llegada: '02/04 06:00', estado: 'pendiente' },
      { id: 'T2', vuelo: 'EK-302', origen: 'DXB', destino: 'PVG', ocupacion: 148, capacidadMax: 400, salida: '02/04 08:30', llegada: '02/04 16:45', estado: 'pendiente' },
    ],
    rutaAnterior: ['MAD', 'PVG'],
  },
  {
    id: 'RUT-006',
    origen: 'LHR',
    destino: 'NRT',
    origenCiudad: 'Londres',
    destinoCiudad: 'Tokio',
    escalas: ['LHR', 'NRT'],
    estado: 'completado',
    cumplimiento: 'verde',
    tiempoEstimado: '—',
    fechaIngreso: '2026-03-27T14:00:00Z',
    fechaLimite: '2026-03-29T14:00:00Z',
    fechaEntrega: haceHoras(30),
    progreso: 100,
    plazoCompromiso: 'Entrega distinto continente (2 días)',
    tramos: [
      { id: 'T1', vuelo: 'BA-005', origen: 'LHR', destino: 'NRT', ocupacion: 324, capacidadMax: 400, salida: '27/03 08:00', llegada: '27/03 22:00', estado: 'completado' },
    ],
    rutaAnterior: null,
  },
  {
    id: 'RUT-007',
    origen: 'YYZ',
    destino: 'SIN',
    origenCiudad: 'Toronto',
    destinoCiudad: 'Singapur',
    escalas: ['YYZ', 'SIN'],
    estado: 'en_transito',
    cumplimiento: 'ambar',
    tiempoEstimado: '26 h',
    fechaIngreso: '2026-03-31T20:00:00Z',
    fechaLimite: '2026-04-02T20:00:00Z',
    fechaEntrega: haceHoras(1),
    progreso: 55,
    plazoCompromiso: 'Entrega distinto continente (2 días)',
    tramos: [
      { id: 'T1', vuelo: 'AC-030', origen: 'YYZ', destino: 'SIN', ocupacion: 296, capacidadMax: 400, salida: '02/04 06:30', llegada: '03/04 02:00', estado: 'en_transito' },
    ],
    rutaAnterior: ['YYZ', 'LHR', 'SIN'],
  },
]

export function obtenerRutaMockPorId(id) {
  return RUTAS_MOCK.find((r) => r.id === id) ?? null
}
