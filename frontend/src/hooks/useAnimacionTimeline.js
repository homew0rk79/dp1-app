import { useEffect, useRef, useState, useCallback } from 'react'
import useSimulacionStore from '../store/simulacionStore'
import { SA_SALTO_ALGORITMO_MIN } from '../constants/restricciones'

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
  const playing            = useSimulacionStore((s) => s.playingAnimacion)
  const setPlayingAnimacion = useSimulacionStore((s) => s.setPlayingAnimacion)
  const tiempoAnimacion    = useSimulacionStore((s) => s.tiempoAnimacion)

  // Leer valores iniciales del store una sola vez al montar (sin suscripción reactiva)
  const { tiempoAnimacion: initialTiempo, velocidadAnimacion: initialVelocidad } =
    useSimulacionStore.getState()

  const [tiempoDisplay, setTiempoDisplay] = useState(initialTiempo)
  const [velocidad, setVelocidadState]  = useState(initialVelocidad)

  const tiempoRef    = useRef(initialTiempo)
  const velocidadRef = useRef(initialVelocidad)
  const lastSaltoRef = useRef(Math.floor(initialTiempo / SA_SALTO_ALGORITMO_MIN))

  useEffect(() => {
    if (Math.abs(tiempoRef.current - tiempoAnimacion) <= 1) return
    tiempoRef.current = tiempoAnimacion
    lastSaltoRef.current = Math.floor(tiempoAnimacion / SA_SALTO_ALGORITMO_MIN)
    setTiempoDisplay(tiempoAnimacion)
  }, [tiempoAnimacion])

  const play  = useCallback(() => setPlayingAnimacion(true), [setPlayingAnimacion])
  const pause = useCallback(() => setPlayingAnimacion(false), [setPlayingAnimacion])

  const seekTo = useCallback((t) => {
    tiempoRef.current  = t
    lastSaltoRef.current = Math.floor(t / SA_SALTO_ALGORITMO_MIN)
    setTiempoDisplay(t)
    setTiempoAnimacion(t)
  }, [setTiempoAnimacion])

  const setVelocidad = useCallback((v) => {
    velocidadRef.current = v
    setVelocidadState(v)
    setVelocidadAnimacion(v)
  }, [setVelocidadAnimacion])

  const cargarManifest = useCallback((data) => {
    tiempoRef.current  = 0
    lastSaltoRef.current = 0
    setTiempoDisplay(0)
    setPlayingAnimacion(false)
    setManifest(data)

    // En día a día la velocidad queda fija en 60x (1 s real = 1 min simulado)
    // y la animación arranca sola: es una simulación en tiempo real.
    const escenario = useSimulacionStore.getState().escenarioActivo
    if (escenario === 'DIA_A_DIA') {
      velocidadRef.current = 60
      setVelocidadState(60)
      setVelocidadAnimacion(60)
      setPlayingAnimacion(true)
    }
  }, [setManifest, setPlayingAnimacion, setVelocidadAnimacion])

  const actualizarManifest = useCallback((data) => {
    useSimulacionStore.getState().updateManifest(data)
  }, [])

  const onTick = useCallback((t) => {
    setTiempoDisplay(t)
    // Actualizar el store cada hora simulada — el Sidebar lee esto para KPIs y aeropuertos
    const nuevoSalto = Math.floor(t / SA_SALTO_ALGORITMO_MIN)
    if (nuevoSalto !== lastSaltoRef.current) {
      lastSaltoRef.current = nuevoSalto
      setTiempoAnimacion(t)
    }
    if (manifest && t >= manifest.duracionTotalMinutos) {
      // En día a día el manifest crece con cada replanificación: no pausar,
      // el reloj continúa cuando llega el siguiente bloque.
      if (useSimulacionStore.getState().escenarioActivo !== 'DIA_A_DIA') {
        setPlayingAnimacion(false)
      }
    }
  }, [manifest, setPlayingAnimacion, setTiempoAnimacion])

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
    actualizarManifest,
  }
}

export default useAnimacionTimeline
