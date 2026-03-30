export const ESTADOS_MALETA = {
  EN_ESPERA: 'EN_ESPERA',
  EN_TRANSITO: 'EN_TRANSITO',
  ENTREGADO: 'ENTREGADO',
  DEMORADO: 'DEMORADO',
  REPLANIFICADO: 'REPLANIFICADO',
}

export const ETIQUETAS_ESTADO = {
  [ESTADOS_MALETA.EN_ESPERA]: 'En espera',
  [ESTADOS_MALETA.EN_TRANSITO]: 'En tránsito',
  [ESTADOS_MALETA.ENTREGADO]: 'Entregado',
  [ESTADOS_MALETA.DEMORADO]: 'Demorado',
  [ESTADOS_MALETA.REPLANIFICADO]: 'Replanificado',
}

export const ESTADOS_TRAMO = {
  PENDIENTE: 'PENDIENTE',
  EN_TRANSITO: 'EN_TRANSITO',
  COMPLETADO: 'COMPLETADO',
}

export const ESTADOS_ENVIO = {
  PENDIENTE: 'Pendiente',
  ALMACEN: 'En almacén',
  TRANSITO: 'En tránsito',
  REPLANIFICADO: 'Replanificado',
  ENTREGADO: 'Entregado',
  DEMORADO: 'Demorado',
  RIESGO: 'En Riesgo',
};

export const SEMAFORO = {
  VERDE: 'bg-green-500', // Cumplimiento normal
  AMBAR: 'bg-yellow-500', // Riesgo de incumplimiento (>50% tiempo transcurrido)
  ROJO: 'bg-red-500'      // Plazo vencido o crítico
};
