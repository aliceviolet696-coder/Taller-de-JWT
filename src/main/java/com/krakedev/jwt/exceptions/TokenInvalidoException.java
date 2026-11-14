package com.krakedev.jwt.exceptions;

/**
 * Cabecera Authorization ausente, vacía o sin el prefijo "Bearer ".
 */
public class TokenInvalidoException extends RuntimeException {

    public TokenInvalidoException(String mensaje) {
        super(mensaje);
    }

    public static TokenInvalidoException porCabeceraAusente() {
        return new TokenInvalidoException(
                "Debe enviar la cabecera Authorization con el formato: Bearer <token>");
    }
}
