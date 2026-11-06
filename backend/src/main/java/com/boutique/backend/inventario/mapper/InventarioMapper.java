package com.boutique.backend.inventario.mapper;

import com.boutique.backend.empresa.entity.Sucursal;
import com.boutique.backend.inventario.dto.InventarioCreateRequest;
import com.boutique.backend.inventario.dto.InventarioLimitesRequest;
import com.boutique.backend.inventario.dto.InventarioResponse;
import com.boutique.backend.inventario.entity.Inventario;
import com.boutique.backend.producto.entity.VarianteProducto;
import org.springframework.stereotype.Component;

@Component
public class InventarioMapper {

    public Inventario toEntity(InventarioCreateRequest request, VarianteProducto variante, Sucursal sucursal) {
        Inventario inventario = new Inventario();
        inventario.setVariante(variante);
        inventario.setSucursal(sucursal);
        inventario.setStockMinimo(request.stockMinimo());
        inventario.setStockMaximo(request.stockMaximo());
        return inventario;
    }

    public void updateLimites(InventarioLimitesRequest request, Inventario inventario) {
        inventario.setStockMinimo(request.stockMinimo());
        inventario.setStockMaximo(request.stockMaximo());
    }

    public InventarioResponse toResponse(Inventario inventario) {
        return new InventarioResponse(inventario.getId(), inventario.getVariante().getId(),
            inventario.getSucursal().getId(), inventario.getStock(), inventario.getStockMinimo(),
            inventario.getStockMaximo());
    }
}
