# -*- coding: utf-8 -*-
"""
Genera un checklist en PDF del estado de avance de la solución dp1-app
contra los criterios de evaluación del curso.

Estados: COMPLETO, PARCIAL, FALTA.
Cada item incluye una nota corta justificando la evaluación
con base en el código fuente revisado.
"""

from reportlab.lib.pagesizes import A4
from reportlab.lib.units import cm
from reportlab.lib import colors
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak
)
from reportlab.lib.enums import TA_LEFT, TA_CENTER
from datetime import datetime

# Estado de cada criterio: (numero, texto, estado, nota)
# Estados: "C" = Completo, "P" = Parcial, "F" = Falta
ITEMS = [
    # ── Versión / metadata ────────────────────────────────────────────────────
    ("Versión: semana (avance) o versión (sw o prototipo)",
        "C", "Versión funcional ya desplegada en VM (avance al 95%)."),
    ("Fecha de subida o de la presentación",
        "C", "Registrada en el avance: 11/06/2026."),
    ("Comentarios o problemas reportados antes de la revisión",
        "P", "Existen comentarios internos del equipo; falta consolidarlos por escrito."),

    # ── Planificador ──────────────────────────────────────────────────────────
    ("Nombre del algoritmo planificador",
        "C", "Tabu Search (principal, en producción) + ACO (alternativo, en algoritmos-exp-num/ACO)."),
    ("Nivel de avance del planificador",
        "P", "Tabu Search al 95%; ajustes finos pendientes."),
    ("Estudiantes asignados al planificador",
        "C", "Equipo lo tiene asignado (último cambio: Alvaro)."),

    # ── Visualizador ──────────────────────────────────────────────────────────
    ("Nombre del algoritmo o tecnologías para el visualizador",
        "C", "Leaflet + canvas (CanvasVuelos) + WebSocket STOMP."),
    ("Nivel de avance del visualizador",
        "P", "Al 95%, ajustes menores pendientes."),
    ("Estudiantes asignados al visualizador",
        "C", "Todos los integrantes participan."),

    # ── Revisión video simulación ─────────────────────────────────────────────
    ("¿Revisaron el video de simulación consumiendo datos?",
        "F", "No se ha registrado revisión formal del video por integrante."),

    # ── Tiempos del algoritmo (Ta, Sa, Sc) ────────────────────────────────────
    ("Tiempo de ejecución del algoritmo: Ta",
        "P", "Tabu Search ~12 s/run (50k envíos), ACO ~50 s/run en colapso. TBD documentar oficial."),
    ("Tiempo del salto del algoritmo: Sa",
        "F", "No declarado explícitamente; depende de iteraciones (200) y tenencia (30)."),
    ("Salto (tiempo) del eje del consumo Sc",
        "F", "No documentado; el visualizador consume snapshots WS pero no se ha medido."),

    # ── Despliegue ────────────────────────────────────────────────────────────
    ("¿Está desplegado en VM el componente frontend?",
        "C", "Sí, desplegado en VM AWS."),
    ("¿Está desplegado en VM el componente backend?",
        "C", "Sí, desplegado en VM AWS junto con PostgreSQL."),

    # ── Diagramas ─────────────────────────────────────────────────────────────
    ("Diagrama de consumo de datos en bloques de tiempo (Sc por Sa)",
        "F", "No se cuenta con el diagrama."),
    ("Diagrama de interacción de varios navegadores durante simulación",
        "F", "No se cuenta con el diagrama (sí está implementado a nivel WebSocket multitenant)."),

    # ── Mapa: configuración inicial ───────────────────────────────────────────
    ("¿Cómo se configura el mapa en la pantalla principal?",
        "C", "Apartado de Configuración con selector de escenario y fecha desde el sidebar."),

    # ── Mantenimiento de atributos ────────────────────────────────────────────
    ("Establece/actualiza atributos de almacenes (mantenimiento)",
        "C", "Pantalla Configuración: capacidad y datos por aeropuerto; capacidad se actualiza en tiempo real."),
    ("Establece/actualiza atributos de unidades de transporte (UT)",
        "P", "Capacidad de vuelos definida; mantenimiento CRUD básico vía endpoints DB."),
    ("Establece/actualiza atributos de tramos (UT)",
        "P", "Vuelos definen tramos origen-destino-horarios; mantenimiento manual."),
    ("Carga/sube data histórica/futura (con o sin RDBMS)",
        "C", "Carga vía componente de subida en Configuración; PostgreSQL persiste datos."),

    # ── Datos de simulación ───────────────────────────────────────────────────
    ("¿Qué fecha(s)/hora usan para pruebas de simulación del periodo?",
        "C", "Fechas en marzo 2026 para pruebas rápidas, diciembre para estrés."),
    ("¿Está usando los datos sin reducción de registros?",
        "C", "Sí, dataset completo disponible (9.5M envíos cargados)."),
    ("¿La carga de datos es independiente de los escenarios?",
        "C", "Sí, los datos se cargan una vez en AWS y se reutilizan en cualquier escenario."),

    # ── Inicio simulación ─────────────────────────────────────────────────────
    ("Inicio: ¿La carga de archivos/datos de envíos está en otra opción?",
        "C", "Sí, en Configuración mediante componente de subida de archivos."),
    ("Inicio: ¿Pide fecha y hora hasta nivel de minuto?",
        "P", "Pide fecha (día, mes, año); no incluye hora ni minuto."),
    ("Inicio: ¿Tiempo desde botón hasta mostrar el mapa?",
        "C", "El mapa se muestra desde el inicio (instantáneo)."),
    ("Inicio: ¿Tiempo hasta que el transporte se mueva?",
        "C", "Aprox. 10 segundos para iniciar animación."),

    # ── Mapa: presentación y zoom ─────────────────────────────────────────────
    ("Mapa: ¿Se presenta todo en español?",
        "C", "Toda la UI en español, sin mezclar idiomas."),
    ("Mapa: ¿el zoom in/out es adecuado para ver zonas/detalles?",
        "C", "Leaflet con zoomControl, minZoom=2 (mundial) hasta detalle de ciudad."),
    ("Mapa al iniciar: ¿Presenta la pantalla principal completa?",
        "C", "Sí, MapaInteractivo ocupa el área principal."),
    ("Mapa: ¿Aprovecha la pantalla para mostrar todos los almacenes simultáneamente?",
        "C", "Sí, 29 aeropuertos visibles desde el zoom inicial."),
    ("Mapa al iniciar: ¿Presenta pantalla limpia (otros elementos no disponibles)?",
        "P", "Mapa se muestra; otros elementos disponibles pero sin animación hasta planificar."),

    # ── Mapa: tiempos (4 indicadores) ─────────────────────────────────────────
    ("Mapa: ¿Presenta fecha-hora del momento simulado (5D y colapso)?",
        "C", "SimulacionControles muestra Día N (dd/mm/yyyy) · HH:MM."),
    ("Mapa: ¿Presenta tiempo transcurrido del momento simulado?",
        "P", "Tiempo simulado mostrado; tiempo transcurrido desde inicio no diferenciado."),
    ("Mapa: ¿Presenta fecha-hora del momento actual (presente)?",
        "F", "No se muestra reloj de tiempo real del navegador en el mapa."),
    ("Mapa: ¿Presenta tiempo transcurrido hasta el momento actual?",
        "P", "El sidebar muestra cronómetro HH:MM:SS de ejecución."),
    ("Mapa: ¿Los 4 (o 2) datos de tiempos se presentan adecuadamente?",
        "P", "2 de 4 tiempos visibles (simulado + cronómetro); falta tiempo real."),

    # ── Mapa: ícono de almacén ────────────────────────────────────────────────
    ("Mapa: ¿Cada almacén está en la ubicación prevista?",
        "C", "CircleMarker con lat/lng reales de cada aeropuerto."),
    ("Mapa: ¿El ícono del almacén tiene tamaño idóneo?",
        "C", "radius=10, visible y proporcional al zoom."),
    ("Mapa: ¿El ícono del almacén representa un aeropuerto?",
        "P", "Usa CircleMarker (círculo) sin ícono específico de aeropuerto."),
    ("Mapa: ¿El ícono del almacén contrasta con el mapa?",
        "C", "Bordes blancos y colores semáforo claros."),
    ("Mapa: ¿El ícono presenta colores semáforo según stock?",
        "C", "Verde/ámbar/rojo según rangos configurables."),
    ("Mapa: ¿Presenta stock/ocupación del almacén (números o %)?",
        "C", "Popup muestra 'Almacén X/Y' y 'Ocupación %'."),

    # ── Mapa: ícono de UT (vuelos en canvas) ──────────────────────────────────
    ("Mapa: ¿Cada UT en la ubicación prevista?",
        "C", "CanvasVuelos interpola posición entre origen y destino."),
    ("Mapa: ¿Ícono de UT tiene tamaño idóneo?",
        "C", "Círculo de 4px, ajustable."),
    ("Mapa: ¿Ícono de UT representa un avión?",
        "F", "Se usa un círculo (no un ícono de avión)."),
    ("Mapa: ¿Ícono de UT contrasta con resto?",
        "C", "Borde blanco + color según ocupación."),
    ("Mapa: ¿Ícono de UT presenta colores semáforo según stock?",
        "C", "fillRatio determina color verde/ámbar/rojo."),
    ("Mapa: ¿Presenta stock/ocupación de cada UT (números o %)?",
        "F", "No se muestra cantidad/% por vuelo al pasar el mouse."),

    # ── Mapa: movimiento UT ───────────────────────────────────────────────────
    ("Mapa: ¿UT se desplaza con fluidez (sin saltos, sin anomalías)?",
        "C", "requestAnimationFrame con interpolación lineal entre posiciones."),
    ("Mapa: ¿UT se presenta coherentemente con el desplazamiento?",
        "P", "Círculo se mueve por curva pero sin alineación rotacional con el rumbo."),

    # ── Mapa: tramo (línea) ───────────────────────────────────────────────────
    ("Mapa: ¿Se presenta el tramo origen-destino como línea al inicio del vuelo?",
        "C", "Bezier cuadrática con cpY desplazado por hemisferio."),
    ("Mapa: ¿Nivel de grosor de la línea es adecuado?",
        "C", "lineWidth = clamp(1.2..3.5) según número de maletas."),
    ("Mapa: ¿Se borra/cambia la línea luego de recorrida la UT?",
        "P", "La línea se redibuja cada frame; no hay efecto de borrado tras llegada."),

    # ── Mapa: cancelaciones ───────────────────────────────────────────────────
    ("Mapa: ¿Cada cancelación se presenta en el mapa?",
        "F", "Replanificación se ejecuta pero la cancelación no tiene marcador visual en mapa."),
    ("Mapa: ¿La cancelación se ve durante el tiempo previsto?",
        "F", "Mismo motivo anterior."),

    # ── Panel: estructura ─────────────────────────────────────────────────────
    ("Panel: ¿Se presentan paneles contraídos en ubicaciones idóneas?",
        "C", "Sidebar colapsable + popup desplegable con detalle de maletas."),

    # ── Panel: lista UT ───────────────────────────────────────────────────────
    ("Panel: ¿Lista de UT con su ocupación en número o %?",
        "P", "Hay tabla de Vuelos en Reportes y GestionRutas; lista pura de UT pendiente."),
    ("Panel: ¿Desde lista UT, acceso a envíos que traslada?",
        "P", "En GestionRutas se cruza envío ↔ vuelos; no es navegación directa desde UT."),
    ("Panel: ¿Desde lista UT, acceso a productos (maletas) que traslada?",
        "P", "Cantidad de maletas por envío disponible; falta drill-down por producto."),
    ("Panel: ¿Se presenta stock de cada UT (semáforo-vacío)?",
        "P", "En tabla de Reportes hay barra de ocupación; en panel principal pendiente."),
    ("Panel: ¿Búsqueda de UT por código o tramo?",
        "P", "En GestionRutas hay buscador (origen/destino), no por código de UT."),
    ("Panel: ¿Búsqueda de UT en origen?",
        "C", "GestionRutas filtra por origen."),
    ("Panel: ¿Búsqueda de UT en destino?",
        "C", "GestionRutas filtra por destino."),
    ("Panel: ¿Filtro UT por código-patrón?",
        "F", "No implementado."),
    ("Panel: ¿Filtro UT en origen?",
        "C", "Disponible en GestionRutas."),
    ("Panel: ¿Filtro UT en destino?",
        "C", "Disponible en GestionRutas."),
    ("Panel: ¿Orden UT por nivel de ocupación?",
        "P", "Ordenamiento por riesgo disponible; por ocupación numérica pendiente."),
    ("Panel: ¿Orden UT por hora de salida?",
        "F", "No implementado."),
    ("Panel: ¿Orden UT por hora de llegada?",
        "F", "No implementado."),
    ("Panel: ¿Orden UT por origen?",
        "P", "Tabla soporta sort por columna; verificar configuración."),
    ("Panel: ¿Orden UT por destino?",
        "P", "Tabla soporta sort por columna; verificar configuración."),

    # ── Panel: lista almacenes ────────────────────────────────────────────────
    ("Panel: ¿Lista almacenes con ocupación en número o %?",
        "C", "Sidebar muestra tabla con barra de ocupación y semáforo."),
    ("Panel: ¿Acceso a envíos en almacén (tránsito y destino)?",
        "C", "Implementado en popup desplegable del mapa (commit reciente)."),
    ("Panel: ¿Acceso a productos en almacén?",
        "C", "Mismo popup incluye cantidad de maletas por envío."),
    ("Panel: ¿Lista almacenes en colores semáforo?",
        "C", "Componente Semaforo + getColorSemaforo."),
    ("Panel: ¿Información planificada de envíos que entran?",
        "F", "No se muestra detalle planificado pre-llegada por almacén."),
    ("Panel: ¿Información planificada de productos que entran?",
        "F", "Mismo motivo."),
    ("Panel: ¿Información planificada de envíos que salen?",
        "F", "No se muestra cola de salida."),
    ("Panel: ¿Información planificada de productos que salen?",
        "F", "Mismo motivo."),
    ("Panel: ¿Filtro de almacenes por código-patrón?",
        "F", "No implementado."),
    ("Panel: ¿Filtro de almacenes por continente?",
        "P", "Reportes agrupan por continente; filtro interactivo pendiente."),
    ("Panel: ¿Orden de almacenes por nivel de ocupación?",
        "P", "Ordenamiento manual disponible; no garantizado por % en sidebar."),
    ("Panel: ¿Orden de almacenes por hora próxima salida UT?",
        "F", "No implementado."),
    ("Panel: ¿Orden de almacenes por hora próxima llegada UT?",
        "F", "No implementado."),

    # ── Panel: envíos ─────────────────────────────────────────────────────────
    ("Panel: ¿Lista planificada de envíos con destino/UT/cantidad?",
        "C", "GestionRutas muestra todos los envíos con origen, destino y estado."),
    ("Panel: ¿Lista de envíos en vuelo con origen/destino/UT/cantidad?",
        "C", "Visible al filtrar por estado 'en_transito'."),
    ("Panel: ¿Lista de envíos entregados en últimas X horas (4h)?",
        "F", "No hay filtro temporal de 'últimas X horas'."),
    ("Panel: ¿Filtro envíos por origen (tramo o ruta)?",
        "C", "GestionRutas filtra por origen."),
    ("Panel: ¿Filtro envíos por destino (tramo o ruta)?",
        "C", "GestionRutas filtra por destino."),

    # ── Botones a demanda ─────────────────────────────────────────────────────
    ("Botón: ¿Mostrar ruta de maleta en mapa según ID a demanda?",
        "F", "Ruta visible en GestionRutas pero sin overlay sobre el mapa principal."),
    ("Botón: ¿Mostrar ruta anterior con datos de escalas?",
        "P", "DetalleRuta muestra escalas en panel; no se superpone al mapa."),
    ("Botón: ¿Mostrar rutas de envío en mapa según ID a demanda?",
        "F", "No implementado overlay en el mapa principal."),
    ("Botón: ¿Mostrar rutas anteriores con datos de escalas?",
        "P", "DetalleRuta muestra escalas; falta visualización geográfica."),

    # ── Vinculación mapa ↔ panel ──────────────────────────────────────────────
    ("Vinculación: ¿Seleccionar almacén en panel y enfocar en mapa?",
        "F", "No hay flujo panel→mapa con foco automático."),
    ("Vinculación: ¿Seleccionar almacén en mapa y reflejarlo en panel?",
        "F", "No hay flujo mapa→panel con enlace automático."),
    ("Vinculación: ¿Seleccionar UT en panel y enfocar en mapa?",
        "F", "No implementado."),
    ("Vinculación: ¿Seleccionar UT en mapa y reflejarlo en panel?",
        "F", "No implementado."),
    ("Vinculación: ¿Seleccionar envío en panel y enfocar en mapa?",
        "F", "No implementado overlay de ruta."),
    ("Vinculación: ¿Filtrar por semáforo de almacenes y reflejar en mapa?",
        "F", "No implementado filtro cross-componente."),
    ("Vinculación: ¿Filtrar por semáforo de UT y reflejar en mapa?",
        "F", "No implementado."),
    ("Vinculación: ¿Otros filtros de almacenes reflejados en mapa?",
        "F", "No implementado."),
    ("Vinculación: ¿Otros filtros de UT reflejados en mapa?",
        "F", "No implementado."),

    # ── Indicadores globales ──────────────────────────────────────────────────
    ("Mapa global: ¿Indicador de llenado de la flota en conjunto?",
        "P", "KPI 'maletas en tránsito' + 'vuelos saturados' disponibles en sidebar."),
    ("Mapa global: ¿Indicador de flota como semáforo?",
        "F", "Se muestra como número, no como semáforo agregado."),
    ("Mapa global: ¿Indicador de llenado de almacenes en conjunto?",
        "P", "KPI 'aeropuertos saturados' disponible."),
    ("Mapa global: ¿Indicador de almacenes como semáforo?",
        "F", "Se muestra como número, no como semáforo agregado."),

    # ── Reglas de negocio ─────────────────────────────────────────────────────
    ("Mapa: ¿Se respeta tiempo de permanencia mínima de maletas en aeropuerto?",
        "P", "El algoritmo respeta tiempos de espera de vuelo; restricción mínima no validada."),

    # ── Multi-navegador ───────────────────────────────────────────────────────
    ("Navegador: ¿2+ visualizadores con interacción independiente para 1 planificador?",
        "C", "WebSocket broadcast permite que múltiples clientes reciban snapshots simultáneamente."),

    # ── Escenarios y reportes finales ─────────────────────────────────────────
    ("Escenarios: ¿Reporte de última planificación estable al finalizar simulación periodo?",
        "C", "ReportesPage muestra métricas finales tras completar la planificación."),
    ("Escenarios: ¿Reporte de última planificación estable al cerrar día a día?",
        "P", "Reportes funcionan tras completar; cierre manual del escenario DIA_A_DIA pendiente de probar."),
    ("Escenarios: ¿Reporte de última planificación estable al finalizar colapso?",
        "P", "Modal AlertaColapso disponible; reporte se accede vía menú Reportes."),

    # ── Percepción global ─────────────────────────────────────────────────────
    ("Percepción: ¿Completo? Está al día.",
        "P", "Avance al 95%; pendientes ajustes finos."),
    ("Percepción: ¿Apropiado?",
        "C", "Cumple con el alcance y stack solicitados (Java + React)."),
    ("Percepción: ¿Está claro? No ambiguo.",
        "C", "Documentación en CLAUDE.md y código modular."),
    ("Percepción: ¿Factible? Lograble en el plazo.",
        "C", "Demostrablemente funcional, en VM."),
]


