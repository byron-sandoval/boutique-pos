package com.boutique.backend.empresa.controller;

import java.net.URI;

import com.boutique.backend.empresa.dto.SucursalEstadoRequest;
import com.boutique.backend.empresa.dto.SucursalRequest;
import com.boutique.backend.empresa.dto.SucursalResponse;
import com.boutique.backend.empresa.service.SucursalService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/sucursales")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class SucursalController {

    private final SucursalService sucursalService;

    @GetMapping
    public PagedModel<SucursalResponse> listar(
        @RequestParam(required = false) @Positive Long empresaId,
        @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        Page<SucursalResponse> page = empresaId == null
            ? sucursalService.listar(pageable)
            : sucursalService.listarPorEmpresa(empresaId, pageable);
        return new PagedModel<>(page);
    }

    @GetMapping("/{id}")
    public SucursalResponse obtenerPorId(@PathVariable @Positive Long id) {
        return sucursalService.obtenerPorId(id);
    }

    @PostMapping
    public ResponseEntity<SucursalResponse> crear(@Valid @RequestBody SucursalRequest request) {
        SucursalResponse response = sucursalService.crear(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public SucursalResponse actualizar(@PathVariable @Positive Long id, @Valid @RequestBody SucursalRequest request) {
        return sucursalService.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    public SucursalResponse cambiarEstado(
        @PathVariable @Positive Long id,
        @Valid @RequestBody SucursalEstadoRequest request
    ) {
        return sucursalService.cambiarEstado(id, request.activa());
    }
}
