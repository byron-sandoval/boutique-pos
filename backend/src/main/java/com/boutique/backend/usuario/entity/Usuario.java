package com.boutique.backend.usuario.entity;

import com.boutique.backend.empresa.entity.Sucursal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "keycloak_id", nullable = false, length = 100, unique = true)
    private String keycloakId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @Column(name = "cedula", nullable = false, length = 20, unique = true)
    private String cedula;

    @Column(name = "nombre", nullable = false, length = 50)
    private String nombre;

    @Column(name = "apellido", nullable = true, length = 50)
    private String apellido;

    @Column(name = "telefono", nullable = true, length = 20)
    private String telefono;

    @Column(name = "email", nullable = true, length = 254)
    private String email;

    @Column(name = "username", nullable = true, length = 50)
    private String username;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
}
