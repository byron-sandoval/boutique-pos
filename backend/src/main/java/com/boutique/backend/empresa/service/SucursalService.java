package com.boutique.backend.empresa.service;

import com.boutique.backend.empresa.dto.SucursalRequest;
import com.boutique.backend.empresa.dto.SucursalResponse;
import com.boutique.backend.empresa.entity.Empresa;
import com.boutique.backend.empresa.entity.Sucursal;
import com.boutique.backend.empresa.mapper.SucursalMapper;
import com.boutique.backend.empresa.repository.EmpresaRepository;
import com.boutique.backend.empresa.repository.SucursalRepository;
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
public class SucursalService {

    private final SucursalRepository sucursalRepository;
    private final EmpresaRepository empresaRepository;
    private final SucursalMapper sucursalMapper;

    public Page<SucursalResponse> listar(@NotNull Pageable pageable) {
        return sucursalRepository.findAll(pageable).map(sucursalMapper::toResponse);
    }

    public Page<SucursalResponse> listarPorEmpresa(@NotNull @Positive Long empresaId, @NotNull Pageable pageable) {
        buscarEmpresa(empresaId);
        return sucursalRepository.findByEmpresaId(empresaId, pageable).map(sucursalMapper::toResponse);
    }

    public SucursalResponse obtenerPorId(@NotNull @Positive Long id) {
        return sucursalMapper.toResponse(buscarSucursal(id));
    }

    @Transactional
    public SucursalResponse crear(@NotNull @Valid SucursalRequest request) {
        Empresa empresa = buscarEmpresa(request.empresaId());
        Sucursal sucursal = sucursalMapper.toEntity(request, empresa);
        return sucursalMapper.toResponse(sucursalRepository.save(sucursal));
    }

    @Transactional
    public SucursalResponse actualizar(@NotNull @Positive Long id, @NotNull @Valid SucursalRequest request) {
        Sucursal sucursal = buscarSucursal(id);
        Empresa empresa = buscarEmpresa(request.empresaId());
        sucursalMapper.updateEntity(request, empresa, sucursal);
        return sucursalMapper.toResponse(sucursalRepository.save(sucursal));
    }

    @Transactional
    public SucursalResponse cambiarEstado(@NotNull @Positive Long id, boolean activa) {
        Sucursal sucursal = buscarSucursal(id);
        sucursal.setActiva(activa);
        return sucursalMapper.toResponse(sucursalRepository.save(sucursal));
    }

    private Empresa buscarEmpresa(Long id) {
        return empresaRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe la empresa con ID " + id + "."));
    }

    private Sucursal buscarSucursal(Long id) {
        return sucursalRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe la sucursal con ID " + id + "."));
    }
}
