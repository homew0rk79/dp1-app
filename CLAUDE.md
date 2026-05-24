# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Contexto del dominio

**Tasf.B2B** es un sistema de planificación y monitoreo de traslado de maletas entre aeropuertos de América, Asia y Europa. La empresa recibe maletas de aerolíneas y las transporta usando vuelos propios, con compromisos de plazo.

Restricciones de negocio que el código debe respetar siempre (definidas en `frontend/src/constants/restricciones.js`):
- Plazo máximo mismo continente: **1 día**; distinto continente: **2 días**
- Capacidad por vuelo: **150–250 maletas** (intra) / **150–400** (inter)
- Capacidad almacén por aeropuerto: **500–800 maletas**
- Tiempo de tránsito: **0.5 días** (mismo continente) / **1 día** (inter)

Los tres escenarios de simulación son `DIA_A_DIA`, `PERIODO` (3, 5 o 7 días) y `COLAPSO` (hasta saturación).

## Comandos de desarrollo

### Backend (Java 21 + Spring Boot)
```bash
cd backend
./mvnw spring-boot:run          # Levanta en http://localhost:8080
./mvnw test                     # Corre tests
./mvnw clean package            # Genera JAR en target/
```
En Windows usar `mvnw.cmd` en lugar de `./mvnw`.

### Frontend (React + Vite)
```bash
cd frontend
npm install      # Solo la primera vez
npm run dev      # Dev server en http://localhost:5173
npm run build    # Build de producción en dist/
npm run preview  # Preview del build
```

El dev server proxea `/api/*` y `/ws/*` hacia `http://localhost:8080` (ver `vite.config.js`).

## Arquitectura

### Backend (`backend/src/main/java/com/tasfb2b/`)

El flujo principal es: `PlanificadorController` → `PlanificadorService` → `TabuSearch` → `WebSocketEventPublisher`.

- **`service/PlanificadorService`**: Orquesta todo. Lanza Tabu Search en un hilo background, publica eventos WebSocket con progreso, maneja replanificación ante cancelaciones de vuelos.
- **`algorithm/`**: Implementación de Tabu Search. `SolucionInicial` construye la solución greedy de arranque; `TabuSearch` hace búsqueda local con lista tabú de tamaño configurable y criterio de aspiración; `GrafoVuelos` representa la red de vuelos.
- **`data/DataLoader`**: Carga aeropuertos, vuelos y envíos desde archivos `.txt` en `docs/data/`. Las rutas se configuran en `application.properties`.
- **`config/WebSocketConfig`**: Endpoint STOMP en `/ws` con fallback SockJS. Los topics son `/topic/progreso`, `/topic/snapshot` y `/topic/completado`.
- **`controller/`**: Tres controladores REST — `PlanificadorController` (iniciar/estado/métricas), `RutasController` (rutas y replanificación), `DatosController` (aeropuertos y vuelos).

Parámetros del algoritmo configurables en `application.properties`: `tasf.algoritmo.iteraciones`, `tasf.algoritmo.tenencia`, `tasf.algoritmo.muestra`.

### Frontend (`frontend/src/`)

Dos responsabilidades principales:
1. **Visualizador** (`/visualizador`): mapa Leaflet en tiempo real conectado por WebSocket a `/topic/snapshot`.
2. **Panel de gestión**: rutas, maletas, reportes y configuración.

El control de simulación (selector de escenario + botones iniciar/pausar/detener) vive en el **`Sidebar`**, no en ninguna página. Al iniciar, navega automáticamente a `/visualizador`.

**Convenciones de equipo (obligatorias):**
- Componentes: PascalCase con carpeta propia y `ComponenteNombre.module.css` adjunto.
- Hooks: prefijo `use`, sin renderizado.
- Services: un archivo por módulo, solo llaman a la API REST; los componentes **no** llaman services directamente — lo hacen las pages o los hooks.
- Los colores semáforo **siempre** se calculan con `utils/semaforo.js`, nunca hardcodeados.
- Los valores del enunciado (plazos, capacidades) **siempre** vienen de `constants/restricciones.js`.
- Todo texto visible en la UI va en **español**. Zona horaria de referencia: **UTC**.

**Estado global (Zustand):**
- `simulacionStore`: escenario activo, estado de ejecución, `colapsoDetectado`, parámetros.
- `maletasStore`: cache de maletas activas, actualizado por WebSocket.
- `configuracionStore`: rangos del semáforo y capacidades por defecto.

**Flujo de replanificación:** usuario cancela un vuelo en `/gestion-rutas` → `POST /api/replanificacion/vuelo-cancelado` → backend replanifica envíos afectados → WebSocket publica snapshot actualizado.

## Datos de prueba

Los archivos de datos están en `docs/data/`:
- `c.1inf54.26.1.v1.Aeropuerto.husos.v1.20250818__estudiantes.txt` — red de aeropuertos
- `planes_vuelo.txt` — plan de vuelos
- `_envios_preliminar_/` — envíos por aeropuerto de origen (un `.txt` por aeropuerto ICAO)

El backend los lee al iniciar la simulación; las rutas se configuran en `application.properties` relativas al directorio de trabajo (raíz del proyecto cuando se corre con Maven).

## Documentación del proyecto

- `docs/c.1inf54.26-1.b.situacion.autentica_v20260324-1.pdf` — enunciado del caso de negocio
- `docs/01.definicion.producto.v02.pdf` — definición del producto
- `docs/03.lista.exigencias.v03.pdf` — lista de exigencias (exigibles y deseables) con IDs de requisito
- `docs/21.dis.selec.algoritmos.v03.pdf` — justificación de la selección de Tabu Search y diseño del ACO como alternativa
- `frontend/ESTRUCTURA.md` — arquitectura detallada del frontend con responsabilidades por carpeta
