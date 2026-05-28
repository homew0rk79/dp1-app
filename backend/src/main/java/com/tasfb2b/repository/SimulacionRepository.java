package com.tasfb2b.repository;

import com.tasfb2b.model.Simulacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SimulacionRepository extends JpaRepository<Simulacion, Long> {
    Optional<Simulacion> findTopByOrderByFechaCreacionDesc();
    Optional<Simulacion> findTopByEstadoOrderByFechaCreacionDesc(String estado);
}
