package com.boutique.backend.producto.controller;

import com.boutique.backend.shared.exception.ConflictoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {CategoriaController.class, UnidadMedidaController.class, ProductoController.class})
class CatalogoErrorHandler {

    @ExceptionHandler(ConflictoException.class)
    ProblemDetail conflicto(ConflictoException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }
}
