package com.tasfb2b.repository;

import com.tasfb2b.model.Ruta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RutaRepository extends JpaRepository<Ruta, Long> {
    List<Ruta> findBySimulacionId(Long simulacionId);
    List<Ruta> findTop300BySimulacionIdOrderByCumplimientoDesc(Long simulacionId);
    Optional<Ruta> findBySimulacionIdAndEnvioId(Long simulacionId, String envioId);
}
