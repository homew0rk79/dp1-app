import { createContext, useContext } from 'react'

const OverlayRutaContext = createContext(null)

export function OverlayRutaProvider({ value, children }) {
  return (
    <OverlayRutaContext.Provider value={value}>
      {children}
    </OverlayRutaContext.Provider>
  )
}

export function useOverlayRuta() {
  return useContext(OverlayRutaContext)
}
