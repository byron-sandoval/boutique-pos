package com.boutique.backend.producto.controller;

import java.net.URI;

import com.boutique.backend.producto.dto.EstadoCatalogoRequest;
import com.boutique.backend.producto.dto.ProductoRequest;
import com.boutique.backend.producto.dto.ProductoResponse;
import com.boutique.backend.producto.service.ProductoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService productoService;

    @GetMapping
    public PagedModel<ProductoResponse> listar(
        @RequestParam(required = false) @Positive Long categoriaId,
        @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        Page<ProductoResponse> page = categoriaId == null
            ? productoService.listar(pageable)
            : productoService.listarPorCategoria(categoriaId, pageable);
        return new PagedModel<>(page);
    }

    @GetMapping("/{id}")
    public ProductoResponse obtenerPorId(@PathVariable @Positive Long id) {
        return productoService.obtenerPorId(id);
    }

    @PostMapping
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest request) {
        ProductoResponse response = productoService.crear(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public ProductoResponse actualizar(@PathVariable @Positive Long id, @Valid @RequestBody ProductoRequest request) {
        return productoService.actualizar(id, request);
    }


    @PatchMapping("/{id}/estado")
    public ProductoResponse cambiarEstado(@PathVariable @Positive Long id, @Valid @RequestBody EstadoCatalogoRequest request) {
        return productoService.cambiarEstado(id, request.activo());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable @Positive Long id) {
        productoService.cambiarEstado(id, false);
        return ResponseEntity.noContent().build();
    }
}
