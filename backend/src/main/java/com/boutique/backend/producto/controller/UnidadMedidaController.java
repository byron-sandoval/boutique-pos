package com.boutique.backend.producto.controller;

import java.net.URI;

import com.boutique.backend.producto.dto.EstadoCatalogoRequest;
import com.boutique.backend.producto.dto.UnidadMedidaRequest;
import com.boutique.backend.producto.dto.UnidadMedidaResponse;
import com.boutique.backend.producto.service.UnidadMedidaService;
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
@RequestMapping("/api/unidades-medida")
@RequiredArgsConstructor
public class UnidadMedidaController {

    private final UnidadMedidaService unidadMedidaService;

    @GetMapping
    public PagedModel<UnidadMedidaResponse> listar(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return new PagedModel<>(unidadMedidaService.listar(pageable));
    }

    @GetMapping("/{id}")
    public UnidadMedidaResponse obtenerPorId(@PathVariable @Positive Long id) {
        return unidadMedidaService.obtenerPorId(id);
    }

    @PostMapping
    public ResponseEntity<UnidadMedidaResponse> crear(@Valid @RequestBody UnidadMedidaRequest request) {
        UnidadMedidaResponse response = unidadMedidaService.crear(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public UnidadMedidaResponse actualizar(@PathVariable @Positive Long id, @Valid @RequestBody UnidadMedidaRequest request) {
        return unidadMedidaService.actualizar(id, request);
    }


    @PatchMapping("/{id}/estado")
    public UnidadMedidaResponse cambiarEstado(@PathVariable @Positive Long id, @Valid @RequestBody EstadoCatalogoRequest request) {
        return unidadMedidaService.cambiarEstado(id, request.activo());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable @Positive Long id) {
        unidadMedidaService.cambiarEstado(id, false);
        return ResponseEntity.noContent().build();
    }
}
