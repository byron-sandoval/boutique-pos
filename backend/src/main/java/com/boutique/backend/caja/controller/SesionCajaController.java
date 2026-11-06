package com.boutique.backend.caja.controller;

import java.net.URI;

import com.boutique.backend.caja.dto.SesionCajaAperturaRequest;
import com.boutique.backend.caja.dto.SesionCajaCierreRequest;
import com.boutique.backend.caja.dto.SesionCajaResponse;
import com.boutique.backend.caja.entity.EstadoCaja;
import com.boutique.backend.caja.service.SesionCajaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/cajas")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CAJERO')")
public class SesionCajaController {

    private final SesionCajaService cajaService;

    @GetMapping
    public PagedModel<SesionCajaResponse> listar(@RequestParam(required = false) @Positive Long sucursalId,
        @RequestParam(required = false) @Positive Long usuarioId,
        @RequestParam(required = false) EstadoCaja estado,
        @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return new PagedModel<>(cajaService.listar(sucursalId, usuarioId, estado, pageable));
    }

    @GetMapping("/actual")
    public SesionCajaResponse obtenerActual() {
        return cajaService.obtenerActual();
    }

    @GetMapping("/{id}")
    public SesionCajaResponse obtenerPorId(@PathVariable @Positive Long id) {
        return cajaService.obtenerPorId(id);
    }

    @PostMapping
    public ResponseEntity<SesionCajaResponse> abrir(@Valid @RequestBody SesionCajaAperturaRequest request) {
        SesionCajaResponse response = cajaService.abrir(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
            .buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PatchMapping("/{id}/cierre")
    public SesionCajaResponse cerrar(@PathVariable @Positive Long id,
        @Valid @RequestBody SesionCajaCierreRequest request) {
        return cajaService.cerrar(id, request);
    }
}
