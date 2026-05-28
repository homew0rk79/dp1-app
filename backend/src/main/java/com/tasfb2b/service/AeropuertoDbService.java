package com.tasfb2b.service;

import com.tasfb2b.model.Aeropuerto;
import com.tasfb2b.repository.AeropuertoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

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
}