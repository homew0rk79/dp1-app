package com.tasfb2b.controller;

import com.tasfb2b.dto.AlmacenMantenimientoDTO;
import com.tasfb2b.dto.ConfiguracionMapaDTO;
import com.tasfb2b.dto.TramoMantenimientoDTO;
import com.tasfb2b.dto.UnidadTransporteDTO;
import com.tasfb2b.model.Aeropuerto;
import com.tasfb2b.model.ConfiguracionMapa;
import com.tasfb2b.model.TramoMantenimiento;
import com.tasfb2b.model.UnidadTransporte;
import com.tasfb2b.model.Vuelo;
import com.tasfb2b.repository.AeropuertoRepository;
import com.tasfb2b.repository.ConfiguracionMapaRepository;
import com.tasfb2b.repository.TramoMantenimientoRepository;
import com.tasfb2b.repository.UnidadTransporteRepository;
import com.tasfb2b.repository.VueloRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mantenimiento-mapa")
public class MantenimientoMapaController {

    private final AeropuertoRepository aeropuertoRepository;
    private final UnidadTransporteRepository unidadTransporteRepository;
    private final TramoMantenimientoRepository tramoMantenimientoRepository;
    private final ConfiguracionMapaRepository configuracionMapaRepository;
    private final VueloRepository vueloRepository;

