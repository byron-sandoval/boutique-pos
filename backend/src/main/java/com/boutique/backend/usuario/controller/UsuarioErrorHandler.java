package com.boutique.backend.usuario.controller;

import com.boutique.backend.shared.exception.ServicioExternoException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice(assignableTypes = UsuarioController.class)
public class UsuarioErrorHandler {

    // Evita incluir valores rechazados de contrasenas en respuestas y logs del resolver.
    @ExceptionHandler({MethodArgumentNotValidException.class, HandlerMethodValidationException.class,
        ConstraintViolationException.class, HttpMessageNotReadableException.class})
    public ProblemDetail solicitudInvalida(Exception exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
            "Revisa los campos obligatorios, longitudes, correo, rol y contrasena de la solicitud.");
    }

    @ExceptionHandler(ServicioExternoException.class)
    public ProblemDetail integracionNoDisponible(ServicioExternoException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
    }
}
