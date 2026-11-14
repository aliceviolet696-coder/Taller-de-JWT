package com.krakedev.jwt.services;

import java.util.Locale;

import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.krakedev.jwt.entidades.Usuario;
import com.krakedev.jwt.exceptions.CredencialesInvalidasException;
import com.krakedev.jwt.exceptions.RolInvalidoException;
import com.krakedev.jwt.exceptions.UsuarioDuplicadoException;
import com.krakedev.jwt.repositories.UsuarioRepository;

/**
 * Reglas de negocio de identidad del refugio.
 *
 * Hito 1 (vulnerable, ya reemplazado):
 *     usuario.setPassword(password);
 *     return usuario.getPassword().equals(password);
 *
 * Hito 2 (implementación actual): la contraseña se persiste como hash BCrypt
 * con salt aleatorio y se verifica con BCrypt.checkpw.
 */
@Service
public class UsuarioService {

    public static final String ROL_ADMIN = "ADMIN";
    public static final String ROL_USER = "USER";

    /** Coste de BCrypt: 2^12 iteraciones. */
    private static final int BCRYPT_COST = 12;

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Registra un usuario guardando SOLO el hash BCrypt de su contraseña.
     */
    @Transactional
    public Usuario registrar(String username, String password, String rol) {
        String usernameNormalizado = username == null ? "" : username.trim();
        String rolNormalizado = normalizarRol(rol);

        if (usernameNormalizado.isEmpty()) {
            throw new IllegalArgumentException("El username no puede estar vacío");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("La password no puede estar vacía");
        }
        if (usuarioRepository.existsByUsername(usernameNormalizado)) {
            throw new UsuarioDuplicadoException(usernameNormalizado);
        }

        String hash = BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_COST));

        Usuario usuario = new Usuario();
        usuario.setUsername(usernameNormalizado);
        usuario.setPassword(hash);
        usuario.setRol(rolNormalizado);

        return usuarioRepository.save(usuario);
    }

    /**
     * Verifica las credenciales comparando la contraseña recibida contra el
     * hash almacenado. La comparación es criptográfica, no un .equals().
     */
    @Transactional(readOnly = true)
    public Usuario autenticar(String username, String password) {
        String usernameNormalizado = username == null ? "" : username.trim();

        Usuario usuario = usuarioRepository.findByUsername(usernameNormalizado)
                .orElseThrow(CredencialesInvalidasException::new);

        if (!passwordCoincide(password, usuario.getPassword())) {
            throw new CredencialesInvalidasException();
        }
        return usuario;
    }

    private boolean passwordCoincide(String passwordRecibida, String hashAlmacenado) {
        if (passwordRecibida == null || hashAlmacenado == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(passwordRecibida, hashAlmacenado);
        } catch (IllegalArgumentException e) {
            // El valor almacenado no es un hash BCrypt válido (por ejemplo, un
            // registro heredado del Hito 1 en texto plano). Se rechaza el acceso.
            log.warn("Se rechazó un login porque el registro no contiene un hash BCrypt válido");
            return false;
        }
    }

    private String normalizarRol(String rol) {
        String rolNormalizado = rol == null ? "" : rol.trim().toUpperCase(Locale.ROOT);
        if (!ROL_ADMIN.equals(rolNormalizado) && !ROL_USER.equals(rolNormalizado)) {
            throw new RolInvalidoException(rol);
        }
        return rolNormalizado;
    }
}
