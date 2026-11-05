CREATE TABLE empresa (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 nombre VARCHAR(100) NOT NULL,
 ruc VARCHAR(20) UNIQUE,
 regimen_fiscal VARCHAR(100),
 direccion VARCHAR(255) NOT NULL,
 telefono VARCHAR(20),
 correo VARCHAR(100),
 logo_url VARCHAR(500),
 eslogan VARCHAR(150),
 porcentaje_iva NUMERIC(5,2) NOT NULL DEFAULT 15.00,
 CONSTRAINT chk_empresa_iva CHECK (porcentaje_iva BETWEEN 0 AND 100)
);

CREATE TABLE sucursal (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 empresa_id BIGINT NOT NULL REFERENCES empresa(id),
 nombre VARCHAR(100) NOT NULL,
 direccion VARCHAR(255) NOT NULL,
 telefono VARCHAR(20),
 activa BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE usuario (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 keycloak_id VARCHAR(100) NOT NULL UNIQUE,
 sucursal_id BIGINT NOT NULL REFERENCES sucursal(id),
 cedula VARCHAR(20) NOT NULL UNIQUE,
 nombre VARCHAR(50) NOT NULL,
 apellido VARCHAR(50),
 telefono VARCHAR(20),
 email VARCHAR(254),
 username VARCHAR(50),
 activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE cliente (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 cedula VARCHAR(20) UNIQUE,
 pasaporte VARCHAR(30) UNIQUE,
 nombre VARCHAR(100) NOT NULL,
 direccion VARCHAR(255),
 telefono VARCHAR(20),
 activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE proveedor (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 nombre VARCHAR(100) NOT NULL,
 nombre_contacto VARCHAR(100),
 direccion VARCHAR(250),
 telefono VARCHAR(20),
 email VARCHAR(100),
 activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE categoria (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 nombre VARCHAR(50) NOT NULL UNIQUE,
 descripcion VARCHAR(255),
 activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE unidad_medida (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 nombre VARCHAR(30) NOT NULL UNIQUE,
 simbolo VARCHAR(10),
 activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE producto (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 categoria_id BIGINT NOT NULL REFERENCES categoria(id),
 unidad_medida_id BIGINT NOT NULL REFERENCES unidad_medida(id),
 codigo VARCHAR(50) NOT NULL UNIQUE,
 nombre VARCHAR(100) NOT NULL,
 descripcion VARCHAR(255),
 marca VARCHAR(100),
 precio NUMERIC(12,2) NOT NULL,
 costo NUMERIC(12,2) NOT NULL,
 ultimo_costo NUMERIC(12,2),
 imagen_url VARCHAR(500),
 activo BOOLEAN NOT NULL DEFAULT TRUE,
 CONSTRAINT chk_producto_precio CHECK (precio >= 0),
 CONSTRAINT chk_producto_costo CHECK (costo >= 0),
 CONSTRAINT chk_producto_ultimo_costo CHECK (ultimo_costo IS NULL OR ultimo_costo >= 0)
);

CREATE TABLE variante_producto (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 producto_id BIGINT NOT NULL REFERENCES producto(id),
 talla VARCHAR(20),
 color VARCHAR(50),
 sku VARCHAR(50) NOT NULL UNIQUE,
 codigo_barras VARCHAR(50) UNIQUE,
 activo BOOLEAN NOT NULL DEFAULT TRUE,
 CONSTRAINT uq_variante_producto UNIQUE NULLS NOT DISTINCT (producto_id, talla, color)
);

CREATE TABLE inventario (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 variante_id BIGINT NOT NULL REFERENCES variante_producto(id),
 sucursal_id BIGINT NOT NULL REFERENCES sucursal(id),
 stock INTEGER NOT NULL DEFAULT 0,
 stock_minimo INTEGER NOT NULL DEFAULT 0,
 stock_maximo INTEGER,
 CONSTRAINT uq_inventario_variante_sucursal UNIQUE (variante_id, sucursal_id),
 CONSTRAINT chk_inventario_stock CHECK (stock >= 0),
 CONSTRAINT chk_inventario_minimo CHECK (stock_minimo >= 0),
 CONSTRAINT chk_inventario_maximo CHECK (stock_maximo IS NULL OR stock_maximo >= stock_minimo)
);

CREATE TABLE moneda (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 codigo VARCHAR(3) NOT NULL UNIQUE,
 nombre VARCHAR(50) NOT NULL,
 simbolo VARCHAR(5) NOT NULL,
 activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE serie_factura (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 sucursal_id BIGINT NOT NULL REFERENCES sucursal(id),
 prefijo VARCHAR(20) NOT NULL DEFAULT 'FAC-',
 digitos INTEGER NOT NULL DEFAULT 6,
 siguiente_numero BIGINT NOT NULL DEFAULT 1,
 activa BOOLEAN NOT NULL DEFAULT TRUE,
 CONSTRAINT uq_serie_factura_prefijo UNIQUE (sucursal_id, prefijo),
 CONSTRAINT chk_serie_factura_digitos CHECK (digitos BETWEEN 1 AND 15),
 CONSTRAINT chk_serie_factura_numero CHECK (siguiente_numero > 0)
);

CREATE TABLE sesion_caja (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 sucursal_id BIGINT NOT NULL REFERENCES sucursal(id),
 abierto_por_id BIGINT NOT NULL REFERENCES usuario(id),
 cerrado_por_id BIGINT REFERENCES usuario(id),
 estado VARCHAR(20) NOT NULL DEFAULT 'ABIERTA',
 abierto_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 cerrado_en TIMESTAMPTZ,
 efectivo_inicial NUMERIC(14,2) NOT NULL DEFAULT 0.00,
 efectivo_cierre NUMERIC(14,2),
 efectivo_esperado NUMERIC(14,2),
 diferencia NUMERIC(14,2),
 total_ventas NUMERIC(14,2) NOT NULL DEFAULT 0.00,
 total_efectivo NUMERIC(14,2) NOT NULL DEFAULT 0.00,
 total_tarjeta NUMERIC(14,2) NOT NULL DEFAULT 0.00,
 total_transferencia NUMERIC(14,2) NOT NULL DEFAULT 0.00,
 cantidad_ventas INTEGER NOT NULL DEFAULT 0,
 notas TEXT,
 CONSTRAINT chk_caja_estado CHECK (estado IN ('ABIERTA','CERRADA')),
 CONSTRAINT chk_caja_montos CHECK (
  efectivo_inicial >= 0 AND total_ventas >= 0 AND total_efectivo >= 0
  AND total_tarjeta >= 0 AND total_transferencia >= 0 AND cantidad_ventas >= 0
  AND (efectivo_cierre IS NULL OR efectivo_cierre >= 0)
  AND (efectivo_esperado IS NULL OR efectivo_esperado >= 0)
 ),
 CONSTRAINT chk_caja_cierre CHECK (
  (estado = 'ABIERTA' AND cerrado_en IS NULL AND cerrado_por_id IS NULL
   AND efectivo_cierre IS NULL AND efectivo_esperado IS NULL AND diferencia IS NULL)
  OR
  (estado = 'CERRADA' AND cerrado_en IS NOT NULL AND cerrado_por_id IS NOT NULL
   AND efectivo_cierre IS NOT NULL AND efectivo_esperado IS NOT NULL AND diferencia IS NOT NULL)
 )
);

CREATE UNIQUE INDEX uq_caja_abierta_usuario
 ON sesion_caja (abierto_por_id) WHERE estado = 'ABIERTA';

CREATE TABLE venta (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 sucursal_id BIGINT NOT NULL REFERENCES sucursal(id),
 usuario_id BIGINT NOT NULL REFERENCES usuario(id),
 cliente_id BIGINT REFERENCES cliente(id),
 moneda_id BIGINT NOT NULL REFERENCES moneda(id),
 serie_factura_id BIGINT NOT NULL REFERENCES serie_factura(id),
 sesion_caja_id BIGINT NOT NULL REFERENCES sesion_caja(id),
 no_factura BIGINT NOT NULL,
 numero_factura VARCHAR(50) NOT NULL,
 fecha TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 subtotal NUMERIC(12,2) NOT NULL,
 descuento NUMERIC(12,2) NOT NULL DEFAULT 0.00,
 iva NUMERIC(12,2) NOT NULL DEFAULT 0.00,
 total NUMERIC(12,2) NOT NULL,
 metodo_pago VARCHAR(20) NOT NULL,
 tipo_cambio_aplicado NUMERIC(12,4) NOT NULL DEFAULT 1.0000,
 importe_recibido NUMERIC(12,2),
 cambio NUMERIC(12,2) NOT NULL DEFAULT 0.00,
 estado VARCHAR(20) NOT NULL DEFAULT 'COMPLETADA',
 motivo_anulacion VARCHAR(255),
 anulada_en TIMESTAMPTZ,
 anulada_por BIGINT REFERENCES usuario(id),
 CONSTRAINT uq_venta_factura UNIQUE (serie_factura_id, no_factura),
 CONSTRAINT uq_venta_numero_sucursal UNIQUE (sucursal_id, numero_factura),
 CONSTRAINT chk_venta_no_factura CHECK (no_factura > 0),
 CONSTRAINT chk_venta_metodo_pago CHECK (metodo_pago IN ('EFECTIVO','TRANSFERENCIA','TARJETA')),
 CONSTRAINT chk_venta_estado CHECK (estado IN ('COMPLETADA','ANULADA')),
 CONSTRAINT chk_venta_montos CHECK (
  subtotal >= 0 AND descuento >= 0 AND descuento <= subtotal AND iva >= 0
  AND total >= 0 AND cambio >= 0 AND (importe_recibido IS NULL OR importe_recibido >= 0)
 ),
 CONSTRAINT chk_venta_tipo_cambio CHECK (tipo_cambio_aplicado > 0),
 CONSTRAINT chk_venta_anulacion CHECK (
  (estado = 'COMPLETADA' AND anulada_en IS NULL AND anulada_por IS NULL)
  OR (estado = 'ANULADA' AND anulada_en IS NOT NULL AND anulada_por IS NOT NULL)
 )
);

CREATE TABLE detalle_venta (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 venta_id BIGINT NOT NULL REFERENCES venta(id),
 variante_id BIGINT NOT NULL REFERENCES variante_producto(id),
 cantidad INTEGER NOT NULL,
 precio_unitario NUMERIC(12,2) NOT NULL,
 descuento NUMERIC(12,2) NOT NULL DEFAULT 0.00,
 monto NUMERIC(12,2) NOT NULL,
 CONSTRAINT chk_detalle_venta_cantidad CHECK (cantidad > 0),
 CONSTRAINT chk_detalle_venta_montos CHECK (
  precio_unitario >= 0 AND descuento >= 0
  AND descuento <= cantidad * precio_unitario AND monto >= 0
 )
);

CREATE TABLE ingreso (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 sucursal_id BIGINT NOT NULL REFERENCES sucursal(id),
 proveedor_id BIGINT REFERENCES proveedor(id),
 usuario_id BIGINT NOT NULL REFERENCES usuario(id),
 fecha TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 no_documento VARCHAR(50) NOT NULL,
 total NUMERIC(12,2) NOT NULL,
 observaciones VARCHAR(255),
 activo BOOLEAN NOT NULL DEFAULT TRUE,
 CONSTRAINT chk_ingreso_total CHECK (total >= 0)
);

CREATE TABLE detalle_ingreso (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 ingreso_id BIGINT NOT NULL REFERENCES ingreso(id),
 variante_id BIGINT NOT NULL REFERENCES variante_producto(id),
 cantidad INTEGER NOT NULL,
 costo_unitario NUMERIC(12,2) NOT NULL,
 monto NUMERIC(12,2) NOT NULL,
 CONSTRAINT chk_detalle_ingreso_cantidad CHECK (cantidad > 0),
 CONSTRAINT chk_detalle_ingreso_montos CHECK (costo_unitario >= 0 AND monto >= 0)
);

CREATE TABLE devolucion (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 venta_id BIGINT NOT NULL REFERENCES venta(id),
 usuario_id BIGINT NOT NULL REFERENCES usuario(id),
 fecha TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 motivo VARCHAR(255),
 total NUMERIC(12,2) NOT NULL DEFAULT 0.00,
 CONSTRAINT chk_devolucion_total CHECK (total >= 0)
);

CREATE TABLE detalle_devolucion (
 id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 devolucion_id BIGINT NOT NULL REFERENCES devolucion(id),
 detalle_venta_id BIGINT NOT NULL REFERENCES detalle_venta(id),
 cantidad INTEGER NOT NULL,
 precio_unitario NUMERIC(12,2) NOT NULL,
 monto_total NUMERIC(12,2) NOT NULL,
 regresa_inventario BOOLEAN NOT NULL DEFAULT TRUE,
 CONSTRAINT chk_detalle_devolucion_cantidad CHECK (cantidad > 0),
 CONSTRAINT chk_detalle_devolucion_montos CHECK (precio_unitario >= 0 AND monto_total >= 0)
);
