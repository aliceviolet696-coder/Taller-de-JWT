package com.krakedev.jwt.exceptions;

public class UsuarioDuplicadoException extends RuntimeException {

    public UsuarioDuplicadoException(String username) {
        super("El usuario '" + username + "' ya está registrado en el refugio");
    }
}
