package com.boutique.backend.proveedor.service;

import com.boutique.backend.proveedor.dto.ProveedorRequest;
import com.boutique.backend.proveedor.dto.ProveedorResponse;
import com.boutique.backend.proveedor.entity.Proveedor;
import com.boutique.backend.proveedor.mapper.ProveedorMapper;
import com.boutique.backend.proveedor.repository.ProveedorRepository;
import com.boutique.backend.shared.exception.RecursoNoEncontradoException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;
    private final ProveedorMapper proveedorMapper;

    public Page<ProveedorResponse> listar(@NotNull Pageable pageable) {
        return proveedorRepository.findAll(pageable).map(proveedorMapper::toResponse);
    }

    public ProveedorResponse obtenerPorId(@NotNull @Positive Long id) {
        return proveedorMapper.toResponse(buscarProveedor(id));
    }

    @Transactional
    public ProveedorResponse crear(@NotNull @Valid ProveedorRequest request) {
        Proveedor proveedor = proveedorMapper.toEntity(normalizar(request));
        return proveedorMapper.toResponse(proveedorRepository.save(proveedor));
    }

    @Transactional
    public ProveedorResponse actualizar(@NotNull @Positive Long id, @NotNull @Valid ProveedorRequest request) {
        Proveedor proveedor = buscarProveedor(id);
        proveedorMapper.updateEntity(normalizar(request), proveedor);
        return proveedorMapper.toResponse(proveedorRepository.save(proveedor));
    }

    @Transactional
    public ProveedorResponse cambiarEstado(@NotNull @Positive Long id, boolean activo) {
        Proveedor proveedor = buscarProveedor(id);
        proveedor.setActivo(activo);
        return proveedorMapper.toResponse(proveedorRepository.save(proveedor));
    }

    private ProveedorRequest normalizar(ProveedorRequest request) {
        return new ProveedorRequest(
            request.nombre().trim(),
            textoOpcional(request.nombreContacto()),
            request.direccion(),
            textoOpcional(request.telefono()),
            textoOpcional(request.email()),
            request.activo()
        );
    }

    private String textoOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private Proveedor buscarProveedor(Long id) {
        return proveedorRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe el proveedor con ID " + id + "."));
    }
}
