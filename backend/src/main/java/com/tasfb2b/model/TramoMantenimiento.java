package com.tasfb2b.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "tramo_mantenimiento")
@Getter
@Setter
@NoArgsConstructor
public class TramoMantenimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String utAsignada;
    private String origen;
    private String destino;
    private LocalDateTime horaSalida;
    private LocalDateTime horaLlegada;
    private String estado;
}
