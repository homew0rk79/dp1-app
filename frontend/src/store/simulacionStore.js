import { create } from 'zustand'
import { DURACIONES_PERIODO, FECHA_INICIO_DATOS } from '../constants/restricciones'

const useSimulacionStore = create((set) => ({
  escenarioActivo: null,
  estadoEjecucion: 'IDLE',
  colapsoDetectado: false,
  tiempoSegundos: 0,
  inicioEjecucionReal: null,
  wsVersion: 0,
  parametros: {
    duracionPeriodo: DURACIONES_PERIODO[1], // 5 días por defecto
    fechaInicio: FECHA_INICIO_DATOS,
  },

  // Animación — persiste entre cambios de ruta
  manifest: null,
  tiempoAnimacion: 0,       // minuto simulado actual (actualizado por día, no por frame)
  velocidadAnimacion: 120,  // min simulados / segundo real
  playingAnimacion: false,

  setEscenario: (escenario) => set({ escenarioActivo: escenario }),
  setEstado: (estado) => set({ estadoEjecucion: estado }),
  setColapso: (valor) => set({ colapsoDetectado: valor }),
  setInicioEjecucionReal: (valor) => set({ inicioEjecucionReal: valor }),
  setParametros: (parametros) =>
    set((s) => ({ parametros: { ...s.parametros, ...parametros } })),
  incrementarTiempo: () => set((s) => ({ tiempoSegundos: s.tiempoSegundos + 1 })),
  setManifest: (manifest) => set({ manifest, tiempoAnimacion: 0, playingAnimacion: false }),
  updateManifest: (manifest) => set((s) => ({ manifest, wsVersion: s.wsVersion + 1 })),
  clearManifest: () => set({ manifest: null, tiempoAnimacion: 0, playingAnimacion: false }),
  setTiempoAnimacion: (t) => set({ tiempoAnimacion: t }),
  setVelocidadAnimacion: (v) => set({ velocidadAnimacion: v }),
  setPlayingAnimacion: (playing) => set({ playingAnimacion: playing }),
  resetear: () =>
    set((s) => ({
      escenarioActivo: null,
      estadoEjecucion: 'IDLE',
      colapsoDetectado: false,
      tiempoSegundos: 0,
      inicioEjecucionReal: null,
      manifest: null,
      tiempoAnimacion: 0,
      playingAnimacion: false,
      wsVersion: s.wsVersion + 1,
    })),
}))

export default useSimulacionStore
