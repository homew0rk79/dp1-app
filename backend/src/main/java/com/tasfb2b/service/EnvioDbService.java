package com.tasfb2b.service;

import com.tasfb2b.model.Envio;
import com.tasfb2b.repository.EnvioRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EnvioDbService {

    private final EnvioRepository envioRepository;

    public EnvioDbService(EnvioRepository envioRepository) {
        this.envioRepository = envioRepository;
    }

    public List<Envio> listarTodos() {
        return envioRepository.findAll();
    }

    public Envio guardar(Envio envio) {
        return envioRepository.save(envio);
    }

    public List<Envio> buscarPorRangoFecha(LocalDateTime desde, LocalDateTime hasta) {
        return envioRepository.findByFechaHoraRegistroGreaterThanEqualAndFechaHoraRegistroLessThan(desde, hasta);
    }

    public long contarPorRangoFecha(LocalDateTime desde, LocalDateTime hasta) {
        return envioRepository.countByFechaHoraRegistroGreaterThanEqualAndFechaHoraRegistroLessThan(desde, hasta);
    }

    public long contarGlobales() {
        return envioRepository.countByIdOriginalIsNotNull();
    }

    public List<Envio> listarGlobales() {
        return envioRepository.findByIdOriginalIsNotNull();
    }
}