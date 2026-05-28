package com.tasfb2b.service;

import com.tasfb2b.model.Envio;
import com.tasfb2b.repository.EnvioRepository;
import org.springframework.stereotype.Service;

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
}