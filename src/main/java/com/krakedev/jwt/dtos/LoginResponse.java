package com.krakedev.jwt.dtos;

import java.time.Instant;

/**
 * Respuesta del login: el JWT firmado y sus datos de vigencia.
 */
public record LoginResponse(
        String token,
        String tipo,
        String username,
        String rol,
        long validezSegundos,
        Instant emitidoEn,
        Instant expiraEn) {
}
