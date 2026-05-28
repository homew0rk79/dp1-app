import { create } from 'zustand'

const usePlanificadorStore = create((set) => ({
  conectado: false,
  progreso: null,
  snapshot: null,
  completado: null,
  error: null,

  setConectado: (conectado) => set({ conectado }),
  setProgreso: (progreso) => set({ progreso }),
  setSnapshot: (snapshot) => set({ snapshot }),
  setCompletado: (completado) => set({ completado }),
  setError: (error) => set({ error }),

  resetPlanificador: () =>
    set({
      progreso: null,
      snapshot: null,
      completado: null,
      error: null,
    }),
}))

export default usePlanificadorStore
