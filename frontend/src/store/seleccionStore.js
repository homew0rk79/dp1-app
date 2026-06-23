import { create } from 'zustand'

const useSeleccionStore = create((set) => ({
  aeropuertoSeleccionado: null,
  vueloSeleccionado: null,
  envioSeleccionado: null,
  overlayRuta: null,
  filtrosMapa: {
    almacenes: {
      texto: '',
      continente: 'Todos',
      semaforo: 'todos',
      visibles: [],
    },
    ut: {
      texto: '',
      semaforo: 'todos',
      visibles: [],
    },
  },

  setAeropuertoSeleccionado: (codigo) =>
    set({ aeropuertoSeleccionado: codigo, vueloSeleccionado: null, envioSeleccionado: null }),

  setVueloSeleccionado: (key) =>
    set({ vueloSeleccionado: key, aeropuertoSeleccionado: null, envioSeleccionado: null }),

  setEnvioSeleccionado: (id, overlayRuta = null) =>
    set({
      envioSeleccionado: id,
      vueloSeleccionado: null,
      aeropuertoSeleccionado: null,
      overlayRuta,
    }),

  setOverlayRuta: (overlayRuta) => set({ overlayRuta }),
  limpiarOverlayRuta: () => set({ overlayRuta: null }),

  setFiltroAlmacenes: (patch) =>
    set((s) => ({
      filtrosMapa: {
        ...s.filtrosMapa,
        almacenes: { ...s.filtrosMapa.almacenes, ...patch },
      },
    })),

  setFiltroUT: (patch) =>
    set((s) => ({
      filtrosMapa: {
        ...s.filtrosMapa,
        ut: { ...s.filtrosMapa.ut, ...patch },
      },
    })),

  limpiarSeleccion: () =>
    set({
      aeropuertoSeleccionado: null,
      vueloSeleccionado: null,
      envioSeleccionado: null,
      overlayRuta: null,
    }),
}))

export default useSeleccionStore
