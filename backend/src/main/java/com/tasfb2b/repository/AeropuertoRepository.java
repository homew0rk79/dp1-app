package com.tasfb2b.repository;

import com.tasfb2b.model.Aeropuerto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AeropuertoRepository extends JpaRepository<Aeropuerto, Long> {
    Optional<Aeropuerto> findByCodigo(String codigo);
}
