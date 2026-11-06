package com.boutique.backend.proveedor.entity;

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
@Table(name = "proveedor")
@Getter
@Setter
@NoArgsConstructor
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "nombre_contacto", nullable = true, length = 100)
    private String nombreContacto;

    @Column(name = "direccion", nullable = true, length = 250)
    private String direccion;

    @Column(name = "telefono", nullable = true, length = 20)
    private String telefono;

    @Column(name = "email", nullable = true, length = 100)
    private String email;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
}
