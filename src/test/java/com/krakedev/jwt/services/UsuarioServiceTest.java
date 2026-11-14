package com.krakedev.jwt.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.krakedev.jwt.entidades.Usuario;
import com.krakedev.jwt.exceptions.CredencialesInvalidasException;
import com.krakedev.jwt.exceptions.RolInvalidoException;
import com.krakedev.jwt.exceptions.UsuarioDuplicadoException;
import com.krakedev.jwt.repositories.UsuarioRepository;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    void registraUnUsuarioConHashBcryptYNoConTextoPlano() {
        when(usuarioRepository.existsByUsername("adoptante_pedro")).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        Usuario guardado = usuarioService.registrar(
                "adoptante_pedro", "mi_clave_secreta_123", "user");

        assertEquals("adoptante_pedro", guardado.getUsername());
        assertEquals("USER", guardado.getRol());
        assertNotEquals("mi_clave_secreta_123", guardado.getPassword());
        assertTrue(guardado.getPassword().startsWith("$2a$"),
                "El hash BCrypt debe comenzar con $2a$");
        assertTrue(BCrypt.checkpw("mi_clave_secreta_123", guardado.getPassword()));
    }

    @Test
    void noRegistraDosVecesElMismoUsername() {
        when(usuarioRepository.existsByUsername("duplicado")).thenReturn(true);

        assertThrows(UsuarioDuplicadoException.class,
                () -> usuarioService.registrar("duplicado", "clave_segura_123", "USER"));

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void rechazaRolesFueraDeAdminYUser() {
        assertThrows(RolInvalidoException.class,
                () -> usuarioService.registrar("supervisor", "clave_segura_123", "SUPERVISOR"));
    }

    @Test
    void autenticaCuandoLaPasswordCoincideConElHash() {
        String hash = BCrypt.hashpw("mi_clave_secreta_123", BCrypt.gensalt(4));
        Usuario usuario = new Usuario("voluntario_carlos", hash, "ADMIN");
        when(usuarioRepository.findByUsername("voluntario_carlos")).thenReturn(Optional.of(usuario));

        Usuario autenticado = usuarioService.autenticar("voluntario_carlos", "mi_clave_secreta_123");

        assertEquals("voluntario_carlos", autenticado.getUsername());
        assertEquals("ADMIN", autenticado.getRol());
    }

    @Test
    void rechazaUnaPasswordIncorrecta() {
        String hash = BCrypt.hashpw("mi_clave_secreta_123", BCrypt.gensalt(4));
        Usuario usuario = new Usuario("voluntario_carlos", hash, "ADMIN");
        when(usuarioRepository.findByUsername("voluntario_carlos")).thenReturn(Optional.of(usuario));

        assertThrows(CredencialesInvalidasException.class,
                () -> usuarioService.autenticar("voluntario_carlos", "clave_incorrecta"));
    }

    @Test
    void rechazaUnUsuarioInexistente() {
        when(usuarioRepository.findByUsername("fantasma")).thenReturn(Optional.empty());

        assertThrows(CredencialesInvalidasException.class,
                () -> usuarioService.autenticar("fantasma", "cualquier_clave"));
    }

    @Test
    void rechazaUnRegistroHeredadoConPasswordEnTextoPlano() {
        // Simula una fila del Hito 1 que nunca fue migrada a BCrypt.
        Usuario legado = new Usuario("adoptante_pedro", "mi_clave_secreta_123", "USER");
        when(usuarioRepository.findByUsername("adoptante_pedro")).thenReturn(Optional.of(legado));

        assertThrows(CredencialesInvalidasException.class,
                () -> usuarioService.autenticar("adoptante_pedro", "mi_clave_secreta_123"));
    }
}
