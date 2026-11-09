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
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class ProductoController {

    private final ProductoService productoService;

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CAJERO')")
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

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CAJERO')")
    @GetMapping("/{id}")
    public ProductoResponse obtenerPorId(@PathVariable @Positive Long id) {
        return productoService.obtenerPorId(id);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest request) {
        return creado(productoService.crear(request));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductoResponse> crearConImagen(@Valid @RequestPart("producto") ProductoRequest request,
        @RequestPart(name = "imagen", required = false) MultipartFile imagen) {
        return creado(productoService.crear(request, imagen));
    }

    private ResponseEntity<ProductoResponse> creado(ProductoResponse response) {
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ProductoResponse actualizar(@PathVariable @Positive Long id, @Valid @RequestBody ProductoRequest request) {
        return productoService.actualizar(id, request);
    }


    @PutMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProductoResponse actualizarConImagen(@PathVariable @Positive Long id,
        @Valid @RequestPart("producto") ProductoRequest request,
        @RequestPart(name = "imagen", required = false) MultipartFile imagen) {
        return productoService.actualizar(id, request, imagen);
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
