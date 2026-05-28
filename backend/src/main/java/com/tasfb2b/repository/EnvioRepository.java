package com.tasfb2b.repository;

import com.tasfb2b.model.Envio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface EnvioRepository extends JpaRepository<Envio, String> {
    List<Envio> findByFechaHoraRegistroGreaterThanEqualAndFechaHoraRegistroLessThan(
        LocalDateTime desde, LocalDateTime hasta);

    long countByFechaHoraRegistroGreaterThanEqualAndFechaHoraRegistroLessThan(
        LocalDateTime desde, LocalDateTime hasta);

    long countByIdOriginalIsNotNull();

    List<Envio> findByIdOriginalIsNotNull();
}
