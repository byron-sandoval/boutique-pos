package com.boutique.backend.cliente.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cliente")
@Getter
@Setter
@NoArgsConstructor
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "cedula", nullable = true, length = 20, unique = true)
    private String cedula;

    @Column(name = "pasaporte", nullable = true, length = 30, unique = true)
    private String pasaporte;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "direccion", nullable = true, length = 255)
    private String direccion;

    @Column(name = "telefono", nullable = true, length = 20)
    private String telefono;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
}
