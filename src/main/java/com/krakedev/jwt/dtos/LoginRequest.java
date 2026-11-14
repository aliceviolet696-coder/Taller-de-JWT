package com.krakedev.jwt.dtos;

import jakarta.validation.constraints.NotBlank;

/**
 * Cuerpo del POST /api/auth/login.
 */
public record LoginRequest(

        @NotBlank(message = "El username es obligatorio")
        String username,

        @NotBlank(message = "La password es obligatoria")
        String password) {
}
