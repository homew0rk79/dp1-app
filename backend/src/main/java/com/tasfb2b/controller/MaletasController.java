package com.tasfb2b.controller;

import com.tasfb2b.model.Ruta;
import com.tasfb2b.model.Simulacion;
import com.tasfb2b.model.TramoRuta;
import com.tasfb2b.repository.RutaRepository;
import com.tasfb2b.repository.SimulacionRepository;
import com.tasfb2b.repository.TramoRutaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/maletas")
public class MaletasController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final SimulacionRepository simulacionRepository;
    private final RutaRepository rutaRepository;
    private final TramoRutaRepository tramoRutaRepository;

    public MaletasController(SimulacionRepository simulacionRepository,
                             RutaRepository rutaRepository,
                             TramoRutaRepository tramoRutaRepository) {
        this.simulacionRepository = simulacionRepository;
        this.rutaRepository = rutaRepository;
        this.tramoRutaRepository = tramoRutaRepository;
    }

    @GetMapping
    public List<Map<String, Object>> listar(@RequestParam(defaultValue = "300") int limite) {
        return ultimaSimulacionCompletada()
            .map(sim -> rutaRepository.findBySimulacionId(sim.getId()).stream()
                .limit(Math.max(1, limite))
                .map(this::toMaleta)
                .collect(Collectors.toList()))
            .orElseGet(Collections::emptyList);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> obtenerDetalle(@PathVariable String id) {
        Optional<Simulacion> sim = ultimaSimulacionCompletada();
        if (sim.isEmpty()) return ResponseEntity.notFound().build();
        return rutaRepository.findBySimulacionIdAndEnvioId(sim.get().getId(), id)
            .map(r -> ResponseEntity.ok(toMaleta(r)))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> registrar() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body(Map.of("error", "El registro manual de envios aun no esta integrado con el planificador."));
    }

    @PatchMapping("/{id}/ubicacion")
    public ResponseEntity<Map<String, String>> actualizarUbicacion(@PathVariable String id) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body(Map.of("error", "La ubicacion proviene del estado de simulacion y no se edita manualmente."));
    }

    @PatchMapping("/{id}/entregar")
    public ResponseEntity<Map<String, String>> marcarEntregada(@PathVariable String id) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
            .body(Map.of("error", "La entrega se calcula por eventos del planificador."));
    }

    private Optional<Simulacion> ultimaSimulacionCompletada() {
        return simulacionRepository.findTopByEstadoOrderByFechaCreacionDesc("COMPLETADO");
    }

    private Map<String, Object> toMaleta(Ruta ruta) {
        List<TramoRuta> tramos = tramoRutaRepository.findByRutaIdOrderByOrdenAsc(ruta.getId());
        List<String> paradas = new ArrayList<>();
        paradas.add(valor(ruta.getOrigen()));
        for (TramoRuta tramo : tramos) {
            if (tramo.getVuelo() != null) paradas.add(tramo.getVuelo().getDestino());
        }
        if (paradas.size() == 1 && ruta.getDestino() != null) paradas.add(ruta.getDestino());

        String estado = ruta.isSinSolucion()
            ? "Demorado"
            : "Entregado";
        String riesgo = "rojo".equalsIgnoreCase(ruta.getCumplimiento())
            ? "rojo"
            : "ambar".equalsIgnoreCase(ruta.getCumplimiento()) ? "ambar" : "verde";
        int tiempo = ruta.getTiempoTotalMinutos() != null ? ruta.getTiempoTotalMinutos() : 0;
        int plazo = ruta.getPlazoMaximoMinutos() != null ? ruta.getPlazoMaximoMinutos() : 0;
        int progreso = ruta.isSinSolucion() ? 0 : 100;

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", ruta.getEnvio() != null ? ruta.getEnvio().getId() : valor(ruta.getIdEnvioOriginal()));
        item.put("aerolinea", "Tasf.B2B");
        item.put("origen", valor(ruta.getOrigen()));
        item.put("destino", valor(ruta.getDestino()));
        item.put("origenCiudad", valor(ruta.getOrigen()));
        item.put("destinoCiudad", valor(ruta.getDestino()));
        item.put("cantidad", ruta.getCantidad() != null ? ruta.getCantidad() : 0);
        item.put("estado", estado);
        item.put("riesgo", riesgo);
        item.put("ubicacion", ruta.isSinSolucion() ? "Sin ruta viable" : valor(ruta.getDestino()) + " - Entregado");
        item.put("plazo", plazo > 1440 ? "48h" : "24h");
        item.put("registro", ruta.getEnvio() != null ? ruta.getEnvio().getFechaHoraRegistro().format(FMT) : "-");
        item.put("estimada", !ruta.isSinSolucion() && ruta.getEnvio() != null
            ? ruta.getEnvio().getFechaHoraRegistro().plusMinutes(tiempo).format(FMT)
            : "-");
        item.put("progreso", progreso);
        item.put("ruta", paradas);
        item.put("historial", historial(ruta, tramos));
        return item;
    }

    private List<Map<String, String>> historial(Ruta ruta, List<TramoRuta> tramos) {
        List<Map<String, String>> eventos = new ArrayList<>();
        if (ruta.getEnvio() != null) {
            eventos.add(Map.of(
                "etapa", "Registro inicial",
                "detalle", "Carga registrada en " + valor(ruta.getOrigen()),
                "hora", ruta.getEnvio().getFechaHoraRegistro().format(FMT)
            ));
        }
        for (TramoRuta tramo : tramos) {
            if (tramo.getVuelo() == null) continue;
            eventos.add(Map.of(
                "etapa", "Tramo " + tramo.getOrden(),
                "detalle", tramo.getVuelo().getOrigen() + " -> " + tramo.getVuelo().getDestino(),
                "hora", String.valueOf(tramo.getSalidaAbs())
            ));
        }
        if (ruta.isSinSolucion()) {
            eventos.add(Map.of("etapa", "Sin ruta", "detalle", "No se encontro ruta viable dentro del SLA", "hora", "-"));
        }
        return eventos;
    }

    private static String valor(String value) {
        return value != null ? value : "-";
    }
}
