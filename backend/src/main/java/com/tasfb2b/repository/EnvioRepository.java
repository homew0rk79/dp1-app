package com.tasfb2b.repository;

import com.tasfb2b.model.Envio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EnvioRepository extends JpaRepository<Envio, String> {
    List<Envio> findByFechaHoraRegistroGreaterThanEqualAndFechaHoraRegistroLessThan(
        LocalDateTime desde, LocalDateTime hasta);

    long countByFechaHoraRegistroGreaterThanEqualAndFechaHoraRegistroLessThan(
        LocalDateTime desde, LocalDateTime hasta);

    long countByIdOriginalIsNotNull();

    List<Envio> findByIdOriginalIsNotNull();

    Optional<Envio> findFirstByIdOriginal(String idOriginal);

    boolean existsByIdOriginal(String idOriginal);

    Optional<Envio> findByOrigenAndIdOriginal(String origen, String idOriginal);

    boolean existsByOrigenAndIdOriginal(String origen, String idOriginal);

    @Query("select distinct e.origen from Envio e where e.origen is not null order by e.origen")
    List<String> findDistinctOrigenes();
}
