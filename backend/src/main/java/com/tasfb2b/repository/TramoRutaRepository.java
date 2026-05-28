package com.tasfb2b.repository;

import com.tasfb2b.model.TramoRuta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TramoRutaRepository extends JpaRepository<TramoRuta, Long> {
    List<TramoRuta> findByRutaIdOrderByOrdenAsc(Long rutaId);
    List<TramoRuta> findByRutaSimulacionId(Long simulacionId);
}
