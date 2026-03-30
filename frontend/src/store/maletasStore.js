import { create } from 'zustand'

const useMaletasStore = create((set) => ({
  maletas: [],          // lista de maletas activas
  maletaSeleccionada: null,

  setMaletas: (maletas) => set({ maletas }),
  actualizarMaleta: (maletaActualizada) =>
    set((s) => ({
      maletas: s.maletas.map((m) =>
        m.id === maletaActualizada.id ? { ...m, ...maletaActualizada } : m
      ),
    })),
  seleccionarMaleta: (maleta) => set({ maletaSeleccionada: maleta }),
  limpiar: () => set({ maletas: [], maletaSeleccionada: null }),
}))

export default useMaletasStore
