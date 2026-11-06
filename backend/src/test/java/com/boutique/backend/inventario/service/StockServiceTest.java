package com.boutique.backend.inventario.service;

import com.boutique.backend.inventario.repository.InventarioRepository;
import com.boutique.backend.shared.exception.ConflictoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {
    @Mock InventarioRepository repository;

    @Test
    void aceptaSumaAtomica() {
        when(repository.sumarStock(4L, 1L, 5)).thenReturn(1);
        assertDoesNotThrow(() -> new StockService(repository).ingresar(4L, 1L, 5));
    }

    @Test
    void comunicaDesbordamientoDeStock() {
        when(repository.sumarStock(4L, 1L, 5)).thenReturn(0);
        assertThrows(ConflictoException.class, () -> new StockService(repository).ingresar(4L, 1L, 5));
    }

    @Test
    void aceptaRetiroAtomico() {
        when(repository.restarStock(4L, 1L, 5)).thenReturn(1);
        assertDoesNotThrow(() -> new StockService(repository).retirar(4L, 1L, 5));
    }

    @Test
    void comunicaStockInsuficiente() {
        when(repository.restarStock(4L, 1L, 5)).thenReturn(0);
        assertThrows(ConflictoException.class, () -> new StockService(repository).retirar(4L, 1L, 5));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void noPermiteInvertirElSentidoDelMovimiento(int cantidad) {
        StockService service = new StockService(repository);
        assertThrows(ConflictoException.class, () -> service.ingresar(4L, 1L, cantidad));
        assertThrows(ConflictoException.class, () -> service.retirar(4L, 1L, cantidad));
        verifyNoInteractions(repository);
    }
}
