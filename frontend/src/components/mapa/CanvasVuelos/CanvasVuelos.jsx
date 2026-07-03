import { useEffect, useRef, useCallback } from 'react'
import { useMap } from 'react-leaflet'
import useSeleccionStore from '../../../store/seleccionStore'

/**
 * Canvas overlay sobre Leaflet que anima las ocurrencias de vuelo activas en tiempo T.
 *
 * Dibuja arcos bezier curvos + un ícono de avión por vuelo.
 * Colores según % de ocupación: azul → amarillo → rojo.
 * La lógica de tiempo (avanzar T) vive aquí para evitar re-renders en el árbol React.
 */

/** Bearing en radianes entre dos puntos lat/lng (0 = Norte, positivo = sentido horario). */
function calcularBearing(lat1, lng1, lat2, lng2) {
  const toRad = (d) => (d * Math.PI) / 180
  const dLng = toRad(lng2 - lng1)
  const y = Math.sin(dLng) * Math.cos(toRad(lat2))
  const x =
    Math.cos(toRad(lat1)) * Math.sin(toRad(lat2)) -
    Math.sin(toRad(lat1)) * Math.cos(toRad(lat2)) * Math.cos(dLng)
  return Math.atan2(y, x)
}

/** Dibuja un ícono de avión centrado en (0,0) apuntando hacia arriba (Norte). */
function drawAirplaneIcon(ctx, size, fillColor) {
  // Fuselaje
  ctx.beginPath()
  ctx.moveTo(0, -size)
  ctx.lineTo(size * 0.2, 0)
  ctx.lineTo(size * 0.15, size * 0.82)
  ctx.lineTo(0, size * 0.65)
  ctx.lineTo(-size * 0.15, size * 0.82)
  ctx.lineTo(-size * 0.2, 0)
  ctx.closePath()
  ctx.fillStyle = fillColor
  ctx.fill()
  ctx.strokeStyle = 'rgba(255,255,255,0.9)'
  ctx.lineWidth = 0.9
  ctx.stroke()

  // Alas
  ctx.beginPath()
  ctx.moveTo(0, -size * 0.05)
  ctx.lineTo(-size * 0.88, size * 0.28)
  ctx.lineTo(-size * 0.62, size * 0.42)
  ctx.lineTo(0, size * 0.2)
  ctx.lineTo(size * 0.62, size * 0.42)
  ctx.lineTo(size * 0.88, size * 0.28)
  ctx.closePath()
  ctx.fillStyle = fillColor
  ctx.fill()
  ctx.strokeStyle = 'rgba(255,255,255,0.9)'
  ctx.lineWidth = 0.7
  ctx.stroke()
}

