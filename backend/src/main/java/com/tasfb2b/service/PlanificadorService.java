package com.tasfb2b.service;

import com.tasfb2b.algorithm.GrafoVuelos;
import com.tasfb2b.algorithm.Solucion;
import com.tasfb2b.algorithm.SolucionInicial;
import com.tasfb2b.algorithm.TabuSearch;
import com.tasfb2b.data.DataLoader;
import com.tasfb2b.dto.*;
import com.tasfb2b.model.Aeropuerto;
import com.tasfb2b.model.Envio;
import com.tasfb2b.model.Ruta;
import com.tasfb2b.model.Vuelo;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class PlanificadorService {

    public enum Estado { IDLE, CARGANDO, PLANIFICANDO, COMPLETADO, ERROR }

    // ── Rutas de datos ────────────────────────────────────────────────────────
    @Value("${tasf.datos.aeropuertos:datos/aeropuertos.txt}")
    private String rutaAeropuertos;

    @Value("${tasf.datos.vuelos:datos/planes_vuelo.txt}")
    private String rutaVuelos;

    @Value("${tasf.datos.envios:datos/envios}")
    private String directorioEnvios;

    // ── Parámetros del algoritmo ──────────────────────────────────────────────
    @Value("${tasf.algoritmo.iteraciones:200}")
    private int iteraciones;

    @Value("${tasf.algoritmo.tenencia:30}")
    private int tenencia;

    @Value("${tasf.algoritmo.muestra:200}")
    private int muestra;

    // ── Umbrales del semáforo global ──────────────────────────────────────────
    @Value("${tasf.semaforo.umbral-verde:90.0}")
    private double umbralVerde;

    @Value("${tasf.semaforo.umbral-ambar:70.0}")
    private double umbralAmbar;

    // ── Estado en ejecución ───────────────────────────────────────────────────
    private final AtomicReference<Estado> estado = new AtomicReference<>(Estado.IDLE);
    private volatile String mensajeEstado = "Sin planificación activa";
    private volatile int progreso = 0;
    private volatile String escenarioActual = null;

    // Día 0 del algoritmo (igual que Envio.FECHA_INICIO_SIMULACION)
    private static final LocalDate DIA_CERO_ALGORITMO = LocalDate.of(2026, 1, 1);
    // Primer día con datos reales
    private static final LocalDate PRIMER_DIA_DATOS   = LocalDate.of(2026, 1, 2);

    // ── Datos cargados ────────────────────────────────────────────────────────
    private volatile Map<String, Aeropuerto> aeropuertosCargados = null;
    private volatile List<Vuelo> vuelosCargados = null;
    private volatile Solucion solucionActual = null;
    private volatile LocalDate fechaInicioSimulacion = PRIMER_DIA_DATOS;

    private final WebSocketEventPublisher wsPublisher;

    public PlanificadorService(WebSocketEventPublisher wsPublisher) {
        this.wsPublisher = wsPublisher;
    }

    /** Carga aeropuertos y vuelos al arrancar el servidor para que el mapa los muestre de inmediato. */
    @PostConstruct
    public void cargarDatosIniciales() {
        try {
            DataLoader loader = new DataLoader(rutaAeropuertos, rutaVuelos, directorioEnvios);
            aeropuertosCargados = loader.cargarAeropuertos();
            vuelosCargados      = loader.cargarVuelos();
        } catch (Exception ex) {
            System.err.println("[PlanificadorService] No se pudieron cargar datos iniciales: " + ex.getMessage());
        }
    }

    // =========================================================================
    // Consultas de estado (para REST polling)
    // =========================================================================

    public EstadoDTO getEstado() {
        return new EstadoDTO(estado.get().name(), progreso, mensajeEstado, escenarioActual);
    }

    public MetricasDTO getMetricas() {
        if (solucionActual == null) return null;
        double pct = solucionActual.getPorcentajeCumplimientoPlazo();
        return new MetricasDTO(
            solucionActual.getTotalEnvios(),
            solucionActual.contarEnviosConRuta(),
            solucionActual.contarEnviosSinRuta(),
            solucionActual.contarViolacionesPlazo(),
            Math.round(pct * 100.0) / 100.0,
            solucionActual.contarVuelosSaturados(),
            solucionActual.contarAeropuertosSaturados(),
            solucionActual.contarDiasAeropuertoSaturados(),
            Math.round(solucionActual.getTiempoPromedioEntrega() * 10.0) / 10.0,
            Math.round(solucionActual.getEscalasPromedio() * 100.0) / 100.0,
            solucionActual.getCostoTotal(),
            calcularSemaforoGlobal(pct)
        );
    }

    public List<AeropuertoDTO> getAeropuertos() {
        if (aeropuertosCargados == null) return Collections.emptyList();
        return aeropuertosCargados.values().stream()
            .map(a -> new AeropuertoDTO(
                a.getCodigo(), a.getCiudad(), a.getPais(), a.getContinente(),
                a.getGmt(), a.getCapacidadMax(), a.getOcupacionActual(),
                a.getLat(), a.getLng()))
            .sorted(Comparator.comparing(AeropuertoDTO::getContinente)
                .thenComparing(AeropuertoDTO::getCodigo))
            .collect(Collectors.toList());
    }

    public List<VueloDTO> getVuelos() {
        if (vuelosCargados == null) return Collections.emptyList();
        return vuelosCargados.stream()
            .map(v -> new VueloDTO(
                v.getOrigen(), v.getDestino(),
                formatearMinutos(v.getSalidaMinutos()),
                formatearMinutos(v.getLlegadaMinutos()),
                v.getCapacidadMax(), v.getOcupacion()))
            .collect(Collectors.toList());
    }

    // =========================================================================
    // Iniciar planificación
    // =========================================================================

    public synchronized void iniciar(String escenario, String fechaInicioStr, int numDias) {
        Estado est = estado.get();
        if (est == Estado.CARGANDO || est == Estado.PLANIFICANDO) {
            throw new IllegalStateException("Hay una planificación en curso");
        }
        escenarioActual = escenario;
        solucionActual = null;
        LocalDate fechaInicio = parsearFecha(fechaInicioStr);
        Thread t = new Thread(() -> ejecutar(escenario, fechaInicio, numDias), "planificador");
        t.setDaemon(true);
        t.start();
    }

    // =========================================================================
    // Lógica de planificación (hilo de fondo)
    // =========================================================================

    private void ejecutar(String escenario, LocalDate fechaInicio, int numDias) {
        try {
            fechaInicioSimulacion = fechaInicio != null ? fechaInicio : PRIMER_DIA_DATOS;
            setEstado(Estado.CARGANDO, 5, "Cargando aeropuertos y vuelos...", 0);

            DataLoader loader = new DataLoader(rutaAeropuertos, rutaVuelos, directorioEnvios);
            Map<String, Aeropuerto> aeropuertos = loader.cargarAeropuertos();
            List<Vuelo> vuelos = loader.cargarVuelos();
            aeropuertosCargados = aeropuertos;
            vuelosCargados = vuelos;

            setEstado(Estado.CARGANDO, 15, "Cargando envíos (" + escenario + ")...", 0);

            List<Envio> envios = cargarEnviosSegunEscenario(loader, escenario, fechaInicio, numDias);

            for (Envio e : envios) {
                Aeropuerto orig = aeropuertos.get(e.getOrigen());
                Aeropuerto dest = aeropuertos.get(e.getDestino());
                if (orig != null && dest != null) {
                    boolean mismoC = orig.getContinente().equals(dest.getContinente());
                    e.setPlazoMaximoMinutos(mismoC ? 1440 : 2880);
                }
            }

            setEstado(Estado.PLANIFICANDO, 25, "Construyendo grafo y solución inicial...", 0);

            Map<String, Integer> capAeropuertos = new HashMap<>();
            aeropuertos.forEach((k, v) -> capAeropuertos.put(k, v.getCapacidadMax()));

            GrafoVuelos grafo = new GrafoVuelos(vuelos);
            SolucionInicial si = new SolucionInicial(grafo, capAeropuertos);
            Solucion inicial = si.construir(envios);

            // Primer snapshot: solución greedy antes de optimizar
            publicarSnapshot(inicial, aeropuertos, 0);
            setEstado(Estado.PLANIFICANDO, 40, "Ejecutando Tabu Search (" + iteraciones + " iteraciones)...",
                inicial.getCostoTotal());

            TabuSearch ts = new TabuSearch(grafo, iteraciones, tenencia, muestra);

            Solucion mejor = ts.ejecutar(inicial, envios, (iter, mejorGlobal) -> {
                int pct = 40 + (int)(iter * 55.0 / iteraciones);
                String msg = "Iteración " + iter + "/" + iteraciones
                    + " | costo: " + String.format("%,.0f", mejorGlobal.getCostoTotal());
                setEstado(Estado.PLANIFICANDO, pct, msg, mejorGlobal.getCostoTotal());
                publicarSnapshot(mejorGlobal, aeropuertos, iter);
            });

            solucionActual = mejor;
            setEstado(Estado.COMPLETADO, 100, "Planificación completada", mejor.getCostoTotal());

            // Snapshot y métricas finales
            publicarSnapshot(mejor, aeropuertos, iteraciones);
            wsPublisher.publicarCompletado(getMetricas());

        } catch (Exception ex) {
            setEstado(Estado.ERROR, progreso, "Error: " + ex.getMessage(), 0);
            ex.printStackTrace();
        }
    }

    private List<Envio> cargarEnviosSegunEscenario(DataLoader loader,
                                                    String escenario,
                                                    LocalDate fechaInicio,
                                                    int numDias) throws Exception {
        switch (escenario.toUpperCase()) {
            case "DIA_A_DIA": {
                LocalDate fecha = fechaInicio != null ? fechaInicio : PRIMER_DIA_DATOS;
                long totalDias = ChronoUnit.DAYS.between(PRIMER_DIA_DATOS, fecha) + 1;
                return loader.cargarEnviosPorPeriodo(PRIMER_DIA_DATOS, (int) totalDias);
            }
            case "PERIODO": {
                LocalDate fecha = fechaInicio != null ? fechaInicio : PRIMER_DIA_DATOS;
                int dias = numDias > 0 ? numDias : 5;
                long totalDias = ChronoUnit.DAYS.between(PRIMER_DIA_DATOS, fecha) + dias;
                return loader.cargarEnviosPorPeriodo(PRIMER_DIA_DATOS, (int) totalDias);
            }
            case "COLAPSO":
            default:
                return loader.cargarEnvios(-1);
        }
    }

    // =========================================================================
    // Construcción de snapshot WebSocket
    // =========================================================================

    private void publicarSnapshot(Solucion sol, Map<String, Aeropuerto> aeropuertos, int iter) {
        Map<String, Integer> ocupMax = sol.getOcupacionMaximaPorAeropuerto();
        Map<String, Integer> maletasPorRuta = sol.getMaletasPorRuta();

        List<SnapshotEventDTO.AeropuertoItem> itemsAeropuerto = aeropuertos.values().stream()
            .map(a -> {
                int ocup = ocupMax.getOrDefault(a.getCodigo(), 0);
                double pct = a.getCapacidadMax() > 0
                    ? Math.round(ocup * 1000.0 / a.getCapacidadMax()) / 10.0
                    : 0;
                return new SnapshotEventDTO.AeropuertoItem(
                    a.getCodigo(), a.getCiudad(), a.getContinente(),
                    ocup, a.getCapacidadMax(), pct,
                    calcularSemaforoAeropuerto(pct)
                );
            })
            .collect(Collectors.toList());

        List<SnapshotEventDTO.RutaItem> itemsRuta = maletasPorRuta.entrySet().stream()
            .filter(e -> e.getValue() > 0)
            .map(e -> {
                String[] parts = e.getKey().split("-");
                return new SnapshotEventDTO.RutaItem(parts[0], parts[1], e.getValue());
            })
            .collect(Collectors.toList());

        wsPublisher.publicarSnapshot(
            new SnapshotEventDTO(iter, sol.getCostoTotal(), itemsAeropuerto, itemsRuta)
        );
    }

    // =========================================================================
    // Utilidades
    // =========================================================================

    private void setEstado(Estado e, int p, String msg, double costo) {
        estado.set(e);
        progreso = p;
        mensajeEstado = msg;
        wsPublisher.publicarProgreso(p, msg, e.name(), costo);
    }

    private String calcularSemaforoGlobal(double pctCumplimiento) {
        if (pctCumplimiento >= umbralVerde) return "VERDE";
        if (pctCumplimiento >= umbralAmbar) return "AMBAR";
        return "ROJO";
    }

    private static String calcularSemaforoAeropuerto(double pctOcupacion) {
        if (pctOcupacion < 70.0) return "VERDE";
        if (pctOcupacion < 90.0) return "AMBAR";
        return "ROJO";
    }

    private static LocalDate parsearFecha(String fechaStr) {
        if (fechaStr == null || fechaStr.isBlank()) return null;
        return LocalDate.parse(fechaStr, DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private static String formatearMinutos(int minutos) {
        return String.format("%02d:%02d", minutos / 60, minutos % 60);
    }

    // =========================================================================
    // =========================================================================
    // Manifest de animación
    // =========================================================================

    public AnimacionManifestDTO getAnimacionManifest() {
        if (solucionActual == null || vuelosCargados == null || aeropuertosCargados == null) {
            return null;
        }

        Map<String, Vuelo> vuelos = new HashMap<>();
        for (Vuelo v : vuelosCargados) {
            vuelos.put(v.getClave(), v);
        }

        List<OcurrenciaVueloDTO> ocurrencias = new ArrayList<>();
        int maxLlegada = 0;

        for (Map.Entry<String, Integer> e : solucionActual.getOcupacionVuelos().entrySet()) {
            if (e.getValue() <= 0) continue;
            String[] k = e.getKey().split("-");
            if (k.length < 4) continue;

            String origen = k[0];
            String destino = k[1];
            int salidaMinutos;
            int dia;
            try {
                salidaMinutos = Integer.parseInt(k[2]);
                dia = Integer.parseInt(k[3].substring(1));
            } catch (NumberFormatException ex) {
                continue;
            }

            String clave = origen + "-" + destino + "-" + salidaMinutos;
            Vuelo vuelo = vuelos.get(clave);
            if (vuelo == null) continue;

            int salidaAbs  = dia * 1440 + salidaMinutos;
            int llegadaAbs = salidaAbs + vuelo.getDuracionMinutos();

            ocurrencias.add(new OcurrenciaVueloDTO(
                origen, destino, salidaAbs, llegadaAbs, e.getValue(), vuelo.getCapacidadMax()));
            maxLlegada = Math.max(maxLlegada, llegadaAbs);
        }

        ocurrencias.sort(Comparator.comparingInt(OcurrenciaVueloDTO::getSalidaAbs));

        Map<String, Map<Integer, Integer>> ocupDiaria = solucionActual.getOcupacionDiariaAeropuerto();
        List<AeropuertoManifestDTO> aeropuertos = aeropuertosCargados.values().stream()
            .map(a -> new AeropuertoManifestDTO(
                a.getCodigo(), a.getCiudad(), a.getPais(), a.getContinente(),
                a.getLat(), a.getLng(), a.getCapacidadMax(),
                ocupDiaria.getOrDefault(a.getCodigo(), Collections.emptyMap())))
            .collect(Collectors.toList());

        int fechaInicioMin = (int)(ChronoUnit.DAYS.between(DIA_CERO_ALGORITMO, fechaInicioSimulacion) * 1440);
        return new AnimacionManifestDTO(maxLlegada, fechaInicioMin, ocurrencias, aeropuertos);
    }

    // =========================================================================
    // RAN-02: Rutas y replanificación
    // =========================================================================

    public List<RutaResumenDTO> getRutasResumen(int limite) {
        if (solucionActual == null) return Collections.emptyList();
        return solucionActual.getRutas().stream()
            .sorted(Comparator.comparing(r -> !esRiesgosa(r)))
            .limit(limite)
            .map(this::toResumen)
            .collect(Collectors.toList());
    }

    public RutaDetalleDTO getRutaDetalle(String envioId) {
        if (solucionActual == null) return null;
        Ruta ruta = solucionActual.getRutas().stream()
            .filter(r -> r.getEnvio().getId().equals(envioId))
            .findFirst().orElse(null);
        if (ruta == null) return null;
        return toDetalle(ruta);
    }

    public synchronized ReplanificacionResultDTO replanificarPorVueloCancelado(
            String origen, String destino, int horaSalidaMinutos) {

        if (solucionActual == null) {
            throw new IllegalStateException("No hay solución activa. Ejecute una planificación primero.");
        }
        if (vuelosCargados == null || aeropuertosCargados == null) {
            throw new IllegalStateException("Datos de vuelos o aeropuertos no cargados.");
        }

        List<Envio> afectados = solucionActual.getRutas().stream()
            .filter(r -> !r.isSinSolucion())
            .filter(r -> r.getVuelos().stream().anyMatch(v ->
                v.getOrigen().equals(origen) &&
                v.getDestino().equals(destino) &&
                v.getSalidaMinutos() == horaSalidaMinutos))
            .map(Ruta::getEnvio)
            .collect(Collectors.toList());

        if (afectados.isEmpty()) {
            return new ReplanificacionResultDTO(0, 0, 0,
                "El vuelo " + origen + "→" + destino + " (" + formatearMinutos(horaSalidaMinutos) +
                ") no está siendo utilizado por ningún envío en la solución actual.");
        }

        List<Vuelo> vuelosActivos = vuelosCargados.stream()
            .filter(v -> !(v.getOrigen().equals(origen) &&
                           v.getDestino().equals(destino) &&
                           v.getSalidaMinutos() == horaSalidaMinutos))
            .collect(Collectors.toList());

        GrafoVuelos grafoReducido = new GrafoVuelos(vuelosActivos);
        Map<String, Integer> capAeropuertos = new HashMap<>();
        aeropuertosCargados.forEach((k, a) -> capAeropuertos.put(k, a.getCapacidadMax()));
        SolucionInicial si = new SolucionInicial(grafoReducido, capAeropuertos);

        int reasignados = 0, sinRuta = 0;
        for (Envio envio : afectados) {
            Ruta nueva = si.construirRuta(envio);
            solucionActual.agregarRuta(nueva);
            if (nueva.isSinSolucion()) sinRuta++; else reasignados++;
        }

        publicarSnapshot(solucionActual, aeropuertosCargados, 0);

        String msg = String.format("Replanificación completada: %d envío(s) afectados, %d reasignados, %d sin ruta.",
            afectados.size(), reasignados, sinRuta);
        wsPublisher.publicarProgreso(progreso, msg, estado.get().name(), solucionActual.getCostoTotal());

        return new ReplanificacionResultDTO(afectados.size(), reasignados, sinRuta, msg);
    }

    // ─── Conversión Ruta → DTO ────────────────────────────────────────────────

    private boolean esRiesgosa(Ruta r) {
        if (r.isSinSolucion()) return true;
        int t = r.calcularTiempoTotal();
        int p = r.getEnvio().getPlazoMaximoMinutos();
        return p > 0 && t != Integer.MAX_VALUE && t > p;
    }

    private String cumplimientoDeRuta(Ruta r) {
        if (r.isSinSolucion()) return "rojo";
        int t = r.calcularTiempoTotal();
        if (t == Integer.MAX_VALUE) return "rojo";
        int p = r.getEnvio().getPlazoMaximoMinutos();
        if (p <= 0 || t <= p) return "verde";
        if (t <= p + 240) return "ambar";
        return "rojo";
    }

    private RutaResumenDTO toResumen(Ruta r) {
        Envio e = r.getEnvio();
        String estado = r.isSinSolucion() ? "sin_ruta" : "en_transito";
        String tiempo = r.isSinSolucion() ? "—" : formatearTiempoDuracion(r.calcularTiempoTotal());
        DateTimeFormatter dtfR = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String fechaIngresoR = e.getFechaHoraRegistro().format(dtfR) + " UTC";
        String fechaLimiteR  = e.getFechaHoraRegistro().plusMinutes(e.getPlazoMaximoMinutos()).format(dtfR) + " UTC";
        return new RutaResumenDTO(
            e.getId(), e.getOrigen(), e.getDestino(),
            getCiudad(e.getOrigen()), getCiudad(e.getDestino()),
            estado, cumplimientoDeRuta(r), tiempo, fechaIngresoR, fechaLimiteR
        );
    }

    private RutaDetalleDTO toDetalle(Ruta r) {
        Envio e = r.getEnvio();
        int plazoMin = e.getPlazoMaximoMinutos();
        String plazoStr = plazoMin <= 1440
            ? "24 h (mismo continente)" : "48 h (distinto continente)";
        DateTimeFormatter dtfFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String fechaIngreso = e.getFechaHoraRegistro().format(dtfFecha) + " UTC";
        String fechaLimite  = e.getFechaHoraRegistro().plusMinutes(plazoMin).format(dtfFecha) + " UTC";

        List<RutaDetalleDTO.TramoDTO> tramos = new ArrayList<>();
        if (!r.isSinSolucion()) {
            int tActual = e.getMinutosRegistro();
            int idx = 0;
            LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
            for (Vuelo v : r.getVuelos()) {
                int salidaAbs = GrafoVuelos.proximaSalidaAbsoluta(tActual, v.getSalidaMinutos(), 30);
                int llegadaAbs = salidaAbs + v.getDuracionMinutos();
                tramos.add(new RutaDetalleDTO.TramoDTO(
                    "t" + (++idx),
                    v.getOrigen() + "→" + v.getDestino() + " (" + formatearMinutos(v.getSalidaMinutos()) + ")",
                    v.getOrigen(), v.getDestino(),
                    v.getSalidaMinutos(),
                    0, v.getCapacidadMax(),
                    base.plusMinutes(salidaAbs).format(dtfFecha) + " UTC",
                    base.plusMinutes(llegadaAbs).format(dtfFecha) + " UTC",
                    "pendiente"
                ));
                tActual = llegadaAbs;
            }
        }

        return new RutaDetalleDTO(
            e.getId(), e.getOrigen(), e.getDestino(),
            getCiudad(e.getOrigen()), getCiudad(e.getDestino()),
            r.isSinSolucion() ? "sin_ruta" : "en_transito",
            cumplimientoDeRuta(r),
            r.isSinSolucion() ? "—" : formatearTiempoDuracion(r.calcularTiempoTotal()),
            0, plazoStr, fechaIngreso, fechaLimite, tramos
        );
    }

    private String getCiudad(String codigo) {
        if (aeropuertosCargados == null) return codigo;
        Aeropuerto a = aeropuertosCargados.get(codigo);
        return a != null ? a.getCiudad() : codigo;
    }

    private static String formatearTiempoDuracion(int minutos) {
        if (minutos == Integer.MAX_VALUE || minutos <= 0) return "—";
        int h = minutos / 60;
        int m = minutos % 60;
        if (h == 0) return m + " min";
        return m == 0 ? h + " h" : h + " h " + m + " min";
    }
}
