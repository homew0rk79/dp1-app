import { create } from 'zustand'

const useSeleccionStore = create((set) => ({
  aeropuertoSeleccionado: null,
  vueloSeleccionado: null,

  setAeropuertoSeleccionado: (codigo) =>
    set({ aeropuertoSeleccionado: codigo, vueloSeleccionado: null }),

  setVueloSeleccionado: (key) =>
    set({ vueloSeleccionado: key, aeropuertoSeleccionado: null }),

  limpiarSeleccion: () =>
    set({ aeropuertoSeleccionado: null, vueloSeleccionado: null }),
}))

export default useSeleccionStore