package com.krakedev.jwt.utils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.krakedev.jwt.entidades.Usuario;
import com.krakedev.jwt.exceptions.TokenInvalidoException;

/**
 * Utilidad central para firmar, generar y verificar los JWT del refugio.
 *
 * - Algoritmo: HMAC-SHA256 (HS256)
 * - Emisor: patitas-al-rescate
 * - Vigencia: 30 minutos (1800 segundos)
 *
 * El token incluye el username en "sub" y el rol en el claim "rol".
 */
@Component
public class JwtUtil {

    public static final String PREFIJO_BEARER = "Bearer ";
    public static final long VALIDEZ_SEGUNDOS = 30 * 60;

    private final Algorithm algorithm;
    private final JWTVerifier verifier;
    private final String issuer;
    private final long expirationSeconds;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.issuer:patitas-al-rescate}") String issuer,
            @Value("${jwt.expiration-seconds:1800}") long expirationSeconds) {

        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException(
                    "jwt.secret debe tener al menos 32 bytes (256 bits) para firmar con HS256");
        }
        if (expirationSeconds <= 0) {
            throw new IllegalArgumentException("jwt.expiration-seconds debe ser mayor que cero");
        }

        this.algorithm = Algorithm.HMAC256(secret);
        this.issuer = issuer;
        this.expirationSeconds = expirationSeconds;
        this.verifier = JWT.require(algorithm).withIssuer(issuer).build();
    }

    /**
     * Genera un token firmado para el usuario autenticado.
     */
    public TokenGenerado generarToken(Usuario usuario) {
        Instant emitidoEn = Instant.now();
        Instant expiraEn = emitidoEn.plusSeconds(expirationSeconds);

        String token = JWT.create()
                .withIssuer(issuer)
                .withSubject(usuario.getUsername())
                .withClaim("rol", usuario.getRol())
                .withClaim("id", usuario.getId())
                .withIssuedAt(Date.from(emitidoEn))
                .withExpiresAt(Date.from(expiraEn))
                .sign(algorithm);

        return new TokenGenerado(token, emitidoEn, expiraEn);
    }

    /**
     * Verifica la firma, el emisor y la expiración del token.
     *
     * @throws com.auth0.jwt.exceptions.JWTVerificationException si el token fue
     *         alterado, expiró o no fue firmado por este servidor.
     */
    public DecodedJWT validarToken(String token) {
        return verifier.verify(token);
    }

    /**
     * Extrae el token de una cabecera "Authorization: Bearer &lt;token&gt;".
     */
    public String extraerTokenDeCabecera(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            throw TokenInvalidoException.porCabeceraAusente();
        }

        String[] partes = authorization.trim().split("\\s+");
        if (partes.length != 2 || !"Bearer".equalsIgnoreCase(partes[0]) || partes[1].isBlank()) {
            throw new TokenInvalidoException(
                    "La cabecera Authorization debe tener el formato: Bearer <token>");
        }
        return partes[1];
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    public String getIssuer() {
        return issuer;
    }

    /**
     * Resultado de firmar un token, con las fechas exactas incluidas en él.
     */
    public record TokenGenerado(String token, Instant emitidoEn, Instant expiraEn) {
    }
}
