# Evidencia tecnica de simulacion y visualizador

## Parametros de tiempo

- Ta = 30 min reales: tiempo objetivo de ejecucion/reproduccion para una simulacion de periodo.
- Sa = 60 min simulados: salto de avance usado por el visualizador para actualizar ocupacion por hora simulada.
- Sc = 60 min simulados: salto del eje del consumo por bloques.

Los valores estan publicados en `frontend/src/constants/restricciones.js` y `PlanificadorService`.

## Consumo de datos por bloques de tiempo

Endpoint: `GET /api/planificacion/consumo-bloques`

El backend agrupa el manifest de animacion en bloques Sc. Cada bloque devuelve:

- `bloque`
- `inicioMin`
- `finMin`
- `saltoMin`
- `maletas`
- `vuelosActivos`

El visualizador consume este endpoint y muestra un diagrama compacto de barras sobre el mapa.

## WebSocket multicliente

Flujo:

```mermaid
flowchart LR
  B[Backend PlanificadorService] --> P[WebSocketEventPublisher]
  P --> T1[/topic/planificacion/progreso]
  P --> T2[/topic/planificacion/snapshot]
  P --> T3[/topic/planificacion/completado]
  P --> T4[/topic/planificacion/colapso]
  T1 --> N1[Navegador A]
  T2 --> N1
  T3 --> N1
  T4 --> N1
  T1 --> N2[Navegador B]
  T2 --> N2
  T3 --> N2
  T4 --> N2
```

Cada navegador monta `PlanificadorWSListener`, se suscribe a los mismos topics STOMP y recibe los eventos publicados por `SimpMessagingTemplate`.

## Video de simulacion consumiendo datos

El componente `CanvasVuelos` consume `AnimacionManifestDTO` desde `GET /api/planificacion/animacion`. El manifest contiene ocurrencias de vuelo con salida/llegada absolutas, maletas y capacidad. La animacion avanza por `requestAnimationFrame`, mueve los vuelos activos segun el minuto simulado y redibuja arcos/puntos sin recargar la pagina.
