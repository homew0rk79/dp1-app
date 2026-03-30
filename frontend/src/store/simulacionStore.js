import { create } from 'zustand'
import { ESCENARIOS } from '../constants/escenarios'
import { ALGORITMOS, DURACIONES_PERIODO } from '../constants/restricciones'

const useSimulacionStore = create((set) => ({
  escenarioActivo: null,
  estadoEjecucion: 'idle', // 'idle' | 'corriendo' | 'pausado' | 'finalizado'
  colapsoDetectado: false,
  parametros: {
    duracionPeriodo: DURACIONES_PERIODO[1], // 5 días por defecto
    algoritmo: ALGORITMOS.ALGORITMO_1,
  },

  setEscenario: (escenario) => set({ escenarioActivo: escenario }),
  setEstado: (estado) => set({ estadoEjecucion: estado }),
  setColapso: (valor) => set({ colapsoDetectado: valor }),
  setParametros: (parametros) =>
    set((s) => ({ parametros: { ...s.parametros, ...parametros } })),
  resetear: () =>
    set({
      escenarioActivo: null,
      estadoEjecucion: 'idle',
      colapsoDetectado: false,
    }),
}))

export default useSimulacionStore
