package com.tasfb2b.service;

import com.tasfb2b.model.Ruta;
import com.tasfb2b.repository.RutaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RutaDbService {

    private final RutaRepository rutaRepository;

    public RutaDbService(RutaRepository rutaRepository) {
        this.rutaRepository = rutaRepository;
    }

    public List<Ruta> listarTodos() {
        return rutaRepository.findAll();
    }

    public Ruta guardar(Ruta ruta) {
        return rutaRepository.save(ruta);
    }

    public List<Ruta> listarPorSimulacion(Long simulacionId) {
        return rutaRepository.findBySimulacionId(simulacionId);
    }

    public List<Ruta> listarTop300PorSimulacion(Long simulacionId) {
        return rutaRepository.findTop300BySimulacionIdOrderByCumplimientoDesc(simulacionId);
    }

    public Optional<Ruta> buscarPorSimulacionYEnvio(Long simulacionId, String envioId) {
        return rutaRepository.findBySimulacionIdAndEnvioId(simulacionId, envioId);
    }
}