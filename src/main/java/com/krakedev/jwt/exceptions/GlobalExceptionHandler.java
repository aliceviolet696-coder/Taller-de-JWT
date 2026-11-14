package com.krakedev.jwt.exceptions;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.krakedev.jwt.dtos.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Traduce las excepciones de negocio a respuestas HTTP consistentes.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarValidacion(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> detalles = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                detalles.putIfAbsent(error.getField(), error.getDefaultMessage()));

        ErrorResponse cuerpo = ErrorResponse.de(
                HttpStatus.BAD_REQUEST.value(),
                "Solicitud inválida",
                "Uno o más campos no cumplen las reglas de validación",
                request.getRequestURI(),
                detalles);

        return ResponseEntity.badRequest().body(cuerpo);
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ErrorResponse> manejarCredenciales(
            CredencialesInvalidasException ex, HttpServletRequest request) {

        return respuesta(HttpStatus.UNAUTHORIZED, "No autorizado", ex.getMessage(), request);
    }

    /**
     * Token alterado, con firma inválida o expirado -> 401 Unauthorized.
     */
    @ExceptionHandler(JWTVerificationException.class)
    public ResponseEntity<ErrorResponse> manejarTokenJwt(
            JWTVerificationException ex, HttpServletRequest request) {

        String mensaje = ex instanceof TokenExpiredException
                ? "El token JWT expiró. Inicie sesión nuevamente para obtener uno nuevo."
                : "Token JWT inválido: la firma digital no coincide o el token fue alterado.";

        return respuesta(HttpStatus.UNAUTHORIZED, "No autorizado", mensaje, request);
    }

    @ExceptionHandler(TokenInvalidoException.class)
    public ResponseEntity<ErrorResponse> manejarTokenAusente(
            TokenInvalidoException ex, HttpServletRequest request) {

        return respuesta(HttpStatus.UNAUTHORIZED, "No autorizado", ex.getMessage(), request);
    }

    @ExceptionHandler(UsuarioDuplicadoException.class)
    public ResponseEntity<ErrorResponse> manejarDuplicado(
            UsuarioDuplicadoException ex, HttpServletRequest request) {

        return respuesta(HttpStatus.CONFLICT, "Conflicto", ex.getMessage(), request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> manejarIntegridad(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        return respuesta(HttpStatus.CONFLICT, "Conflicto",
                "El username ya está registrado o los datos violan una restricción de la base de datos",
                request);
    }

    @ExceptionHandler(RolInvalidoException.class)
    public ResponseEntity<ErrorResponse> manejarRol(
            RolInvalidoException ex, HttpServletRequest request) {

        return respuesta(HttpStatus.BAD_REQUEST, "Solicitud inválida", ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> manejarArgumento(
            IllegalArgumentException ex, HttpServletRequest request) {

        return respuesta(HttpStatus.BAD_REQUEST, "Solicitud inválida", ex.getMessage(), request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> manejarJsonMalformado(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        return respuesta(HttpStatus.BAD_REQUEST, "JSON malformado",
                "El cuerpo de la petición no es un JSON válido o tiene tipos incorrectos",
                request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> manejarRutaInexistente(
            NoResourceFoundException ex, HttpServletRequest request) {

        return respuesta(HttpStatus.NOT_FOUND, "No encontrado",
                "La ruta solicitada no existe", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> manejarMetodoNoSoportado(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

        return respuesta(HttpStatus.METHOD_NOT_ALLOWED, "Método no permitido",
                "El método HTTP " + ex.getMethod() + " no está soportado para esta ruta", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejarInesperado(
            Exception ex, HttpServletRequest request) {

        log.error("Error no controlado en {} {}", request.getMethod(), request.getRequestURI(), ex);

        return new ResponseEntity<>(
                new ErrorResponse(Instant.now(), HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Error interno", "Ocurrió un error inesperado en el servidor",
                        request.getRequestURI(), Map.of()),
                HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ErrorResponse> respuesta(
            HttpStatus status, String error, String mensaje, HttpServletRequest request) {

        return ResponseEntity.status(status)
                .body(ErrorResponse.de(status.value(), error, mensaje, request.getRequestURI()));
    }
}