# ── Estilos PDF ──────────────────────────────────────────────────────────────
COLOR_C = colors.HexColor('#16a34a')   # verde
COLOR_P = colors.HexColor('#d97706')   # ámbar
COLOR_F = colors.HexColor('#dc2626')   # rojo

ETIQUETAS_ESTADO = {
    "C": ("COMPLETO", COLOR_C),
    "P": ("PARCIAL",  COLOR_P),
    "F": ("FALTA",    COLOR_F),
}


def build_pdf(path):
    doc = SimpleDocTemplate(
        path, pagesize=A4,
        leftMargin=1.5*cm, rightMargin=1.5*cm,
        topMargin=1.5*cm, bottomMargin=1.5*cm,
        title="Checklist de avance — dp1-app"
    )
    styles = getSampleStyleSheet()
    titulo = ParagraphStyle(
        "Titulo", parent=styles["Heading1"], fontSize=18, alignment=TA_LEFT,
        textColor=colors.HexColor('#0f172a'), spaceAfter=6
    )
    subt = ParagraphStyle(
        "Subt", parent=styles["Normal"], fontSize=10,
        textColor=colors.HexColor('#475569'), spaceAfter=14
    )
    h2 = ParagraphStyle(
        "H2", parent=styles["Heading2"], fontSize=13,
        textColor=colors.HexColor('#0f172a'), spaceBefore=10, spaceAfter=6
    )
    body = ParagraphStyle(
        "Body", parent=styles["Normal"], fontSize=9, leading=11
    )
    nota = ParagraphStyle(
        "Nota", parent=styles["Normal"], fontSize=8, leading=10,
        textColor=colors.HexColor('#334155')
    )

    story = []

    # ── Portada ───────────────────────────────────────────────────────────────
    story.append(Paragraph("Checklist de avance del software", titulo))
    story.append(Paragraph(
        f"Proyecto dp1-app · Tasf.B2B · {datetime.now().strftime('%d/%m/%Y')}",
        subt
    ))

    # Resumen
    total = len(ITEMS)
    n_c = sum(1 for _, e, _ in ITEMS if e == "C")
    n_p = sum(1 for _, e, _ in ITEMS if e == "P")
    n_f = sum(1 for _, e, _ in ITEMS if e == "F")

    resumen = Table(
        [
            ["Total criterios", "Completos", "Parciales", "Faltan"],
            [str(total), f"{n_c}  ({n_c*100//total}%)",
             f"{n_p}  ({n_p*100//total}%)",
             f"{n_f}  ({n_f*100//total}%)"]
        ],
        colWidths=[4.0*cm, 4.0*cm, 4.0*cm, 4.0*cm],
    )
    resumen.setStyle(TableStyle([
        ('FONT', (0, 0), (-1, 0), 'Helvetica-Bold', 9),
        ('FONT', (0, 1), (-1, 1), 'Helvetica-Bold', 13),
        ('TEXTCOLOR', (0, 0), (-1, 0), colors.HexColor('#475569')),
        ('TEXTCOLOR', (1, 1), (1, 1), COLOR_C),
        ('TEXTCOLOR', (2, 1), (2, 1), COLOR_P),
        ('TEXTCOLOR', (3, 1), (3, 1), COLOR_F),
        ('ALIGN', (0, 0), (-1, -1), 'CENTER'),
        ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#f1f5f9')),
        ('BOX', (0, 0), (-1, -1), 0.5, colors.HexColor('#cbd5e1')),
        ('INNERGRID', (0, 0), (-1, -1), 0.3, colors.HexColor('#e2e8f0')),
        ('TOPPADDING', (0, 0), (-1, -1), 6),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 6),
    ]))
    story.append(resumen)
    story.append(Spacer(1, 0.4*cm))

    leyenda = Paragraph(
        "<b>Leyenda:</b> "
        "<font color='#16a34a'><b>COMPLETO</b></font> = implementado y verificado. "
        "<font color='#d97706'><b>PARCIAL</b></font> = implementación incompleta o falta verificación. "
        "<font color='#dc2626'><b>FALTA</b></font> = no implementado.",
        body
    )
    story.append(leyenda)
    story.append(Spacer(1, 0.4*cm))

    # ── Tabla principal ───────────────────────────────────────────────────────
    story.append(Paragraph("Detalle por criterio", h2))

    data = [["#", "Criterio", "Estado", "Nota / Evidencia"]]
    style_rows = []
    for i, (texto, estado, nota_t) in enumerate(ITEMS, start=1):
        etiqueta, color_bg = ETIQUETAS_ESTADO[estado]
        # Paragraph para permitir wrap en celdas anchas
        data.append([
            Paragraph(str(i), body),
            Paragraph(texto, body),
            Paragraph(f"<b>{etiqueta}</b>", body),
            Paragraph(nota_t, nota),
        ])
        style_rows.append((i, color_bg))

    tabla = Table(
        data,
        colWidths=[0.8*cm, 8.5*cm, 2.0*cm, 6.7*cm],
        repeatRows=1,
    )

    ts = TableStyle([
        # Header
        ('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#0f172a')),
        ('TEXTCOLOR',  (0, 0), (-1, 0), colors.white),
        ('FONT', (0, 0), (-1, 0), 'Helvetica-Bold', 9),
        ('ALIGN', (0, 0), (-1, 0), 'CENTER'),
        # Cuerpo
        ('FONT', (0, 1), (-1, -1), 'Helvetica', 8),
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
        ('TOPPADDING', (0, 0), (-1, -1), 4),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 4),
        ('LEFTPADDING', (0, 0), (-1, -1), 4),
        ('RIGHTPADDING', (0, 0), (-1, -1), 4),
        ('INNERGRID', (0, 0), (-1, -1), 0.25, colors.HexColor('#e2e8f0')),
        ('BOX', (0, 0), (-1, -1), 0.5, colors.HexColor('#cbd5e1')),
    ])
    # Pintar columna de estado del color correspondiente
    for row_idx, color in style_rows:
        ts.add('BACKGROUND', (2, row_idx), (2, row_idx), color)
        ts.add('TEXTCOLOR',  (2, row_idx), (2, row_idx), colors.white)
        ts.add('ALIGN',      (2, row_idx), (2, row_idx), 'CENTER')

    # Filas alternadas
    for row_idx in range(1, len(data)):
        if row_idx % 2 == 0:
            ts.add('BACKGROUND', (0, row_idx), (1, row_idx), colors.HexColor('#f8fafc'))
            ts.add('BACKGROUND', (3, row_idx), (3, row_idx), colors.HexColor('#f8fafc'))

    tabla.setStyle(ts)
    story.append(tabla)

    doc.build(story)


if __name__ == "__main__":
    output = "checklist_avance_dp1.pdf"
    build_pdf(output)
    print(f"PDF generado: {output}")
