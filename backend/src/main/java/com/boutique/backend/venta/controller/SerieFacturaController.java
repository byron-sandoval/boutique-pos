package com.boutique.backend.venta.controller;

import java.net.URI;

import com.boutique.backend.venta.dto.SerieFacturaCreateRequest;
import com.boutique.backend.venta.dto.SerieFacturaEstadoRequest;
import com.boutique.backend.venta.dto.SerieFacturaResponse;
import com.boutique.backend.venta.dto.SerieFacturaUpdateRequest;
import com.boutique.backend.venta.service.SerieFacturaService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/series-factura")
@RequiredArgsConstructor
public class SerieFacturaController {

    private final SerieFacturaService serieService;

    @GetMapping
    public PagedModel<SerieFacturaResponse> listar(@RequestParam(required = false) @Positive Long sucursalId,
        @RequestParam(required = false) Boolean activa,
        @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return new PagedModel<>(serieService.listar(sucursalId, activa, pageable));
    }

    @GetMapping("/{id}")
    public SerieFacturaResponse obtenerPorId(@PathVariable @Positive Long id) {
        return serieService.obtenerPorId(id);
    }

    @PostMapping
    public ResponseEntity<SerieFacturaResponse> crear(@Valid @RequestBody SerieFacturaCreateRequest request) {
        SerieFacturaResponse response = serieService.crear(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
            .buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public SerieFacturaResponse actualizar(@PathVariable @Positive Long id, @Valid @RequestBody SerieFacturaUpdateRequest request) {
        return serieService.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    public SerieFacturaResponse cambiarEstado(@PathVariable @Positive Long id, @Valid @RequestBody SerieFacturaEstadoRequest request) {
        return serieService.cambiarEstado(id, request.activa());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable @Positive Long id) {
        serieService.cambiarEstado(id, false);
        return ResponseEntity.noContent().build();
    }
}
