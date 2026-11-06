package com.boutique.backend.inventario.controller;

import java.net.URI;

import com.boutique.backend.inventario.dto.InventarioCreateRequest;
import com.boutique.backend.inventario.dto.InventarioLimitesRequest;
import com.boutique.backend.inventario.dto.InventarioResponse;
import com.boutique.backend.inventario.service.InventarioService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/inventarios")
@RequiredArgsConstructor
public class InventarioController {

    private final InventarioService inventarioService;

    @GetMapping
    public PagedModel<InventarioResponse> listar(@RequestParam(required = false) @Positive Long sucursalId,
        @RequestParam(required = false) @Positive Long varianteId,
        @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return new PagedModel<>(inventarioService.listar(sucursalId, varianteId, pageable));
    }

    @GetMapping("/{id}")
    public InventarioResponse obtenerPorId(@PathVariable @Positive Long id) {
        return inventarioService.obtenerPorId(id);
    }

    @PostMapping
    public ResponseEntity<InventarioResponse> crear(@Valid @RequestBody InventarioCreateRequest request) {
        InventarioResponse response = inventarioService.crear(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
            .buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}/limites")
    public InventarioResponse actualizarLimites(@PathVariable @Positive Long id,
        @Valid @RequestBody InventarioLimitesRequest request) {
        return inventarioService.actualizarLimites(id, request);
    }
}
