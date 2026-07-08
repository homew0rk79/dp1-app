package com.tasfb2b.controller;

import com.tasfb2b.dto.RutaBusquedaDTO;
import com.tasfb2b.model.Envio;
import com.tasfb2b.model.Ruta;
import com.tasfb2b.model.TramoRuta;
import com.tasfb2b.model.Vuelo;
import com.tasfb2b.repository.AeropuertoRepository;
import com.tasfb2b.repository.EnvioRepository;
import com.tasfb2b.repository.RutaRepository;
import com.tasfb2b.repository.SimulacionRepository;
import com.tasfb2b.repository.TramoRutaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class BusquedaRutaController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final LocalDateTime BASE_SIMULACION = LocalDateTime.of(2026, 1, 1, 0, 0);

    private final RutaRepository rutaRepository;
    private final TramoRutaRepository tramoRutaRepository;
    private final AeropuertoRepository aeropuertoRepository;
    private final EnvioRepository envioRepository;

    public BusquedaRutaController(
            SimulacionRepository simulacionRepository,
            RutaRepository rutaRepository,
            TramoRutaRepository tramoRutaRepository,
            AeropuertoRepository aeropuertoRepository,
            EnvioRepository envioRepository) {
        this.rutaRepository = rutaRepository;
        this.tramoRutaRepository = tramoRutaRepository;
        this.aeropuertoRepository = aeropuertoRepository;
        this.envioRepository = envioRepository;
    }

    @GetMapping("/envios/{origen}/{idEnvio}/ruta")
    public ResponseEntity<?> obtenerRutaEnvio(@PathVariable String origen, @PathVariable String idEnvio) {
        String origenNormalizado = normalizar(origen);
        String idNormalizado = normalizarIdEnvio(idEnvio);
        String idGlobal = idGlobal(origenNormalizado, idNormalizado);
        System.out.printf("[rutas-id] tipo=envio origen=%s idEnvio=%s endpoint=/api/envios/%s/%s/ruta%n",
            origenNormalizado, idNormalizado, origenNormalizado, idNormalizado);

        Optional<Envio> envio = envioRepository.findByOrigenAndIdOriginal(origenNormalizado, idNormalizado);
        if (envio.isEmpty()) return respuestaNoEncontradaCompuesta(origenNormalizado, idNormalizado);

        Optional<Ruta> ruta = buscarRutaActualPorIdGlobal(idGlobal);
        if (ruta.isEmpty()) return respuestaSinRuta();

        return ResponseEntity.ok(toBusqueda("envio", idNormalizado, null, ruta.get()));
    }

    @GetMapping("/envios/{origen}/{idEnvio}/maletas/{numeroMaleta}/ruta")
    public ResponseEntity<?> obtenerRutaMaleta(
            @PathVariable String origen,
            @PathVariable String idEnvio,
            @PathVariable String numeroMaleta) {
        String origenNormalizado = normalizar(origen);
        String idNormalizado = normalizarIdEnvio(idEnvio);
        String numeroNormalizado = normalizarNumeroMaleta(numeroMaleta);
        String idGlobal = idGlobal(origenNormalizado, idNormalizado);
        String idMaleta = idGlobal + "-" + numeroNormalizado;
        System.out.printf("[rutas-id] tipo=maleta origen=%s idEnvio=%s numero=%s endpoint=/api/envios/%s/%s/maletas/%s/ruta%n",
            origenNormalizado, idNormalizado, numeroNormalizado, origenNormalizado, idNormalizado, numeroNormalizado);

        Optional<Envio> envio = envioRepository.findByOrigenAndIdOriginal(origenNormalizado, idNormalizado);
        if (envio.isEmpty()) return respuestaNoEncontradaCompuesta(origenNormalizado, idNormalizado);

        int numero = Integer.parseInt(numeroNormalizado);
        if (numero < 1 || numero > envio.get().getCantidad()) {
            return ResponseEntity.status(404).body(Map.of("error", "No existe esa maleta dentro del envío seleccionado."));
        }

        Optional<Ruta> ruta = buscarRutaActualPorIdGlobal(idGlobal);
        if (ruta.isEmpty()) return respuestaSinRuta();

        return ResponseEntity.ok(toBusqueda("maleta", idMaleta, idMaleta, ruta.get()));
    }

    @GetMapping("/rutas-busqueda/ids-prueba")
    public Map<String, Object> obtenerIdsPrueba() {
        List<Envio> envios = rutaRepository.findTop20ByEnvioIsNotNullOrderByIdDesc().stream()
            .filter(this::esRutaConsultable)
            .map(Ruta::getEnvio)
            .filter(e -> e != null && e.getOrigen() != null && e.getIdOriginal() != null)
            .distinct()
            .limit(10)
            .toList();
        List<Map<String, String>> ejemplos = envios.stream()
            .map(e -> Map.of(
                "origen", e.getOrigen(),
                "idEnvio", e.getIdOriginal(),
                "idCompuesto", e.getId(),
                "maleta", e.getId() + "-001"
            ))
            .toList();
        List<String> idsEnvio = envios.stream()
            .map(Envio::getIdOriginal)
            .toList();

        return Map.of(
            "origenes", envioRepository.findDistinctOrigenes(),
            "ejemplos", ejemplos,
            "idsEnvio", idsEnvio,
            "idsMaleta", ejemplos.stream().map(e -> e.get("maleta")).toList(),
            "mensaje", idsEnvio.isEmpty()
                ? "No hay rutas persistidas en base de datos. Ejecute una planificación para generar rutas consultables."
                : "IDs reales con rutas persistidas disponibles."
        );
    }

    private Optional<Ruta> buscarRutaActualPorIdGlobal(String idGlobal) {
        if (idGlobal == null || idGlobal.isBlank()) return Optional.empty();
        List<Ruta> rutas = rutaRepository.findByEnvioIdOrderByIdDesc(idGlobal).stream()
            .filter(this::esRutaConsultable)
            .toList();
        return rutas.isEmpty() ? Optional.empty() : Optional.of(rutas.get(0));
    }

    private ResponseEntity<Map<String, Object>> respuestaNoEncontradaCompuesta(String origen, String idEnvio) {
        return ResponseEntity.status(404).body(Map.of(
            "error", "No se encontró un envío con ese origen e ID.",
            "origen", origen,
            "idEnvio", idEnvio
        ));
    }

    private ResponseEntity<Map<String, Object>> respuestaSinRuta() {
        return ResponseEntity.status(404).body(Map.of(
            "error", "El envío existe, pero aún no tiene ruta registrada.",
            "existeEnvio", true,
            "sinRuta", true
        ));
    }

    private RutaBusquedaDTO toBusqueda(String tipo, String idBuscado, String idMaleta, Ruta rutaActual) {
        Envio envio = rutaActual.getEnvio();
        String idEnvio = envio != null ? envio.getIdOriginal() : rutaActual.getIdEnvioOriginal();
        String idGlobal = envio != null ? envio.getId() : null;
        RutaBusquedaDTO.RutaMapaDTO actual = toRutaMapa(rutaActual);
        RutaBusquedaDTO.RutaMapaDTO anterior = buscarRutaAnterior(idGlobal, rutaActual.getId())
            .map(this::toRutaMapa)
            .orElse(null);

        return new RutaBusquedaDTO(
            tipo,
            idBuscado,
            idMaleta,
            idEnvio,
            envio != null ? envio.getOrigen() : rutaActual.getOrigen(),
            envio != null ? envio.getDestino() : rutaActual.getDestino(),
            envio != null ? envio.getIdCliente() : null,
            rutaActual.getCantidad() != null ? rutaActual.getCantidad() : 0,
            actual,
            anterior
        );
    }

    private Optional<Ruta> buscarRutaAnterior(String idGlobal, Long idRutaActual) {
        if (idGlobal == null || idGlobal.isBlank()) return Optional.empty();
        return rutaRepository.findByEnvioIdOrderByIdDesc(idGlobal).stream()
            .filter(this::esRutaConsultable)
            .filter(r -> idRutaActual == null || !idRutaActual.equals(r.getId()))
            .findFirst();
    }

    private boolean esRutaConsultable(Ruta ruta) {
        if (ruta == null || ruta.isSinSolucion()) return false;
        if (ruta.getEnvio() == null) return false;
        if (ruta.getOrigen() == null || ruta.getOrigen().isBlank()) return false;
        if (ruta.getDestino() == null || ruta.getDestino().isBlank()) return false;
        return !tramoRutaRepository.findByRutaIdOrderByOrdenAsc(ruta.getId()).isEmpty();
    }

    private RutaBusquedaDTO.RutaMapaDTO toRutaMapa(Ruta ruta) {
        List<TramoRuta> tramos = tramoRutaRepository.findByRutaIdOrderByOrdenAsc(ruta.getId());
        Map<String, RutaBusquedaDTO.AeropuertoRutaDTO> aeropuertos = new LinkedHashMap<>();
        List<RutaBusquedaDTO.TramoMapaDTO> tramosDto = new ArrayList<>();
        List<String> escalas = new ArrayList<>();

        String origenRuta = valor(ruta.getOrigen());
        if (!origenRuta.equals("-")) {
            escalas.add(origenRuta);
            aeropuertoDto(origenRuta).ifPresent(a -> aeropuertos.put(a.getCodigo(), a));
        }

        for (TramoRuta tramo : tramos) {
            Vuelo vuelo = tramo.getVuelo();
            String origen = vuelo != null ? vuelo.getOrigen() : ruta.getOrigen();
            String destino = vuelo != null ? vuelo.getDestino() : ruta.getDestino();
            RutaBusquedaDTO.AeropuertoRutaDTO aeroOrigen = aeropuertoDto(origen).orElse(null);
            RutaBusquedaDTO.AeropuertoRutaDTO aeroDestino = aeropuertoDto(destino).orElse(null);
            if (aeroOrigen != null) aeropuertos.put(aeroOrigen.getCodigo(), aeroOrigen);
            if (aeroDestino != null) aeropuertos.put(aeroDestino.getCodigo(), aeroDestino);
            if (destino != null && !destino.isBlank()) escalas.add(destino);

            tramosDto.add(new RutaBusquedaDTO.TramoMapaDTO(
                tramo.getId(),
                tramo.getOrden() != null ? tramo.getOrden() : 0,
                valor(origen),
                valor(destino),
                aeroOrigen,
                aeroDestino,
                formatearAbs(tramo.getSalidaAbs()),
                formatearAbs(tramo.getLlegadaAbs()),
                tramo.getSalidaAbs(),
                tramo.getLlegadaAbs(),
                valor(tramo.getEstado())
            ));
        }

        if (escalas.size() == 1 && ruta.getDestino() != null && !ruta.getDestino().isBlank()) {
            escalas.add(ruta.getDestino());
            aeropuertoDto(ruta.getDestino()).ifPresent(a -> aeropuertos.put(a.getCodigo(), a));
        }

        return new RutaBusquedaDTO.RutaMapaDTO(
            ruta.getId(),
            valor(ruta.getOrigen()),
            valor(ruta.getDestino()),
            valor(ruta.getEstado()),
            valor(ruta.getCumplimiento()),
            ruta.getCantidad() != null ? ruta.getCantidad() : 0,
            escalas,
            new ArrayList<>(aeropuertos.values()),
            tramosDto
        );
    }

    private Optional<RutaBusquedaDTO.AeropuertoRutaDTO> aeropuertoDto(String codigo) {
        if (codigo == null || codigo.isBlank()) return Optional.empty();
        return aeropuertoRepository.findByCodigo(codigo)
            .map(a -> new RutaBusquedaDTO.AeropuertoRutaDTO(
                a.getCodigo(),
                a.getNombre(),
                a.getCiudad(),
                a.getPais(),
                a.getLat(),
                a.getLng()
            ));
    }

    private static String formatearAbs(Integer minutosAbs) {
        if (minutosAbs == null || minutosAbs <= 0) return "-";
        return BASE_SIMULACION.plusMinutes(minutosAbs).format(FMT) + " UTC";
    }

    private static String normalizar(String valor) {
        return valor != null ? valor.trim().toUpperCase() : "";
    }

    private static String normalizarIdEnvio(String valor) {
        String limpio = normalizar(valor);
        if (limpio.matches("\\d{1,8}")) return completarConCeros(limpio, 9);
        return limpio;
    }

    private static String normalizarNumeroMaleta(String valor) {
        String limpio = normalizar(valor);
        if (limpio.matches("\\d{1,2}")) return completarConCeros(limpio, 3);
        return limpio;
    }

    private static String completarConCeros(String valor, int ancho) {
        if (!valor.matches("\\d+") || valor.length() >= ancho) return valor;
        return "0".repeat(ancho - valor.length()) + valor;
    }

    private static String idGlobal(String origen, String idEnvio) {
        return origen + "-" + idEnvio;
    }

    private static String valor(String value) {
        return value != null && !value.isBlank() ? value : "-";
    }
}
