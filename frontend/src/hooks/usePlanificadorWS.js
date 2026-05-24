import { useEffect, useState, useRef } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'

/**
 * Conexión STOMP/WebSocket con el backend para recibir eventos
 * del planificador en tiempo real.
 *
 * Topics:
 *  /topic/planificacion/progreso   → { porcentaje, mensaje, estado, costoActual }
 *  /topic/planificacion/snapshot   → { iteracion, costoActual, aeropuertos[], rutas[] }
 *  /topic/planificacion/completado → MetricasDTO
 */
function usePlanificadorWS() {
  const [conectado, setConectado]   = useState(false)
  const [progreso, setProgreso]     = useState(null)
  const [snapshot, setSnapshot]     = useState(null)
  const [completado, setCompletado] = useState(null)
  const clientRef = useRef(null)

  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      reconnectDelay: 5000,
      onConnect: () => {
        setConectado(true)
        client.subscribe('/topic/planificacion/progreso', (msg) => {
          setProgreso(JSON.parse(msg.body))
        })
        client.subscribe('/topic/planificacion/snapshot', (msg) => {
          setSnapshot(JSON.parse(msg.body))
        })
        client.subscribe('/topic/planificacion/completado', (msg) => {
          setCompletado(JSON.parse(msg.body))
        })
      },
      onDisconnect: () => setConectado(false),
      onStompError: (frame) => console.error('STOMP error:', frame),
    })

    client.activate()
    clientRef.current = client

    return () => client.deactivate()
  }, [])

  return { conectado, progreso, snapshot, completado }
}

export default usePlanificadorWS
