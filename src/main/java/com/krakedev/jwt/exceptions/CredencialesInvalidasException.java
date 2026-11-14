package com.krakedev.jwt.exceptions;

/**
 * Se lanza cuando el usuario no existe o la contraseña no coincide.
 *
 * El mensaje es intencionalmente genérico para no revelar qué credencial falló
 * (evita la enumeración de usuarios).
 */
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException() {
        super("Credenciales inválidas");
    }
}
