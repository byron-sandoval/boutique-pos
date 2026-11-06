package com.boutique.backend.venta.entity;

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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "serie_factura", uniqueConstraints = {
    @UniqueConstraint(name = "uq_serie_factura_prefijo", columnNames = {"sucursal_id", "prefijo"})
})
@Getter
@Setter
@NoArgsConstructor
public class SerieFactura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @Column(name = "prefijo", nullable = false, length = 20)
    private String prefijo = "FAC-";

    @Column(name = "digitos", nullable = false)
    private Integer digitos = 6;

    @Column(name = "siguiente_numero", nullable = false)
    private Long siguienteNumero = 1L;

    @Column(name = "activa", nullable = false)
    private Boolean activa = true;
}
