import { create } from 'zustand'
import { DURACIONES_PERIODO } from '../constants/restricciones'

const useSimulacionStore = create((set) => ({
  escenarioActivo: null,
  estadoEjecucion: 'idle', // 'idle' | 'corriendo' | 'pausado' | 'finalizado'
  colapsoDetectado: false,
  tiempoSegundos: 0,
  parametros: {
    duracionPeriodo: DURACIONES_PERIODO[1], // 5 días por defecto
  },

  setEscenario: (escenario) => set({ escenarioActivo: escenario }),
  setEstado: (estado) => set({ estadoEjecucion: estado }),
  setColapso: (valor) => set({ colapsoDetectado: valor }),
  setParametros: (parametros) =>
    set((s) => ({ parametros: { ...s.parametros, ...parametros } })),
  incrementarTiempo: () => set((s) => ({ tiempoSegundos: s.tiempoSegundos + 1 })),
  resetear: () =>
    set({
      escenarioActivo: null,
      estadoEjecucion: 'idle',
      colapsoDetectado: false,
      tiempoSegundos: 0,
    }),
}))

export default useSimulacionStore
