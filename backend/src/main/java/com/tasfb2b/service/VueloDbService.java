package com.tasfb2b.service;

import com.tasfb2b.model.Vuelo;
import com.tasfb2b.repository.VueloRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VueloDbService {

    private final VueloRepository vueloRepository;

    public VueloDbService(VueloRepository vueloRepository) {
        this.vueloRepository = vueloRepository;
    }

    public List<Vuelo> listarTodos() {
        return vueloRepository.findAll();
    }

    public Vuelo guardar(Vuelo vuelo) {
        return vueloRepository.save(vuelo);
    }
}