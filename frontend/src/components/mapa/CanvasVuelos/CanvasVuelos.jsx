import { useEffect, useRef, useCallback } from 'react'
import { useMap } from 'react-leaflet'

/**
 * Canvas overlay sobre Leaflet que anima las ocurrencias de vuelo activas en tiempo T.
 *
 * Dibuja arcos bezier curvos + un punto móvil por vuelo.
 * Colores según % de ocupación: azul → amarillo → rojo.
 * La lógica de tiempo (avanzar T) vive aquí para evitar re-renders en el árbol React.
 */
function CanvasVuelos({ manifest, tiempoRef, velocidadRef, playing, onTick }) {
  const map          = useRef(null)
  const mapInstance  = useMap()
  const canvasRef    = useRef(null)
  const playingRef   = useRef(playing)
  const aerosRef     = useRef({})
  const animIdRef    = useRef(null)

  map.current = mapInstance

  useEffect(() => { playingRef.current = playing }, [playing])

  // Construir lookup codigo → aeropuerto al recibir manifest
  useEffect(() => {
    if (!manifest) return
    const lookup = {}
    manifest.aeropuertos.forEach(a => { lookup[a.codigo] = a })
    aerosRef.current = lookup
  }, [manifest])

  // Insertar canvas dentro del overlayPane, ANTES del <svg> de Leaflet,
  // para que los CircleMarkers (SVG) queden encima de los arcos por orden DOM.
  useEffect(() => {
    const overlayPane = mapInstance.getPanes().overlayPane
    const canvas = document.createElement('canvas')
    canvas.style.cssText =
      'position:absolute;top:0;left:0;pointer-events:none;background:transparent'

    const svg = overlayPane.querySelector('svg')
    if (svg) {
      overlayPane.insertBefore(canvas, svg)
    } else {
      overlayPane.appendChild(canvas)
    }
    canvasRef.current = canvas

    return () => {
      if (canvas.parentNode) canvas.parentNode.removeChild(canvas)
    }
  }, [mapInstance])

  // Función de dibujo pura — lee tiempoRef.current directamente
  const draw = useCallback(() => {
    const canvas = canvasRef.current
    if (!canvas || !manifest) return

    const container = map.current.getContainer()
    const W = container.offsetWidth
    const H = container.offsetHeight
    if (canvas.width !== W)  canvas.width  = W
    if (canvas.height !== H) canvas.height = H

    // Counteract the CSS transform Leaflet applies to mapPane during pan/zoom.
    // Without this, the canvas moves with the pane but its drawn content
    // (which uses container-relative coords) appears double-shifted.
    const topLeft = map.current.containerPointToLayerPoint([0, 0])
    canvas.style.transform = `translate(${topLeft.x}px,${topLeft.y}px)`

    const ctx = canvas.getContext('2d')
    ctx.clearRect(0, 0, W, H)

    const T = tiempoRef.current

    for (const o of manifest.ocurrencias) {
      if (o.salidaAbs > T || T > o.llegadaAbs) continue

      const a = aerosRef.current[o.origen]
      const b = aerosRef.current[o.destino]
      if (!a || !b) continue

      const p1 = map.current.latLngToContainerPoint([a.lat, a.lng])
      const p2 = map.current.latLngToContainerPoint([b.lat, b.lng])

      const dx   = p2.x - p1.x
      const dy   = p2.y - p1.y
      const dist = Math.sqrt(dx * dx + dy * dy)
      if (dist < 3) continue

      // Punto de control bezier: perpendicular izquierda al vector de vuelo
      const nx   = -dy / dist
      const ny   =  dx / dist
      const cpX  = (p1.x + p2.x) / 2 + nx * dist * 0.28
      const cpY  = (p1.y + p2.y) / 2 + ny * dist * 0.28

      const fillRatio = Math.min(1, o.maletas / o.capacidadMax)
      const alpha     = 0.35 + fillRatio * 0.45

      const color =
        fillRatio > 0.9 ? `rgba(220,38,38,${alpha})`
        : fillRatio > 0.7 ? `rgba(234,179,8,${alpha})`
        : `rgba(37,99,235,${alpha})`

      // Arco
      ctx.beginPath()
      ctx.moveTo(p1.x, p1.y)
      ctx.quadraticCurveTo(cpX, cpY, p2.x, p2.y)
      ctx.strokeStyle = color
      ctx.lineWidth   = Math.max(1.2, Math.min(3.5, o.maletas / 70))
      ctx.stroke()

      // Punto móvil sobre el arco (bezier paramétrico)
      const tp = Math.max(0, Math.min(1, (T - o.salidaAbs) / (o.llegadaAbs - o.salidaAbs)))
      const bx = (1 - tp) ** 2 * p1.x + 2 * (1 - tp) * tp * cpX + tp ** 2 * p2.x
      const by = (1 - tp) ** 2 * p1.y + 2 * (1 - tp) * tp * cpY + tp ** 2 * p2.y

      const dotColor =
        fillRatio > 0.9 ? 'rgba(220,38,38,0.95)'
        : fillRatio > 0.7 ? 'rgba(234,179,8,0.95)'
        : 'rgba(37,99,235,0.95)'

      ctx.beginPath()
      ctx.arc(bx, by, 4, 0, Math.PI * 2)
      ctx.fillStyle   = dotColor
      ctx.fill()
      ctx.strokeStyle = 'white'
      ctx.lineWidth   = 1.5
      ctx.stroke()
    }
  }, [manifest, tiempoRef])

  // RAF loop — avanza T y dibuja cada frame
  useEffect(() => {
    if (!manifest) return

    let lastTs = null

    const frame = (ts) => {
      if (lastTs !== null && playingRef.current) {
        const dt = Math.min((ts - lastTs) / 1000, 0.1)
        tiempoRef.current = Math.min(
          tiempoRef.current + velocidadRef.current * dt,
          manifest.duracionTotalMinutos,
        )
        onTick?.(tiempoRef.current)
      }
      lastTs = ts
      draw()
      animIdRef.current = requestAnimationFrame(frame)
    }

    animIdRef.current = requestAnimationFrame(frame)

    return () => {
      if (animIdRef.current) cancelAnimationFrame(animIdRef.current)
    }
  }, [manifest, draw, tiempoRef, velocidadRef, onTick])

  // Redibujado al hacer pan/zoom (cuando la animación está pausada)
  useEffect(() => {
    const redraw = () => draw()
    mapInstance.on('move zoom', redraw)
    return () => mapInstance.off('move zoom', redraw)
  }, [mapInstance, draw])

  return null
}

export default CanvasVuelos
