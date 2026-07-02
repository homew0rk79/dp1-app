package com.tasfb2b.service;

import com.tasfb2b.algorithm.GrafoVuelos;
import com.tasfb2b.algorithm.Solucion;
import com.tasfb2b.algorithm.SolucionInicial;
import com.tasfb2b.algorithm.TabuSearch;
import com.tasfb2b.dto.*;
import com.tasfb2b.model.*;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.HashSet;
import java.util.Set;

@Service
public class PlanificadorService {

    public enum Estado { IDLE, CARGANDO, PLANIFICANDO, COMPLETADO, ERROR }

    private static final String ESTADO_EN_TRANSITO = "en_transito";
    private static final String ESTADO_SIN_RUTA = "sin_ruta";
    private static final String ESTADO_CANCELADO = "cancelado";
    private static final int PERMANENCIA_MINIMA_AEROPUERTO_MIN = 30;

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

    // ── Parámetros del escenario DIA_A_DIA ────────────────────────────────────
    /** Iteraciones reducidas para que el tick periódico quepa en el intervalo. */
    @Value("${tasf.algoritmo.iteraciones.diaadia:50}")
    private int iteracionesDiaADia;

    /** Cada cuántos minutos reales se replanifica en día a día. */
    @Value("${tasf.diaadia.intervalo-minutos:5}")
    private int intervaloDiaADiaMin;

    /** Horas simuladas que cubre cada bloque (5 min real × 60× = 5 horas simuladas). */
    @Value("${tasf.diaadia.bloque-horas-simuladas:5}")
    private int bloqueHorasSimuladas;

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
    // Sc: salto del eje de consumo de datos, expresado en minutos simulados.
    @Value("${tasf.simulacion.sc-salto-consumo-min:60}")
    private int saltoConsumoBloqueMinutos;

    // ── Datos cargados ────────────────────────────────────────────────────────
    private volatile Map<String, Aeropuerto> aeropuertosCargados = null;
    private volatile List<Vuelo> vuelosCargados = null;
    private volatile Solucion solucionActual = null;
    private volatile LocalDate fechaInicioSimulacion = PRIMER_DIA_DATOS;
    private volatile LocalDateTime fechaHoraInicioSimulacion = PRIMER_DIA_DATOS.atStartOfDay();
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
    private volatile boolean ejecutandoColapso = false;

    // ── Scheduler de replanificación periódica para DIA_A_DIA ─────────────────
    private volatile ScheduledExecutorService schedulerDiaADia = null;
    private volatile ScheduledFuture<?> tareaDiaADia = null;
    /** Cursor del último bloque ya procesado por el scheduler. */
    private volatile LocalDateTime cursorDiaADia = null;

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

