package com.boutique.backend.cliente.service;

import com.boutique.backend.cliente.dto.ClienteRequest;
import com.boutique.backend.cliente.dto.ClienteResponse;
import com.boutique.backend.cliente.entity.Cliente;
import com.boutique.backend.cliente.mapper.ClienteMapper;
import com.boutique.backend.cliente.repository.ClienteRepository;
import com.boutique.backend.shared.exception.ConflictoException;
import com.boutique.backend.shared.exception.RecursoNoEncontradoException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
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
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final ClienteMapper clienteMapper;

    public Page<ClienteResponse> listar(@NotNull Pageable pageable) {
        return clienteRepository.findAll(pageable).map(clienteMapper::toResponse);
    }

    public ClienteResponse obtenerPorId(@NotNull @Positive Long id) {
        return clienteMapper.toResponse(buscarCliente(id));
    }

    @Transactional
    public ClienteResponse crear(@NotNull @Valid ClienteRequest request) {
        ClienteRequest normalizado = normalizar(request);
        validarIdentificadores(normalizado, null);
        return clienteMapper.toResponse(guardar(clienteMapper.toEntity(normalizado)));
    }

    @Transactional
    public ClienteResponse actualizar(@NotNull @Positive Long id, @NotNull @Valid ClienteRequest request) {
        Cliente cliente = buscarCliente(id);
        ClienteRequest normalizado = normalizar(request);
        validarIdentificadores(normalizado, id);
        clienteMapper.updateEntity(normalizado, cliente);
        return clienteMapper.toResponse(guardar(cliente));
    }

    @Transactional
    public ClienteResponse cambiarEstado(@NotNull @Positive Long id, boolean activo) {
        Cliente cliente = buscarCliente(id);
        cliente.setActivo(activo);
        return clienteMapper.toResponse(guardar(cliente));
    }

    private ClienteRequest normalizar(ClienteRequest request) {
        return new ClienteRequest(
            textoOpcional(request.cedula()),
            textoOpcional(request.pasaporte()),
            request.nombre().trim(),
            request.direccion(),
            request.telefono(),
            request.activo()
        );
    }

    private String textoOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private void validarIdentificadores(ClienteRequest request, Long id) {
        if (request.cedula() != null) {
            boolean duplicada = id == null
                ? clienteRepository.existsByCedula(request.cedula())
                : clienteRepository.existsByCedulaAndIdNot(request.cedula(), id);
            if (duplicada) {
                throw new ConflictoException("Ya existe otro cliente con esa cedula.");
            }
        }
        if (request.pasaporte() != null) {
            boolean duplicado = id == null
                ? clienteRepository.existsByPasaporte(request.pasaporte())
                : clienteRepository.existsByPasaporteAndIdNot(request.pasaporte(), id);
            if (duplicado) {
                throw new ConflictoException("Ya existe otro cliente con ese pasaporte.");
            }
        }
    }

    private Cliente buscarCliente(Long id) {
        return clienteRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe el cliente con ID " + id + "."));
    }

    private Cliente guardar(Cliente cliente) {
        try {
            return clienteRepository.saveAndFlush(cliente);
        } catch (DataIntegrityViolationException exception) {
            // PostgreSQL resuelve tambien duplicados de solicitudes simultaneas.
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof ConstraintViolationException violation) {
                    if ("cliente_cedula_key".equals(violation.getConstraintName())) {
                        throw new ConflictoException("Ya existe otro cliente con esa cedula.", exception);
                    }
                    if ("cliente_pasaporte_key".equals(violation.getConstraintName())) {
                        throw new ConflictoException("Ya existe otro cliente con ese pasaporte.", exception);
                    }
                }
            }
            throw exception;
        }
    }
}
