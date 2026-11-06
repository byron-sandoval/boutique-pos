package com.boutique.backend.moneda.service;

import java.util.Locale;

import com.boutique.backend.moneda.dto.MonedaRequest;
import com.boutique.backend.moneda.dto.MonedaResponse;
import com.boutique.backend.moneda.entity.Moneda;
import com.boutique.backend.moneda.mapper.MonedaMapper;
import com.boutique.backend.moneda.repository.MonedaRepository;
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
public class MonedaService {

    private final MonedaRepository monedaRepository;
    private final MonedaMapper monedaMapper;

    public Page<MonedaResponse> listar(Boolean activo, @NotNull Pageable pageable) {
        Page<Moneda> pagina = activo == null ? monedaRepository.findAll(pageable)
            : monedaRepository.findByActivo(activo, pageable);
        return pagina.map(monedaMapper::toResponse);
    }

    public MonedaResponse obtenerPorId(@NotNull @Positive Long id) {
        return monedaMapper.toResponse(monedaRepository.findById(id)
            .orElseThrow(() -> noEncontrada(id)));
    }

    @Transactional
    public MonedaResponse crear(@NotNull @Valid MonedaRequest request) {
        MonedaRequest normalizado = normalizar(request);
        validarCodigo(normalizado.codigo(), null);
        return monedaMapper.toResponse(guardar(monedaMapper.toEntity(normalizado)));
    }

    @Transactional
    public MonedaResponse actualizar(@NotNull @Positive Long id, @NotNull @Valid MonedaRequest request) {
        Moneda moneda = buscarParaActualizar(id);
        MonedaRequest normalizado = normalizar(request);
        validarCodigo(normalizado.codigo(), id);
        if (!moneda.getCodigo().equalsIgnoreCase(normalizado.codigo()) && monedaRepository.contarVentas(id) > 0) {
            throw new ConflictoException("No puedes cambiar el código de una moneda que ya tiene ventas registradas.");
        }
        monedaMapper.updateEntity(normalizado, moneda);
        return monedaMapper.toResponse(guardar(moneda));
    }

    @Transactional
    public MonedaResponse cambiarEstado(@NotNull @Positive Long id, boolean activo) {
        Moneda moneda = buscarParaActualizar(id);
        moneda.setActivo(activo);
        return monedaMapper.toResponse(guardar(moneda));
    }

    private MonedaRequest normalizar(MonedaRequest request) {
        return new MonedaRequest(request.codigo().toUpperCase(Locale.ROOT), request.nombre().trim(),
            request.simbolo().trim(), request.activo());
    }

    private void validarCodigo(String codigo, Long id) {
        boolean duplicado = id == null ? monedaRepository.existsByCodigoIgnoreCase(codigo)
            : monedaRepository.existsByCodigoIgnoreCaseAndIdNot(codigo, id);
        if (duplicado) {
            throw new ConflictoException("Ya existe otra moneda con el código " + codigo + ".");
        }
    }

    private Moneda buscarParaActualizar(Long id) {
        return monedaRepository.findByIdForUpdate(id).orElseThrow(() -> noEncontrada(id));
    }

    private RecursoNoEncontradoException noEncontrada(Long id) {
        return new RecursoNoEncontradoException("No existe la moneda con ID " + id + ".");
    }

    private Moneda guardar(Moneda moneda) {
        try {
            return monedaRepository.saveAndFlush(moneda);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictoException("No se pudo guardar la moneda; comprueba que su código no esté duplicado.", exception);
        }
    }
}
