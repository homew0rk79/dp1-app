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
  // Vuelos cancelados permanentes para visualización en mapa
  vuelosCancelados: [],

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

  addVueloCancelado: (vuelo) =>
    set((s) => ({
      vuelosCancelados: [...s.vuelosCancelados, vuelo],
    })),

  resetPlanificador: () =>
    set({
      progreso: null,
      snapshot: null,
      completado: null,
      colapso: null,
      error: null,
      alertasCancelacion: [],
      vuelosCancelados: [],
    }),
}))

export default usePlanificadorStore
