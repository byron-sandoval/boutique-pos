package com.boutique.backend.cliente.controller;

import java.net.URI;

import com.boutique.backend.cliente.dto.ClienteEstadoRequest;
import com.boutique.backend.cliente.dto.ClienteRequest;
import com.boutique.backend.cliente.dto.ClienteResponse;
import com.boutique.backend.cliente.service.ClienteService;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/clientes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CAJERO')")
public class ClienteController {

    private final ClienteService clienteService;

    @GetMapping
    public PagedModel<ClienteResponse> listar(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return new PagedModel<>(clienteService.listar(pageable));
    }

    @GetMapping("/{id}")
    public ClienteResponse obtenerPorId(@PathVariable @Positive Long id) {
        return clienteService.obtenerPorId(id);
    }

    @PostMapping
    public ResponseEntity<ClienteResponse> crear(@Valid @RequestBody ClienteRequest request) {
        ClienteResponse response = clienteService.crear(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{id}")
    public ClienteResponse actualizar(@PathVariable @Positive Long id, @Valid @RequestBody ClienteRequest request) {
        return clienteService.actualizar(id, request);
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PatchMapping("/{id}/estado")
    public ClienteResponse cambiarEstado(@PathVariable @Positive Long id, @Valid @RequestBody ClienteEstadoRequest request) {
        return clienteService.cambiarEstado(id, request.activo());
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable @Positive Long id) {
        clienteService.cambiarEstado(id, false);
        return ResponseEntity.noContent().build();
    }
}
