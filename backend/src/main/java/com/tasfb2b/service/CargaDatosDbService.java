package com.tasfb2b.service;

import com.tasfb2b.data.DataLoader;
import com.tasfb2b.model.Aeropuerto;
import com.tasfb2b.repository.AeropuertoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.tasfb2b.model.Vuelo;
import com.tasfb2b.repository.VueloRepository;
import com.tasfb2b.model.Envio;
import com.tasfb2b.repository.EnvioRepository;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import java.io.IOException;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;

@Service
public class CargaDatosDbService {

    private final AeropuertoRepository aeropuertoRepository;
    private final VueloRepository vueloRepository;
    private final EnvioRepository envioRepository;

    @Value("${tasf.datos.aeropuertos}")
    private String rutaAeropuertos;

    @Value("${tasf.datos.vuelos}")
    private String rutaVuelos;

    @Value("${tasf.datos.envios}")
    private String rutaEnvios;

    public CargaDatosDbService(
            AeropuertoRepository aeropuertoRepository,
            VueloRepository vueloRepository,
            EnvioRepository envioRepository
    ) {
        this.aeropuertoRepository = aeropuertoRepository;
        this.vueloRepository = vueloRepository;
        this.envioRepository = envioRepository;
    }

    public void cargarAeropuertos() throws IOException {
        if (aeropuertoRepository.count() > 0) {
            System.out.println("Aeropuertos ya existen en PostgreSQL");
            return;
        }

        DataLoader loader = new DataLoader(
                rutaAeropuertos,
                rutaVuelos,
                rutaEnvios
        );

        Map<String, Aeropuerto> aeropuertos = loader.cargarAeropuertos();

        aeropuertoRepository.saveAll(aeropuertos.values());

        System.out.println("Aeropuertos guardados en PostgreSQL");
    }

    public void cargarVuelos() throws IOException {
        if (vueloRepository.count() > 0) {
            System.out.println("Vuelos ya existen en PostgreSQL");
            return;
        }

        DataLoader loader = new DataLoader(
                rutaAeropuertos,
                rutaVuelos,
                rutaEnvios
        );

        List<Vuelo> vuelos = loader.cargarVuelos();

        vueloRepository.saveAll(vuelos);

        System.out.println("Vuelos guardados en PostgreSQL");
    }

    public void cargarEnvios() throws IOException {
        DataLoader loader = new DataLoader(
                rutaAeropuertos,
                rutaVuelos,
                rutaEnvios
        );

        List<Envio> envios = loader.cargarEnvios(-1);
        long enviosGlobalesEnDb = envioRepository.countByIdOriginalIsNotNull();

        if (enviosGlobalesEnDb >= envios.size()) {
            System.out.println("Envios completos ya existen en PostgreSQL: " + enviosGlobalesEnDb);
            return;
        }

        envioRepository.saveAll(envios);

        System.out.println("Envios guardados en PostgreSQL: " + envios.size());
    }

    public Map<String, Object> cargarAeropuertosDesdeArchivo(MultipartFile archivo) throws IOException {
        validarArchivoTxt(archivo);
        Path tempDir = Files.createTempDirectory("tasf-upload-aeropuertos-");
        try {
            Path archivoTemporal = guardarTemporal(tempDir, archivo, "aeropuertos.txt");
            DataLoader loader = new DataLoader(archivoTemporal.toString(), rutaVuelos, rutaEnvios);
            Map<String, Aeropuerto> aeropuertos = loader.cargarAeropuertos();

            int insertados = 0;
            int actualizados = 0;
            for (Aeropuerto nuevo : aeropuertos.values()) {
                Aeropuerto destino = aeropuertoRepository.findByCodigo(nuevo.getCodigo()).orElse(null);
                if (destino == null) {
                    aeropuertoRepository.save(nuevo);
                    insertados++;
                } else {
                    copiarAeropuerto(nuevo, destino);
                    aeropuertoRepository.save(destino);
                    actualizados++;
                }
            }

            return respuesta("aeropuertos", archivo.getOriginalFilename(), aeropuertos.size(), insertados, actualizados);
        } finally {
            eliminarTemporal(tempDir);
        }
    }

