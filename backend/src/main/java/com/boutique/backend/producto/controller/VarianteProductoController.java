package com.boutique.backend.producto.controller;

import java.net.URI;

import com.boutique.backend.producto.dto.EstadoCatalogoRequest;
import com.boutique.backend.producto.dto.VarianteProductoRequest;
import com.boutique.backend.producto.dto.VarianteProductoResponse;
import com.boutique.backend.producto.service.VarianteProductoService;
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
@RequestMapping("/api/variantes-producto")
@RequiredArgsConstructor
public class VarianteProductoController {

    private final VarianteProductoService varianteProductoService;

    @GetMapping
    public PagedModel<VarianteProductoResponse> listar(
        @RequestParam(required = false) @Positive Long productoId,
        @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        Page<VarianteProductoResponse> page = productoId == null
            ? varianteProductoService.listar(pageable)
            : varianteProductoService.listarPorProducto(productoId, pageable);
        return new PagedModel<>(page);
    }

    @GetMapping("/{id}")
    public VarianteProductoResponse obtenerPorId(@PathVariable @Positive Long id) {
        return varianteProductoService.obtenerPorId(id);
    }

    @PostMapping
    public ResponseEntity<VarianteProductoResponse> crear(@Valid @RequestBody VarianteProductoRequest request) {
        VarianteProductoResponse response = varianteProductoService.crear(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public VarianteProductoResponse actualizar(@PathVariable @Positive Long id, @Valid @RequestBody VarianteProductoRequest request) {
        return varianteProductoService.actualizar(id, request);
    }


    @PatchMapping("/{id}/estado")
    public VarianteProductoResponse cambiarEstado(@PathVariable @Positive Long id, @Valid @RequestBody EstadoCatalogoRequest request) {
        return varianteProductoService.cambiarEstado(id, request.activo());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable @Positive Long id) {
        varianteProductoService.cambiarEstado(id, false);
        return ResponseEntity.noContent().build();
    }
}
