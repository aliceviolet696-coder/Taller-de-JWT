package com.krakedev.jwt.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cuerpo del POST /api/auth/registrar.
 */
public record RegistroRequest(

        @NotBlank(message = "El username es obligatorio")
        @Size(min = 3, max = 50, message = "El username debe tener entre 3 y 50 caracteres")
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "El username solo admite letras, números, punto, guion y guion bajo")
        String username,

        @NotBlank(message = "La password es obligatoria")
        @Size(min = 8, max = 72, message = "La password debe tener entre 8 y 72 caracteres")
        String password,

        @NotBlank(message = "El rol es obligatorio")
        @Pattern(regexp = "(?i)^(ADMIN|USER)$", message = "El rol debe ser ADMIN o USER")
        String rol) {
}
