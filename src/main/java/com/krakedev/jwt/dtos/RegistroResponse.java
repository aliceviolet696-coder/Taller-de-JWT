package com.krakedev.jwt.dtos;

/**
 * Respuesta del registro. Nunca se devuelve la contraseña ni su hash.
 */
public record RegistroResponse(
        Integer id,
        String username,
        String rol,
        String algoritmoPassword,
        String mensaje) {
}
