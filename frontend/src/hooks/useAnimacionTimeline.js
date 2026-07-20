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
    // Día a día: anclar el reloj local al "ahora" simulado del backend para que
    // un navegador que entra a mitad de simulación no arranque desde el minuto 0.
    const escenario = useSimulacionStore.getState().escenarioActivo
    const esDiaADia = escenario === 'DIA_A_DIA'
    const anclaBackend = esDiaADia && data?.tiempoSimuladoActualMin >= 0
      ? data.tiempoSimuladoActualMin
      : 0

    // setManifest resetea tiempoAnimacion a 0 en el store — anclar DESPUÉS.
    setManifest(data)
    tiempoRef.current  = anclaBackend
    lastSaltoRef.current = Math.floor(anclaBackend / SA_SALTO_ALGORITMO_MIN)
    setTiempoDisplay(anclaBackend)
    setTiempoAnimacion(anclaBackend)
    setPlayingAnimacion(false)

    if (esDiaADia) {
      // Velocidad 1 = 1 min simulado por segundo real (factor 60x del enunciado).
      // La animación arranca sola: es una simulación en tiempo real.
      velocidadRef.current = 1
      setVelocidadState(1)
      setVelocidadAnimacion(1)
      setPlayingAnimacion(true)
    }
  }, [setManifest, setPlayingAnimacion, setVelocidadAnimacion, setTiempoAnimacion])

  const actualizarManifest = useCallback((data) => {
    useSimulacionStore.getState().updateManifest(data)

    // Día a día: corregir el drift del reloj local contra el backend tras cada
    // tick, solo si la desviación supera 2 min simulados (evita saltos visuales).
    const escenario = useSimulacionStore.getState().escenarioActivo
    if (escenario === 'DIA_A_DIA' && data?.tiempoSimuladoActualMin >= 0) {
      const drift = Math.abs(tiempoRef.current - data.tiempoSimuladoActualMin)
      if (drift > 2) {
        tiempoRef.current = data.tiempoSimuladoActualMin
        lastSaltoRef.current = Math.floor(data.tiempoSimuladoActualMin / SA_SALTO_ALGORITMO_MIN)
        setTiempoDisplay(data.tiempoSimuladoActualMin)
        setTiempoAnimacion(data.tiempoSimuladoActualMin)
      }
    }
  }, [setTiempoAnimacion])

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
