import { useEffect, useRef } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import usePlanificadorStore from '../store/planificadorStore'

function PlanificadorWSListener() {
  const clientRef = useRef(null)

  useEffect(() => {
    if (clientRef.current) return undefined

    const {
      setConectado,
      setProgreso,
      setSnapshot,
      setCompletado,
      setColapso,
      setError,
    } = usePlanificadorStore.getState()

    const parseMessage = (msg, setter, label) => {
      try {
        setter(JSON.parse(msg.body))
      } catch (err) {
        console.error(`No se pudo parsear evento WebSocket ${label}:`, err)
      }
    }

    const client = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      reconnectDelay: 5000,
      onConnect: () => {
        setConectado(true)
        client.subscribe('/topic/planificacion/progreso', (msg) => {
          parseMessage(msg, setProgreso, 'progreso')
        })
        client.subscribe('/topic/planificacion/snapshot', (msg) => {
          parseMessage(msg, setSnapshot, 'snapshot')
        })
        client.subscribe('/topic/planificacion/completado', (msg) => {
          parseMessage(msg, setCompletado, 'completado')
        })
        client.subscribe('/topic/planificacion/colapso', (msg) => {
          parseMessage(msg, setColapso, 'colapso')
        })
      },
      onDisconnect: () => setConectado(false),
      onWebSocketClose: () => setConectado(false),
      onStompError: (frame) => {
        console.error('STOMP error:', frame)
        setError(frame.headers?.message ?? 'Error STOMP')
      },
    })

    client.activate()
    clientRef.current = client

    return () => {
      clientRef.current = null
      client.deactivate()
    }
  }, [])

  return null
}

export default PlanificadorWSListener
