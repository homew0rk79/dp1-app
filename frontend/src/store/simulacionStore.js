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

  // Animación — persiste entre cambios de ruta
  manifest: null,
  tiempoAnimacion: 0,       // minuto simulado actual (actualizado por día, no por frame)
  velocidadAnimacion: 120,  // min simulados / segundo real

  setEscenario: (escenario) => set({ escenarioActivo: escenario }),
  setEstado: (estado) => set({ estadoEjecucion: estado }),
  setColapso: (valor) => set({ colapsoDetectado: valor }),
  setParametros: (parametros) =>
    set((s) => ({ parametros: { ...s.parametros, ...parametros } })),
  incrementarTiempo: () => set((s) => ({ tiempoSegundos: s.tiempoSegundos + 1 })),
  setManifest: (manifest) => set({ manifest, tiempoAnimacion: 0 }),
  setTiempoAnimacion: (t) => set({ tiempoAnimacion: t }),
  setVelocidadAnimacion: (v) => set({ velocidadAnimacion: v }),
  resetear: () =>
    set({
      escenarioActivo: null,
      estadoEjecucion: 'idle',
      colapsoDetectado: false,
      tiempoSegundos: 0,
      manifest: null,
      tiempoAnimacion: 0,
    }),
}))

export default useSimulacionStore
