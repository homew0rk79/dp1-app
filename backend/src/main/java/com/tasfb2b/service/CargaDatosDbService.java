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

import java.util.List;

import java.io.IOException;
import java.util.Map;

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
}
