package com.boutique.backend.empresa.entity;

import java.math.BigDecimal;

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
@Table(name = "empresa")
@Getter
@Setter
@NoArgsConstructor
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "ruc", nullable = true, length = 20, unique = true)
    private String ruc;

    @Column(name = "regimen_fiscal", nullable = true, length = 100)
    private String regimenFiscal;

    @Column(name = "direccion", nullable = false, length = 255)
    private String direccion;

    @Column(name = "telefono", nullable = true, length = 20)
    private String telefono;

    @Column(name = "correo", nullable = true, length = 100)
    private String correo;

    @Column(name = "logo_url", nullable = true, length = 500)
    private String logoUrl;

    @Column(name = "eslogan", nullable = true, length = 150)
    private String eslogan;

    @Column(name = "porcentaje_iva", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeIva = new BigDecimal("15.00");
}
