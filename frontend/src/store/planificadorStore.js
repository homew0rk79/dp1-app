import { create } from 'zustand'

const usePlanificadorStore = create((set) => ({
  conectado: false,
  progreso: null,
  snapshot: null,
  completado: null,
  colapso: null,
  error: null,

  // Alertas temporales de cancelación/replanificación (#57/#58)
  alertasCancelacion: [],

  setConectado: (conectado) => set({ conectado }),
  setProgreso: (progreso) => set({ progreso }),
  setSnapshot: (snapshot) => set({ snapshot }),
  setCompletado: (completado) => set({ completado }),
  setColapso: (colapso) => set({ colapso }),
  setError: (error) => set({ error }),

  addAlertaCancelacion: ({ origen, destino }) =>
    set((s) => ({
      alertasCancelacion: [
        ...s.alertasCancelacion,
        {
          id: `${origen}-${destino}-${Date.now()}`,
          origen,
          destino,
          ts: Date.now(),
        },
      ],
    })),

  removeAlertaCancelacion: (id) =>
    set((s) => ({
      alertasCancelacion: s.alertasCancelacion.filter((a) => a.id !== id),
    })),

  resetPlanificador: () =>
    set({
      progreso: null,
      snapshot: null,
      completado: null,
      colapso: null,
      error: null,
      alertasCancelacion: [],
    }),
}))

export default usePlanificadorStore
