package com.tasfb2b.controller;

import com.tasfb2b.dto.AnimacionManifestDTO;
import com.tasfb2b.dto.MetricasDTO;
import com.tasfb2b.dto.OcurrenciaVueloDTO;
import com.tasfb2b.model.Aeropuerto;
import com.tasfb2b.model.Ruta;
import com.tasfb2b.model.Simulacion;
import com.tasfb2b.repository.AeropuertoRepository;
import com.tasfb2b.repository.RutaRepository;
import com.tasfb2b.repository.SimulacionRepository;
import com.tasfb2b.service.PlanificadorService;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reportes")
public class ReportesController {

    private static final DateTimeFormatter DIA_FMT = DateTimeFormatter.ofPattern("dd/MM");

    private final PlanificadorService planificadorService;
    private final SimulacionRepository simulacionRepository;
    private final RutaRepository rutaRepository;
    private final AeropuertoRepository aeropuertoRepository;

    public ReportesController(PlanificadorService planificadorService,
                              SimulacionRepository simulacionRepository,
                              RutaRepository rutaRepository,
                              AeropuertoRepository aeropuertoRepository) {
        this.planificadorService = planificadorService;
        this.simulacionRepository = simulacionRepository;
        this.rutaRepository = rutaRepository;
        this.aeropuertoRepository = aeropuertoRepository;
    }

    @GetMapping("/resumen")
    public Map<String, Object> resumen() {
        MetricasDTO m = metricas();
        return Map.of(
            "totalEnvios", m.getTotalEnvios(),
            "cumplimiento", redondear(m.getPorcentajeCumplimiento()),
            "vuelosActivos", m.getVuelosUtilizados(),
            "aeropuertos", m.getAeropuertosProcesados()
        );
    }

    @GetMapping("/desempeno")
    public Map<String, Object> desempeno() {
        MetricasDTO m = metricas();
        List<Ruta> rutas = rutasUltimaSimulacion();
        int demorados = m.getEnviosSinRuta() + m.getViolacionesPlazo();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalEnvios", m.getTotalEnvios());
        data.put("aTiempo", Math.max(0, m.getEnviosConRuta() - m.getViolacionesPlazo()));
        data.put("demorados", demorados);
        data.put("cancelaciones", m.getReplanificacionesEjecutadas());
        data.put("serieBarras", serieBarras(rutas));
        data.put("serieCumplimiento", serieCumplimiento(rutas));
        data.put("topDemorados", topDemorados(rutas));
        return data;
    }

    @GetMapping("/ocupacion")
    public Map<String, Object> ocupacion() {
        AnimacionManifestDTO manifest = planificadorService.getAnimacionManifest();
        List<Map<String, Object>> vuelos = manifest != null
            ? manifest.getOcurrencias().stream().limit(12).map(this::toVueloOcupacion).collect(Collectors.toList())
            : Collections.emptyList();
        List<Map<String, Object>> almacenes = manifest != null
            ? manifest.getAeropuertos().stream().map(a -> {
                int max = a.getOcupacionPorDia().values().stream().mapToInt(Integer::intValue).max().orElse(0);
                return Map.<String, Object>of(
                    "codigo", a.getCodigo(),
                    "nombre", a.getCiudad() + " (" + a.getCodigo() + ")",
                    "capacidad", a.getCapacidadMax(),
                    "almacenadas", max
                );
            }).collect(Collectors.toList())
            : aeropuertoRepository.findAll().stream().map(a -> Map.<String, Object>of(
                "codigo", a.getCodigo(),
                "nombre", a.getCiudad() + " (" + a.getCodigo() + ")",
                "capacidad", a.getCapacidadMax(),
                "almacenadas", a.getOcupacionActual()
            )).collect(Collectors.toList());
        return Map.of("vuelos", vuelos, "almacenes", almacenes);
    }

    @GetMapping("/algoritmos")
    public Map<String, Object> algoritmos() {
        MetricasDTO m = metricas();
        String etiqueta = escenarioUltimaSimulacion();
        Map<String, Object> fila = new LinkedHashMap<>();
        fila.put("escenario", etiqueta);
        fila.put("tiempo", m.getTiempoTotalMs());
        fila.put("calidad", redondear(m.getPorcentajeCumplimiento()));
        fila.put("cumplimiento", redondear(m.getPorcentajeCumplimiento()));
        return Map.of(
            "filas", List.of(fila),
            "datosTiempo", List.of(Map.of("etiqueta", etiqueta, "valor", m.getTiempoTotalMs())),
            "datosMetricas", List.of(Map.of(
                "etiqueta", etiqueta,
                "calidad", redondear(m.getPorcentajeCumplimiento()),
                "cumplimiento", redondear(m.getPorcentajeCumplimiento())
            ))
        );
    }

    @GetMapping("/estable")
    public Map<String, Object> reporteEstable(@RequestParam(required = false) String escenario) {
        Optional<Simulacion> sim = escenario != null && !escenario.isBlank()
            ? simulacionRepository.findTopByEstadoAndEscenarioOrderByFechaCreacionDesc("COMPLETADO", escenario)
            : ultimaSimulacionCompletada();
        return sim.map(this::toReporteEstable)
            .orElseGet(() -> Map.of(
                "disponible", false,
                "mensaje", "No existe una simulacion completada para generar reporte estable"
            ));
    }

    @GetMapping("/demorados")
    public List<Map<String, Object>> demorados() {
        return topDemorados(rutasUltimaSimulacion());
    }

    private MetricasDTO metricas() {
        MetricasDTO metricas = planificadorService.getMetricas();
        if (metricas != null) return metricas;
        return new MetricasDTO(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "gris");
    }