function CanvasVuelos({ manifest, tiempoRef, velocidadRef, playing, onTick, avanceTickMin = 1, onCancelVuelo, filtrosUT }) {
  const map          = useRef(null)
  const mapInstance  = useMap()
  const canvasRef    = useRef(null)
  const playingRef   = useRef(playing)
  const aerosRef     = useRef({})
  const animIdRef    = useRef(null)
  const tiempoContinuoRef = useRef(0)
  // Hit areas para tooltip: [{ x, y, o, bearing }]
  const hitAreasRef  = useRef([])
  const tooltipRef   = useRef(null)
  const vueloSeleccionado = useSeleccionStore((s) => s.vueloSeleccionado)
  const setVueloSeleccionado = useSeleccionStore((s) => s.setVueloSeleccionado)

  map.current = mapInstance

  useEffect(() => { playingRef.current = playing }, [playing])

  // Construir lookup codigo → aeropuerto al recibir manifest
  useEffect(() => {
    if (!manifest) return
    const lookup = {}
    manifest.aeropuertos.forEach(a => { lookup[a.codigo] = a })
    aerosRef.current = lookup
    tiempoContinuoRef.current = tiempoRef.current
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

  // Tooltip DOM — se añade al contenedor del mapa (encima del canvas)
  useEffect(() => {
    const container = mapInstance.getContainer()
    const tooltip = document.createElement('div')
    tooltip.style.cssText = [
      'position:absolute',
      'z-index:10000',
      'pointer-events:none',
      'background:rgba(15,23,42,0.92)',
      'color:#f8fafc',
      'padding:7px 11px',
      'border-radius:7px',
      'font-size:12px',
      'line-height:1.55',
      'white-space:nowrap',
      'border:1px solid rgba(255,255,255,0.14)',
      'box-shadow:0 4px 14px rgba(0,0,0,0.3)',
      'display:none',
    ].join(';')
    container.appendChild(tooltip)
    tooltipRef.current = tooltip
    return () => {
      if (tooltip.parentNode) tooltip.parentNode.removeChild(tooltip)
    }
  }, [mapInstance])

  // Mousemove sobre el mapa → hit-test contra posiciones de UTs dibujadas
  useEffect(() => {
    const RADIUS = 14 // px de detección

    const handleMove = (e) => {
      const cp = mapInstance.mouseEventToContainerPoint(e.originalEvent)
      const { x, y } = cp
      let found = null
      for (const hit of hitAreasRef.current) {
        const dx = hit.x - x
        const dy = hit.y - y
        if (dx * dx + dy * dy <= RADIUS * RADIUS) { found = hit; break }
      }
      const tip = tooltipRef.current
      if (!tip) return
      if (found) {
        const pct = found.o.capacidadMax > 0
          ? Math.round((found.o.maletas / found.o.capacidadMax) * 1000) / 10
          : 0
        const c = pct > 90 ? '#f44336' : pct > 70 ? '#ff9800' : '#4caf50'
        tip.innerHTML = `
          <div style="font-weight:700;margin-bottom:4px;color:${c}">
            ${found.o.origen} → ${found.o.destino}
          </div>
          <div>Maletas: <strong>${found.o.maletas} / ${found.o.capacidadMax}</strong></div>
          <div>Ocupación: <strong style="color:${c}">${pct.toFixed(1)}%</strong></div>
          <div style="margin-top:6px; font-size:10px; color:#94a3b8; border-top: 1px solid rgba(255,255,255,0.1); padding-top: 4px;">
            Clic: seleccionar UT Â· doble clic: cancelar vuelo
          </div>
        `
        tip.style.display = 'block'
        tip.style.left = `${x + 16}px`
        tip.style.top = `${y - 8}px`
      } else {
        tip.style.display = 'none'
      }
    }

    const buscarHit = (e) => {
      const cp = mapInstance.mouseEventToContainerPoint(e.originalEvent)
      const { x, y } = cp
      for (const hit of hitAreasRef.current) {
        const dx = hit.x - x
        const dy = hit.y - y
        if (dx * dx + dy * dy <= RADIUS * RADIUS) return hit
      }
      return null
    }

    const handleClick = (e) => {
      const found = buscarHit(e)
      if (found) {
        setVueloSeleccionado(found.key)
      }
    }

    const handleDblClick = (e) => {
      const found = buscarHit(e)
      if (found && onCancelVuelo) {
        onCancelVuelo(found.o)
      }
    }

    const handleLeave = () => {
      if (tooltipRef.current) tooltipRef.current.style.display = 'none'
    }

    mapInstance.on('mousemove', handleMove)
    mapInstance.on('click', handleClick)
    mapInstance.on('dblclick', handleDblClick)
    mapInstance.getContainer().addEventListener('mouseleave', handleLeave)
    return () => {
      mapInstance.off('mousemove', handleMove)
      mapInstance.off('click', handleClick)
      mapInstance.off('dblclick', handleDblClick)
      mapInstance.getContainer().removeEventListener('mouseleave', handleLeave)
    }
  }, [mapInstance, onCancelVuelo, setVueloSeleccionado])

  // Función de dibujo pura — lee tiempoRef.current directamente
  const draw = useCallback(() => {
    const canvas = canvasRef.current
    if (!canvas || !manifest) return

    const container = map.current.getContainer()
    const W = container.offsetWidth
    const H = container.offsetHeight
    if (canvas.width !== W)  canvas.width  = W
    if (canvas.height !== H) canvas.height = H

    const topLeft = map.current.containerPointToLayerPoint([0, 0])
    canvas.style.transform = `translate(${topLeft.x}px,${topLeft.y}px)`

    const ctx = canvas.getContext('2d')
    ctx.clearRect(0, 0, W, H)

    const T    = tiempoRef.current
    const zoom = map.current.getZoom()
    // Ícono escala con zoom: pequeño en vista mundial, mayor al acercar
    const iconSize = Math.max(3.5, Math.min(9, (zoom - 1) * 0.9))

    const newHits = []
    const hayFiltroUT = Boolean(
      filtrosUT && (filtrosUT.semaforo !== 'todos' || filtrosUT.texto?.trim())
    )
    const utVisibles = hayFiltroUT ? new Set(filtrosUT?.visibles ?? []) : null

    for (const o of manifest.ocurrencias) {
      if (o.salidaAbs > T || T > o.llegadaAbs) continue  // vuelo no activo o ya llegó (#56)

      const vueloKey = `${o.origen}-${o.destino}-${o.salidaAbs}`
      if (utVisibles && !utVisibles.has(vueloKey)) continue

      const a = aerosRef.current[o.origen]
      const b = aerosRef.current[o.destino]
      if (!a || !b) continue

      const p1 = map.current.latLngToContainerPoint([a.lat, a.lng])
      const p2 = map.current.latLngToContainerPoint([b.lat, b.lng])

      const dx   = p2.x - p1.x
      const dy   = p2.y - p1.y
      const dist = Math.sqrt(dx * dx + dy * dy)
      if (dist < 3) continue

      const nx   = -dy / dist
      const ny   =  dx / dist
      const cpX  = (p1.x + p2.x) / 2 + nx * dist * 0.28
      const cpY  = (p1.y + p2.y) / 2 + ny * dist * 0.28

      const fillRatio = Math.min(1, o.maletas / (o.capacidadMax || 1))
      const alpha     = 0.35 + fillRatio * 0.45
      const seleccionado = vueloSeleccionado === vueloKey

      const color =
        seleccionado ? 'rgba(14,165,233,0.95)'
        : fillRatio > 0.9 ? `rgba(220,38,38,${alpha})`
        : fillRatio > 0.7 ? `rgba(234,179,8,${alpha})`
        : `rgba(37,99,235,${alpha})`

      const dotColor =
        seleccionado ? 'rgba(14,165,233,1)'
        : fillRatio > 0.9 ? 'rgba(220,38,38,0.95)'
        : fillRatio > 0.7 ? 'rgba(234,179,8,0.95)'
        : 'rgba(37,99,235,0.95)'

      // Arco Bézier
      ctx.beginPath()
      ctx.moveTo(p1.x, p1.y)
      ctx.quadraticCurveTo(cpX, cpY, p2.x, p2.y)
      ctx.strokeStyle = color
      ctx.lineWidth   = seleccionado ? 4.5 : Math.max(1.2, Math.min(3.5, o.maletas / 70))
      ctx.stroke()

      // Posición del avión sobre el arco (paramétrico Bézier) — #53 bearing
      const tp = Math.max(0, Math.min(1, (T - o.salidaAbs) / (o.llegadaAbs - o.salidaAbs)))
      const bx = (1 - tp) ** 2 * p1.x + 2 * (1 - tp) * tp * cpX + tp ** 2 * p2.x
      const by = (1 - tp) ** 2 * p1.y + 2 * (1 - tp) * tp * cpY + tp ** 2 * p2.y

      // Tangente de la curva Bézier en el punto tp (para orientar el avión)
      const tdx = 2 * (1 - tp) * (cpX - p1.x) + 2 * tp * (p2.x - cpX)
      const tdy = 2 * (1 - tp) * (cpY - p1.y) + 2 * tp * (p2.y - cpY)
      const canvasBearing = Math.atan2(tdx, -tdy) // atan2(x, -y) → ángulo respecto a "arriba"

      // Ícono de avión rotado (#48 + #53)
      ctx.save()
      ctx.translate(bx, by)
      ctx.rotate(canvasBearing)
      drawAirplaneIcon(ctx, iconSize, dotColor)
      ctx.restore()

      newHits.push({ x: bx, y: by, o, key: vueloKey })
    }

    hitAreasRef.current = newHits
  }, [manifest, tiempoRef, filtrosUT, vueloSeleccionado])

  // RAF loop — avanza T y dibuja cada frame
  useEffect(() => {
    if (!manifest) return

    let lastWallMs = null

    const frame = () => {
      const now = Date.now()
      if (lastWallMs !== null && playingRef.current) {
        // Date.now() en vez de ts del RAF: captura tiempo real incluso tras tab oculta
        const dt = (now - lastWallMs) / 1000
        const pasoMin = Math.max(1, Number(avanceTickMin) || 1)
        tiempoContinuoRef.current = Math.max(tiempoContinuoRef.current, tiempoRef.current)
        tiempoContinuoRef.current = Math.min(
          tiempoContinuoRef.current + velocidadRef.current * dt,
          manifest.duracionTotalMinutos,
        )
        tiempoRef.current = Math.min(
          Math.floor(tiempoContinuoRef.current / pasoMin) * pasoMin,
          manifest.duracionTotalMinutos,
        )
        onTick?.(tiempoRef.current)
      } else {
        tiempoContinuoRef.current = tiempoRef.current
      }
      lastWallMs = now
      draw()
      animIdRef.current = requestAnimationFrame(frame)
    }

    animIdRef.current = requestAnimationFrame(frame)

    return () => {
      if (animIdRef.current) cancelAnimationFrame(animIdRef.current)
    }
  }, [manifest, draw, tiempoRef, velocidadRef, onTick, avanceTickMin])

  // Redibujado al hacer pan/zoom (cuando la animación está pausada)
  useEffect(() => {
    const redraw = () => draw()
    mapInstance.on('move zoom', redraw)
    return () => mapInstance.off('move zoom', redraw)
  }, [mapInstance, draw])

  return null
}

export default CanvasVuelos
