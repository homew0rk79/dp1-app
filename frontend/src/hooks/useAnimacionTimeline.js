import { useRef, useState, useCallback } from 'react'
import useSimulacionStore from '../store/simulacionStore'

/**
 * Gestiona el estado de la animación de vuelos.
 *
 * manifest, velocidadAnimacion → Zustand (sobreviven cambios de ruta)
 * tiempoRef, velocidadRef     → refs locales (evitan re-renders en el RAF loop)
 * tiempoAnimacion en store    → actualizado solo al cambiar de día simulado
 *   (evita 60 escrituras/seg al store; el Sidebar lo lee para los porcentajes)
 */
function useAnimacionTimeline() {
  const manifest            = useSimulacionStore((s) => s.manifest)
  const setManifest         = useSimulacionStore((s) => s.setManifest)
  const setTiempoAnimacion  = useSimulacionStore((s) => s.setTiempoAnimacion)
  const setVelocidadAnimacion = useSimulacionStore((s) => s.setVelocidadAnimacion)

  // Leer valores iniciales del store una sola vez al montar (sin suscripción reactiva)
  const { tiempoAnimacion: initialTiempo, velocidadAnimacion: initialVelocidad } =
    useSimulacionStore.getState()

  const [playing, setPlaying]           = useState(false)  // siempre pausa al volver
  const [tiempoDisplay, setTiempoDisplay] = useState(initialTiempo)
  const [velocidad, setVelocidadState]  = useState(initialVelocidad)

  const tiempoRef    = useRef(initialTiempo)
  const velocidadRef = useRef(initialVelocidad)
  const lastDiaRef   = useRef(Math.floor(initialTiempo / 60))

  const play  = useCallback(() => setPlaying(true), [])
  const pause = useCallback(() => setPlaying(false), [])

  const seekTo = useCallback((t) => {
    tiempoRef.current  = t
    lastDiaRef.current = Math.floor(t / 1440)
    setTiempoDisplay(t)
    setTiempoAnimacion(t)
  }, [setTiempoAnimacion])

  const setVelocidad = useCallback((v) => {
    velocidadRef.current = v
    setVelocidadState(v)
    setVelocidadAnimacion(v)
  }, [setVelocidadAnimacion])

  const cargarManifest = useCallback((data) => {
    const inicio = data.fechaInicioMinutos ?? 0
    tiempoRef.current  = inicio
    lastDiaRef.current = Math.floor(inicio / 1440)
    setTiempoDisplay(inicio)
    setPlaying(false)
    setManifest(data)
  }, [setManifest])

  const onTick = useCallback((t) => {
    setTiempoDisplay(t)
    // Actualizar el store cada hora simulada — el Sidebar lee esto para KPIs y aeropuertos
    const newHora = Math.floor(t / 60)
    if (newHora !== lastDiaRef.current) {
      lastDiaRef.current = newHora
      setTiempoAnimacion(t)
    }
    if (manifest && t >= manifest.duracionTotalMinutos) {
      setPlaying(false)
    }
  }, [manifest, setTiempoAnimacion])

  return {
    manifest,
    cargarManifest,
    playing,
    velocidad,
    velocidadRef,
    tiempoRef,
    tiempoDisplay,
    play,
    pause,
    seekTo,
    setVelocidad,
    onTick,
  }
}

export default useAnimacionTimeline
