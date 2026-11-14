package com.krakedev.jwt.dtos;

import java.time.Instant;

/**
 * Respuesta del endpoint protegido GET /api/auth/perfil.
 */
public record PerfilResponse(
        Integer id,
        String username,
        String rol,
        String mensaje,
        Instant expiracionToken,
        boolean autenticado) {
}
