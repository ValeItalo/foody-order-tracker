package com.foody.ordertracker;

import com.foody.ordertracker.entity.User;
import com.foody.ordertracker.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Sem @Transactional de propósito: queremos commits reais, como em produção.
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:sqlite:target/test-orders.db?foreign_keys=on")
@DisplayName("Autenticação e proteção de rotas (HTTP + SQLite)")
class AuthSecurityIntegrationTest {

    private static final String PROTECTED_PATH = "/orders";

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;

    @BeforeEach
    void cleanUsers() {
        // Banco isolado em target/. Esta classe não grava pedidos; a classe de pedidos limpa os seus.
        userRepository.deleteAll();
    }

    private ResultActions register(String name, String email, String password) throws Exception {
        return mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"%s","email":"%s","password":"%s"}
                        """.formatted(name, email, password)));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}
                        """.formatted(email, password)));
    }

    private String registerAndLogin(String email, String password) throws Exception {
        register("Ana", email, password).andExpect(status().isCreated());
        String body = login(email, password)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    @Nested
    @DisplayName("Cadastro")
    class Register {

        @Test
        @DisplayName("dados válidos, então 201, e-mail normalizado e sem expor a senha")
        void shouldRegisterAndNormalizeEmail() throws Exception {
            register("Ana", "ANA@Foody.com", "senha-segura-1")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email").value("ana@foody.com"))
                    .andExpect(jsonPath("$.password").doesNotExist())
                    .andExpect(jsonPath("$.passwordHash").doesNotExist());
        }

        @Test
        @DisplayName("e-mail já cadastrado (mesmo com caixa diferente), então 409")
        void shouldReturnConflictForDuplicateEmail() throws Exception {
            register("Ana", "ana@foody.com", "senha-segura-1").andExpect(status().isCreated());

            register("Outra", "ANA@foody.com", "outra-senha-1")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409));
        }

        @Test
        @DisplayName("e-mail inválido e senha curta, então 400")
        void shouldReturnBadRequestForInvalidPayload() throws Exception {
            register("Ana", "nao-e-email", "123")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }
    }

    @Nested
    @DisplayName("Login")
    class Login {

        @Test
        @DisplayName("senha errada, então 401")
        void shouldReturnUnauthorizedForWrongPassword() throws Exception {
            register("Ana", "ana@foody.com", "senha-segura-1").andExpect(status().isCreated());

            login("ana@foody.com", "senha-errada-1")
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401));
        }

        @Test
        @DisplayName("e-mail inexistente, então 401")
        void shouldReturnUnauthorizedForUnknownEmail() throws Exception {
            login("ninguem@foody.com", "senha-segura-1")
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Rotas protegidas")
    class ProtectedRoutes {

        @Test
        @DisplayName("sem token, então 401 com ErrorResponse padronizado")
        void shouldReturn401WithoutToken() throws Exception {
            mockMvc.perform(get(PROTECTED_PATH))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401))
                    .andExpect(jsonPath("$.message").isNotEmpty());
        }

        @Test
        @DisplayName("token inválido, então 401")
        void shouldReturn401WithInvalidToken() throws Exception {
            mockMvc.perform(get(PROTECTED_PATH)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer token.invalido.qualquer"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("header Authorization com esquema diferente de Bearer, então 401")
        void shouldReturn401ForNonBearerScheme() throws Exception {
            mockMvc.perform(get(PROTECTED_PATH)
                            .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpzZW5oYQ=="))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("token válido, então passa pela segurança (200 em GET /orders, não 401)")
        void shouldPassSecurityWithValidToken() throws Exception {
            String token = registerAndLogin("ana@foody.com", "senha-segura-1");

            mockMvc.perform(get(PROTECTED_PATH)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("token válido de usuário removido, então 401")
        void shouldReturn401ForDeletedUser() throws Exception {
            String token = registerAndLogin("ana@foody.com", "senha-segura-1");
            userRepository.deleteAll();

            mockMvc.perform(get(PROTECTED_PATH)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Banco (premissa do catch do AuthService)")
    class Database {

        @Test
        @DisplayName("dois usuários com o mesmo e-mail, então o banco recusa o segundo (constraint UNIQUE)")
        void shouldRejectDuplicateEmailAtDatabaseLevel() {
            userRepository.saveAndFlush(new User("Ana", "ana@foody.com", "hash"));

            assertThatThrownBy(() ->
                    userRepository.saveAndFlush(new User("Outra", "ana@foody.com", "hash")))
                    .isInstanceOf(DataAccessException.class);
        }
    }
}