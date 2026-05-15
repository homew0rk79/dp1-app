import { create } from 'zustand'
import { DURACIONES_PERIODO, FECHA_INICIO_DATOS } from '../constants/restricciones'

const useSimulacionStore = create((set) => ({
  escenarioActivo: null,
  estadoEjecucion: 'idle', // 'idle' | 'corriendo' | 'pausado' | 'finalizado'
  colapsoDetectado: false,
  tiempoSegundos: 0,
  wsVersion: 0,
  parametros: {
    duracionPeriodo: DURACIONES_PERIODO[1], // 5 días por defecto
    fechaInicio: FECHA_INICIO_DATOS,
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
  setManifest: (manifest) => set({ manifest, tiempoAnimacion: manifest?.fechaInicioMinutos ?? 0 }),
  setTiempoAnimacion: (t) => set({ tiempoAnimacion: t }),
  setVelocidadAnimacion: (v) => set({ velocidadAnimacion: v }),
  resetear: () =>
    set((s) => ({
      escenarioActivo: null,
      estadoEjecucion: 'idle',
      colapsoDetectado: false,
      tiempoSegundos: 0,
      manifest: null,
      tiempoAnimacion: 0,
      wsVersion: s.wsVersion + 1,
    })),
}))

export default useSimulacionStore