    public MantenimientoMapaController(
            AeropuertoRepository aeropuertoRepository,
            UnidadTransporteRepository unidadTransporteRepository,
            TramoMantenimientoRepository tramoMantenimientoRepository,
            ConfiguracionMapaRepository configuracionMapaRepository,
            VueloRepository vueloRepository) {
        this.aeropuertoRepository = aeropuertoRepository;
        this.unidadTransporteRepository = unidadTransporteRepository;
        this.tramoMantenimientoRepository = tramoMantenimientoRepository;
        this.configuracionMapaRepository = configuracionMapaRepository;
        this.vueloRepository = vueloRepository;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> manejarValidacion(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @GetMapping("/almacenes")
    public List<AlmacenMantenimientoDTO> listarAlmacenes() {
        return aeropuertoRepository.findAll().stream().map(this::toAlmacenDTO).toList();
    }

    @PostMapping("/almacenes")
    public AlmacenMantenimientoDTO crearAlmacen(@RequestBody AlmacenMantenimientoDTO dto) {
        validarAlmacen(dto);
        Aeropuerto aeropuerto = new Aeropuerto();
        aplicarAlmacen(aeropuerto, dto);
        return toAlmacenDTO(aeropuertoRepository.save(aeropuerto));
    }

    @PutMapping("/almacenes/{id}")
    public ResponseEntity<AlmacenMantenimientoDTO> actualizarAlmacen(
            @PathVariable Long id,
            @RequestBody AlmacenMantenimientoDTO dto) {
        validarAlmacen(dto);
        return aeropuertoRepository.findById(id)
                .map(aeropuerto -> {
                    aplicarAlmacen(aeropuerto, dto);
                    return ResponseEntity.ok(toAlmacenDTO(aeropuertoRepository.save(aeropuerto)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/ut")
    public List<UnidadTransporteDTO> listarUT() {
        sincronizarUTDesdeVuelos();
        return unidadTransporteRepository.findAll().stream().map(this::toUnidadDTO).toList();
    }

    @PostMapping("/ut")
    public UnidadTransporteDTO crearUT(@RequestBody UnidadTransporteDTO dto) {
        validarUT(dto);
        UnidadTransporte ut = new UnidadTransporte();
        aplicarUT(ut, dto);
        return toUnidadDTO(unidadTransporteRepository.save(ut));
    }

    @PutMapping("/ut/{id}")
    public ResponseEntity<UnidadTransporteDTO> actualizarUT(@PathVariable Long id, @RequestBody UnidadTransporteDTO dto) {
        validarUT(dto);
        return unidadTransporteRepository.findById(id)
                .map(ut -> {
                    aplicarUT(ut, dto);
                    return ResponseEntity.ok(toUnidadDTO(unidadTransporteRepository.save(ut)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/ut/{id}/capacidad")
    public ResponseEntity<UnidadTransporteDTO> actualizarCapacidadUT(
            @PathVariable Long id,
            @RequestBody UnidadTransporteDTO dto) {
        if (dto.getCapacidadMax() <= 0) {
            throw new IllegalArgumentException("La capacidad de la UT debe ser mayor a cero.");
        }
        return unidadTransporteRepository.findById(id)
                .map(ut -> {
                    ut.setCapacidadMax(dto.getCapacidadMax());
                    return ResponseEntity.ok(toUnidadDTO(unidadTransporteRepository.save(ut)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/tramos")
    public List<TramoMantenimientoDTO> listarTramos() {
        sincronizarTramosDesdeVuelos();
        eliminarTramosDuplicados();
        return tramoMantenimientoRepository.findAll().stream().map(this::toTramoDTO).toList();
    }

    @PostMapping("/tramos")
    public TramoMantenimientoDTO crearTramo(@RequestBody TramoMantenimientoDTO dto) {
        validarTramo(dto);
        TramoMantenimiento tramo = new TramoMantenimiento();
        aplicarTramo(tramo, dto);
        return toTramoDTO(tramoMantenimientoRepository.save(tramo));
    }

    @PutMapping("/tramos/{id}")
    public ResponseEntity<TramoMantenimientoDTO> actualizarTramo(
            @PathVariable Long id,
            @RequestBody TramoMantenimientoDTO dto) {
        validarTramo(dto);
        return tramoMantenimientoRepository.findById(id)
                .map(tramo -> {
                    aplicarTramo(tramo, dto);
                    return ResponseEntity.ok(toTramoDTO(tramoMantenimientoRepository.save(tramo)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/tramos/{id}/horarios")
    public ResponseEntity<TramoMantenimientoDTO> actualizarHorariosTramo(
            @PathVariable Long id,
            @RequestBody TramoMantenimientoDTO dto) {
        validarHorariosTramo(dto.getHoraSalida(), dto.getHoraLlegada());
        return tramoMantenimientoRepository.findById(id)
                .map(tramo -> {
                    tramo.setHoraSalida(dto.getHoraSalida());
                    tramo.setHoraLlegada(dto.getHoraLlegada());
                    return ResponseEntity.ok(toTramoDTO(tramoMantenimientoRepository.save(tramo)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/configuracion")
    public ConfiguracionMapaDTO obtenerConfiguracion() {
        return toConfigDTO(configuracionMapaRepository.findById(1L).orElseGet(this::crearConfigDefault));
    }

    @PutMapping("/configuracion")
    public ConfiguracionMapaDTO guardarConfiguracion(@RequestBody ConfiguracionMapaDTO dto) {
        ConfiguracionMapa config = configuracionMapaRepository.findById(1L).orElseGet(this::crearConfigDefault);
        config.setMostrarAlmacenes(dto.isMostrarAlmacenes());
        config.setMostrarUT(dto.isMostrarUT());
        config.setMostrarTramos(dto.isMostrarTramos());
        config.setZoomInicial(Math.max(0, Math.min(18, dto.getZoomInicial())));
        config.setCentroLat(dto.getCentroLat());
        config.setCentroLng(dto.getCentroLng());
        config.setColorTramos(colorOrDefault(dto.getColorTramos(), "#2563eb"));
        config.setColorUT(colorOrDefault(dto.getColorUT(), "#2563eb"));
        config.setColorAlmacenes(colorOrDefault(dto.getColorAlmacenes(), "#22c55e"));
        return toConfigDTO(configuracionMapaRepository.save(config));
    }

    private ConfiguracionMapa crearConfigDefault() {
        ConfiguracionMapa config = new ConfiguracionMapa();
        config.setId(1L);
        return configuracionMapaRepository.save(config);
    }

    private void sincronizarUTDesdeVuelos() {
        vueloRepository.findAll().forEach(vuelo -> {
            String codigo = codigoVuelo(vuelo);
            if (unidadTransporteRepository.findByCodigo(codigo).isPresent()) return;
            UnidadTransporte ut = new UnidadTransporte();
            ut.setCodigo(codigo);
            ut.setTipo("Avión");
            ut.setUbicacionActual(vuelo.getOrigen());
            ut.setCapacidadMax(vuelo.getCapacidadMax());
            ut.setEstado("Disponible");
            unidadTransporteRepository.save(ut);
        });
    }

    private void sincronizarTramosDesdeVuelos() {
        vueloRepository.findAll().forEach(vuelo -> {
            String codigo = codigoVuelo(vuelo);
            String origen = vuelo.getOrigen().toUpperCase();
            String destino = vuelo.getDestino().toUpperCase();
            LocalDateTime salida = horaSalida(vuelo);
            if (tramoMantenimientoRepository.findByUtAsignada(codigo).isPresent()) {
                return;
            }
            TramoMantenimiento tramo = new TramoMantenimiento();
            tramo.setUtAsignada(codigo);
            tramo.setOrigen(origen);
            tramo.setDestino(destino);
            tramo.setHoraSalida(salida);
            tramo.setHoraLlegada(horaLlegada(vuelo));
            tramo.setEstado("Programado");
            tramoMantenimientoRepository.save(tramo);
        });
    }

    private void eliminarTramosDuplicados() {
        Map<String, TramoMantenimiento> porUt = new HashMap<>();
        tramoMantenimientoRepository.findAll().forEach(tramo -> {
            String clave = tramo.getUtAsignada();
            TramoMantenimiento existente = porUt.get(clave);
            if (existente == null || tramo.getId() < existente.getId()) {
                if (existente != null) tramoMantenimientoRepository.delete(existente);
                porUt.put(clave, tramo);
                return;
            }
            tramoMantenimientoRepository.delete(tramo);
        });
    }

    private String codigoVuelo(Vuelo vuelo) {
        return (vuelo.getOrigen() + "-" + vuelo.getDestino() + "-" + vuelo.getSalidaMinutos()).toUpperCase();
    }

    private LocalDateTime horaSalida(Vuelo vuelo) {
        return LocalDateTime.of(2026, 1, 1, vuelo.getSalidaMinutos() / 60, vuelo.getSalidaMinutos() % 60);
    }

    private LocalDateTime horaLlegada(Vuelo vuelo) {
        int llegadaAbs = vuelo.getLlegadaMinutos() >= vuelo.getSalidaMinutos()
                ? vuelo.getLlegadaMinutos()
                : vuelo.getLlegadaMinutos() + 1440;
        return LocalDateTime.of(2026, 1, 1, 0, 0).plusMinutes(llegadaAbs);
    }

    private void validarAlmacen(AlmacenMantenimientoDTO dto) {
        if (isBlank(dto.getCodigo()) || isBlank(dto.getCiudad()) || isBlank(dto.getPais())) {
            throw new IllegalArgumentException("Código, ciudad y país son obligatorios.");
        }
        if (dto.getCapacidadMax() <= 0) throw new IllegalArgumentException("La capacidad debe ser mayor a cero.");
    }

    private void validarUT(UnidadTransporteDTO dto) {
        if (isBlank(dto.getCodigo()) || isBlank(dto.getTipo()) || isBlank(dto.getUbicacionActual())) {
            throw new IllegalArgumentException("Código, tipo y ubicación/base son obligatorios.");
        }
        if (dto.getCapacidadMax() <= 0) throw new IllegalArgumentException("La capacidad debe ser mayor a cero.");
    }

    private void validarTramo(TramoMantenimientoDTO dto) {
        if (isBlank(dto.getUtAsignada()) || isBlank(dto.getOrigen()) || isBlank(dto.getDestino())) {
            throw new IllegalArgumentException("UT asignada, origen y destino son obligatorios.");
        }
        if (dto.getOrigen().equalsIgnoreCase(dto.getDestino())) {
            throw new IllegalArgumentException("Origen y destino no pueden ser iguales.");
        }
        validarHorariosTramo(dto.getHoraSalida(), dto.getHoraLlegada());
    }

    private void validarHorariosTramo(LocalDateTime salida, LocalDateTime llegada) {
        if (salida == null || llegada == null || !llegada.isAfter(salida)) {
            throw new IllegalArgumentException("La llegada debe ser posterior a la salida.");
        }
    }

    private void aplicarAlmacen(Aeropuerto aeropuerto, AlmacenMantenimientoDTO dto) {
        aeropuerto.setCodigo(dto.getCodigo().trim().toUpperCase());
        aeropuerto.setNombre(isBlank(dto.getNombre()) ? dto.getCiudad().trim() : dto.getNombre().trim());
        aeropuerto.setCiudad(dto.getCiudad().trim());
        aeropuerto.setPais(dto.getPais().trim());
        aeropuerto.setContinente(isBlank(dto.getContinente()) ? "" : dto.getContinente().trim());
        aeropuerto.setLat(dto.getLat());
        aeropuerto.setLng(dto.getLng());
        aeropuerto.setCapacidadMax(dto.getCapacidadMax());
        aeropuerto.setOcupacionActual(Math.max(0, dto.getOcupacionActual()));
    }

    private void aplicarUT(UnidadTransporte ut, UnidadTransporteDTO dto) {
        ut.setCodigo(dto.getCodigo().trim().toUpperCase());
        ut.setTipo(dto.getTipo().trim());
        ut.setUbicacionActual(dto.getUbicacionActual().trim().toUpperCase());
        ut.setCapacidadMax(dto.getCapacidadMax());
        ut.setEstado(isBlank(dto.getEstado()) ? "Disponible" : dto.getEstado().trim());
    }

    private void aplicarTramo(TramoMantenimiento tramo, TramoMantenimientoDTO dto) {
        tramo.setUtAsignada(dto.getUtAsignada().trim());
        tramo.setOrigen(dto.getOrigen().trim().toUpperCase());
        tramo.setDestino(dto.getDestino().trim().toUpperCase());
        tramo.setHoraSalida(dto.getHoraSalida());
        tramo.setHoraLlegada(dto.getHoraLlegada());
        tramo.setEstado(isBlank(dto.getEstado()) ? "Programado" : dto.getEstado().trim());
    }

    private AlmacenMantenimientoDTO toAlmacenDTO(Aeropuerto a) {
        return new AlmacenMantenimientoDTO(
                a.getId(), a.getCodigo(), a.getNombre(), a.getCiudad(), a.getPais(), a.getContinente(),
                a.getLat(), a.getLng(), a.getCapacidadMax(), a.getOcupacionActual());
    }

    private UnidadTransporteDTO toUnidadDTO(UnidadTransporte ut) {
        return new UnidadTransporteDTO(
                ut.getId(), ut.getCodigo(), ut.getTipo(), ut.getUbicacionActual(),
                ut.getCapacidadMax(), ut.getEstado());
    }

    private TramoMantenimientoDTO toTramoDTO(TramoMantenimiento t) {
        return new TramoMantenimientoDTO(
                t.getId(), t.getUtAsignada(), t.getOrigen(), t.getDestino(),
                t.getHoraSalida(), t.getHoraLlegada(), t.getEstado());
    }

    private ConfiguracionMapaDTO toConfigDTO(ConfiguracionMapa c) {
        return new ConfiguracionMapaDTO(
                c.isMostrarAlmacenes(), c.isMostrarUT(), c.isMostrarTramos(),
                c.getZoomInicial(), c.getCentroLat(), c.getCentroLng(),
                colorOrDefault(c.getColorTramos(), "#2563eb"),
                colorOrDefault(c.getColorUT(), "#2563eb"),
                colorOrDefault(c.getColorAlmacenes(), "#22c55e"));
    }

    private String colorOrDefault(String color, String fallback) {
        return isBlank(color) ? fallback : color;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
