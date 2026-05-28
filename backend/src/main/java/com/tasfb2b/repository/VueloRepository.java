package com.tasfb2b.repository;

import com.tasfb2b.model.Vuelo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VueloRepository extends JpaRepository<Vuelo, Long> {
    Optional<Vuelo> findFirstByOrigenAndDestinoAndSalidaMinutos(String origen, String destino, int salidaMinutos);
}
