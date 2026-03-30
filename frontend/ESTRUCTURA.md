# Estructura del Frontend — Tasf.B2B
## Índice

1. [Visión general](#visión-general)
2. [Árbol de carpetas](#árbol-de-carpetas)
3. [Detalle por carpeta](#detalle-por-carpeta)
   - [assets](#assets)
   - [components/common](#componentscommon)
   - [components/mapa](#componentsmapa)
   - [components/simulacion](#componentssimulacion)
   - [components/reportes](#componentsreportes)
   - [pages](#pages)
   - [hooks](#hooks)
   - [services](#services)
   - [store](#store)
   - [utils](#utils)
   - [constants](#constants)
4. [Convenciones del equipo](#convenciones-del-equipo)
5. [Dependencias principales](#dependencias-principales)

---

## Visión general

El frontend está dividido en dos grandes responsabilidades:

- **Visualizador**: mapa interactivo en tiempo real que muestra el estado de las operaciones (maletas, rutas, aeropuertos) usando colores de semáforo.
- **Panel de gestión**: pantallas para registrar maletas, gestionar rutas, ejecutar simulaciones, ver reportes y configurar parámetros del sistema.

La comunicación con el backend Java se hace mediante llamadas REST (para operaciones) y WebSocket (para actualizaciones en tiempo real del mapa).

---

## Árbol de carpetas

```
frontend/
├── index.html
├── vite.config.js
├── package.json
├── ESTRUCTURA.md               ← este archivo
└── src/
    ├── main.jsx
    ├── App.jsx
    ├── assets/
    ├── components/
    │   ├── common/
    │   ├── mapa/
    │   ├── simulacion/
    │   └── reportes/
    ├── pages/
    │   ├── Visualizador/
    │   ├── GestionMaletas/
    │   ├── GestionRutas/
    │   ├── Simulacion/
    │   ├── Reportes/
    │   └── Configuracion/
    ├── hooks/
    ├── services/
    ├── store/
    ├── utils/
    └── constants/
```

---

## Detalle por carpeta

---

### `assets/`

Archivos estáticos: imágenes, íconos SVG y fuentes.

```
assets/
└── iconos/
    ├── aeropuerto.svg    # Ícono de marcador en el mapa para aeropuertos
    ├── maleta.svg        # Ícono para representar maletas en tránsito
    └── avion.svg         # Ícono para representar vuelos activos
```

> Agregar aquí cualquier recurso gráfico que se use en más de una pantalla.
> No guardar imágenes específicas de una sola página — esas van junto a su componente.

---

### `components/common/`

Componentes de UI genéricos y reutilizables en cualquier parte del sistema.
**No deben contener lógica de negocio** — solo presentación y comportamiento básico de UI.

```
components/common/
├── Semaforo/
│   ├── Semaforo.jsx          # Indicador visual de tres estados: verde / ámbar / rojo.
│   └── Semaforo.module.css   # Recibe un valor numérico y los rangos configurados,
│                             # y muestra el color correspondiente.
│
├── Tabla/
│   ├── Tabla.jsx             # Tabla con paginación y soporte para filtros.
│   └── Tabla.module.css      # Usada en listas de maletas, rutas, reportes, etc.
│
├── Modal/
│   ├── Modal.jsx             # Ventana emergente genérica con overlay.
│   └── Modal.module.css      # Recibe título, contenido y acciones como props.
│
├── Badge/
│   ├── Badge.jsx             # Etiqueta pequeña de estado (ej: "En tránsito", "Demorado").
│   └── Badge.module.css      # Usa colores consistentes con el sistema de semáforo.
│
├── PanelMetrica/
│   ├── PanelMetrica.jsx      # Tarjeta de KPI: muestra un número grande con etiqueta
│   └── PanelMetrica.module.css # y color de semáforo. Usada en dashboards y reportes.
│
└── Cargando/
    └── Cargando.jsx          # Spinner o skeleton para estados de carga.
```

---

### `components/mapa/`

Componentes exclusivos del **visualizador de mapa interactivo**.
Usan la librería `react-leaflet`. Cada componente representa una capa o elemento visual del mapa.

```
components/mapa/
├── MapaInteractivo/
│   ├── MapaInteractivo.jsx        # Componente contenedor principal del mapa.
│   └── MapaInteractivo.module.css # Inicializa Leaflet, define el viewport y
│                                  # renderiza todas las capas hijas.
│
├── IconoAeropuerto/
│   ├── IconoAeropuerto.jsx        # Marcador personalizado en el mapa para cada
│   └── IconoAeropuerto.module.css # aeropuerto. El color del ícono refleja el estado
│                                  # del almacén (verde/ámbar/rojo según ocupación).
│
├── TrayectoriaRuta/
│   ├── TrayectoriaRuta.jsx        # Línea o polilínea dibujada sobre el mapa que
│   └── TrayectoriaRuta.module.css # representa la ruta asignada a un grupo de maletas,
│                                  # incluyendo escalas intermedias.
│
├── PanelDetalleAeropuerto/
│   ├── PanelDetalleAeropuerto.jsx        # Popup que aparece al hacer clic sobre un
│   └── PanelDetalleAeropuerto.module.css # aeropuerto en el mapa. Muestra: nombre,
│                                         # ocupación del almacén, vuelos activos y
│                                         # maletas en tránsito.
│
├── CapaEscenario/
│   ├── CapaEscenario.jsx          # Overlay visual que cambia según el escenario activo
│   └── CapaEscenario.module.css   # (día a día, periodo o colapso). Por ejemplo,
│                                  # en colapso puede mostrar alertas sobre el mapa.
│
└── LeyendaMapa/
    ├── LeyendaMapa.jsx            # Leyenda fija en el mapa que explica los colores
    └── LeyendaMapa.module.css     # del semáforo y los íconos usados.
```

---

### `components/simulacion/`

Componentes relacionados con el control y configuración de los **3 escenarios de simulación**.

```
components/simulacion/
├── SelectorEscenario/
│   ├── SelectorEscenario.jsx        # Permite elegir entre los 3 escenarios:
│   └── SelectorEscenario.module.css # "Día a día", "Simulación de periodo" y
│                                    # "Simulación hasta colapso".
│
├── ConfiguradorPeriodo/
│   ├── ConfiguradorPeriodo.jsx        # Formulario de parámetros para la simulación
│   └── ConfiguradorPeriodo.module.css # de periodo: duración (3, 5 o 7 días),
│                                      # algoritmo activo y carga inicial de maletas.
│
├── PanelControlSimulacion/
│   ├── PanelControlSimulacion.jsx        # Barra de control con botones: iniciar,
│   └── PanelControlSimulacion.module.css # pausar y detener la simulación.
│                                         # También muestra el tiempo simulado transcurrido.
│
└── IndicadorColapso/
    ├── IndicadorColapso.jsx        # Alerta visual que se activa cuando el sistema
    └── IndicadorColapso.module.css # detecta que las operaciones han colapsado
                                    # (envíos incumpliendo plazos sistemáticamente).
```

---

### `components/reportes/`

Componentes gráficos reutilizables dentro de las pantallas de reportes.

```
components/reportes/
├── GraficoBarras/
│   └── GraficoBarras.jsx              # Gráfico de barras para mostrar métricas
│                                      # como maletas entregadas vs. demoradas por día.
│
├── GraficoLinea/
│   └── GraficoLinea.jsx               # Gráfico de línea para evolución temporal
│                                      # de indicadores durante la simulación.
│
└── TablaComparativaAlgoritmos/
    └── TablaComparativaAlgoritmos.jsx  # Tabla comparativa del desempeño de los dos
                                        # algoritmos metaheurísticos: tiempo de ejecución,
                                        # calidad de solución y tasa de cumplimiento.
```

> Se recomienda usar **Recharts** para los gráficos (ligero, compatible con React).

---

### `pages/`

Cada carpeta representa una **pantalla principal** del sistema. El archivo `Page.jsx` es el punto de entrada de esa ruta. Puede importar componentes de `components/` y usar hooks y servicios.

```
pages/
│
├── Visualizador/
│   └── VisualizadorPage.jsx      # Pantalla principal del sistema.
│                                 # Muestra el MapaInteractivo con todos sus elementos:
│                                 # aeropuertos, rutas activas, estados del semáforo.
│                                 # Se actualiza en tiempo real vía WebSocket.
│                                 # Visible para: Operador, Supervisor, Planificador.
│
├── GestionMaletas/
│   ├── GestionMaletasPage.jsx    # Lista de todos los envíos registrados con filtros
│   │                             # por estado, aerolínea, origen y destino.
│   │
│   ├── FormularioRegistroMaleta.jsx  # Formulario para registrar un nuevo grupo de
│   │                                 # maletas: aerolínea, origen, destino, cantidad.
│   │                                 # Valida que no exceda capacidad del primer vuelo.
│   │
│   └── DetalleMaleta.jsx         # Vista del historial completo de un envío:
│                                 # aeropuertos visitados, tiempos, vuelos usados,
│                                 # estado actual y plan de viaje asignado.
│
├── GestionRutas/
│   ├── GestionRutasPage.jsx      # Lista de rutas activas e históricas.
│   │                             # Muestra estado de cada tramo (pendiente /
│   │                             # en tránsito / completado) y ocupación del vuelo.
│   │
│   └── DetalleRuta.jsx           # Vista de una ruta específica: tramos, vuelos
│                                 # asignados, tiempos estimados y estado general.
│                                 # Permite reasignar si hubo cancelación.
│
├── Simulacion/
│   └── SimulacionPage.jsx        # Pantalla para configurar y ejecutar los 3 escenarios.
│                                 # Contiene: SelectorEscenario, ConfiguradorPeriodo,
│                                 # PanelControlSimulacion e IndicadorColapso.
│                                 # Al ejecutar, redirige al Visualizador.
│
├── Reportes/
│   ├── ReportesPage.jsx          # Índice de reportes disponibles post-simulación.
│   │
│   ├── ReporteDesempeno.jsx      # Tasa de entregas a tiempo, vuelos cancelados
│   │                             # y maletas demoradas por escenario ejecutado.
│   │
│   ├── ReporteOcupacion.jsx      # Ocupación de vuelos y almacenes en aeropuertos,
│   │                             # con indicadores semáforo según rangos configurados.
│   │
│   └── ReporteAlgoritmos.jsx     # Comparativa técnica de los dos algoritmos:
│                                 # tiempo de ejecución, calidad de solución y
│                                 # métricas de cumplimiento por escenario.
│
└── Configuracion/
    ├── ConfiguracionPage.jsx     # Menú de configuración del sistema.
    │
    ├── ConfigAeropuertos.jsx     # CRUD de aeropuertos: nombre, ciudad, continente
    │                             # y capacidad de almacén (500–800 maletas).
    │
    ├── ConfigVuelos.jsx          # CRUD de vuelos: origen, destino, capacidad
    │                             # (150–400 maletas) y frecuencia diaria.
    │
    └── ConfigSemaforo.jsx        # Configuración de rangos numéricos para los colores
                                  # verde / ámbar / rojo usados en todo el sistema.
```

---

### `hooks/`

Custom hooks de React. Encapsulan lógica reutilizable que involucra estado o efectos.
**No deben renderizar nada** — solo retornar datos y funciones.

```
hooks/
├── useWebSocket.js     # Gestiona la conexión WebSocket con el backend.
│                       # Expone: { mensaje, conectado, desconectar }.
│                       # Usado por el Visualizador para recibir actualizaciones
│                       # del mapa sin recargar la página.
│
├── useSimulacion.js    # Controla el estado de la simulación activa:
│                       # escenario seleccionado, parámetros, si está corriendo.
│                       # Expone: { escenario, iniciar, pausar, detener, estado }.
│
└── useSemaforo.js      # Dado un valor numérico y los rangos configurados,
                        # retorna el color correspondiente ('verde'|'ambar'|'rojo').
                        # Usado por IconoAeropuerto, Semaforo, reportes, etc.
```

---

### `services/`

Funciones que se comunican con la API REST del backend Java.
Cada archivo agrupa los endpoints relacionados con un módulo del sistema.

```
services/
├── api.js                  # Instancia base de axios con la URL del backend
│                           # y headers comunes. Todos los demás services la importan.
│
├── maletasService.js       # registrarMaleta(), listarMaletas(), obtenerDetalle(),
│                           # actualizarUbicacion(), marcarEntregada()
│
├── rutasService.js         # obtenerRutas(), obtenerDetalleRuta(),
│                           # reasignarRuta(), listarRutasHistoricas()
│
├── simulacionService.js    # iniciarSimulacion(), detenerSimulacion(),
│                           # obtenerEstadoSimulacion(), obtenerResultados()
│
├── reportesService.js      # getReporteDesempeno(), getReporteOcupacion(),
│                           # getReporteAlgoritmos(), getReporteDemorados()
│
└── configuracionService.js # getAeropuertos(), guardarAeropuerto(),
                            # getVuelos(), guardarVuelo(),
                            # getRangosSemaforo(), guardarRangosSemaforo()
```

---

### `store/`

Estado global de la aplicación usando **Zustand**.
Cada archivo maneja un dominio específico del estado compartido entre páginas.

```
store/
├── simulacionStore.js      # Estado de la simulación:
│                           # - escenarioActivo (DIA_A_DIA | PERIODO | COLAPSO)
│                           # - estadoEjecucion (idle | corriendo | pausado | finalizado)
│                           # - parametros (duración, algoritmo seleccionado)
│                           # - colapsoDetectado (boolean)
│
├── maletasStore.js         # Cache de maletas activas y su última ubicación conocida.
│                           # Actualizado por WebSocket en tiempo real.
│
└── configuracionStore.js   # Parámetros globales del sistema:
                            # - rangosSemaforo { verde, ambar, rojo }
                            # - capacidades por defecto
                            # - algoritmoActivo (ALGORITMO_1 | ALGORITMO_2)
```

---

### `utils/`

Funciones puras de lógica auxiliar. No usan estado de React ni hacen llamadas al backend.

```
utils/
├── tiempos.js       # calcularPlazoMaximo(origen, destino): retorna 1 o 2 días
│                    # según si las ciudades son del mismo continente.
│                    # estaEnPlazo(fechaIngreso, fechaActual, plazo): boolean.
│
├── semaforo.js      # getColorSemaforo(valor, rangos): retorna 'verde'|'ambar'|'rojo'.
│                    # Contiene la lógica centralizada del semáforo para que
│                    # sea consistente en todos los componentes del sistema.
│
└── formatters.js    # formatearFecha(date): fecha legible en español (ej: "29 mar 2026")
                     # formatearEstado(estado): texto en español para los estados internos
                     # formatearCapacidad(actual, max): "320 / 500 maletas"
```

---

### `constants/`

Valores fijos del dominio del negocio. Se importan desde cualquier parte del proyecto.

```
constants/
├── escenarios.js       # export const ESCENARIOS = {
│                       #   DIA_A_DIA: 'DIA_A_DIA',
│                       #   PERIODO: 'PERIODO',
│                       #   COLAPSO: 'COLAPSO'
│                       # }
│
├── estados.js          # Estados posibles de una maleta:
│                       # EN_ESPERA, EN_TRANSITO, ENTREGADO, DEMORADO, REPLANIFICADO
│
└── restricciones.js    # Valores del enunciado que no cambian:
                        # PLAZO_MISMO_CONTINENTE = 1 (día)
                        # PLAZO_DISTINTO_CONTINENTE = 2 (días)
                        # CAPACIDAD_VUELO_MIN = 150
                        # CAPACIDAD_VUELO_MAX_CONTINENTAL = 250
                        # CAPACIDAD_VUELO_MAX_INTERCONTINENTAL = 400
                        # CAPACIDAD_ALMACEN_MIN = 500
                        # CAPACIDAD_ALMACEN_MAX = 800
```

---

## Convenciones del equipo

### Nombrado de archivos

| Tipo | Convención | Ejemplo |
|---|---|---|
| Componente React | PascalCase | `MapaInteractivo.jsx` |
| Hook | camelCase con prefijo `use` | `useSimulacion.js` |
| Service | camelCase con sufijo `Service` | `maletasService.js` |
| Store | camelCase con sufijo `Store` | `simulacionStore.js` |
| Utilidad | camelCase | `formatters.js` |
| Constante | camelCase | `restricciones.js` |
| Estilos | mismo nombre del componente + `.module.css` | `Semaforo.module.css` |

### Estructura interna de un componente

Cada componente vive en su propia carpeta con al menos dos archivos:

```
MiComponente/
├── MiComponente.jsx
└── MiComponente.module.css
```

### Reglas generales

- Un componente en `components/` **no debe** llamar directamente a un service — eso va en la `page` o en un `hook`.
- Los colores de semáforo **siempre** se calculan usando `utils/semaforo.js` — no hardcodear colores en los componentes.
- Los valores del enunciado (plazos, capacidades) **siempre** vienen de `constants/restricciones.js`.
- Todo texto visible en la interfaz debe estar en **español**.
- La zona horaria de referencia es **UTC**.

---

## Dependencias principales

| Librería | Uso |
|---|---|
| `react-leaflet` + `leaflet` | Mapa interactivo |
| `zustand` | Estado global |
| `axios` | Llamadas HTTP al backend Java |
| `recharts` | Gráficos en reportes |
| `react-router-dom` | Navegación entre páginas |
