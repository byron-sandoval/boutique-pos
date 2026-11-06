package com.boutique.backend.venta.controller;

import com.boutique.backend.venta.dto.DevolucionRequest;
import com.boutique.backend.venta.dto.DevolucionResponse;
import com.boutique.backend.venta.dto.DevolucionResumenResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/devoluciones")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class DevolucionController {

    private static final String PENDIENTE = "El funcionamiento de devoluciones está pendiente de definir.";

    @GetMapping
    public PagedModel<DevolucionResumenResponse> listar(@RequestParam(required = false) @Positive Long ventaId,
        @RequestParam(required = false) @Positive Long sucursalId,
        @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, PENDIENTE);
    }

    @GetMapping("/{id}")
    public DevolucionResponse obtenerPorId(@PathVariable @Positive Long id) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, PENDIENTE);
    }

    @PostMapping
    public ResponseEntity<DevolucionResponse> crear(@Valid @RequestBody DevolucionRequest request) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, PENDIENTE);
    }

    @PutMapping("/{id}")
    public DevolucionResponse actualizar(@PathVariable @Positive Long id, @Valid @RequestBody DevolucionRequest request) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, PENDIENTE);
    }

    // El cuerpo de la edición parcial se definirá junto con las reglas de devoluciones.
    @PatchMapping("/{id}")
    public DevolucionResponse actualizarParcialmente(@PathVariable @Positive Long id) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, PENDIENTE);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable @Positive Long id) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, PENDIENTE);
    }
}