    public Map<String, Object> cargarVuelosDesdeArchivo(MultipartFile archivo) throws IOException {
        validarArchivoTxt(archivo);
        Path tempDir = Files.createTempDirectory("tasf-upload-vuelos-");
        try {
            Path archivoTemporal = guardarTemporal(tempDir, archivo, "planes_vuelo.txt");
            DataLoader loader = new DataLoader(rutaAeropuertos, archivoTemporal.toString(), rutaEnvios);
            List<Vuelo> vuelos = loader.cargarVuelos();

            int insertados = 0;
            int actualizados = 0;
            for (Vuelo nuevo : vuelos) {
                Vuelo destino = vueloRepository
                    .findFirstByOrigenAndDestinoAndSalidaMinutos(
                        nuevo.getOrigen(), nuevo.getDestino(), nuevo.getSalidaMinutos())
                    .orElse(null);
                if (destino == null) {
                    vueloRepository.save(nuevo);
                    insertados++;
                } else {
                    copiarVuelo(nuevo, destino);
                    vueloRepository.save(destino);
                    actualizados++;
                }
            }

            return respuesta("vuelos", archivo.getOriginalFilename(), vuelos.size(), insertados, actualizados);
        } finally {
            eliminarTemporal(tempDir);
        }
    }

    public Map<String, Object> cargarEnviosDesdeArchivos(List<MultipartFile> archivos) throws IOException {
        if (archivos == null || archivos.isEmpty()) {
            throw new IllegalArgumentException("Debe subir al menos un archivo de envios.");
        }

        Path tempDir = Files.createTempDirectory("tasf-upload-envios-");
        try {
            for (MultipartFile archivo : archivos) {
                validarArchivoTxt(archivo);
                String nombre = archivo.getOriginalFilename() != null ? archivo.getOriginalFilename() : "";
                if (!nombre.matches("_envios_[A-Z]{4}_\\.txt")) {
                    throw new IllegalArgumentException(
                        "Archivo de envios invalido: " + nombre + ". Use el formato _envios_XXXX_.txt");
                }
                guardarTemporal(tempDir, archivo, nombre);
            }

            DataLoader loader = new DataLoader(rutaAeropuertos, rutaVuelos, tempDir.toString());
            List<Envio> envios = loader.cargarEnvios(-1);

            int insertados = 0;
            int actualizados = 0;
            for (Envio envio : envios) {
                if (envioRepository.existsById(envio.getId())) {
                    actualizados++;
                } else {
                    insertados++;
                }
            }
            envioRepository.saveAll(envios);

            Map<String, Object> respuesta = respuesta("envios", archivos.size() + " archivo(s)", envios.size(), insertados, actualizados);
            respuesta.put("archivos", archivos.stream().map(MultipartFile::getOriginalFilename).toList());
            return respuesta;
        } finally {
            eliminarTemporal(tempDir);
        }
    }

    private void validarArchivoTxt(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("El archivo esta vacio.");
        }
        String nombre = archivo.getOriginalFilename();
        if (nombre == null || !nombre.toLowerCase(Locale.ROOT).endsWith(".txt")) {
            throw new IllegalArgumentException("Solo se permiten archivos .txt.");
        }
    }

    private Path guardarTemporal(Path tempDir, MultipartFile archivo, String nombreDestino) throws IOException {
        Path destino = tempDir.resolve(nombreDestino).normalize();
        if (!destino.startsWith(tempDir)) {
            throw new IllegalArgumentException("Nombre de archivo invalido.");
        }
        Files.copy(archivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
        return destino;
    }

    private void eliminarTemporal(Path tempDir) throws IOException {
        if (tempDir == null || !Files.exists(tempDir)) return;
        try (var paths = Files.walk(tempDir)) {
            paths.sorted(Comparator.reverseOrder())
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ignored) {
                        // El archivo temporal no debe bloquear la respuesta de carga.
                    }
                });
        }
    }

    private void copiarAeropuerto(Aeropuerto origen, Aeropuerto destino) {
        destino.setCiudad(origen.getCiudad());
        destino.setPais(origen.getPais());
        destino.setContinente(origen.getContinente());
        destino.setGmt(origen.getGmt());
        destino.setCapacidadMax(origen.getCapacidadMax());
        destino.setLat(origen.getLat());
        destino.setLng(origen.getLng());
        destino.setOcupacionActual(origen.getOcupacionActual());
    }

    private void copiarVuelo(Vuelo origen, Vuelo destino) {
        destino.setDestino(origen.getDestino());
        destino.setLlegadaMinutos(origen.getLlegadaMinutos());
        destino.setCapacidadMax(origen.getCapacidadMax());
        destino.setOcupacion(origen.getOcupacion());
    }

    private Map<String, Object> respuesta(String tipo, String fuente, int procesados, int insertados, int actualizados) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("tipo", tipo);
        datos.put("fuente", fuente);
        datos.put("procesados", procesados);
        datos.put("insertados", insertados);
        datos.put("actualizados", actualizados);
        datos.put("mensaje", "Carga de " + tipo + " completada");
        return datos;
    }
}
