package com.boutique.backend.inventario.controller;

import java.net.URI;

import com.boutique.backend.inventario.dto.IngresoRequest;
import com.boutique.backend.inventario.dto.IngresoResponse;
import com.boutique.backend.inventario.dto.IngresoResumenResponse;
import com.boutique.backend.inventario.service.IngresoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/ingresos")
@RequiredArgsConstructor
public class IngresoController {

    private final IngresoService ingresoService;

    @GetMapping
    public PagedModel<IngresoResumenResponse> listar(@RequestParam(required = false) @Positive Long sucursalId,
        @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return new PagedModel<>(ingresoService.listar(sucursalId, pageable));
    }

    @GetMapping("/{id}")
    public IngresoResponse obtenerPorId(@PathVariable @Positive Long id) {
        return ingresoService.obtenerPorId(id);
    }

    @PostMapping
    public ResponseEntity<IngresoResponse> crear(@Valid @RequestBody IngresoRequest request) {
        IngresoResponse response = ingresoService.crear(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
            .buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> anular(@PathVariable @Positive Long id) {
        ingresoService.anular(id);
        return ResponseEntity.noContent().build();
    }
}