    private List<Ruta> rutasUltimaSimulacion() {
        return ultimaSimulacionCompletada()
            .map(s -> rutaRepository.findBySimulacionId(s.getId()))
            .orElseGet(Collections::emptyList);
    }

    private Optional<Simulacion> ultimaSimulacionCompletada() {
        return simulacionRepository.findTopByEstadoOrderByFechaCreacionDesc("COMPLETADO");
    }

    private String escenarioUltimaSimulacion() {
        return ultimaSimulacionCompletada()
            .map(s -> s.getEscenario() + " - " + s.getNumDias() + " dias")
            .orElse("Sin simulacion");
    }

    private Map<String, Object> toReporteEstable(Simulacion s) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("disponible", true);
        data.put("simulacionId", s.getId());
        data.put("escenario", s.getEscenario());
        data.put("tipoCierre", tipoCierre(s));
        data.put("estado", s.getEstado());
        data.put("fechaInicio", s.getFechaInicio());
        data.put("fechaFin", s.getFechaFin());
        data.put("fechaGeneracion", s.getFechaActualizacion());
        data.put("totalEnvios", s.getTotalEnvios());
        data.put("enviosConRuta", s.getEnviosConRuta());
        data.put("enviosSinRuta", s.getEnviosSinRuta());
        data.put("cumplimiento", redondear(s.getPorcentajeCumplimiento()));
        data.put("vuelosSaturados", s.getVuelosSaturados());
        data.put("aeropuertosSaturados", s.getAeropuertosSaturados());
        data.put("eventosFueraRangoTemporal", s.getEventosFueraRangoTemporal());
        data.put("replanificaciones", s.getReplanificacionesEjecutadas());
        data.put("semaforo", s.getSemaforo());
        return data;
    }

    private static String tipoCierre(Simulacion s) {
        String escenario = s.getEscenario() != null ? s.getEscenario() : "";
        if ("COLAPSO".equalsIgnoreCase(escenario)) return "FINALIZACION_COLAPSO";
        if ("DIA_A_DIA".equalsIgnoreCase(escenario)) return "CIERRE_DIA";
        return "FINALIZACION_SIMULACION";
    }

    private List<Map<String, Object>> serieBarras(List<Ruta> rutas) {
        return rutas.stream()
            .filter(r -> r.getEnvio() != null)
            .collect(Collectors.groupingBy(r -> r.getEnvio().getFechaHoraRegistro().toLocalDate(), TreeMap::new, Collectors.toList()))
            .entrySet().stream()
            .map(e -> {
                int total = e.getValue().size();
                int demorados = (int) e.getValue().stream().filter(this::esDemorada).count();
                return Map.<String, Object>of(
                    "etiqueta", e.getKey().format(DIA_FMT),
                    "aTiempo", total - demorados,
                    "demorados", demorados,
                    "cancelaciones", 0
                );
            })
            .collect(Collectors.toList());
    }

    private List<Map<String, Object>> serieCumplimiento(List<Ruta> rutas) {
        return rutas.stream()
            .filter(r -> r.getEnvio() != null)
            .collect(Collectors.groupingBy(r -> r.getEnvio().getFechaHoraRegistro().toLocalDate(), TreeMap::new, Collectors.toList()))
            .entrySet().stream()
            .map(e -> {
                int total = e.getValue().size();
                long aTiempo = e.getValue().stream().filter(r -> !esDemorada(r)).count();
                return Map.<String, Object>of(
                    "etiqueta", e.getKey().format(DIA_FMT),
                    "cumplimiento", total == 0 ? 0 : redondear(aTiempo * 100.0 / total)
                );
            })
            .collect(Collectors.toList());
    }

    private List<Map<String, Object>> topDemorados(List<Ruta> rutas) {
        return rutas.stream()
            .filter(this::esDemorada)
            .sorted(Comparator.comparingInt(this::excesoMinutos).reversed())
            .limit(10)
            .map(r -> Map.<String, Object>of(
                "id", r.getEnvio() != null ? r.getEnvio().getId() : r.getIdEnvioOriginal(),
                "ruta", valor(r.getOrigen()) + " -> " + valor(r.getDestino()),
                "exceso", formatearDuracion(Math.max(0, excesoMinutos(r))),
                "causa", r.isSinSolucion() ? "Sin ruta viable dentro del SLA" : "Exceso sobre plazo"
            ))
            .collect(Collectors.toList());
    }

    private Map<String, Object> toVueloOcupacion(OcurrenciaVueloDTO o) {
        return Map.of(
            "id", o.getOrigen() + "-" + o.getDestino() + "-" + o.getSalidaAbs(),
            "ruta", o.getOrigen() + " -> " + o.getDestino(),
            "tipo", "Operacional",
            "capacidad", o.getCapacidadMax(),
            "ocupacion", o.getMaletas()
        );
    }

    private boolean esDemorada(Ruta r) {
        return r.isSinSolucion()
            || "rojo".equalsIgnoreCase(r.getCumplimiento())
            || excesoMinutos(r) > 0;
    }

    private int excesoMinutos(Ruta r) {
        int tiempo = r.getTiempoTotalMinutos() != null ? r.getTiempoTotalMinutos() : 0;
        int plazo = r.getPlazoMaximoMinutos() != null ? r.getPlazoMaximoMinutos() : 0;
        return tiempo == Integer.MAX_VALUE ? Integer.MAX_VALUE : tiempo - plazo;
    }

    private static double redondear(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static String formatearDuracion(int minutos) {
        if (minutos == Integer.MAX_VALUE) return "Sin ruta";
        int h = minutos / 60;
        int m = minutos % 60;
        return h + "h " + m + "min";
    }

    private static String valor(String value) {
        return value != null ? value : "-";
    }
}
