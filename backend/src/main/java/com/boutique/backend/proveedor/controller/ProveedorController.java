package com.boutique.backend.proveedor.controller;

import java.net.URI;

import com.boutique.backend.proveedor.dto.ProveedorEstadoRequest;
import com.boutique.backend.proveedor.dto.ProveedorRequest;
import com.boutique.backend.proveedor.dto.ProveedorResponse;
import com.boutique.backend.proveedor.service.ProveedorService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/proveedores")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class ProveedorController {

    private final ProveedorService proveedorService;

    @GetMapping
    public PagedModel<ProveedorResponse> listar(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return new PagedModel<>(proveedorService.listar(pageable));
    }

    @GetMapping("/{id}")
    public ProveedorResponse obtenerPorId(@PathVariable @Positive Long id) {
        return proveedorService.obtenerPorId(id);
    }

    @PostMapping
    public ResponseEntity<ProveedorResponse> crear(@Valid @RequestBody ProveedorRequest request) {
        ProveedorResponse response = proveedorService.crear(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public ProveedorResponse actualizar(@PathVariable @Positive Long id, @Valid @RequestBody ProveedorRequest request) {
        return proveedorService.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    public ProveedorResponse cambiarEstado(@PathVariable @Positive Long id, @Valid @RequestBody ProveedorEstadoRequest request) {
        return proveedorService.cambiarEstado(id, request.activo());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable @Positive Long id) {
        proveedorService.cambiarEstado(id, false);
        return ResponseEntity.noContent().build();
    }
}
