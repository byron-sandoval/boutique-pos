package com.boutique.backend.inventario.entity;

import java.math.BigDecimal;
import java.time.Instant;

import com.boutique.backend.empresa.entity.Sucursal;
import com.boutique.backend.proveedor.entity.Proveedor;
import com.boutique.backend.usuario.entity.Usuario;

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
@Table(name = "ingreso")
@Getter
@Setter
@NoArgsConstructor
public class Ingreso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "proveedor_id", nullable = true)
    private Proveedor proveedor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha", nullable = false)
    private Instant fecha = Instant.now();

    @Column(name = "no_documento", nullable = false, length = 50)
    private String noDocumento;

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "observaciones", nullable = true, length = 255)
    private String observaciones;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
}
