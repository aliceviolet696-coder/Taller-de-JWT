package com.krakedev.jwt.exceptions;

public class RolInvalidoException extends RuntimeException {

    public RolInvalidoException(String rol) {
        super("Rol inválido: '" + rol + "'. Los roles permitidos son ADMIN y USER");
    }
}
