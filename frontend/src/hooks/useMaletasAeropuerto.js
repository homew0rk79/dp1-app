import { useEffect, useState } from 'react'
import { simulacionService } from '../services/simulacionService'

export default function useMaletasAeropuerto(codigo, tiempoMin, habilitado) {
  const [maletas, setMaletas] = useState(null)
  const [error, setError]     = useState(null)

  useEffect(() => {
    if (!habilitado) {
      setMaletas(null)
      setError(null)
      return
    }
    let cancelado = false
    setMaletas(null)
    setError(null)

    simulacionService.obtenerMaletasEnAeropuerto(codigo, tiempoMin)
      .then((res) => { if (!cancelado) setMaletas(res.data || []) })
      .catch(() => { if (!cancelado) setError('No se pudo obtener el detalle') })

    return () => { cancelado = true }
  }, [codigo, tiempoMin, habilitado])

  return { maletas, error }
}
