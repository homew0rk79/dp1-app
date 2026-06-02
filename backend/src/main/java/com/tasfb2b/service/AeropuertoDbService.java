package com.tasfb2b.service;

import com.tasfb2b.model.Aeropuerto;
import com.tasfb2b.repository.AeropuertoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AeropuertoDbService {

    private final AeropuertoRepository aeropuertoRepository;

    public AeropuertoDbService(AeropuertoRepository aeropuertoRepository) {
        this.aeropuertoRepository = aeropuertoRepository;
    }

    public List<Aeropuerto> listarTodos() {
        return aeropuertoRepository.findAll();
    }

    public Aeropuerto guardar(Aeropuerto aeropuerto) {
        return aeropuertoRepository.save(aeropuerto);
    }

    public Optional<Aeropuerto> buscarPorCodigo(String codigo) {
        return aeropuertoRepository.findByCodigo(codigo);
    }
}