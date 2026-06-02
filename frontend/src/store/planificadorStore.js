import { create } from 'zustand'

const usePlanificadorStore = create((set) => ({
  conectado: false,
  progreso: null,
  snapshot: null,
  completado: null,
  colapso: null,
  error: null,

  setConectado: (conectado) => set({ conectado }),
  setProgreso: (progreso) => set({ progreso }),
  setSnapshot: (snapshot) => set({ snapshot }),
  setCompletado: (completado) => set({ completado }),
  setColapso: (colapso) => set({ colapso }),
  setError: (error) => set({ error }),

  resetPlanificador: () =>
    set({
      progreso: null,
      snapshot: null,
      completado: null,
      colapso: null,
      error: null,
    }),
}))

export default usePlanificadorStore
