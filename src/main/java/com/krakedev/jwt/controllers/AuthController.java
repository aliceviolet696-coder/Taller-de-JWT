package com.krakedev.jwt.controllers;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.krakedev.jwt.dtos.LoginRequest;
import com.krakedev.jwt.dtos.LoginResponse;
import com.krakedev.jwt.dtos.PerfilResponse;
import com.krakedev.jwt.dtos.RegistroRequest;
import com.krakedev.jwt.dtos.RegistroResponse;
import com.krakedev.jwt.entidades.Usuario;
import com.krakedev.jwt.services.UsuarioService;
import com.krakedev.jwt.utils.JwtUtil;

import jakarta.validation.Valid;

/**
 * API de identidad del refugio "Patitas al Rescate".
 *
 * POST /api/auth/registrar  -> crea ADMIN o USER con contraseña hasheada
 * POST /api/auth/login      -> valida credenciales y emite un JWT (30 min)
 * GET  /api/auth/perfil     -> endpoint protegido manualmente con Bearer token
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioService usuarioService;
    private final JwtUtil jwtUtil;

    public AuthController(UsuarioService usuarioService, JwtUtil jwtUtil) {
        this.usuarioService = usuarioService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/registrar")
    public ResponseEntity<RegistroResponse> registrar(@Valid @RequestBody RegistroRequest request) {
        Usuario usuario = usuarioService.registrar(
                request.username(),
                request.password(),
                request.rol());

        RegistroResponse respuesta = new RegistroResponse(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getRol(),
                "BCrypt (costo 12)",
                "Usuario registrado correctamente. La contraseña se almacenó como hash BCrypt.");

        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Usuario usuario = usuarioService.autenticar(request.username(), request.password());

        JwtUtil.TokenGenerado generado = jwtUtil.generarToken(usuario);

        LoginResponse respuesta = new LoginResponse(
                generado.token(),
                "Bearer",
                usuario.getUsername(),
                usuario.getRol(),
                jwtUtil.getExpirationSeconds(),
                generado.emitidoEn(),
                generado.expiraEn());

        return ResponseEntity.ok(respuesta);
    }

    /**
     * Hito 4: protección manual del perfil.
     *
     * No se usa Spring Security todavía: el controlador lee la cabecera
     * Authorization, extrae el Bearer token y lo verifica con JwtUtil. Si la
     * firma fue alterada o el token expiró, se responde 401 Unauthorized.
     */
    @GetMapping("/perfil")
    public ResponseEntity<PerfilResponse> perfil(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {

        String token = jwtUtil.extraerTokenDeCabecera(authorization);
        DecodedJWT jwt = jwtUtil.validarToken(token);

        String username = jwt.getSubject();
        String rol = jwt.getClaim("rol").asString();
        Integer id = jwt.getClaim("id").asInt();

        PerfilResponse respuesta = new PerfilResponse(
                id,
                username,
                rol,
                mensajeSegunRol(username, rol),
                jwt.getExpiresAtAsInstant(),
                true);

        return ResponseEntity.ok(respuesta);
    }

    private String mensajeSegunRol(String username, String rol) {
        if (UsuarioService.ROL_ADMIN.equals(rol)) {
            return "Hola, " + username + ". Bienvenido al panel de administración de Patitas al Rescate: "
                    + "podés registrar rescates, actualizar fichas de salud y autorizar adopciones.";
        }
        if (UsuarioService.ROL_USER.equals(rol)) {
            return "Hola, " + username + ". Bienvenido a Patitas al Rescate: "
                    + "podés ver las mascotas disponibles y postular tu solicitud de adopción.";
        }
        return "Hola, " + username + ". Tu rol '" + rol + "' no tiene un panel asignado.";
    }
}
