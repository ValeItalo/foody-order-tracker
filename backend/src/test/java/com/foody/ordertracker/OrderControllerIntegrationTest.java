package com.foody.ordertracker;

import com.foody.ordertracker.entity.Order;
import com.foody.ordertracker.entity.OrderStatus;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

    private String orderJson(String customerName) {
        return """
                {"customerName":"%s","deliveryAddress":"Rua A, 100",
                 "items":[{"productName":"Pizza","quantity":1}]}
                """.formatted(customerName);
    }

    private int createOrderFor(String token, String customerName) throws Exception {
        String body = mockMvc.perform(post("/orders")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(orderJson(customerName)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    // Ainda não existe PATCH; mudamos o status direto no banco só para preparar o cenário do filtro.
    private void forceStatus(int id, OrderStatus newStatus) {
        Order order = orderRepository.findWithItemsById((long) id).orElseThrow();
        order.updateStatus(newStatus);
        orderRepository.save(order);
    }

    // Envia um POST inválido, exige 400 padronizado e devolve a mensagem.
    private String postExpecting400(String token, String json) throws Exception {
        String body = mockMvc.perform(post("/orders")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.message");
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

    @Nested
    @DisplayName("Validação do payload")
    class Validation {

        @Test
        @DisplayName("nome do cliente em branco, então 400 citando customerName")
        void shouldRejectBlankCustomerName() throws Exception {
            String token = authenticate();

            String message = postExpecting400(token, """
                    {"customerName":"  ","deliveryAddress":"Rua A, 100",
                     "items":[{"productName":"Pizza","quantity":1}]}
                    """);

            assertThat(message).contains("customerName");
        }

        @Test
        @DisplayName("lista de itens vazia, então 400 citando items")
        void shouldRejectEmptyItems() throws Exception {
            String token = authenticate();

            String message = postExpecting400(token, """
                    {"customerName":"Maria","deliveryAddress":"Rua A, 100","items":[]}
                    """);

            assertThat(message).contains("items");
        }

        @Test
        @DisplayName("quantidade zero em um item, então 400 citando items[0].quantity")
        void shouldRejectZeroQuantity() throws Exception {
            String token = authenticate();

            String message = postExpecting400(token, """
                    {"customerName":"Maria","deliveryAddress":"Rua A, 100",
                     "items":[{"productName":"Pizza","quantity":0}]}
                    """);

            assertThat(message).contains("items[0].quantity");
        }

        @Test
        @DisplayName("quantidade ausente, então 400 (não vira 0 em silêncio)")
        void shouldRejectMissingQuantity() throws Exception {
            String token = authenticate();

            String message = postExpecting400(token, """
                    {"customerName":"Maria","deliveryAddress":"Rua A, 100",
                     "items":[{"productName":"Pizza"}]}
                    """);

            assertThat(message).contains("items[0].quantity");
        }
    }

    @Nested
    @DisplayName("Busca por ID inexistente")
    class NotFound {

        @Test
        @DisplayName("ID que não existe, então 404 padronizado citando o ID")
        void shouldReturn404ForUnknownId() throws Exception {
            String token = authenticate();

            String body = mockMvc.perform(get("/orders/{id}", 999999)
                            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andReturn().getResponse().getContentAsString();

            String message = JsonPath.read(body, "$.message");
            assertThat(message).contains("999999");
        }
    }

    @Nested
    @DisplayName("Filtro e ordenação da lista")
    class ListFilters {

        @Test
        @DisplayName("?status=EM_PREPARO, então só os pedidos nesse status")
        void shouldFilterByStatus() throws Exception {
            String token = authenticate();
            createOrderFor(token, "Cliente Um");
            int second = createOrderFor(token, "Cliente Dois");
            forceStatus(second, OrderStatus.EM_PREPARO);

            mockMvc.perform(get("/orders")
                            .param("status", "EM_PREPARO")
                            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(second))
                    .andExpect(jsonPath("$[0].status").value("EM_PREPARO"));
        }

        @Test
        @DisplayName("?status= sem nenhum pedido nesse status, então 200 com lista vazia")
        void shouldReturnEmptyListWhenNoOrderMatches() throws Exception {
            String token = authenticate();
            createOrderFor(token, "Cliente Um");

            mockMvc.perform(get("/orders")
                            .param("status", "ENTREGUE")
                            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @DisplayName("sem filtro, o pedido mais recente vem primeiro")
        void shouldListNewestFirst() throws Exception {
            String token = authenticate();
            createOrderFor(token, "Mais Antigo");
            Thread.sleep(50); // garante createdAt diferente entre os dois
            createOrderFor(token, "Mais Novo");

            mockMvc.perform(get("/orders")
                            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].customerName").value("Mais Novo"))
                    .andExpect(jsonPath("$[1].customerName").value("Mais Antigo"));
        }

        @Test
        @DisplayName("?status com valor inválido, então 400 padronizado listando os valores aceitos")
        void shouldReturn400ForInvalidStatus() throws Exception {
            String token = authenticate();

            String body = mockMvc.perform(get("/orders")
                            .param("status", "INVALIDO")
                            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andReturn().getResponse().getContentAsString();

            String message = JsonPath.read(body, "$.message");
            assertThat(message).contains("status", "INVALIDO", "EM_PREPARO");
        }
    }
    @Nested
    @DisplayName("Atualização de status")
    class UpdateStatus {

        private static final String TO_PREPARING = "{\"status\":\"EM_PREPARO\"}";

        @Test
        @DisplayName("transição válida, então 200 com o novo status, persistido")
        void shouldUpdateStatusAndPersist() throws Exception {
            String token = authenticate();
            int id = createOrder(token);

            mockMvc.perform(patch("/orders/{id}/status", id)
                            .header(HttpHeaders.AUTHORIZATION, bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(TO_PREPARING))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.status").value("EM_PREPARO"))
                    .andExpect(jsonPath("$.items.length()").value(2));

            // Prova que gravou no banco (dirty checking), e não só na resposta.
            mockMvc.perform(get("/orders/{id}", id)
                            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("EM_PREPARO"));
        }

        @Test
        @DisplayName("pulo de etapa, então 409 citando origem e destino, e o status não muda")
        void shouldReturn409ForSkippedStep() throws Exception {
            String token = authenticate();
            int id = createOrder(token);

            String body = mockMvc.perform(patch("/orders/{id}/status", id)
                            .header(HttpHeaders.AUTHORIZATION, bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"ENTREGUE\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andReturn().getResponse().getContentAsString();

            String message = JsonPath.read(body, "$.message");
            assertThat(message).contains("RECEBIDO", "ENTREGUE");

            mockMvc.perform(get("/orders/{id}", id)
                            .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                    .andExpect(jsonPath("$.status").value("RECEBIDO"));
        }

        @Test
        @DisplayName("mesmo status repetido, então 409")
        void shouldReturn409ForSameStatus() throws Exception {
            String token = authenticate();
            int id = createOrder(token);

            mockMvc.perform(patch("/orders/{id}/status", id)
                            .header(HttpHeaders.AUTHORIZATION, bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"RECEBIDO\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409));
        }

        @Test
        @DisplayName("pedido inexistente, então 404 padronizado citando o ID")
        void shouldReturn404ForUnknownOrder() throws Exception {
            String token = authenticate();

            String body = mockMvc.perform(patch("/orders/{id}/status", 999999)
                            .header(HttpHeaders.AUTHORIZATION, bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(TO_PREPARING))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andReturn().getResponse().getContentAsString();

            String message = JsonPath.read(body, "$.message");
            assertThat(message).contains("999999");
        }

        @Test
        @DisplayName("sem token, então 401")
        void shouldReturn401WithoutToken() throws Exception {
            mockMvc.perform(patch("/orders/{id}/status", 1)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(TO_PREPARING))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.status").value(401));
        }
    }
}