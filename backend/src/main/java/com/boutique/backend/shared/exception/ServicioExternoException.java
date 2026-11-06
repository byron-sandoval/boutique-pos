package com.boutique.backend.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class ServicioExternoException extends RuntimeException {

    public ServicioExternoException(String message) {
        super(message);
    }
}
