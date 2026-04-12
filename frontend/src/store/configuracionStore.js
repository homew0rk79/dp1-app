import { create } from 'zustand'

const useConfiguracionStore = create((set) => ({
  rangosSemaforo: {
    verde: 60,   // 0–60%  → verde
    ambar: 85,   // 61–85% → ámbar
                 // 86–100% → rojo
  },
  aeropuertos: [],
  vuelos: [],

  setRangosSemaforo: (rangos) => set({ rangosSemaforo: rangos }),
  setAeropuertos: (aeropuertos) => set({ aeropuertos }),
  setVuelos: (vuelos) => set({ vuelos }),
}))

export default useConfiguracionStore
