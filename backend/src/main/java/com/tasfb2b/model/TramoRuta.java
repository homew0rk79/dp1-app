package com.tasfb2b.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tramo_ruta")
@Getter
@Setter
@NoArgsConstructor
public class TramoRuta {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tramo_ruta_seq")
    @SequenceGenerator(name = "tramo_ruta_seq", sequenceName = "tramo_ruta_id_seq", allocationSize = 500)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ruta_id")
    private Ruta ruta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vuelo_id")
    private Vuelo vuelo;

    private Long idRuta;
    private Long idVuelo;

    private Integer orden = 0;
    private Integer horaSalidaProgramada = 0;
    private Integer horaLlegadaProgramada = 0;
    private Integer salidaAbs = 0;
    private Integer llegadaAbs = 0;
    private Integer capacidadReservada = 0;
    private String estado;
}
