package com.krakedev.jwt.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.Test;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.krakedev.jwt.entidades.Usuario;
import com.krakedev.jwt.exceptions.TokenInvalidoException;

class JwtUtilTest {

    private static final String SECRETO = "clave-de-prueba-con-mas-de-32-bytes-para-hs256";
    private static final String EMISOR = "patitas-al-rescate";

    private final JwtUtil jwtUtil = new JwtUtil(SECRETO, EMISOR, 1800);

    private Usuario usuarioDemo() {
        Usuario usuario = new Usuario("voluntario_carlos", "$2a$12$hashDePrueba", "ADMIN");
        usuario.setId(1);
        return usuario;
    }

    @Test
    void generaUnTokenValidoConSubjectYRol() {
        JwtUtil.TokenGenerado generado = jwtUtil.generarToken(usuarioDemo());

        assertNotNull(generado.token());
        assertEquals(3, generado.token().split("\\.").length, "Un JWT debe tener 3 segmentos");

        DecodedJWT decoded = jwtUtil.validarToken(generado.token());
        assertEquals("voluntario_carlos", decoded.getSubject());
        assertEquals("ADMIN", decoded.getClaim("rol").asString());
        assertEquals(EMISOR, decoded.getIssuer());
        assertEquals(1800, jwtUtil.getExpirationSeconds());
        assertTrue(decoded.getExpiresAtAsInstant().isAfter(Instant.now()));
        assertEquals(1800, java.time.Duration.between(
                decoded.getIssuedAtAsInstant(), decoded.getExpiresAtAsInstant()).getSeconds(),
                "La vigencia estricta del token debe ser de 30 minutos");
    }

    @Test
    void rechazaUnTokenConUnSoloCaracterAlterado() {
        String token = jwtUtil.generarToken(usuarioDemo()).token();

        int posicion = token.lastIndexOf('.') + 2;
        char original = token.charAt(posicion);
        char reemplazo = original == 'A' ? 'B' : 'A';
        String alterado = token.substring(0, posicion) + reemplazo + token.substring(posicion + 1);

        assertFalse(token.equals(alterado));
        assertThrows(JWTVerificationException.class, () -> jwtUtil.validarToken(alterado));
    }

    @Test
    void rechazaUnTokenFirmadoConOtraClave() {
        String tokenAjeno = JWT.create()
                .withIssuer(EMISOR)
                .withSubject("intruso")
                .withClaim("rol", "ADMIN")
                .withExpiresAt(Date.from(Instant.now().plusSeconds(600)))
                .sign(Algorithm.HMAC256("otra-clave-distinta-de-mas-de-32-bytes"));

        assertThrows(JWTVerificationException.class, () -> jwtUtil.validarToken(tokenAjeno));
    }

    @Test
    void rechazaUnTokenExpirado() {
        String expirado = JWT.create()
                .withIssuer(EMISOR)
                .withSubject("voluntario_carlos")
                .withClaim("rol", "ADMIN")
                .withExpiresAt(Date.from(Instant.now().minusSeconds(60)))
                .sign(Algorithm.HMAC256(SECRETO));

        assertThrows(TokenExpiredException.class, () -> jwtUtil.validarToken(expirado));
    }

    @Test
    void exigeUnSecretoDeAlMenos32Bytes() {
        assertThrows(IllegalArgumentException.class, () -> new JwtUtil("corto", EMISOR, 1800));
    }

    @Test
    void extraeElTokenDeLaCabeceraBearer() {
        assertEquals("abc.def.ghi", jwtUtil.extraerTokenDeCabecera("Bearer abc.def.ghi"));
        assertEquals("abc.def.ghi", jwtUtil.extraerTokenDeCabecera("bearer   abc.def.ghi"));
    }

    @Test
    void rechazaCabecerasInvalidas() {
        assertThrows(TokenInvalidoException.class, () -> jwtUtil.extraerTokenDeCabecera(null));
        assertThrows(TokenInvalidoException.class, () -> jwtUtil.extraerTokenDeCabecera(""));
        assertThrows(TokenInvalidoException.class, () -> jwtUtil.extraerTokenDeCabecera("Basic abc"));
        assertThrows(TokenInvalidoException.class, () -> jwtUtil.extraerTokenDeCabecera("Bearer"));
    }
}
