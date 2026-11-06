package com.boutique.backend.caja.entity;

import java.math.BigDecimal;
import java.time.Instant;

import com.boutique.backend.empresa.entity.Sucursal;
import com.boutique.backend.usuario.entity.Usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "sesion_caja")
@Getter
@Setter
@NoArgsConstructor
public class SesionCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "abierto_por_id", nullable = false)
    private Usuario abiertoPor;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "cerrado_por_id", nullable = true)
    private Usuario cerradoPor;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoCaja estado = EstadoCaja.ABIERTA;

    @Column(name = "abierto_en", nullable = false)
    private Instant abiertoEn = Instant.now();

    @Column(name = "cerrado_en", nullable = true)
    private Instant cerradoEn;

    @Column(name = "efectivo_inicial", nullable = false, precision = 14, scale = 2)
    private BigDecimal efectivoInicial = new BigDecimal("0.00");

    @Column(name = "efectivo_cierre", nullable = true, precision = 14, scale = 2)
    private BigDecimal efectivoCierre;

    @Column(name = "efectivo_esperado", nullable = true, precision = 14, scale = 2)
    private BigDecimal efectivoEsperado;

    @Column(name = "diferencia", nullable = true, precision = 14, scale = 2)
    private BigDecimal diferencia;

    @Column(name = "total_ventas", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalVentas = new BigDecimal("0.00");

    @Column(name = "total_efectivo", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalEfectivo = new BigDecimal("0.00");

    @Column(name = "total_tarjeta", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalTarjeta = new BigDecimal("0.00");

    @Column(name = "total_transferencia", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalTransferencia = new BigDecimal("0.00");

    @Column(name = "cantidad_ventas", nullable = false)
    private Integer cantidadVentas = 0;

    @Column(name = "notas", nullable = true, columnDefinition = "text")
    private String notas;
}
