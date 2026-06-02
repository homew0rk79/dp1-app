package com.tasfb2b.service;

import com.tasfb2b.model.Simulacion;
import com.tasfb2b.repository.SimulacionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SimulacionDbService {

    private final SimulacionRepository simulacionRepository;

    public SimulacionDbService(SimulacionRepository simulacionRepository) {
        this.simulacionRepository = simulacionRepository;
    }

    public List<Simulacion> listarTodos() {
        return simulacionRepository.findAll();
    }

    public Simulacion guardar(Simulacion simulacion) {
        return simulacionRepository.save(simulacion);
    }

    public Optional<Simulacion> buscarUltima() {
        return simulacionRepository.findTopByOrderByFechaCreacionDesc();
    }

    public Optional<Simulacion> buscarUltimaCompletada(String estado) {
        return simulacionRepository.findTopByEstadoOrderByFechaCreacionDesc(estado);
    }
}
