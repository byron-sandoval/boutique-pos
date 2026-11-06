package com.boutique.backend.inventario.service;

import com.boutique.backend.inventario.repository.InventarioRepository;
import com.boutique.backend.shared.exception.ConflictoException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class StockService {

    private final InventarioRepository inventarioRepository;

    public void ingresar(Long varianteId, Long sucursalId, int cantidad) {
        validarCantidad(cantidad);
        if (inventarioRepository.sumarStock(varianteId, sucursalId, cantidad) != 1) {
            throw new ConflictoException("La cantidad supera el stock permitido para la variante " + varianteId + ".");
        }
    }

    public void retirar(Long varianteId, Long sucursalId, int cantidad) {
        validarCantidad(cantidad);
        if (inventarioRepository.restarStock(varianteId, sucursalId, cantidad) != 1) {
            throw new ConflictoException("Stock insuficiente para la variante " + varianteId + ".");
        }
    }

    private void validarCantidad(int cantidad) {
        if (cantidad <= 0) {
            throw new ConflictoException("La cantidad del movimiento debe ser mayor que cero.");
        }
    }
}
