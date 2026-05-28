package com.tasfb2b.service;

import com.tasfb2b.model.TramoRuta;
import com.tasfb2b.repository.TramoRutaRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TramoRutaDbService {

    private final TramoRutaRepository tramoRutaRepository;

    public TramoRutaDbService(TramoRutaRepository tramoRutaRepository) {
        this.tramoRutaRepository = tramoRutaRepository;
    }

    public List<TramoRuta> listarTodos() {
        return tramoRutaRepository.findAll();
    }

    public TramoRuta guardar(TramoRuta tramoRuta) {
        return tramoRutaRepository.save(tramoRuta);
    }
}