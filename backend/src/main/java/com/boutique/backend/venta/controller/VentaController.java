package com.boutique.backend.venta.controller;

import java.net.URI;

import com.boutique.backend.venta.dto.VentaAnulacionRequest;
import com.boutique.backend.venta.dto.VentaRequest;
import com.boutique.backend.venta.dto.VentaResponse;
import com.boutique.backend.venta.dto.VentaResumenResponse;
import com.boutique.backend.venta.entity.EstadoVenta;
import com.boutique.backend.venta.service.VentaService;
import com.boutique.backend.venta.service.VentaAnulacionService;
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
@RequestMapping("/api/ventas")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CAJERO')")
public class VentaController {

    private final VentaService ventaService;
    private final VentaAnulacionService anulacionService;

    @GetMapping
    public PagedModel<VentaResumenResponse> listar(@RequestParam(required = false) @Positive Long sucursalId,
        @RequestParam(required = false) @Positive Long sesionCajaId,
        @RequestParam(required = false) EstadoVenta estado,
        @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return new PagedModel<>(ventaService.listar(sucursalId, sesionCajaId, estado, pageable));
    }

    @GetMapping("/{id}")
    public VentaResponse obtenerPorId(@PathVariable @Positive Long id) {
        return ventaService.obtenerPorId(id);
    }

    @PostMapping
    public ResponseEntity<VentaResponse> crear(@Valid @RequestBody VentaRequest request) {
        VentaResponse response = ventaService.crear(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
            .buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PatchMapping("/{id}/anulacion")
    public VentaResponse anular(@PathVariable @Positive Long id, @Valid @RequestBody VentaAnulacionRequest request) {
        return anulacionService.anular(id, request);
    }
}
