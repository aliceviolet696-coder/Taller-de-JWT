package com.krakedev.jwt.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

/**
 * Prueba end-to-end de los cuatro hitos contra H2 en memoria.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    private static final String PASSWORD = "mi_clave_secreta_123";

    @Autowired
    private MockMvc mockMvc;

    private String usernameUnico(String prefijo) {
        return prefijo + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private void registrar(String username, String password, String rol) throws Exception {
        mockMvc.perform(post("/api/auth/registrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"%s","password":"%s","rol":"%s"}
                        """.formatted(username, password, rol)))
                .andExpect(status().isCreated());
    }

    private String login(String username, String password) throws Exception {
        String cuerpo = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"%s","password":"%s"}
                        """.formatted(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.validezSegundos").value(1800))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return JsonPath.read(cuerpo, "$.token");
    }

    private String alterarUnCaracter(String token) {
        int posicion = token.lastIndexOf('.') + 3;
        char original = token.charAt(posicion);
        char reemplazo = original == 'a' ? 'b' : 'a';
        return token.substring(0, posicion) + reemplazo + token.substring(posicion + 1);
    }

    @Test
    void flujoCompletoDelVoluntarioAdmin() throws Exception {
        String username = usernameUnico("voluntario");

        mockMvc.perform(post("/api/auth/registrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"%s","password":"%s","rol":"ADMIN"}
                        """.formatted(username, PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.rol").value("ADMIN"))
                .andExpect(jsonPath("$.algoritmoPassword").value("BCrypt (costo 12)"))
                .andExpect(jsonPath("$.id").isNumber());

        String token = login(username, PASSWORD);

        mockMvc.perform(get("/api/auth/perfil")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.rol").value("ADMIN"))
                .andExpect(jsonPath("$.autenticado").value(true))
                .andExpect(jsonPath("$.mensaje", containsString("Hola, " + username)))
                .andExpect(jsonPath("$.mensaje", containsString("panel de administraci")));

        // Hito 4: un solo carácter alterado rompe la firma -> 401.
        String tokenAlterado = alterarUnCaracter(token);
        mockMvc.perform(get("/api/auth/perfil")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAlterado))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensaje", containsString("firma")));

        // Sin cabecera Authorization -> 401.
        mockMvc.perform(get("/api/auth/perfil"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void elAdoptanteRecibeElSaludoDeUsuario() throws Exception {
        String username = usernameUnico("adoptante");
        registrar(username, PASSWORD, "USER");

        String token = login(username, PASSWORD);

        mockMvc.perform(get("/api/auth/perfil")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("USER"))
                .andExpect(jsonPath("$.mensaje", containsString("solicitud de adopci")));
    }

    @Test
    void rechazaCredencialesIncorrectas() throws Exception {
        String username = usernameUnico("adoptante");
        registrar(username, PASSWORD, "USER");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"%s","password":"clave_equivocada"}
                        """.formatted(username)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void rechazaUnRegistroDuplicado() throws Exception {
        String username = usernameUnico("duplicado");
        registrar(username, PASSWORD, "USER");

        mockMvc.perform(post("/api/auth/registrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"%s","password":"%s","rol":"USER"}
                        """.formatted(username, PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void validaElCuerpoDelRegistro() throws Exception {
        mockMvc.perform(post("/api/auth/registrar")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"ab","password":"corta","rol":"INVITADO"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detalles.username").exists())
                .andExpect(jsonPath("$.detalles.password").exists())
                .andExpect(jsonPath("$.detalles.rol").exists());
    }
}
