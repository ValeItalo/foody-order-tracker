package com.foody.ordertracker;

import com.foody.ordertracker.repository.OrderRepository;
import com.foody.ordertracker.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Sem @Transactional de propósito: commits reais, como em produção.
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:sqlite:target/test-orders.db?foreign_keys=on")
@DisplayName("Pedidos (HTTP + SQLite)")
class OrderControllerIntegrationTest {

    private static final String VALID_ORDER = """
            {"customerName":"Maria Silva","deliveryAddress":"Rua A, 100",
             "items":[{"productName":"Pizza","quantity":2},
                      {"productName":"Refrigerante","quantity":1}]}
            """;

    @Autowired MockMvc mockMvc;
    @Autowired OrderRepository orderRepository;
    @Autowired UserRepository userRepository;

    @BeforeEach
    void cleanBefore() {
        cleanDatabase();
    }

    @AfterEach
    void cleanAfter() {
        // Não deixa pedidos para trás: o AuthSecurityIntegrationTest apaga usuários no mesmo banco.
        cleanDatabase();
    }

    private void cleanDatabase() {
        // Ordem importa: pedidos referenciam usuários (created_by).
        orderRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String authenticate() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ana","email":"ana@foody.com","password":"senha-segura-1"}
                                """))
                .andExpect(status().isCreated());

        String body = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ana@foody.com","password":"senha-segura-1"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private int createOrder(String token) throws Exception {
        String body = mockMvc.perform(post("/orders")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_ORDER))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Nested
    @DisplayName("Criação")
    class Create {

        @Test
        @DisplayName("dados válidos, então 201, status RECEBIDO e itens devolvidos")
        void shouldCreateOrderAsReceived() throws Exception {
            String token = authenticate();

            mockMvc.perform(post("/orders")
                            .header(HttpHeaders.AUTHORIZATION, bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_ORDER))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber())
                    .andExpect(jsonPath("$.status").value("RECEBIDO"))
                    .andExpect(jsonPath("$.customerName").value("Maria Silva"))
                    .andExpect(jsonPath("$.deliveryAddress").value("Rua A, 100"))
                    .andExpect(jsonPath("$.createdAt").isNotEmpty())
                    .andExpect(jsonPath("$.items.length()").value(2))
                    .andExpect(jsonPath("$.items[0].productName").value("Pizza"))
                    .andExpect(jsonPath("$.items[0].quantity").value(2))
                    .andExpect(jsonPath("$.createdBy").doesNotExist());
        }

        @Test
        @DisplayName("sem token, então 401")
        void shouldReturn401WithoutToken() throws Exception {
            mockMvc.perform(post("/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(VALID_ORDER))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401));
        }
    }

    @Nested
    @DisplayName("Consulta")
    class Read {

        @Test
        @DisplayName("sem pedidos, então 200 com lista vazia")
        void shouldReturnEmptyList() throws Exception {
            String token = authenticate();

            mockMvc.perform(get("/orders")
                            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("com dois pedidos, então a lista traz os dois com itens")
        void shouldListCreatedOrders() throws Exception {
            String token = authenticate();
            createOrder(token);
            createOrder(token);

            mockMvc.perform(get("/orders")
                            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].items.length()").value(2));
        }

        @Test
        @DisplayName("ID existente, então 200 com o pedido")
        void shouldReturnOrderById() throws Exception {
            String token = authenticate();
            int id = createOrder(token);

            mockMvc.perform(get("/orders/{id}", id)
                            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.items.length()").value(2));
        }
    }
}