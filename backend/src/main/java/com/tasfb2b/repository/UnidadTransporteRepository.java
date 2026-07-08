package com.tasfb2b.repository;

import com.tasfb2b.model.UnidadTransporte;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UnidadTransporteRepository extends JpaRepository<UnidadTransporte, Long> {
    Optional<UnidadTransporte> findByCodigo(String codigo);
}
