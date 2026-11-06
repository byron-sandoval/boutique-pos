package com.boutique.backend.producto.controller;

import java.net.URI;

import com.boutique.backend.producto.dto.EstadoCatalogoRequest;
import com.boutique.backend.producto.dto.CategoriaRequest;
import com.boutique.backend.producto.dto.CategoriaResponse;
import com.boutique.backend.producto.service.CategoriaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/categorias")
@RequiredArgsConstructor
public class CategoriaController {

    private final CategoriaService categoriaService;

    @GetMapping
    public PagedModel<CategoriaResponse> listar(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return new PagedModel<>(categoriaService.listar(pageable));
    }

    @GetMapping("/{id}")
    public CategoriaResponse obtenerPorId(@PathVariable @Positive Long id) {
        return categoriaService.obtenerPorId(id);
    }

    @PostMapping
    public ResponseEntity<CategoriaResponse> crear(@Valid @RequestBody CategoriaRequest request) {
        CategoriaResponse response = categoriaService.crear(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public CategoriaResponse actualizar(@PathVariable @Positive Long id, @Valid @RequestBody CategoriaRequest request) {
        return categoriaService.actualizar(id, request);
    }


    @PatchMapping("/{id}/estado")
    public CategoriaResponse cambiarEstado(@PathVariable @Positive Long id, @Valid @RequestBody EstadoCatalogoRequest request) {
        return categoriaService.cambiarEstado(id, request.activo());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable @Positive Long id) {
        categoriaService.cambiarEstado(id, false);
        return ResponseEntity.noContent().build();
    }
}
