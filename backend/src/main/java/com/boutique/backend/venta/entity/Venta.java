package com.boutique.backend.venta.entity;

import java.math.BigDecimal;
import java.time.Instant;

import com.boutique.backend.caja.entity.SesionCaja;
import com.boutique.backend.cliente.entity.Cliente;
import com.boutique.backend.empresa.entity.Sucursal;
import com.boutique.backend.moneda.entity.Moneda;
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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "venta", uniqueConstraints = {
    @UniqueConstraint(name = "uq_venta_factura", columnNames = {"serie_factura_id", "no_factura"}),
    @UniqueConstraint(name = "uq_venta_numero_sucursal", columnNames = {"sucursal_id", "numero_factura"})
})
@Getter
@Setter
@NoArgsConstructor
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sucursal_id", nullable = false)
    private Sucursal sucursal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "cliente_id", nullable = true)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "moneda_id", nullable = false)
    private Moneda moneda;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "serie_factura_id", nullable = false)
    private SerieFactura serieFactura;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sesion_caja_id", nullable = false)
    private SesionCaja sesionCaja;

    @Column(name = "no_factura", nullable = false)
    private Long noFactura;

    @Column(name = "numero_factura", nullable = false, length = 50)
    private String numeroFactura;

    @Column(name = "fecha", nullable = false)
    private Instant fecha = Instant.now();

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "descuento", nullable = false, precision = 12, scale = 2)
    private BigDecimal descuento = new BigDecimal("0.00");

    @Column(name = "iva", nullable = false, precision = 12, scale = 2)
    private BigDecimal iva = new BigDecimal("0.00");

    @Column(name = "total", nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", nullable = false, length = 20)
    private MetodoPago metodoPago;

    @Column(name = "tipo_cambio_aplicado", nullable = false, precision = 12, scale = 4)
    private BigDecimal tipoCambioAplicado = new BigDecimal("1.0000");

    @Column(name = "importe_recibido", nullable = true, precision = 12, scale = 2)
    private BigDecimal importeRecibido;

    @Column(name = "cambio", nullable = false, precision = 12, scale = 2)
    private BigDecimal cambio = new BigDecimal("0.00");

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoVenta estado = EstadoVenta.COMPLETADA;

    @Column(name = "motivo_anulacion", nullable = true, length = 255)
    private String motivoAnulacion;

    @Column(name = "anulada_en", nullable = true)
    private Instant anuladaEn;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "anulada_por", nullable = true)
    private Usuario anuladaPor;
}
