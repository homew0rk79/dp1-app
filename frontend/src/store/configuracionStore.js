import { create } from 'zustand'
import { ALGORITMOS } from '../constants/restricciones'

const useConfiguracionStore = create((set) => ({
  rangosSemaforo: {
    verde: 60,   // 0–60%  → verde
    ambar: 85,   // 61–85% → ámbar
                 // 86–100% → rojo
  },
  algoritmoActivo: ALGORITMOS.ALGORITMO_1,
  aeropuertos: [],
  vuelos: [],

  setRangosSemaforo: (rangos) => set({ rangosSemaforo: rangos }),
  setAlgoritmoActivo: (algoritmo) => set({ algoritmoActivo: algoritmo }),
  setAeropuertos: (aeropuertos) => set({ aeropuertos }),
  setVuelos: (vuelos) => set({ vuelos }),
}))

export default useConfiguracionStore
