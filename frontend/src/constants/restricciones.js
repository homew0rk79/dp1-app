// Plazos de entrega (en dias)
export const PLAZO_MISMO_CONTINENTE = 1
export const PLAZO_DISTINTO_CONTINENTE = 2

// Tiempos de traslado (en dias)
export const TIEMPO_TRASLADO_MISMO_CONTINENTE = 0.5
export const TIEMPO_TRASLADO_DISTINTO_CONTINENTE = 1

// Capacidad de vuelos (maletas)
export const CAPACIDAD_VUELO_MIN = 150
export const CAPACIDAD_VUELO_MAX_CONTINENTAL = 250
export const CAPACIDAD_VUELO_MAX_INTERCONTINENTAL = 400

// Capacidad de almacenes por aeropuerto (maletas)
export const CAPACIDAD_ALMACEN_MIN = 500
export const CAPACIDAD_ALMACEN_MAX = 800

// Duracion de simulacion de periodo (en dias simulados)
export const DURACIONES_PERIODO = [3, 5, 7]

// Tiempo real de ejecucion de simulacion de periodo
export const DURACION_SIMULACION_MIN_MINUTOS = 30
export const DURACION_SIMULACION_MAX_MINUTOS = 90

// Parametros temporales del simulador:
// Ta: granularidad del avance visual por tick de animacion, en minutos simulados.
// Sa: salto temporal para sincronizar estado/KPIs y consultas del algoritmo.
// Sc se obtiene del backend en /api/planificacion/consumo-bloques como saltoMin.
export const TA_EJECUCION_ALGORITMO_MIN = 1
export const SA_SALTO_ALGORITMO_MIN = 60

// Rango de fechas del dataset
export const FECHA_INICIO_SIMULACION_ALGORITMO = '2026-01-01T00:00'
export const FECHA_INICIO_DATOS = '2026-01-02T00:00'
export const FECHA_FIN_DATOS = '2028-12-31'
