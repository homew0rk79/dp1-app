export const AEROPUERTOS_MOCK = [
  { id: 1,  codigo: 'SPIM', nombre: 'Lima',       ciudad: 'Lima',       pais: 'Perú',          continente: 'América', lat: -12.0464, lng:  -77.0428, ocupacion: 45, capacidad: 600, capacidadMax: 600, maletasActuales: 270 },
  { id: 2,  codigo: 'EHAM', nombre: 'Amsterdam',  ciudad: 'Amsterdam',  pais: 'Holanda',       continente: 'Europa',  lat:  52.3105, lng:    4.7683, ocupacion: 78, capacidad: 700, capacidadMax: 700, maletasActuales: 546 },
  { id: 3,  codigo: 'VIDP', nombre: 'Delhi',      ciudad: 'Delhi',      pais: 'India',         continente: 'Asia',    lat:  28.5562, lng:   77.1000, ocupacion: 92, capacidad: 800, capacidadMax: 800, maletasActuales: 736 },
  { id: 4,  codigo: 'SKBO', nombre: 'Bogotá',     ciudad: 'Bogotá',     pais: 'Colombia',      continente: 'América', lat:   4.7016, lng:  -74.1469, ocupacion: 55, capacidad: 750, capacidadMax: 750, maletasActuales: 413 },
  { id: 5,  codigo: 'EKCH', nombre: 'Copenhague', ciudad: 'Copenhague', pais: 'Dinamarca',     continente: 'Europa',  lat:  55.6180, lng:   12.6560, ocupacion: 88, capacidad: 650, capacidadMax: 650, maletasActuales: 572 },
  { id: 6,  codigo: 'OMDB', nombre: 'Dubái',      ciudad: 'Dubái',      pais: 'EAU',           continente: 'Asia',    lat:  25.2048, lng:   55.2708, ocupacion: 62, capacidad: 800, capacidadMax: 800, maletasActuales: 496 },
  { id: 7,  codigo: 'SCEL', nombre: 'Santiago',   ciudad: 'Santiago',   pais: 'Chile',         continente: 'América', lat: -33.3930, lng:  -70.7858, ocupacion: 38, capacidad: 500, capacidadMax: 500, maletasActuales: 190 },
  { id: 8,  codigo: 'EDDI', nombre: 'Berlín',     ciudad: 'Berlín',     pais: 'Alemania',      continente: 'Europa',  lat:  52.3667, lng:   13.5033, ocupacion: 71, capacidad: 700, capacidadMax: 700, maletasActuales: 497 },
  { id: 9,  codigo: 'OERK', nombre: 'Riad',       ciudad: 'Riad',       pais: 'Arabia Saudita', continente: 'Asia',   lat:  24.9576, lng:   46.6988, ocupacion: 85, capacidad: 750, capacidadMax: 750, maletasActuales: 638 },
  { id: 10, codigo: 'SBBR', nombre: 'Brasilia',   ciudad: 'Brasilia',   pais: 'Brasil',        continente: 'América', lat: -15.8711, lng:  -47.9186, ocupacion: 52, capacidad: 550, capacidadMax: 550, maletasActuales: 286 },
  { id: 11, codigo: 'SVMI', nombre: 'Caracas',    ciudad: 'Caracas',    pais: 'Venezuela',     continente: 'América', lat:  10.6031, lng:  -66.9910, ocupacion: 67, capacidad: 600, capacidadMax: 600, maletasActuales: 402 },
  { id: 12, codigo: 'EBCI', nombre: 'Bruselas',   ciudad: 'Bruselas',   pais: 'Bélgica',       continente: 'Europa',  lat:  50.4592, lng:    4.4538, ocupacion: 79, capacidad: 700, capacidadMax: 700, maletasActuales: 553 },
]

/** Mapa código ICAO → aeropuerto mock. */
export const AEROPUERTOS_POR_CODIGO = Object.fromEntries(
  AEROPUERTOS_MOCK.map((a) => [a.codigo, a]),
)