    /** Cachea datos ya persistidos para que el mapa pueda mostrarlos sin leer archivos locales. */
    public void cargarDatosIniciales() {
        try {
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
        ejecutandoColapso = false;
        LocalDateTime fechaInicio = parsearFechaHora(fechaInicioStr);
        Thread t = new Thread(() -> ejecutar(escenario, fechaInicio, numDias), "planificador");
        t.setDaemon(true);
        t.start();
    }

    public void detener() {
        ejecutandoColapso = false;
        detenerSchedulerDiaADia();
        setEstado(Estado.COMPLETADO, 100, "Simulación detenida por el usuario",
            solucionActual != null ? solucionActual.getCostoTotal() : 0);
    }

    /** Cancela y libera el scheduler de día a día si está activo. */
    private void detenerSchedulerDiaADia() {
        if (tareaDiaADia != null) {
            tareaDiaADia.cancel(true);
            tareaDiaADia = null;
        }
        if (schedulerDiaADia != null) {
            schedulerDiaADia.shutdownNow();
            schedulerDiaADia = null;
        }
        cursorDiaADia = null;
    }

    // =========================================================================
    // Lógica de planificación (hilo de fondo)
    // =========================================================================

    private void ejecutar(String escenario, LocalDateTime fechaInicio, int numDias) {
        if ("COLAPSO".equalsIgnoreCase(escenario)) {
            ejecutarColapso(fechaInicio != null ? fechaInicio.toLocalDate() : null);
            return;
        }

        Simulacion simulacion = null;
        PlanificacionStats stats = new PlanificacionStats();
        statsActual = stats;
        long inicioTotal = System.nanoTime();
        try {
            LocalDateTime fechaBase = fechaInicio != null ? fechaInicio : PRIMER_DIA_DATOS.atStartOfDay();
            LocalDate fechaBaseDia = fechaBase.toLocalDate();
            fechaInicioSimulacion = fechaBaseDia;
            fechaHoraInicioSimulacion = fechaBase;
            int dias = resolverDiasEscenario(escenario, numDias);
            simulacion = simulacionRepository.save(new Simulacion(escenario, fechaBaseDia, dias));
            simulacionActualId = simulacion.getId();
            setEstado(Estado.CARGANDO, 5, "Cargando aeropuertos y vuelos...", 0);

            long inicioCarga = System.nanoTime();
            Map<String, Aeropuerto> aeropuertos = aeropuertoRepository.findAll().stream()
                .collect(Collectors.toMap(Aeropuerto::getCodigo, a -> a, (a, b) -> a, LinkedHashMap::new));
            List<Vuelo> vuelos = vueloRepository.findAll();
            validarDatosBaseCargados(aeropuertos, vuelos);
            aeropuertosCargados = aeropuertos;
            vuelosCargados = vuelos;
            stats.aeropuertosProcesados = aeropuertos.size();
            stats.vuelosProcesados = vuelos.size();

            setEstado(Estado.CARGANDO, 15, "Cargando envíos (" + escenario + ")...", 0);

            List<Envio> envios = cargarEnviosSegunEscenario(escenario, fechaBase, dias);
            if (envios.isEmpty()) {
                throw new IllegalStateException(
                    "No hay envíos registrados en la BD para " + escenario + " desde " + fechaBase
                        + " por " + dias + " día(s). Verificá que el rango de fechas tenga datos cargados.");
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

            // Detectar y publicar colapso para DIA_A_DIA y PERIODO
            detectarPrimerColapsoNuevo(mejor, aeropuertos, fechaBaseDia, new HashSet<>())
                .ifPresent(wsPublisher::publicarColapso);

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
            boolean esDiaADia = "DIA_A_DIA".equalsIgnoreCase(escenario);

            if (esDiaADia) {
                // Mantener PLANIFICANDO porque el scheduler seguirá replanificando cada 5 min reales.
                setEstado(Estado.PLANIFICANDO, 100,
                    "Día a día activo — próxima replanificación en " + intervaloDiaADiaMin + " min",
                    mejor.getCostoTotal());
            } else {
                setEstado(Estado.COMPLETADO, 100, "Planificación completada", mejor.getCostoTotal());
            }

            // Snapshot y métricas finales
            if (simulacion != null) {
                simulacion.setEstado(esDiaADia ? Estado.PLANIFICANDO.name() : Estado.COMPLETADO.name());
                simulacion.setFechaActualizacion(LocalDateTime.now());
                aplicarStats(simulacion, stats);
                simulacionRepository.save(simulacion);
            }
            publicarSnapshot(mejor, aeropuertos, iteraciones);
            if (!esDiaADia) wsPublisher.publicarCompletado(getMetricas());

            // Arrancar el scheduler de replanificación periódica para día a día.
            if (esDiaADia) {
                cursorDiaADia = fechaBase.plusDays(1);
                iniciarSchedulerDiaADia(aeropuertos);
            }

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

    // =========================================================================
    // Escenario DIA_A_DIA — scheduler de replanificación periódica
    // =========================================================================

    /** Arranca el scheduler que ejecuta tickDiaADia cada {@code intervaloDiaADiaMin} min reales. */
    private void iniciarSchedulerDiaADia(final Map<String, Aeropuerto> aeropuertos) {
        detenerSchedulerDiaADia();
        schedulerDiaADia = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "scheduler-dia-a-dia");
            t.setDaemon(true);
            return t;
        });
        tareaDiaADia = schedulerDiaADia.scheduleAtFixedRate(
            () -> tickDiaADia(aeropuertos),
            intervaloDiaADiaMin,
            intervaloDiaADiaMin,
            TimeUnit.MINUTES
        );
        System.out.printf(
            "[DIA_A_DIA] Scheduler iniciado: replanificación cada %d min reales (bloque %d h simuladas)%n",
            intervaloDiaADiaMin, bloqueHorasSimuladas);
    }

    /**
     * Tick periódico del escenario día a día: avanza el cursor del bloque,
     * carga envíos nuevos del periodo simulado transcurrido y vuelve a planificar.
     */
    private synchronized void tickDiaADia(Map<String, Aeropuerto> aeropuertos) {
        try {
            if (cursorDiaADia == null || solucionActual == null || vuelosCargados == null) return;

            LocalDateTime cursorAnterior = cursorDiaADia;
            cursorDiaADia = cursorDiaADia.plusHours(bloqueHorasSimuladas);
            long minutosBloque = ChronoUnit.MINUTES.between(cursorAnterior, cursorDiaADia);
            int diasBloque = (int) Math.max(1, Math.ceil(minutosBloque / 1440.0));

            List<Envio> nuevosEnvios = cargarEnviosDbPorPeriodo(cursorAnterior, diasBloque);
            if (nuevosEnvios.isEmpty()) {
                wsPublisher.publicarProgreso(100,
                    "Día a día: sin envíos nuevos en bloque " + cursorAnterior + " → " + cursorDiaADia,
                    "PLANIFICANDO", solucionActual.getCostoTotal());
                return;
            }

            for (Envio e : nuevosEnvios) {
                Aeropuerto orig = aeropuertos.get(e.getOrigen());
                Aeropuerto dest = aeropuertos.get(e.getDestino());
                if (orig != null && dest != null) {
                    boolean mismoC = orig.getContinente().equals(dest.getContinente());
                    e.setPlazoMaximoMinutos(mismoC ? 1440 : 2880);
                }
            }

            // Universo de envíos a re-planificar: los nuevos + los ya pendientes en la solución actual.
            List<Envio> universo = new ArrayList<>(nuevosEnvios);
            for (Ruta r : solucionActual.getRutas()) {
                if (!r.isSinSolucion()) universo.add(r.getEnvio());
            }

            Map<String, Integer> capAeropuertos = new HashMap<>();
            aeropuertos.forEach((k, v) -> capAeropuertos.put(k, v.getCapacidadMax()));

            GrafoVuelos grafo = new GrafoVuelos(vuelosCargados);
            SolucionInicial si = new SolucionInicial(grafo, capAeropuertos);
            Solucion inicial = si.construir(universo);

            TabuSearch ts = new TabuSearch(grafo, iteracionesDiaADia, tenencia, muestra);
            Solucion mejor = ts.ejecutar(inicial, universo, null);

            solucionActual = mejor;
            publicarSnapshot(mejor, aeropuertos, 0);
            setEstado(Estado.PLANIFICANDO, 100,
                String.format(
                    "Día a día: bloque %s → %s replanificado (+%d envíos, costo %.0f)",
                    cursorAnterior, cursorDiaADia, nuevosEnvios.size(), mejor.getCostoTotal()),
                mejor.getCostoTotal());

            System.out.printf(
                "[DIA_A_DIA] tick: +%d envíos del bloque %s..%s | universo=%d | costo=%.0f%n",
                nuevosEnvios.size(), cursorAnterior, cursorDiaADia, universo.size(), mejor.getCostoTotal());

        } catch (Exception ex) {
            System.err.println("[DIA_A_DIA] Error en replanificación periódica: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // =========================================================================
    // Escenario COLAPSO — loop día a día
    // =========================================================================

    private void ejecutarColapso(LocalDate fechaInicio) {
        Simulacion simulacion = null;
        try {
            LocalDate fechaBase = fechaInicio != null ? fechaInicio : PRIMER_DIA_DATOS;
            simulacion = simulacionRepository.save(
                new Simulacion("COLAPSO", fechaBase, 0));
            simulacionActualId = simulacion.getId();
            setEstado(Estado.CARGANDO, 5, "Cargando aeropuertos y vuelos...", 0);

            fechaInicioSimulacion = fechaBase;
            fechaHoraInicioSimulacion = fechaBase.atStartOfDay();

            Map<String, Aeropuerto> aeropuertos = aeropuertoRepository.findAll().stream()
                .collect(Collectors.toMap(Aeropuerto::getCodigo, a -> a, (a, b) -> a, LinkedHashMap::new));
            List<Vuelo> vuelos = vueloRepository.findAll();
            validarDatosBaseCargados(aeropuertos, vuelos);
            aeropuertosCargados = aeropuertos;
            vuelosCargados      = vuelos;

            Map<String, Integer> capAeropuertos = new HashMap<>();
            aeropuertos.forEach((k, v) -> capAeropuertos.put(k, v.getCapacidadMax()));

            GrafoVuelos grafo = new GrafoVuelos(vuelos);
            final int DIAS_CHUNK = 14;
            LocalDate fechaActual = fechaBase;
            int chunk = 0;
            int totalEnvios = 0;
            ColapsoEventDTO colapsoDetectado = null;
            ejecutandoColapso = true;

            Solucion solucionAcumulada = new Solucion(capAeropuertos);
            SolucionInicial si = new SolucionInicial(grafo, capAeropuertos);

            while (ejecutandoColapso) {
                List<Envio> enviosChunk = cargarEnviosDbPorPeriodo(fechaActual.atStartOfDay(), DIAS_CHUNK);
                if (enviosChunk.isEmpty()) break;

                for (Envio e : enviosChunk) {
                    Aeropuerto orig = aeropuertos.get(e.getOrigen());
                    Aeropuerto dest = aeropuertos.get(e.getDestino());
                    if (orig != null && dest != null) {
                        boolean mismoC = orig.getContinente().equals(dest.getContinente());
                        e.setPlazoMaximoMinutos(mismoC ? 1440 : 2880);
                    }
                }
                chunk++;
                totalEnvios += enviosChunk.size();

                LocalDate fechaFin = fechaActual.plusDays(DIAS_CHUNK - 1);
                setEstado(Estado.PLANIFICANDO, Math.min(10 + chunk * 2, 90),
                    "COLAPSO — chunk " + chunk + " | " + fechaBase + " → " + fechaFin
                        + " | " + totalEnvios + " envíos acumulados", 0);

                for (Envio envio : enviosChunk) {
                    si.agregarEnvioASolucion(solucionAcumulada, envio);
                }

                solucionActual = solucionAcumulada;
                publicarSnapshot(solucionAcumulada, aeropuertos, 0);

                Optional<ColapsoEventDTO> colapsoOpt =
                    detectarPrimerColapsoNuevo(solucionAcumulada, aeropuertos, fechaActual, new HashSet<>());
                if (colapsoOpt.isPresent()) {
                    colapsoDetectado = colapsoOpt.get();
                    wsPublisher.publicarColapso(colapsoDetectado);
                    break;
                }

                fechaActual = fechaActual.plusDays(DIAS_CHUNK);
            }

            if (solucionActual != null && simulacion != null) {
                int diasTotales = (int) java.time.temporal.ChronoUnit.DAYS.between(fechaBase, fechaActual) + DIAS_CHUNK;
                simulacion.setFechaInicio(fechaBase);
                simulacion.setNumDias(diasTotales);
                guardarMetricasEnSimulacion(simulacion, solucionActual);
                simulacion.setEstado(Estado.COMPLETADO.name());
                simulacion.setFechaActualizacion(LocalDateTime.now());
                simulacionRepository.save(simulacion);
            }
            String msgFinal = colapsoDetectado != null
                ? "Colapso detectado (" + colapsoDetectado.getTipo() + ") — " + fechaActual
                    + " | " + totalEnvios + " envíos acumulados en " + chunk + " chunks"
                : "Dataset agotado sin colapso — " + totalEnvios + " envíos en " + chunk + " chunks";
            double costoFinal = solucionActual != null ? solucionActual.getCostoTotal() : 0;
            setEstado(Estado.COMPLETADO, 100, msgFinal, costoFinal);
            wsPublisher.publicarCompletado(getMetricas());

        } catch (Exception ex) {
            if (simulacion != null) {
                simulacion.setEstado(Estado.ERROR.name());
                simulacion.setFechaActualizacion(LocalDateTime.now());
                simulacionRepository.save(simulacion);
            }
            setEstado(Estado.ERROR, progreso, "Error en COLAPSO: " + ex.getMessage(), 0);
            ex.printStackTrace();
        } finally {
            ejecutandoColapso = false;
        }
    }

    private ColapsoEventDTO detectarColapsoDesdeMetricas(
            Solucion solucion, Map<String, Aeropuerto> aeropuertos, LocalDate fecha) {
        Map<String, Map<Integer, Integer>> ocupDiaria = solucion.getOcupacionDiariaAeropuerto();
        for (Map.Entry<String, Map<Integer, Integer>> ae : ocupDiaria.entrySet()) {
            String codigo = ae.getKey();
            Aeropuerto a = aeropuertos.get(codigo);
            if (a == null) continue;
            int cap = a.getCapacidadMax();
            if (cap <= 0) continue;
            for (Map.Entry<Integer, Integer> de : ae.getValue().entrySet()) {
                if (de.getValue() > cap) {
                    int pct = Math.round(de.getValue() * 100f / cap);
                    return new ColapsoEventDTO("AEROPUERTO", codigo,
                        String.format("Aeropuerto %s (%s) saturado: %d%% — %d/%d maletas (día %d)",
                            codigo, a.getCiudad(), pct, de.getValue(), cap, de.getKey()),
                        fecha.toString(), de.getKey() * 1440);
                }
            }
        }
        Map<String, Integer> ocupVuelos = solucion.getOcupacionVuelos();
        Map<String, Integer> capVuelos = solucion.getCapacidadMaxVuelos();
        for (Map.Entry<String, Integer> e : ocupVuelos.entrySet()) {
            Integer cap = capVuelos.get(e.getKey());
            if (cap == null || cap <= 0) continue;
            if (e.getValue() > cap) {
                String[] parts = e.getKey().split("-");
                String desc = parts.length >= 2
                    ? String.format("Vuelo %s→%s superó capacidad: %d/%d maletas", parts[0], parts[1], e.getValue(), cap)
                    : String.format("Vuelo %s superó capacidad: %d/%d", e.getKey(), e.getValue(), cap);
                return new ColapsoEventDTO("VUELO", e.getKey(), desc, fecha.toString(), 0);
            }
        }
        for (Ruta ruta : solucion.getRutas()) {
            if (ruta.isSinSolucion() || ruta.getEnvio() == null) continue;
            int t = ruta.calcularTiempoTotal();
            int plazo = ruta.getEnvio().getPlazoMaximoMinutos();
            if (plazo > 0 && t != Integer.MAX_VALUE && t > plazo) {
                return new ColapsoEventDTO("SLA", ruta.getEnvio().getId(),
                    String.format("Envío %s superó plazo: %d min (máx %d) — %s→%s",
                        ruta.getEnvio().getId(), t, plazo,
                        ruta.getEnvio().getOrigen(), ruta.getEnvio().getDestino()),
                    fecha.toString(), 0);
            }
        }
        return null;
    }

    private Optional<ColapsoEventDTO> detectarPrimerColapsoNuevo(
            Solucion solucion,
            Map<String, Aeropuerto> aeropuertos,
            LocalDate fecha,
            Set<String> yaReportados) {

        // 1. Aeropuertos — sweep line para encontrar el primer minuto exacto de overflow real
        Map<String, List<int[]>> intervalosPorAeropuerto = new HashMap<>();
        for (Ruta ruta : solucion.getRutas()) {
            if (ruta.isSinSolucion() || ruta.getVuelos().isEmpty()) continue;
            int cantidad = ruta.getEnvio().getCantidad();
            for (Map.Entry<String, int[]> iv : ruta.calcularIntervalosAlmacen().entrySet()) {
                intervalosPorAeropuerto
                    .computeIfAbsent(iv.getKey(), k -> new ArrayList<>())
                    .add(new int[]{iv.getValue()[0], iv.getValue()[1], cantidad});
            }
        }

        ColapsoEventDTO mejorColapsoAero = null;
        int primerMinutoAero = Integer.MAX_VALUE;

        for (Map.Entry<String, List<int[]>> entry : intervalosPorAeropuerto.entrySet()) {
            String codigo = entry.getKey();
            if (yaReportados.contains("AEROPUERTO:" + codigo)) continue;
            Aeropuerto a = aeropuertos.get(codigo);
            if (a == null) continue;
            int cap = a.getCapacidadMax();

            List<int[]> eventos = new ArrayList<>();
            for (int[] tramo : entry.getValue()) {
                eventos.add(new int[]{tramo[0], +tramo[2]});   // entrada: +cantidad
                eventos.add(new int[]{tramo[1], -tramo[2]});   // salida:  -cantidad
            }
            // Entradas antes que salidas en el mismo minuto → detecta el pico real
            eventos.sort((e1, e2) -> e1[0] != e2[0]
                ? Integer.compare(e1[0], e2[0])
                : Integer.compare(e2[1], e1[1]));

            int ocup = 0;
            for (int[] ev : eventos) {
                ocup += ev[1];
                if (ocup > cap && ev[0] < primerMinutoAero) {
                    primerMinutoAero = ev[0];
                    int pct = cap > 0 ? Math.round(ocup * 100f / cap) : 0;
                    mejorColapsoAero = new ColapsoEventDTO(
                        "AEROPUERTO", codigo,
                        String.format("Aeropuerto %s (%s) saturado: %d%% — %d/%d maletas",
                            codigo, a.getCiudad(), pct, ocup, cap),
                        fecha.toString(),
                        ev[0]
                    );
                    break;
                }
            }
        }

        if (mejorColapsoAero != null) return Optional.of(mejorColapsoAero);

        // 2. Vuelos sobre capacidad
        Map<String, Integer> ocupVuelos = solucion.getOcupacionVuelos();
        Map<String, Integer> capVuelos  = solucion.getCapacidadMaxVuelos();
        for (Map.Entry<String, Integer> e : ocupVuelos.entrySet()) {
            String key = e.getKey();
            if (yaReportados.contains("VUELO:" + key)) continue;
            Integer cap = capVuelos.get(key);
            if (cap == null || cap == 0) continue;
            if (e.getValue() > cap) {
                String[] parts = key.split("-");
                String desc = parts.length >= 2
                    ? String.format("Vuelo %s→%s superó capacidad: %d/%d maletas", parts[0], parts[1], e.getValue(), cap)
                    : String.format("Vuelo %s superó capacidad: %d/%d maletas", key, e.getValue(), cap);
                return Optional.of(new ColapsoEventDTO("VUELO", key, desc, fecha.toString(), 0));
            }
        }

        // 3. Primer envío que viola SLA
        for (Ruta ruta : solucion.getRutas()) {
            if (ruta.isSinSolucion() || ruta.getEnvio() == null) continue;
            String clave = "SLA:" + ruta.getEnvio().getId();
            if (yaReportados.contains(clave)) continue;
            int t = ruta.calcularTiempoTotal();
            int plazo = ruta.getEnvio().getPlazoMaximoMinutos();
            if (plazo > 0 && t != Integer.MAX_VALUE && t > plazo) {
                return Optional.of(new ColapsoEventDTO(
                    "SLA", ruta.getEnvio().getId(),
                    String.format("Envío %s superó plazo: %d min (máx %d) — %s→%s",
                        ruta.getEnvio().getId(), t, plazo,
                        ruta.getEnvio().getOrigen(), ruta.getEnvio().getDestino()),
                    fecha.toString(), 0
                ));
            }
        }

        return Optional.empty();
    }

    private List<Envio> cargarEnviosSegunEscenario(String escenario,
                                                    LocalDateTime fechaInicio,
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

        // Pre-cargar vuelos en memoria para evitar N selects durante el loop
        Map<String, Vuelo> vuelosPorClave = vueloRepository.findAll().stream()
            .collect(Collectors.toMap(
                v -> v.getOrigen() + "|" + v.getDestino() + "|" + v.getSalidaMinutos(),
                v -> v,
                (a, b) -> a
            ));

        transactionTemplate.executeWithoutResult(status -> {
            Set<String> enviosGuardados = new HashSet<>();
            List<TramoRuta> tramosAGuardar = new ArrayList<>();

            for (Ruta ruta : solucion.getRutas()) {
                if (ruta.getEnvio() == null) continue;
                String idEnvio = ruta.getEnvio().getId();
                if (enviosGuardados.contains(idEnvio)) continue;
                enviosGuardados.add(idEnvio);

                ruta.setSimulacion(simulacion);
                ruta.sincronizarDatosPersistentes();
                Ruta rutaGuardada = rutaRepository.save(ruta);

                if (ruta.getVuelos() == null) continue;

                int orden = 1;
                int tiempoActual = ruta.getEnvio().getMinutosRegistro();
                for (Vuelo vuelo : ruta.getVuelos()) {
                    int salidaAbs = GrafoVuelos.proximaSalidaAbsoluta(
                        tiempoActual, vuelo.getSalidaMinutos(), 30);
                    int llegadaAbs = salidaAbs + vuelo.getDuracionMinutos();

                    String clave = vuelo.getOrigen() + "|" + vuelo.getDestino() + "|" + vuelo.getSalidaMinutos();
                    Vuelo vueloDb = vuelosPorClave.getOrDefault(clave, vuelo);

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

                    tramosAGuardar.add(tramo);
                    tiempoActual = llegadaAbs;
                }
            }

            tramoRutaRepository.saveAll(tramosAGuardar);
        });
    }

    private void validarDatosBaseCargados(Map<String, Aeropuerto> aeropuertos, List<Vuelo> vuelos) {
        if (aeropuertos == null || aeropuertos.isEmpty()) {
            throw new IllegalStateException(
                "Falta subir archivo .txt de aeropuertos. Vaya a Configuracion > Carga manual de datos.");
        }
        if (vuelos == null || vuelos.isEmpty()) {
            throw new IllegalStateException(
                "Falta subir archivo .txt de vuelos. Vaya a Configuracion > Carga manual de datos.");
        }
    }

    private List<Envio> cargarEnviosDbPorPeriodo(LocalDateTime fechaInicio, int numDias) throws Exception {
        LocalDateTime desde = fechaInicio != null ? fechaInicio : LocalDate.of(2026, 1, 2).atStartOfDay();
        LocalDateTime hasta = desde.plusDays(numDias);
        return envioRepository
            .findByFechaHoraRegistroGreaterThanEqualAndFechaHoraRegistroLessThan(desde, hasta);
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

    private static int minutosDesdeInicioSimulacion(LocalDateTime fecha) {
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
        return (int) ChronoUnit.MINUTES.between(base, fecha);
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
        int tiempoDisponible = ruta.getEnvio().getMinutosRegistro();
        for (Vuelo vuelo : ruta.getVuelos()) {
            int salidaAbs = GrafoVuelos.proximaSalidaAbsoluta(
                tiempoDisponible, vuelo.getSalidaMinutos(), PERMANENCIA_MINIMA_AEROPUERTO_MIN);
            int espera = salidaAbs - tiempoDisponible;
            if (espera < PERMANENCIA_MINIMA_AEROPUERTO_MIN) {
                return "permanencia en aeropuerto menor a "
                    + PERMANENCIA_MINIMA_AEROPUERTO_MIN + " min antes de "
                    + vuelo.getOrigen() + "->" + vuelo.getDestino();
            }
            tiempoDisponible = salidaAbs + vuelo.getDuracionMinutos();

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

    private static LocalDateTime parsearFechaHora(String fechaStr) {
        if (fechaStr == null || fechaStr.isBlank()) return null;
        if (fechaStr.length() == 10) {
            return LocalDate.parse(fechaStr, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
        }
        return LocalDateTime.parse(fechaStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
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
        int inicioAbs = fechaHoraInicioSimulacion != null
            ? minutosDesdeInicioSimulacion(fechaHoraInicioSimulacion)
            : simOpt.map(Simulacion::getFechaInicio)
                .map(PlanificadorService::minutosDesdeInicioSimulacion)
                .orElse(0);
        int duracionVentanaBase = simOpt.map(s -> s.getNumDias() > 0 ? calcularHorizonteOperacionalMinutos(s.getNumDias()) : 0)
            .orElse(0);
        // Día a día: la ventana crece con los bloques ya replanificados por el scheduler.
        LocalDateTime cursor = cursorDiaADia;
        int minutosCursor = cursor != null ? Math.max(0, minutosDesdeInicioSimulacion(cursor) - inicioAbs) : 0;
        int duracionVentana = cursor != null
            ? Math.max(duracionVentanaBase, minutosCursor + 2880)
            : duracionVentanaBase;
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
                origen, destino, salidaRel, llegadaRel, e.getValue(), vuelo.getCapacidadMax(), salidaMinutos));
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

        int duracionTotal = duracionManifest(maxLlegada, duracionVentana);
        // Día a día: el timeline avanza al ritmo del cursor aunque un bloque no traiga vuelos nuevos.
        if (cursor != null) duracionTotal = Math.max(duracionTotal, minutosCursor);
        return new AnimacionManifestDTO(duracionTotal, inicioAbs, ocurrencias, aeropuertos);
    }

    public List<Map<String, Object>> getConsumoBloques() {
        AnimacionManifestDTO manifest = getAnimacionManifest();
        if (manifest == null || manifest.getDuracionTotalMinutos() <= 0) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> bloques = new ArrayList<>();
        int bloque = 1;
        int saltoMin = Math.max(1, saltoConsumoBloqueMinutos);
        for (int inicio = 0; inicio < manifest.getDuracionTotalMinutos(); inicio += saltoMin) {
            int fin = Math.min(inicio + saltoMin, manifest.getDuracionTotalMinutos());
            int maletas = 0;
            int vuelosActivos = 0;
            for (OcurrenciaVueloDTO ocurrencia : manifest.getOcurrencias()) {
                if (ocurrencia.getSalidaAbs() < fin && ocurrencia.getLlegadaAbs() > inicio) {
                    maletas += ocurrencia.getMaletas();
                    vuelosActivos++;
                }
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("bloque", bloque++);
            item.put("inicioMin", inicio);
            item.put("finMin", fin);
            item.put("saltoMin", saltoMin);
            item.put("maletas", maletas);
            item.put("vuelosActivos", vuelosActivos);
            bloques.add(item);
        }
        return bloques;
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
                    llegadaRelFinal, v.getCapacidadMax(), v.getSalidaMinutos()));
            acc.maletas += intOrZero(t.getCapacidadReservada());
            maxLlegada = Math.max(maxLlegada, llegadaRel);
        }

        List<OcurrenciaVueloDTO> ocurrencias = ocurrenciasMap.values().stream()
            .map(o -> new OcurrenciaVueloDTO(o.origen, o.destino, o.salidaAbs, o.llegadaAbs, o.maletas, o.capacidadMax, o.horaSalidaMinutos))
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
            inicioAbs,
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
    // Vuelos próximos a salir (panel de cancelación interactiva)
    // =========================================================================

    /**
     * Devuelve los próximos vuelos en salir según la solución actual,
     * ordenados por hora de salida y filtrados desde {@code tiempoMin}.
     */
    public List<VueloProximoDTO> getVuelosProximos(int tiempoMin, int limite) {
        if (solucionActual == null || vuelosCargados == null) return Collections.emptyList();

        Map<String, Vuelo> mapaVuelos = new HashMap<>();
        for (Vuelo v : vuelosCargados) {
            mapaVuelos.put(v.getClave(), v);
        }

        List<VueloProximoDTO> resultado = new ArrayList<>();
        for (Map.Entry<String, Integer> e : solucionActual.getOcupacionVuelos().entrySet()) {
            int maletas = e.getValue();
            if (maletas <= 0) continue;
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

            Vuelo vuelo = mapaVuelos.get(origen + "-" + destino + "-" + salidaMinutos);
            if (vuelo == null) continue;

            int salidaAbs = dia * 1440 + salidaMinutos;
            if (salidaAbs < tiempoMin) continue;

            int llegadaAbs = salidaAbs + vuelo.getDuracionMinutos();
            String salidaFmt = String.format("Día %d · %02d:%02d",
                    dia + 1, salidaMinutos / 60, salidaMinutos % 60);
            String clave = origen + "|" + destino + "|" + salidaMinutos;

            resultado.add(new VueloProximoDTO(
                    origen, destino, salidaAbs, llegadaAbs,
                    salidaMinutos, dia, maletas, vuelo.getCapacidadMax(),
                    salidaFmt, clave));
        }

        resultado.sort(Comparator.comparingInt(VueloProximoDTO::getSalidaAbsMin));
        if (resultado.size() > limite) {
            return resultado.subList(0, limite);
        }
        return resultado;
    }

    // =========================================================================
    // Detalle de envíos por aeropuerto (popup del visualizador)
    // =========================================================================

    public List<MaletaEnAeropuertoDTO> getMaletasEnAeropuerto(String codigo, int tiempoMin) {
        if (solucionActual == null || codigo == null) return Collections.emptyList();

        // Ventana = día completo del tiempoMin consultado (igual a la granularidad del algoritmo)
        int diaInicio = (tiempoMin / 1440) * 1440;
        int diaFin    = diaInicio + 1440;

        List<MaletaEnAeropuertoDTO> resultado = new ArrayList<>();

        for (Ruta ruta : solucionActual.getRutas()) {
            if (ruta.isSinSolucion() || ruta.getVuelos().isEmpty()) continue;
            Envio envio = ruta.getEnvio();

            Map<String, int[]> intervalos = ruta.calcularIntervalosAlmacen();
            int[] tramoEnAeropuerto = intervalos.get(codigo);

            if (tramoEnAeropuerto != null) {
                int entrada = tramoEnAeropuerto[0];
                int salida  = tramoEnAeropuerto[1];
                // Incluir si el tramo se solapa con el día consultado (igual criterio que el algoritmo)
                if (entrada < diaFin && salida > diaInicio) {
                    boolean esOrigen     = envio.getOrigen().equals(codigo);
                    boolean presenteAhora = tiempoMin >= entrada && tiempoMin < salida;
                    MaletaEnAeropuertoDTO.Estado estado = esOrigen
                            ? MaletaEnAeropuertoDTO.Estado.PENDIENTE_SALIDA
                            : MaletaEnAeropuertoDTO.Estado.EN_HUB;
                    resultado.add(construirDTO(envio, estado, entrada, salida, false, presenteAhora));
                }
                continue;
            }

            // Caso 2: el aeropuerto es destino final → visible solo durante las 24h siguientes a la entrega
            if (envio.getDestino().equals(codigo)) {
                int llegadaFinal = calcularLlegadaFinal(ruta);
                if (llegadaFinal != Integer.MAX_VALUE && tiempoMin >= llegadaFinal && tiempoMin - llegadaFinal <= 1440) {
                    resultado.add(construirDTO(envio,
                            MaletaEnAeropuertoDTO.Estado.ENTREGADA,
                            llegadaFinal, -1, true, false));
                }
            }
        }

        return resultado;
    }

    /**
     * Retorna la ocupación real en el minuto exacto consultado: suma de maletas cuyo
     * intervalo de almacenamiento contiene ese minuto. Usado por el mapa en tiempo real.
     */
    public Map<String, Integer> getOcupacionActual(int tiempoMin) {
        if (solucionActual == null) return Collections.emptyMap();
        Map<String, Integer> resultado = new HashMap<>();
        for (Ruta ruta : solucionActual.getRutas()) {
            if (ruta.isSinSolucion() || ruta.getVuelos().isEmpty()) continue;
            Map<String, int[]> intervalos = ruta.calcularIntervalosAlmacen();
            for (Map.Entry<String, int[]> e : intervalos.entrySet()) {
                int entrada = e.getValue()[0];
                int salida  = e.getValue()[1];
                if (tiempoMin >= entrada && tiempoMin < salida) {
                    resultado.merge(e.getKey(), ruta.getEnvio().getCantidad(), Integer::sum);
                }
            }
        }
        return resultado;
    }

    private MaletaEnAeropuertoDTO construirDTO(Envio envio,
                                                MaletaEnAeropuertoDTO.Estado estado,
                                                int tiempoLlegada, int tiempoSalida,
                                                boolean esDestinoFinal,
                                                boolean presenteAhora) {
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
                esDestinoFinal,
                presenteAhora
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
        final int horaSalidaMinutos;
        int maletas;

        OcurrenciaAcumulada(String origen, String destino, int salidaAbs, int llegadaAbs, int capacidadMax, int horaSalidaMinutos) {
            this.origen = origen;
            this.destino = destino;
            this.salidaAbs = salidaAbs;
            this.llegadaAbs = llegadaAbs;
            this.capacidadMax = capacidadMax;
            this.horaSalidaMinutos = horaSalidaMinutos;
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

    public synchronized ReplanificacionResultDTO cancelarEnvio(String envioId) {
        if (envioId == null || envioId.isBlank()) {
            throw new IllegalStateException("Debe indicar el ID del envío a cancelar.");
        }
        if (solucionActual == null) {
            throw new IllegalStateException("No hay solución activa. Ejecute una planificación primero.");
        }

        Ruta rutaActual = solucionActual.getRutas().stream()
            .filter(r -> r.getEnvio() != null && envioId.equals(r.getEnvio().getId()))
            .findFirst()
            .orElse(null);
        if (rutaActual == null) {
            throw new IllegalStateException("No se encontró la ruta/envío " + envioId + " en la solución actual.");
        }

        Ruta cancelada = new Ruta(rutaActual.getEnvio());
        cancelada.setSinSolucion(true);
        cancelada.setEstado(ESTADO_CANCELADO);
        solucionActual.agregarRuta(cancelada);

        if (simulacionActualId != null) {
            rutaRepository.findBySimulacionIdAndEnvioId(simulacionActualId, envioId)
                .ifPresent(r -> {
                    r.setSinSolucion(true);
                    r.setEstado(ESTADO_CANCELADO);
                    r.sincronizarDatosPersistentes();
                    rutaRepository.save(r);
                });
        }

        publicarSnapshot(solucionActual, aeropuertosCargados, 0);
        PlanificacionStats stats = statsActual;
        if (stats != null) {
            completarStatsDesdeSolucion(stats, solucionActual);
        }

        String msg = "Envío cancelado: " + envioId;
        System.out.printf("[cancelacion-envio] id=%s estado=%s sinSolucion=%s%n",
            envioId, cancelada.getEstado(), cancelada.isSinSolucion());
        wsPublisher.publicarProgreso(progreso, msg, estado.get().name(), solucionActual.getCostoTotal());
        return new ReplanificacionResultDTO(1, 0, 1, msg,
            envioId, List.of(envioId), List.of(envioId));
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
        return replanificarPorVueloCancelado(origen, destino, horaSalidaMinutos, null);
    }

    public synchronized ReplanificacionResultDTO replanificarPorVueloCancelado(
            String origen, String destino, int horaSalidaMinutos, String idEnvioSolicitante) {

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

        List<String> enviosAfectadosIds = afectados.stream()
            .map(Envio::getId)
            .collect(Collectors.toList());

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
        List<String> enviosCancelados = new ArrayList<>();
        for (Envio envio : afectados) {
            Ruta rutaAntigua = solucionActual.getRutas().stream()
                .filter(r -> r.getEnvio().getId().equals(envio.getId()))
                .findFirst().orElse(null);
                
            if (rutaAntigua == null) continue;

            int index = -1;
            for (int i = 0; i < rutaAntigua.getVuelos().size(); i++) {
                Vuelo v = rutaAntigua.getVuelos().get(i);
                if (v.getOrigen().equals(origen) && v.getDestino().equals(destino) && v.getSalidaMinutos() == horaSalidaMinutos) {
                    index = i;
                    break;
                }
            }

            if (index == -1) continue;

            // Extraer vuelos anteriores al vuelo cancelado
            List<Vuelo> vuelosPrevios = new ArrayList<>(rutaAntigua.getVuelos().subList(0, index));
            
            // Calcular el tiempo de llegada al origen del vuelo cancelado (donde se quedó el paquete)
            int tiempoActual = envio.getMinutosRegistro();
            for (Vuelo v : vuelosPrevios) {
                int salidaAbsoluta = com.tasfb2b.algorithm.GrafoVuelos.proximaSalidaAbsoluta(tiempoActual, v.getSalidaMinutos(), 30);
                tiempoActual = salidaAbsoluta + v.getDuracionMinutos();
            }

            // Crear un envio virtual que nace donde se canceló el vuelo, con la hora en que aterrizó ahí
            Envio envioFicticio = new Envio(
                envio.getIdOriginal(), origen, envio.getDestino(), 
                "20260101", "00", "00", // La fecha no importa tanto si sobreescribimos los minutos luego
                String.valueOf(envio.getCantidad()), envio.getIdCliente()
            );
            // El getter usa FECHA_INICIO_SIMULACION (1 enero 2026), por tanto setear la fechaRegistro 
            // a esa base mas el tiempoActual hace que getMinutosRegistro() == tiempoActual
            envioFicticio.setFechaHoraRegistro(java.time.LocalDate.of(2026, 1, 1)
                .atStartOfDay().plusMinutes(tiempoActual));
            envioFicticio.setPlazoMaximoMinutos(envio.getPlazoMaximoMinutos()); // Mantener plazo si es necesario

            Ruta rutaRestante = si.construirRuta(envioFicticio);
            
            Ruta nueva = new Ruta(envio);
            vuelosPrevios.forEach(nueva::agregarVuelo);
            if (!rutaRestante.isSinSolucion()) {
                rutaRestante.getVuelos().forEach(nueva::agregarVuelo);
                nueva.setEstado(ESTADO_EN_TRANSITO);
                reasignados++;
            } else {
                nueva.setSinSolucion(true);
                nueva.setEstado(ESTADO_CANCELADO);
                enviosCancelados.add(envio.getId());
                sinRuta++;
            }
            
            solucionActual.agregarRuta(nueva);
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

        return new ReplanificacionResultDTO(afectados.size(), reasignados, sinRuta, msg,
            idEnvioSolicitante, enviosAfectadosIds, enviosCancelados);
    }

    // ─── Conversión Ruta → DTO ────────────────────────────────────────────────

    private boolean esRiesgosa(Ruta r) {
        if (esCancelada(r)) return true;
        if (r.isSinSolucion()) return true;
        int t = r.calcularTiempoTotal();
        int p = r.getEnvio().getPlazoMaximoMinutos();
        return p > 0 && t != Integer.MAX_VALUE && t > p;
    }

    private String cumplimientoDeRuta(Ruta r) {
        if (esCancelada(r)) return "rojo";
        if (r.isSinSolucion()) return "rojo";
        int t = r.calcularTiempoTotal();
        if (t == Integer.MAX_VALUE) return "rojo";
        int p = r.getEnvio().getPlazoMaximoMinutos();
        if (p <= 0 || t <= p) return "verde";
        if (t <= p + 240) return "ambar";
        return "rojo";
    }

    private boolean esCancelada(Ruta r) {
        return ESTADO_CANCELADO.equalsIgnoreCase(r.getEstado());
    }

    private String estadoVisualRuta(Ruta r) {
        if (esCancelada(r)) return ESTADO_CANCELADO;
        if (r.isSinSolucion()) return ESTADO_SIN_RUTA;
        String estadoRuta = r.getEstado();
        return estadoRuta != null && !estadoRuta.isBlank() ? estadoRuta : ESTADO_EN_TRANSITO;
    }

    private RutaResumenDTO toResumen(Ruta r) {
        Envio e = r.getEnvio();
        String estado = estadoVisualRuta(r);
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
            estadoVisualRuta(r),
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
            estadoVisualRuta(r),
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
