package com.boutique.backend.empresa.service;

import com.boutique.backend.empresa.dto.EmpresaRequest;
import com.boutique.backend.empresa.dto.EmpresaResponse;
import com.boutique.backend.empresa.entity.Empresa;
import com.boutique.backend.empresa.mapper.EmpresaMapper;
import com.boutique.backend.empresa.repository.EmpresaRepository;
import com.boutique.backend.shared.exception.ConflictoException;
import com.boutique.backend.shared.exception.RecursoNoEncontradoException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmpresaService {

    private final EmpresaRepository empresaRepository;
    private final EmpresaMapper empresaMapper;

    public Page<EmpresaResponse> listar(@NotNull Pageable pageable) {
        return empresaRepository.findAll(pageable).map(empresaMapper::toResponse);
    }

    public EmpresaResponse obtenerPorId(@NotNull @Positive Long id) {
        return empresaMapper.toResponse(buscarEmpresa(id));
    }

    @Transactional
    public EmpresaResponse crear(@NotNull @Valid EmpresaRequest request) {
        String ruc = normalizarRuc(request.ruc());
        if (ruc != null && empresaRepository.existsByRuc(ruc)) {
            throw new ConflictoException("Ya existe una empresa con ese RUC.");
        }

        Empresa empresa = empresaMapper.toEntity(request);
        empresa.setRuc(ruc);
        return empresaMapper.toResponse(guardar(empresa));
    }

    @Transactional
    public EmpresaResponse actualizar(@NotNull @Positive Long id, @NotNull @Valid EmpresaRequest request) {
        Empresa empresa = buscarEmpresa(id);
        String ruc = normalizarRuc(request.ruc());
        if (ruc != null && empresaRepository.existsByRucAndIdNot(ruc, id)) {
            throw new ConflictoException("Ya existe otra empresa con ese RUC.");
        }

        empresaMapper.updateEntity(request, empresa);
        empresa.setRuc(ruc);
        return empresaMapper.toResponse(guardar(empresa));
    }

    private Empresa buscarEmpresa(Long id) {
        return empresaRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe la empresa con ID " + id + "."));
    }

    private String normalizarRuc(String ruc) {
        return ruc == null || ruc.isBlank() ? null : ruc.trim();
    }

    private Empresa guardar(Empresa empresa) {
        try {
            return empresaRepository.saveAndFlush(empresa);
        } catch (DataIntegrityViolationException exception) {
            // La restriccion de PostgreSQL resuelve tambien las creaciones simultaneas.
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof org.hibernate.exception.ConstraintViolationException violation
                    && "empresa_ruc_key".equals(violation.getConstraintName())) {
                    throw new ConflictoException("Ya existe una empresa con ese RUC.", exception);
                }
            }
            throw exception;
        }
    }
}
