package com.boutique.backend.empresa.controller;

import java.net.URI;

import com.boutique.backend.empresa.dto.EmpresaRequest;
import com.boutique.backend.empresa.dto.EmpresaResponse;
import com.boutique.backend.empresa.service.EmpresaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/empresas")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class EmpresaController {

    private final EmpresaService empresaService;

    @GetMapping("/nombre")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_CAJERO')")
    public NombreEmpresaResponse nombre() {
        return new NombreEmpresaResponse(empresaService.nombreConfigurado());
    }

    public record NombreEmpresaResponse(String nombre) {}

    @GetMapping
    public PagedModel<EmpresaResponse> listar(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return new PagedModel<>(empresaService.listar(pageable));
    }

    @GetMapping("/{id}")
    public EmpresaResponse obtenerPorId(@PathVariable @Positive Long id) {
        return empresaService.obtenerPorId(id);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EmpresaResponse> crear(@Valid @RequestBody EmpresaRequest request) {
        return creada(empresaService.crear(request));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EmpresaResponse> crearConLogo(@Valid @RequestPart("empresa") EmpresaRequest request,
        @RequestPart(name = "logo", required = false) MultipartFile logo) {
        return creada(empresaService.crear(request, logo));
    }

    private ResponseEntity<EmpresaResponse> creada(EmpresaResponse response) {
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public EmpresaResponse actualizar(@PathVariable @Positive Long id, @Valid @RequestBody EmpresaRequest request) {
        return empresaService.actualizar(id, request);
    }

    @PutMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public EmpresaResponse actualizarConLogo(@PathVariable @Positive Long id,
        @Valid @RequestPart("empresa") EmpresaRequest request,
        @RequestPart(name = "logo", required = false) MultipartFile logo) {
        return empresaService.actualizar(id, request, logo);
    }
}
