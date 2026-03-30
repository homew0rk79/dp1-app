import { useEffect, useRef, useState } from 'react'

/**
 * Gestiona la conexión WebSocket con el backend.
 * Úsalo en el Visualizador para recibir actualizaciones en tiempo real.
 *
 * @param {string} url - URL del WebSocket (ej: '/ws/operaciones')
 * @returns {{ mensaje: any, conectado: boolean, desconectar: Function }}
 */
function useWebSocket(url) {
  const [mensaje, setMensaje] = useState(null)
  const [conectado, setConectado] = useState(false)
  const wsRef = useRef(null)

  useEffect(() => {
    const ws = new WebSocket(`ws://${window.location.host}${url}`)
    wsRef.current = ws

    ws.onopen = () => setConectado(true)
    ws.onclose = () => setConectado(false)
    ws.onmessage = (event) => {
      try {
        setMensaje(JSON.parse(event.data))
      } catch {
        setMensaje(event.data)
      }
    }

    return () => ws.close()
  }, [url])

  const desconectar = () => wsRef.current?.close()

  return { mensaje, conectado, desconectar }
}

export default useWebSocket
