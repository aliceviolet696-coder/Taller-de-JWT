package com.krakedev.jwt.dtos;

import java.time.Instant;
import java.util.Map;

/**
 * Formato uniforme de error para toda la API.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String mensaje,
        String ruta,
        Map<String, String> detalles) {

    public static ErrorResponse de(int status, String error, String mensaje, String ruta) {
        return new ErrorResponse(Instant.now(), status, error, mensaje, ruta, Map.of());
    }

    public static ErrorResponse de(int status, String error, String mensaje, String ruta,
            Map<String, String> detalles) {
        return new ErrorResponse(Instant.now(), status, error, mensaje, ruta, detalles);
    }
}
