package com.tasfb2b.repository;

import com.tasfb2b.model.TramoMantenimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TramoMantenimientoRepository extends JpaRepository<TramoMantenimiento, Long> {
    Optional<TramoMantenimiento> findByUtAsignada(String utAsignada);
}
