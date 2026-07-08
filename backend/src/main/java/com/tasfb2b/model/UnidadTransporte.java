package com.tasfb2b.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "unidad_transporte")
@Getter
@Setter
@NoArgsConstructor
public class UnidadTransporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo;

    private String tipo;
    private String ubicacionActual;
    private int capacidadMax;
    private String estado;
}
