package com.tasfb2b.service;

import com.tasfb2b.algorithm.GrafoVuelos;
import com.tasfb2b.algorithm.Solucion;
import com.tasfb2b.algorithm.SolucionInicial;
import com.tasfb2b.algorithm.TabuSearch;
import com.tasfb2b.data.DataLoader;
import com.tasfb2b.dto.*;
import com.tasfb2b.model.*;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.tasfb2b.repository.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.HashSet;
import java.util.Set;

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

    @Value("${tasf.colapso.dias-busqueda:14}")
    private int diasBusquedaColapso;

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
    private volatile PlanificacionStats statsActual = new PlanificacionStats();

    private final WebSocketEventPublisher wsPublisher;

    private final RutaRepository rutaRepository;
    private final TramoRutaRepository tramoRutaRepository;
    private final SimulacionRepository simulacionRepository;
    private final AeropuertoRepository aeropuertoRepository;
    private final VueloRepository vueloRepository;
    private final EnvioRepository envioRepository;
    private final TransactionTemplate transactionTemplate;
    private volatile Long simulacionActualId = null;
    private volatile int replanificacionesEjecutadas = 0;

    public PlanificadorService(
            WebSocketEventPublisher wsPublisher,
            RutaRepository rutaRepository,
            TramoRutaRepository tramoRutaRepository,
            SimulacionRepository simulacionRepository,
            AeropuertoRepository aeropuertoRepository,
            VueloRepository vueloRepository,
            EnvioRepository envioRepository,
            PlatformTransactionManager transactionManager
    ) {
        this.wsPublisher = wsPublisher;
        this.rutaRepository = rutaRepository;
        this.tramoRutaRepository = tramoRutaRepository;
        this.simulacionRepository = simulacionRepository;
        this.aeropuertoRepository = aeropuertoRepository;
        this.vueloRepository = vueloRepository;
        this.envioRepository = envioRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /** Carga aeropuertos y vuelos al arrancar el servidor para que el mapa los muestre de inmediato. */
    @PostConstruct
    public void cargarDatosIniciales() {
        try {
            long inicioCarga = System.nanoTime();
            asegurarAeropuertosYVuelosEnDb();
            aeropuertosCargados = aeropuertoRepository.findAll().stream()
                .collect(Collectors.toMap(Aeropuerto::getCodigo, a -> a, (a, b) -> a, LinkedHashMap::new));
            vuelosCargados = vueloRepository.findAll();
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
        if (solucionActual == null) {
            return getUltimaSimulacion()
                .filter(s -> "COMPLETADO".equals(s.getEstado()))
                .map(this::toMetricasDTO)
                .orElse(null);
        }
        return toMetricasDTO(solucionActual);
    }

    public List<AeropuertoDTO> getAeropuertos() {
        List<Aeropuerto> aeropuertos = aeropuertoRepository.findAll();
        if (aeropuertos.isEmpty() && aeropuertosCargados != null) {
            aeropuertos = new ArrayList<>(aeropuertosCargados.values());
        }
        return aeropuertos.stream()
            .map(a -> new AeropuertoDTO(
                a.getCodigo(), a.getCiudad(), a.getPais(), a.getContinente(),
                a.getGmt(), a.getCapacidadMax(), a.getOcupacionActual(),
                a.getLat(), a.getLng()))
            .sorted(Comparator.comparing(AeropuertoDTO::getContinente)
                .thenComparing(AeropuertoDTO::getCodigo))
            .collect(Collectors.toList());
    }

    public List<VueloDTO> getVuelos() {
        List<Vuelo> vuelos = vueloRepository.findAll();
        if (vuelos.isEmpty() && vuelosCargados != null) {
            vuelos = vuelosCargados;
        }
        return vuelos.stream()
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
        statsActual = new PlanificacionStats();
        replanificacionesEjecutadas = 0;
        LocalDate fechaInicio = parsearFecha(fechaInicioStr);
        Thread t = new Thread(() -> ejecutar(escenario, fechaInicio, numDias), "planificador");
        t.setDaemon(true);
        t.start();
    }

    // =========================================================================
    // Lógica de planificación (hilo de fondo)
    // =========================================================================

    private void ejecutar(String escenario, LocalDate fechaInicio, int numDias) {
        Simulacion simulacion = null;
        PlanificacionStats stats = new PlanificacionStats();
        statsActual = stats;
        long inicioTotal = System.nanoTime();
        try {
            LocalDate fechaBase = fechaInicio != null ? fechaInicio : PRIMER_DIA_DATOS;
            fechaInicioSimulacion = fechaBase;
            int dias = resolverDiasEscenario(escenario, numDias);
            simulacion = simulacionRepository.save(new Simulacion(escenario, fechaBase, dias));
            simulacionActualId = simulacion.getId();
            setEstado(Estado.CARGANDO, 5, "Cargando aeropuertos y vuelos...", 0);

            long inicioCarga = System.nanoTime();
            asegurarAeropuertosYVuelosEnDb();
            Map<String, Aeropuerto> aeropuertos = aeropuertoRepository.findAll().stream()
                .collect(Collectors.toMap(Aeropuerto::getCodigo, a -> a, (a, b) -> a, LinkedHashMap::new));
            List<Vuelo> vuelos = vueloRepository.findAll();
            aeropuertosCargados = aeropuertos;
            vuelosCargados = vuelos;
            stats.aeropuertosProcesados = aeropuertos.size();
            stats.vuelosProcesados = vuelos.size();

            setEstado(Estado.CARGANDO, 15, "Cargando envíos (" + escenario + ")...", 0);

            List<Envio> envios = cargarEnviosSegunEscenario(escenario, fechaBase, dias);
            if (envios.isEmpty()) {
                throw new IllegalStateException(
                    "No hay envios en PostgreSQL para " + escenario + " desde " + fechaBase
                        + " por " + dias + " dia(s). Cargue envios o seleccione un periodo con datos.");
            }
            stats.totalEnvios = envios.size();
            stats.maletasSimuladas = envios.stream().mapToInt(Envio::getCantidad).sum();
            stats.tiempoCargaMs = millisDesde(inicioCarga);
            logStats("carga", stats);

            for (Envio e : envios) {
                Aeropuerto orig = aeropuertos.get(e.getOrigen());
                Aeropuerto dest = aeropuertos.get(e.getDestino());
                if (orig != null && dest != null) {
                    boolean mismoC = orig.getContinente().equals(dest.getContinente());
                    e.setPlazoMaximoMinutos(mismoC ? 1440 : 2880);
                }
            }

            setEstado(Estado.PLANIFICANDO, 25, "Construyendo grafo y solución inicial...", 0);

            long inicioPlanificacion = System.nanoTime();
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
                stats.iteracionesEjecutadas = iter;
                int pct = 40 + (int)(iter * 55.0 / iteraciones);
                String msg = "Iteración " + iter + "/" + iteraciones
                    + " | costo: " + String.format("%,.0f", mejorGlobal.getCostoTotal());
                setEstado(Estado.PLANIFICANDO, pct, msg, mejorGlobal.getCostoTotal());
                publicarSnapshot(mejorGlobal, aeropuertos, iter);
            });

            stats.iteracionesEjecutadas = Math.max(stats.iteracionesEjecutadas, iteraciones);
            stats.tiempoPlanificacionMs = millisDesde(inicioPlanificacion);

            int inicioVentanaAbs = minutosDesdeInicioSimulacion(fechaBase);
            int finVentanaAbs = inicioVentanaAbs + calcularHorizonteOperacionalMinutos(dias);
            int invalidadas = invalidarRutasFueraDeReglas(mejor, aeropuertos, finVentanaAbs);
            stats.eventosFueraRangoTemporal = invalidadas;
            if (invalidadas > 0) {
                System.out.printf(
                    "[PlanificadorService][validacion-tiempo] rutas invalidadas por SLA/ventana/vuelos: %,d | ventana=[%d,%d) minutos%n",
                    invalidadas, inicioVentanaAbs, finVentanaAbs);
            }

            solucionActual = mejor;
            long inicioPersistencia = System.nanoTime();
            try {
                guardarRutasEnDb(mejor, simulacion);
                guardarMetricasEnSimulacion(simulacion, mejor);
            } catch (Exception persistEx) {
                System.err.println("[PlanificadorService] La planificacion termino, pero no se pudieron persistir rutas: "
                    + persistEx.getMessage());
                persistEx.printStackTrace();
            }
            stats.tiempoPersistenciaMs = millisDesde(inicioPersistencia);
            stats.tiempoTotalMs = millisDesde(inicioTotal);
            completarStatsDesdeSolucion(stats, mejor);
            persistirStats(simulacion, stats);
            logStats("final", stats);
            setEstado(Estado.COMPLETADO, 100, "Planificación completada", mejor.getCostoTotal());

            // Snapshot y métricas finales
            if (simulacion != null) {
                simulacion.setEstado(Estado.COMPLETADO.name());
                simulacion.setFechaActualizacion(LocalDateTime.now());
                aplicarStats(simulacion, stats);
                simulacionRepository.save(simulacion);
            }
            publicarSnapshot(mejor, aeropuertos, iteraciones);
            wsPublisher.publicarCompletado(getMetricas());

        } catch (Exception ex) {
            if (simulacion != null) {
                simulacion.setEstado(Estado.ERROR.name());
                simulacion.setFechaActualizacion(LocalDateTime.now());
                simulacionRepository.save(simulacion);
            }
            setEstado(Estado.ERROR, progreso, "Error: " + ex.getMessage(), 0);
            ex.printStackTrace();
        }
    }

    private List<Envio> cargarEnviosSegunEscenario(String escenario,
                                                    LocalDate fechaInicio,
                                                    int numDias) throws Exception {
        switch (escenario.toUpperCase()) {
            case "DIA_A_DIA": {
                return cargarEnviosDbPorPeriodo(fechaInicio, 1);
            }
            case "PERIODO": {
                return cargarEnviosDbPorPeriodo(fechaInicio, numDias > 0 ? numDias : 5);
            }
            case "COLAPSO":
            default: {
                System.out.printf(
                    "[PlanificadorService] COLAPSO: usando ventana controlada de %,d dias desde %s. No se cargara el historico completo.%n",
                    numDias, fechaInicio);
                return cargarEnviosDbPorPeriodo(fechaInicio, numDias);
            }
        }
    }

    private int resolverDiasEscenario(String escenario, int numDias) {
        if ("DIA_A_DIA".equalsIgnoreCase(escenario)) return 1;
        if ("COLAPSO".equalsIgnoreCase(escenario)) {
            return Math.max(numDias > 0 ? numDias : 0, diasBusquedaColapso);
        }
        return numDias > 0 ? numDias : 5;
    }

    // =========================================================================
    // Construcción de snapshot WebSocket
    // =========================================================================

    private void publicarSnapshot(Solucion sol, Map<String, Aeropuerto> aeropuertos, int iter) {
        PlanificacionStats stats = statsActual;
        if (stats != null) stats.eventosProcesados++;
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

    private void guardarRutasEnDb(Solucion solucion, Simulacion simulacion) {
        if (solucion == null || simulacion == null) return;

        transactionTemplate.executeWithoutResult(status -> {
            Set<String> enviosGuardados = new HashSet<>();

            for (Ruta ruta : solucion.getRutas()) {

                if (ruta.getEnvio() == null) continue;

                String idEnvio = ruta.getEnvio().getId();

                if (enviosGuardados.contains(idEnvio)) continue;

                enviosGuardados.add(idEnvio);

                ruta.setSimulacion(simulacion);
                ruta.sincronizarDatosPersistentes();
                Ruta rutaGuardada = rutaRepository.save(ruta);

                int orden = 1;
                if (ruta.getVuelos() == null) continue;

                int tiempoActual = ruta.getEnvio().getMinutosRegistro();
                for (Vuelo vuelo : ruta.getVuelos()) {
                    int salidaAbs = GrafoVuelos.proximaSalidaAbsoluta(
                        tiempoActual, vuelo.getSalidaMinutos(), 30);
                    int llegadaAbs = salidaAbs + vuelo.getDuracionMinutos();
                    Vuelo vueloDb = vueloRepository
                        .findFirstByOrigenAndDestinoAndSalidaMinutos(
                            vuelo.getOrigen(), vuelo.getDestino(), vuelo.getSalidaMinutos())
                        .orElse(vuelo);

                    TramoRuta tramo = new TramoRuta();

                    tramo.setRuta(rutaGuardada);
                    tramo.setVuelo(vueloDb);
                    tramo.setIdRuta(rutaGuardada.getId());
                    tramo.setIdVuelo(vueloDb.getId());
                    tramo.setOrden(orden++);
                    tramo.setHoraSalidaProgramada(vuelo.getSalidaMinutos());
                    tramo.setHoraLlegadaProgramada(vuelo.getLlegadaMinutos());
                    tramo.setSalidaAbs(salidaAbs);
                    tramo.setLlegadaAbs(llegadaAbs);
                    tramo.setCapacidadReservada(ruta.getEnvio().getCantidad());
                    tramo.setEstado("PROGRAMADO");

                    tramoRutaRepository.save(tramo);
                    tiempoActual = llegadaAbs;
                }
            }
        });
    }

    private void asegurarAeropuertosYVuelosEnDb() throws Exception {
        DataLoader loader = new DataLoader(rutaAeropuertos, rutaVuelos, directorioEnvios);
        if (aeropuertoRepository.count() == 0) {
            aeropuertoRepository.saveAll(loader.cargarAeropuertos().values());
        }
        if (vueloRepository.count() == 0) {
            vueloRepository.saveAll(loader.cargarVuelos());
        }
    }

    private void asegurarEnviosEnDb(LocalDate fechaInicio, int numDias) throws Exception {
        DataLoader loader = new DataLoader(rutaAeropuertos, rutaVuelos, directorioEnvios);
        List<Envio> envios = (fechaInicio != null && numDias > 0)
            ? loader.cargarEnviosPorPeriodo(fechaInicio, numDias)
            : loader.cargarEnvios(-1);
        envioRepository.saveAll(envios);
    }

    private void asegurarTodosLosEnviosEnDb() throws Exception {
        DataLoader loader = new DataLoader(rutaAeropuertos, rutaVuelos, directorioEnvios);
        List<Envio> enviosArchivo = loader.cargarEnvios(-1);
        long enviosGlobalesEnDb = envioRepository.countByIdOriginalIsNotNull();

        if (enviosGlobalesEnDb < enviosArchivo.size()) {
            envioRepository.saveAll(enviosArchivo);
            System.out.println("[PlanificadorService] Envios completos sincronizados en PostgreSQL: "
                + enviosArchivo.size());
        } else {
            System.out.println("[PlanificadorService] Envios completos ya disponibles en PostgreSQL: "
                + enviosGlobalesEnDb);
        }
    }

    private List<Envio> cargarEnviosDbPorPeriodo(LocalDate fechaInicio, int numDias) throws Exception {
        LocalDate fecha = fechaInicio != null ? fechaInicio : LocalDate.of(2026, 1, 2);
        LocalDateTime desde = fecha.atStartOfDay();
        LocalDateTime hasta = fecha.plusDays(numDias).atStartOfDay();
        long enDbAntes = envioRepository
            .countByFechaHoraRegistroGreaterThanEqualAndFechaHoraRegistroLessThan(desde, hasta);

        DataLoader loader = new DataLoader(rutaAeropuertos, rutaVuelos, directorioEnvios);
        long esperadosArchivo = loader.contarEnviosPorPeriodo(fecha, numDias);

        if (esperadosArchivo == 0) {
            return Collections.emptyList();
        }

        if (enDbAntes == esperadosArchivo) {
            System.out.printf(
                "[PlanificadorService] Periodo %s a %s validado contra archivos: %,d envios. Usando PostgreSQL sin parseo completo.%n",
                desde, hasta, esperadosArchivo);
            return envioRepository
                .findByFechaHoraRegistroGreaterThanEqualAndFechaHoraRegistroLessThan(desde, hasta);
        }

        System.out.printf(
            "[PlanificadorService] Periodo %s a %s incompleto en PostgreSQL: %,d en DB vs %,d en archivos. Sincronizando fuente real.%n",
            desde, hasta, enDbAntes, esperadosArchivo);

        List<Envio> enviosArchivo = loader.cargarEnviosPorPeriodo(fecha, numDias);
        return envioRepository.saveAll(enviosArchivo);
    }

    private void guardarMetricasEnSimulacion(Simulacion simulacion, Solucion solucion) {
        if (simulacion == null || solucion == null) return;
        MetricasDTO m = toMetricasDTO(solucion);
        simulacion.setTotalEnvios(m.getTotalEnvios());
        simulacion.setEnviosConRuta(m.getEnviosConRuta());
        simulacion.setEnviosSinRuta(m.getEnviosSinRuta());
        simulacion.setViolacionesPlazo(m.getViolacionesPlazo());
        simulacion.setPorcentajeCumplimiento(m.getPorcentajeCumplimiento());
        simulacion.setVuelosSaturados(m.getVuelosSaturados());
        simulacion.setAeropuertosSaturados(m.getAeropuertosSaturados());
        simulacion.setDiasAeropuertoSaturados(m.getDiasAeropuertoSaturados());
        simulacion.setTiempoPromedioEntregaMinutos(m.getTiempoPromedioEntregaMinutos());
        simulacion.setEscalasPromedio(m.getEscalasPromedio());
        simulacion.setCostoTotal(m.getCostoTotal());
        simulacion.setSemaforo(m.getSemaforo());
        int inicioAbs = simulacion.getFechaInicio() != null
            ? minutosDesdeInicioSimulacion(simulacion.getFechaInicio())
            : 0;
        int horizonte = simulacion.getNumDias() > 0
            ? calcularHorizonteOperacionalMinutos(simulacion.getNumDias())
            : 0;
        simulacion.setDuracionTotalMinutos(calcularDuracionRealRelativa(solucion, inicioAbs, horizonte));
        simulacion.setFechaActualizacion(LocalDateTime.now());
        simulacionRepository.save(simulacion);
    }

    private MetricasDTO toMetricasDTO(Solucion solucion) {
        double pct = solucion.getPorcentajeCumplimientoPlazo();
        MetricasDTO metricas = new MetricasDTO(
            solucion.getTotalEnvios(),
            solucion.contarEnviosConRuta(),
            solucion.contarEnviosSinRuta(),
            solucion.contarViolacionesPlazo(),
            Math.round(pct * 100.0) / 100.0,
            solucion.contarVuelosSaturados(),
            solucion.contarAeropuertosSaturados(),
            solucion.contarDiasAeropuertoSaturados(),
            Math.round(solucion.getTiempoPromedioEntrega() * 10.0) / 10.0,
            Math.round(solucion.getEscalasPromedio() * 100.0) / 100.0,
            solucion.getCostoTotal(),
            calcularSemaforoGlobal(pct)
        );
        aplicarStats(metricas, statsActual);
        return metricas;
    }

    private MetricasDTO toMetricasDTO(Simulacion s) {
        MetricasDTO metricas = new MetricasDTO(
            s.getTotalEnvios(), s.getEnviosConRuta(), s.getEnviosSinRuta(),
            s.getViolacionesPlazo(), s.getPorcentajeCumplimiento(),
            s.getVuelosSaturados(), s.getAeropuertosSaturados(), s.getDiasAeropuertoSaturados(),
            s.getTiempoPromedioEntregaMinutos(), s.getEscalasPromedio(), s.getCostoTotal(),
            s.getSemaforo()
        );
        PlanificacionStats stats = new PlanificacionStats();
        stats.tiempoCargaMs = longOrZero(s.getTiempoCargaMs());
        stats.tiempoPlanificacionMs = longOrZero(s.getTiempoPlanificacionMs());
        stats.tiempoPersistenciaMs = longOrZero(s.getTiempoPersistenciaMs());
        stats.tiempoTotalMs = longOrZero(s.getTiempoTotalMs());
        stats.aeropuertosProcesados = intOrZero(s.getAeropuertosProcesados());
        stats.vuelosProcesados = intOrZero(s.getVuelosProcesados());
        stats.rutasGeneradas = intOrZero(s.getRutasGeneradas());
        stats.eventosProcesados = intOrZero(s.getEventosProcesados());
        stats.maletasSimuladas = intOrZero(s.getMaletasSimuladas());
        stats.vuelosUtilizados = intOrZero(s.getVuelosUtilizados());
        stats.iteracionesEjecutadas = intOrZero(s.getIteracionesEjecutadas());
        stats.replanificacionesEjecutadas = intOrZero(s.getReplanificacionesEjecutadas());
        stats.tiempoMaximoEntregaMinutos = doubleOrZero(s.getTiempoMaximoEntregaMinutos());
        stats.rutasInvalidas = intOrZero(s.getRutasInvalidas());
        stats.retrasos = intOrZero(s.getRetrasos());
        stats.eventosFueraRangoTemporal = intOrZero(s.getEventosFueraRangoTemporal());
        aplicarStats(metricas, stats);
        return metricas;
    }

    private Optional<Simulacion> getUltimaSimulacion() {
        if (simulacionActualId != null) {
            Optional<Simulacion> actual = simulacionRepository.findById(simulacionActualId);
            if (actual.isPresent()) return actual;
        }
        return simulacionRepository.findTopByEstadoOrderByFechaCreacionDesc(Estado.COMPLETADO.name())
            .or(simulacionRepository::findTopByOrderByFechaCreacionDesc);
    }

    private int calcularDuracionPersistida(Long simulacionId) {
        if (simulacionId == null) return 0;
        return tramoRutaRepository.findByRutaSimulacionId(simulacionId).stream()
            .mapToInt(t -> intOrZero(t.getLlegadaAbs()))
            .max()
            .orElse(0);
    }

    private int calcularDuracionRealRelativa(Solucion solucion, int inicioAbs, int horizonte) {
        if (solucion == null) return horizonte;
        int ultimaLlegada = solucion.getRutas().stream()
            .filter(r -> !r.isSinSolucion())
            .mapToInt(Ruta::calcularLlegadaFinalAbs)
            .filter(t -> t != Integer.MAX_VALUE)
            .map(t -> Math.max(0, t - inicioAbs))
            .max()
            .orElse(0);
        if (ultimaLlegada <= 0) return horizonte;
        return horizonte > 0 ? Math.min(ultimaLlegada, horizonte) : ultimaLlegada;
    }

    // =========================================================================
    // Utilidades
    // =========================================================================

    private static long millisDesde(long inicioNano) {
        return Math.round((System.nanoTime() - inicioNano) / 1_000_000.0);
    }

    private static int minutosDesdeInicioSimulacion(LocalDate fecha) {
        LocalDate base = LocalDate.of(2026, 1, 1);
        return (int) ChronoUnit.DAYS.between(base, fecha) * 1440;
    }

    private static int calcularHorizonteOperacionalMinutos(int numDias) {
        return Math.max(0, numDias) * 1440 + 2880;
    }

    private static int duracionManifest(int ultimaLlegadaRelativa, int horizonte) {
        if (ultimaLlegadaRelativa <= 0) return Math.max(0, horizonte);
        return horizonte > 0
            ? Math.min(ultimaLlegadaRelativa, horizonte)
            : ultimaLlegadaRelativa;
    }

    private void completarStatsDesdeSolucion(PlanificacionStats stats, Solucion solucion) {
        if (stats == null || solucion == null) return;
        stats.rutasGeneradas = solucion.getRutas().size();
        stats.vuelosUtilizados = (int) solucion.getOcupacionVuelos().values().stream()
            .filter(v -> v != null && v > 0)
            .count();
        stats.tiempoMaximoEntregaMinutos = solucion.getRutas().stream()
            .filter(r -> !r.isSinSolucion())
            .mapToInt(Ruta::calcularTiempoTotal)
            .filter(t -> t != Integer.MAX_VALUE)
            .max()
            .orElse(0);
        stats.rutasInvalidas = solucion.contarEnviosSinRuta();
        stats.retrasos = solucion.contarViolacionesPlazo();
        stats.replanificacionesEjecutadas = replanificacionesEjecutadas;
    }

    private int invalidarRutasFueraDeReglas(Solucion solucion,
                                            Map<String, Aeropuerto> aeropuertos,
                                            int finVentanaAbs) {
        if (solucion == null) return 0;

        List<Ruta> invalidas = new ArrayList<>();
        for (Ruta ruta : new ArrayList<>(solucion.getRutas())) {
            if (ruta.isSinSolucion()) continue;
            String motivo = motivoInvalidezTemporal(ruta, aeropuertos, finVentanaAbs);
            if (motivo != null) {
                System.out.printf(
                    "[PlanificadorService][SLA] Ruta %s invalida: %s | ingreso=%d llegada=%d duracion=%d plazo=%d%n",
                    ruta.getEnvio().getId(),
                    motivo,
                    ruta.getEnvio().getMinutosRegistro(),
                    ruta.calcularLlegadaFinalAbs(),
                    ruta.calcularTiempoTotal(),
                    ruta.getEnvio().getPlazoMaximoMinutos());
                Ruta sinRuta = new Ruta(ruta.getEnvio());
                sinRuta.setSinSolucion(true);
                invalidas.add(sinRuta);
            }
        }
        invalidas.forEach(solucion::agregarRuta);
        return invalidas.size();
    }

    private String motivoInvalidezTemporal(Ruta ruta,
                                           Map<String, Aeropuerto> aeropuertos,
                                           int finVentanaAbs) {
        int tiempoTotal = ruta.calcularTiempoTotal();
        int plazo = ruta.getEnvio().getPlazoMaximoMinutos();
        if (tiempoTotal == Integer.MAX_VALUE) return "sin tiempo calculable";
        if (plazo > 0 && tiempoTotal > plazo) {
            return "excede SLA de " + plazo + " min";
        }

        int llegadaAbs = ruta.calcularLlegadaFinalAbs();
        if (llegadaAbs > finVentanaAbs) {
            return "llega fuera de la ventana simulada";
        }

        if (ruta.getVuelos() == null) return null;
        for (Vuelo vuelo : ruta.getVuelos()) {
            Aeropuerto origen = aeropuertos.get(vuelo.getOrigen());
            Aeropuerto destino = aeropuertos.get(vuelo.getDestino());
            boolean mismoContinente = origen != null && destino != null
                && Objects.equals(origen.getContinente(), destino.getContinente());
            int maxVuelo = mismoContinente ? 720 : 1440;
            if (vuelo.getDuracionMinutos() > maxVuelo) {
                return "vuelo " + vuelo.getOrigen() + "->" + vuelo.getDestino()
                    + " dura " + vuelo.getDuracionMinutos() + " min y excede " + maxVuelo + " min";
            }
        }
        return null;
    }

    private void aplicarStats(Simulacion simulacion, PlanificacionStats stats) {
        if (simulacion == null || stats == null) return;
        simulacion.setTiempoCargaMs(stats.tiempoCargaMs);
        simulacion.setTiempoPlanificacionMs(stats.tiempoPlanificacionMs);
        simulacion.setTiempoPersistenciaMs(stats.tiempoPersistenciaMs);
        simulacion.setTiempoTotalMs(stats.tiempoTotalMs);
        simulacion.setAeropuertosProcesados(stats.aeropuertosProcesados);
        simulacion.setVuelosProcesados(stats.vuelosProcesados);
        simulacion.setRutasGeneradas(stats.rutasGeneradas);
        simulacion.setEventosProcesados(stats.eventosProcesados);
        simulacion.setMaletasSimuladas(stats.maletasSimuladas);
        simulacion.setVuelosUtilizados(stats.vuelosUtilizados);
        simulacion.setIteracionesEjecutadas(stats.iteracionesEjecutadas);
        simulacion.setReplanificacionesEjecutadas(stats.replanificacionesEjecutadas);
        simulacion.setTiempoMaximoEntregaMinutos(stats.tiempoMaximoEntregaMinutos);
        simulacion.setRutasInvalidas(stats.rutasInvalidas);
        simulacion.setRetrasos(stats.retrasos);
        simulacion.setEventosFueraRangoTemporal(stats.eventosFueraRangoTemporal);
    }

    private void aplicarStats(MetricasDTO metricas, PlanificacionStats stats) {
        if (metricas == null || stats == null) return;
        metricas.setTiempoCargaMs(stats.tiempoCargaMs);
        metricas.setTiempoPlanificacionMs(stats.tiempoPlanificacionMs);
        metricas.setTiempoPersistenciaMs(stats.tiempoPersistenciaMs);
        metricas.setTiempoTotalMs(stats.tiempoTotalMs);
        metricas.setAeropuertosProcesados(stats.aeropuertosProcesados);
        metricas.setVuelosProcesados(stats.vuelosProcesados);
        metricas.setRutasGeneradas(stats.rutasGeneradas);
        metricas.setEventosProcesados(stats.eventosProcesados);
        metricas.setMaletasSimuladas(stats.maletasSimuladas);
        metricas.setVuelosUtilizados(stats.vuelosUtilizados);
        metricas.setIteracionesEjecutadas(stats.iteracionesEjecutadas);
        metricas.setReplanificacionesEjecutadas(stats.replanificacionesEjecutadas);
        metricas.setTiempoMaximoEntregaMinutos(stats.tiempoMaximoEntregaMinutos);
        metricas.setRutasInvalidas(stats.rutasInvalidas);
        metricas.setRetrasos(stats.retrasos);
        metricas.setEventosFueraRangoTemporal(stats.eventosFueraRangoTemporal);
    }

    private void aplicarStats(ProgresoEventDTO progreso, PlanificacionStats stats) {
        if (progreso == null || stats == null) return;
        progreso.setTiempoCargaMs(stats.tiempoCargaMs);
        progreso.setTiempoPlanificacionMs(stats.tiempoPlanificacionMs);
        progreso.setTiempoPersistenciaMs(stats.tiempoPersistenciaMs);
        progreso.setTiempoTotalMs(stats.tiempoTotalMs);
        progreso.setAeropuertosProcesados(stats.aeropuertosProcesados);
        progreso.setVuelosProcesados(stats.vuelosProcesados);
        progreso.setRutasGeneradas(stats.rutasGeneradas);
        progreso.setEventosProcesados(stats.eventosProcesados);
        progreso.setMaletasSimuladas(stats.maletasSimuladas);
        progreso.setVuelosUtilizados(stats.vuelosUtilizados);
        progreso.setIteracionesEjecutadas(stats.iteracionesEjecutadas);
        progreso.setReplanificacionesEjecutadas(stats.replanificacionesEjecutadas);
        progreso.setTiempoMaximoEntregaMinutos(stats.tiempoMaximoEntregaMinutos);
        progreso.setRutasInvalidas(stats.rutasInvalidas);
        progreso.setRetrasos(stats.retrasos);
        progreso.setEventosFueraRangoTemporal(stats.eventosFueraRangoTemporal);
    }

    private void persistirStats(Simulacion simulacion, PlanificacionStats stats) {
        if (simulacion == null || stats == null) return;
        aplicarStats(simulacion, stats);
        simulacionRepository.save(simulacion);
    }

    private void logStats(String fase, PlanificacionStats stats) {
        if (stats == null) return;
        System.out.printf(
            "[PlanificadorService][%s] carga=%dms planificacion=%dms persistencia=%dms total=%dms | aeropuertos=%d vuelos=%d envios=%d maletas=%d rutas=%d vuelosUsados=%d eventos=%d iteraciones=%d replanificaciones=%d%n",
            fase,
            stats.tiempoCargaMs,
            stats.tiempoPlanificacionMs,
            stats.tiempoPersistenciaMs,
            stats.tiempoTotalMs,
            stats.aeropuertosProcesados,
            stats.vuelosProcesados,
            stats.totalEnvios,
            stats.maletasSimuladas,
            stats.rutasGeneradas,
            stats.vuelosUtilizados,
            stats.eventosProcesados,
            stats.iteracionesEjecutadas,
            stats.replanificacionesEjecutadas
        );
    }

    private void setEstado(Estado e, int p, String msg, double costo) {
        estado.set(e);
        progreso = p;
        mensajeEstado = msg;
        PlanificacionStats stats = statsActual;
        if (stats != null) stats.eventosProcesados++;
        ProgresoEventDTO evento = new ProgresoEventDTO(p, msg, e.name(), costo);
        aplicarStats(evento, stats);
        wsPublisher.publicarProgreso(evento);
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
            return getAnimacionManifestPersistida().orElse(null);
        }
        Optional<Simulacion> simOpt = getUltimaSimulacion();
        int inicioAbs = simOpt.map(Simulacion::getFechaInicio)
            .map(PlanificadorService::minutosDesdeInicioSimulacion)
            .orElse(0);
        int duracionVentana = simOpt.map(s -> s.getNumDias() > 0 ? calcularHorizonteOperacionalMinutos(s.getNumDias()) : 0)
            .orElse(0);
        int diaInicio = inicioAbs / 1440;

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
            int salidaRel = salidaAbs - inicioAbs;
            int llegadaRel = llegadaAbs - inicioAbs;
            if (duracionVentana > 0 && (llegadaRel < 0 || salidaRel > duracionVentana)) continue;
            salidaRel = Math.max(0, salidaRel);
            llegadaRel = duracionVentana > 0 ? Math.min(duracionVentana, llegadaRel) : llegadaRel;

            ocurrencias.add(new OcurrenciaVueloDTO(
                origen, destino, salidaRel, llegadaRel, e.getValue(), vuelo.getCapacidadMax()));
            maxLlegada = Math.max(maxLlegada, llegadaRel);
        }

        ocurrencias.sort(Comparator.comparingInt(OcurrenciaVueloDTO::getSalidaAbs));

        Map<String, Map<Integer, Integer>> ocupDiaria = solucionActual.getOcupacionDiariaAeropuerto();
        List<AeropuertoManifestDTO> aeropuertos = aeropuertosCargados.values().stream()
            .map(a -> new AeropuertoManifestDTO(
                a.getCodigo(), a.getCiudad(), a.getPais(), a.getContinente(),
                a.getLat(), a.getLng(), a.getCapacidadMax(),
                ocupacionRelativa(ocupDiaria.getOrDefault(a.getCodigo(), Collections.emptyMap()),
                    diaInicio, duracionVentana)))
            .collect(Collectors.toList());

        return new AnimacionManifestDTO(duracionManifest(maxLlegada, duracionVentana), ocurrencias, aeropuertos);
    }

    private Optional<AnimacionManifestDTO> getAnimacionManifestPersistida() {
        Optional<Simulacion> simOpt = getUltimaSimulacion()
            .filter(s -> "COMPLETADO".equals(s.getEstado()));
        if (simOpt.isEmpty()) return Optional.empty();
        Simulacion sim = simOpt.get();
        int inicioAbs = sim.getFechaInicio() != null ? minutosDesdeInicioSimulacion(sim.getFechaInicio()) : 0;
        int duracionVentana = sim.getNumDias() > 0 ? calcularHorizonteOperacionalMinutos(sim.getNumDias()) : 0;
        int diaInicio = inicioAbs / 1440;

        List<TramoRuta> tramos = tramoRutaRepository.findByRutaSimulacionId(sim.getId());
        if (tramos.isEmpty()) return Optional.empty();

        Map<String, OcurrenciaAcumulada> ocurrenciasMap = new LinkedHashMap<>();
        int maxLlegada = 0;
        for (TramoRuta t : tramos) {
            Vuelo v = t.getVuelo();
            if (v == null) continue;
            int salidaAbs = intOrZero(t.getSalidaAbs());
            int llegadaAbs = intOrZero(t.getLlegadaAbs());
            if (salidaAbs <= 0 || llegadaAbs <= 0) continue;
            int salidaRel = salidaAbs - inicioAbs;
            int llegadaRel = llegadaAbs - inicioAbs;
            if (duracionVentana > 0 && (llegadaRel < 0 || salidaRel > duracionVentana)) continue;
            salidaRel = Math.max(0, salidaRel);
            llegadaRel = duracionVentana > 0 ? Math.min(duracionVentana, llegadaRel) : llegadaRel;
            String key = v.getOrigen() + "-" + v.getDestino() + "-" + salidaRel + "-" + llegadaRel;
            int salidaRelFinal = salidaRel;
            int llegadaRelFinal = llegadaRel;
            OcurrenciaAcumulada acc = ocurrenciasMap.computeIfAbsent(key,
                k -> new OcurrenciaAcumulada(v.getOrigen(), v.getDestino(), salidaRelFinal,
                    llegadaRelFinal, v.getCapacidadMax()));
            acc.maletas += intOrZero(t.getCapacidadReservada());
            maxLlegada = Math.max(maxLlegada, llegadaRel);
        }

        List<OcurrenciaVueloDTO> ocurrencias = ocurrenciasMap.values().stream()
            .map(o -> new OcurrenciaVueloDTO(o.origen, o.destino, o.salidaAbs, o.llegadaAbs, o.maletas, o.capacidadMax))
            .sorted(Comparator.comparingInt(OcurrenciaVueloDTO::getSalidaAbs))
            .collect(Collectors.toList());

        Map<String, Map<Integer, Integer>> ocupacionPorDia = calcularOcupacionDiariaDesdeDb(sim.getId());
        List<AeropuertoManifestDTO> aeropuertos = aeropuertoRepository.findAll().stream()
            .map(a -> new AeropuertoManifestDTO(
                a.getCodigo(), a.getCiudad(), a.getPais(), a.getContinente(),
                a.getLat(), a.getLng(), a.getCapacidadMax(),
                ocupacionRelativa(ocupacionPorDia.getOrDefault(a.getCodigo(), Collections.emptyMap()),
                    diaInicio, duracionVentana)))
            .collect(Collectors.toList());

        return Optional.of(new AnimacionManifestDTO(
            duracionManifest(maxLlegada, duracionVentana),
            ocurrencias,
            aeropuertos
        ));
    }

    private Map<String, Map<Integer, Integer>> calcularOcupacionDiariaDesdeDb(Long simulacionId) {
        Map<String, Map<Integer, Integer>> ocupacion = new HashMap<>();
        for (Ruta ruta : rutaRepository.findBySimulacionId(simulacionId)) {
            if (ruta.isSinSolucion() || ruta.getEnvio() == null) continue;
            List<TramoRuta> tramos = tramoRutaRepository.findByRutaIdOrderByOrdenAsc(ruta.getId());
            if (tramos.isEmpty()) continue;
            int cantidad = ruta.getEnvio().getCantidad();
            int entrada = ruta.getEnvio().getMinutosRegistro();
            for (TramoRuta tramo : tramos) {
                Vuelo vuelo = tramo.getVuelo();
                String aeropuerto = vuelo != null ? vuelo.getOrigen() : null;
                if (aeropuerto == null) continue;
                int salida = intOrZero(tramo.getSalidaAbs());
                if (salida > entrada) {
                    int dayStart = entrada / 1440;
                    int dayEnd = (salida - 1) / 1440;
                    Map<Integer, Integer> dias = ocupacion.computeIfAbsent(aeropuerto, k -> new HashMap<>());
                    for (int d = dayStart; d <= dayEnd; d++) {
                        dias.merge(d, cantidad, Integer::sum);
                    }
                }
                entrada = intOrZero(tramo.getLlegadaAbs());
            }
        }
        return ocupacion;
    }

    private static Map<Integer, Integer> ocupacionRelativa(Map<Integer, Integer> ocupacionAbs,
                                                           int diaInicio,
                                                           int duracionVentanaMin) {
        if (ocupacionAbs == null || ocupacionAbs.isEmpty()) return Collections.emptyMap();
        int dias = duracionVentanaMin > 0 ? (int) Math.ceil(duracionVentanaMin / 1440.0) : Integer.MAX_VALUE;
        Map<Integer, Integer> relativa = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : ocupacionAbs.entrySet()) {
            int diaRel = entry.getKey() - diaInicio;
            if (diaRel < 0) continue;
            if (dias != Integer.MAX_VALUE && diaRel > dias) continue;
            relativa.put(diaRel, entry.getValue());
        }
        return relativa;
    }

    // =========================================================================
    // Detalle de envíos por aeropuerto (popup del visualizador)
    // =========================================================================

    public List<MaletaEnAeropuertoDTO> getMaletasEnAeropuerto(String codigo, int tiempoMin) {
        if (solucionActual == null || codigo == null) return Collections.emptyList();

        List<MaletaEnAeropuertoDTO> resultado = new ArrayList<>();

        for (Ruta ruta : solucionActual.getRutas()) {
            if (ruta.isSinSolucion() || ruta.getVuelos().isEmpty()) continue;
            Envio envio = ruta.getEnvio();

            Map<String, int[]> intervalos = ruta.calcularIntervalosAlmacen();
            int[] tramoEnAeropuerto = intervalos.get(codigo);

            if (tramoEnAeropuerto != null) {
                int entrada = tramoEnAeropuerto[0];
                int salida  = tramoEnAeropuerto[1];
                if (tiempoMin >= entrada && tiempoMin < salida) {
                    boolean esOrigen = envio.getOrigen().equals(codigo);
                    MaletaEnAeropuertoDTO.Estado estado = esOrigen
                            ? MaletaEnAeropuertoDTO.Estado.PENDIENTE_SALIDA
                            : MaletaEnAeropuertoDTO.Estado.EN_HUB;
                    resultado.add(construirDTO(envio, estado, entrada, salida, false));
                }
                continue;
            }

            if (envio.getDestino().equals(codigo)) {
                int llegadaFinal = calcularLlegadaFinal(ruta);
                if (llegadaFinal != Integer.MAX_VALUE && tiempoMin >= llegadaFinal) {
                    resultado.add(construirDTO(envio,
                            MaletaEnAeropuertoDTO.Estado.ENTREGADA,
                            llegadaFinal, -1, true));
                }
            }
        }

        return resultado;
    }

    private MaletaEnAeropuertoDTO construirDTO(Envio envio,
                                                MaletaEnAeropuertoDTO.Estado estado,
                                                int tiempoLlegada, int tiempoSalida,
                                                boolean esDestinoFinal) {
        return new MaletaEnAeropuertoDTO(
                envio.getId(),
                envio.getOrigen(),
                envio.getDestino(),
                getCiudad(envio.getOrigen()),
                getCiudad(envio.getDestino()),
                envio.getCantidad(),
                estado,
                tiempoLlegada,
                tiempoSalida,
                esDestinoFinal
        );
    }

    private static int calcularLlegadaFinal(Ruta ruta) {
        if (ruta.isSinSolucion() || ruta.getVuelos().isEmpty()) return Integer.MAX_VALUE;
        int tiempoActual = ruta.getEnvio().getMinutosRegistro();
        for (Vuelo v : ruta.getVuelos()) {
            int salidaAbs = GrafoVuelos.proximaSalidaAbsoluta(tiempoActual, v.getSalidaMinutos(), 30);
            tiempoActual = salidaAbs + v.getDuracionMinutos();
        }
        return tiempoActual;
    }

    private static class OcurrenciaAcumulada {
        final String origen;
        final String destino;
        final int salidaAbs;
        final int llegadaAbs;
        final int capacidadMax;
        int maletas;

        OcurrenciaAcumulada(String origen, String destino, int salidaAbs, int llegadaAbs, int capacidadMax) {
            this.origen = origen;
            this.destino = destino;
            this.salidaAbs = salidaAbs;
            this.llegadaAbs = llegadaAbs;
            this.capacidadMax = capacidadMax;
        }
    }

    private static class PlanificacionStats {
        int totalEnvios;
        int aeropuertosProcesados;
        int vuelosProcesados;
        int rutasGeneradas;
        int eventosProcesados;
        int maletasSimuladas;
        int vuelosUtilizados;
        int iteracionesEjecutadas;
        int replanificacionesEjecutadas;
        double tiempoMaximoEntregaMinutos;
        int rutasInvalidas;
        int retrasos;
        int eventosFueraRangoTemporal;
        long tiempoCargaMs;
        long tiempoPlanificacionMs;
        long tiempoPersistenciaMs;
        long tiempoTotalMs;
    }

    // =========================================================================
    // RAN-02: Rutas y replanificación
    // =========================================================================

    public List<RutaResumenDTO> getRutasResumen(int limite) {
        return getRutasResumen(limite, null);
    }

    public List<RutaResumenDTO> getRutasResumen(int limite, Long simulacionId) {
        if (solucionActual != null) {
            return solucionActual.getRutas().stream()
                .sorted(Comparator.comparing(r -> !esRiesgosa(r)))
                .limit(limite)
                .map(this::toResumen)
                .collect(Collectors.toList());
        }
        Optional<Simulacion> sim = simulacionId != null
            ? simulacionRepository.findById(simulacionId)
            : getUltimaSimulacion();
        if (sim.isEmpty()) return Collections.emptyList();
        return rutaRepository.findBySimulacionId(sim.get().getId()).stream()
            .sorted(Comparator.comparing(r -> !esRiesgosa(r)))
            .limit(limite)
            .map(this::toResumen)
            .collect(Collectors.toList());
    }

    public RutaDetalleDTO getRutaDetalle(String envioId) {
        if (solucionActual != null) {
            Ruta ruta = solucionActual.getRutas().stream()
                .filter(r -> r.getEnvio().getId().equals(envioId))
                .findFirst().orElse(null);
            if (ruta != null) return toDetalle(ruta);
        }
        Optional<Simulacion> sim = getUltimaSimulacion();
        if (sim.isEmpty()) return null;
        return rutaRepository.findBySimulacionIdAndEnvioId(sim.get().getId(), envioId)
            .map(this::toDetallePersistida)
            .orElse(null);
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
        replanificacionesEjecutadas++;
        PlanificacionStats stats = statsActual;
        if (stats != null) {
            stats.replanificacionesEjecutadas = replanificacionesEjecutadas;
            completarStatsDesdeSolucion(stats, solucionActual);
        }

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
        if (!r.isSinSolucion() && r.getVuelos() != null) {
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

    private RutaDetalleDTO toDetallePersistida(Ruta r) {
        Envio e = r.getEnvio();
        int plazoRuta = r.getPlazoMaximoMinutos() != null ? r.getPlazoMaximoMinutos() : 0;
        int plazoMin = plazoRuta > 0
            ? plazoRuta
            : e.getPlazoMaximoMinutos();
        String plazoStr = plazoMin <= 1440
            ? "24 h (mismo continente)" : "48 h (distinto continente)";
        DateTimeFormatter dtfFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String fechaIngreso = e.getFechaHoraRegistro().format(dtfFecha) + " UTC";
        String fechaLimite  = e.getFechaHoraRegistro().plusMinutes(plazoMin).format(dtfFecha) + " UTC";

        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
        List<RutaDetalleDTO.TramoDTO> tramos = tramoRutaRepository
            .findByRutaIdOrderByOrdenAsc(r.getId()).stream()
            .map(t -> {
                Vuelo v = t.getVuelo();
                String origen = v != null ? v.getOrigen() : r.getOrigen();
                String destino = v != null ? v.getDestino() : r.getDestino();
                int salidaMinutos = v != null ? v.getSalidaMinutos() : intOrZero(t.getHoraSalidaProgramada());
                int capacidadMax = v != null ? v.getCapacidadMax() : 0;
                int salidaAbs = intOrZero(t.getSalidaAbs());
                int llegadaAbs = intOrZero(t.getLlegadaAbs());
                return new RutaDetalleDTO.TramoDTO(
                    "t" + intOrZero(t.getOrden()),
                    origen + "->" + destino + " (" + formatearMinutos(salidaMinutos) + ")",
                    origen, destino,
                    salidaMinutos,
                    intOrZero(t.getCapacidadReservada()), capacidadMax,
                    base.plusMinutes(salidaAbs).format(dtfFecha) + " UTC",
                    base.plusMinutes(llegadaAbs).format(dtfFecha) + " UTC",
                    t.getEstado()
                );
            })
            .collect(Collectors.toList());

        return new RutaDetalleDTO(
            e.getId(), e.getOrigen(), e.getDestino(),
            getCiudad(e.getOrigen()), getCiudad(e.getDestino()),
            r.isSinSolucion() ? "sin_ruta" : r.getEstado(),
            r.getCumplimiento() != null ? r.getCumplimiento() : cumplimientoDeRuta(r),
            r.isSinSolucion() ? "-" : formatearTiempoDuracion(r.calcularTiempoTotal()),
            0, plazoStr, fechaIngreso, fechaLimite, tramos
        );
    }

    private String getCiudad(String codigo) {
        if (aeropuertosCargados != null) {
            Aeropuerto a = aeropuertosCargados.get(codigo);
            if (a != null) return a.getCiudad();
        }
        return aeropuertoRepository.findByCodigo(codigo)
            .map(Aeropuerto::getCiudad)
            .orElse(codigo);
    }

    private static int intOrZero(Integer value) {
        return value != null ? value : 0;
    }

    private static long longOrZero(Long value) {
        return value != null ? value : 0L;
    }

    private static double doubleOrZero(Double value) {
        return value != null ? value : 0.0;
    }

    private static String formatearTiempoDuracion(int minutos) {
        if (minutos == Integer.MAX_VALUE || minutos <= 0) return "—";
        int h = minutos / 60;
        int m = minutos % 60;
        if (h == 0) return m + " min";
        return m == 0 ? h + " h" : h + " h " + m + " min";
    }
}
