package com.boutique.backend.moneda.controller;

import java.net.URI;

import com.boutique.backend.moneda.dto.MonedaEstadoRequest;
import com.boutique.backend.moneda.dto.MonedaRequest;
import com.boutique.backend.moneda.dto.MonedaResponse;
import com.boutique.backend.moneda.service.MonedaService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/monedas")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class MonedaController {

    private final MonedaService monedaService;

    @GetMapping
    public PagedModel<MonedaResponse> listar(@RequestParam(required = false) Boolean activo,
        @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return new PagedModel<>(monedaService.listar(activo, pageable));
    }

    @GetMapping("/{id}")
    public MonedaResponse obtenerPorId(@PathVariable @Positive Long id) {
        return monedaService.obtenerPorId(id);
    }

    @PostMapping
    public ResponseEntity<MonedaResponse> crear(@Valid @RequestBody MonedaRequest request) {
        MonedaResponse response = monedaService.crear(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
            .buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public MonedaResponse actualizar(@PathVariable @Positive Long id, @Valid @RequestBody MonedaRequest request) {
        return monedaService.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    public MonedaResponse cambiarEstado(@PathVariable @Positive Long id, @Valid @RequestBody MonedaEstadoRequest request) {
        return monedaService.cambiarEstado(id, request.activo());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable @Positive Long id) {
        monedaService.cambiarEstado(id, false);
        return ResponseEntity.noContent().build();
    }
}
