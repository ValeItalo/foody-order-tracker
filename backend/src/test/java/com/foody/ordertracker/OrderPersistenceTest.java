package com.foody.ordertracker;

import com.foody.ordertracker.entity.Order;
import com.foody.ordertracker.entity.OrderStatus;
import com.foody.ordertracker.entity.User;
import com.foody.ordertracker.repository.OrderRepository;
import com.foody.ordertracker.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:sqlite:target/test-orders.db?foreign_keys=on")
@DisplayName("Persistência de pedidos (SQLite + JPA)")
class OrderPersistenceTest {

    @Autowired UserRepository userRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired EntityManager em;

    @Nested
    @DisplayName("Dado um pedido salvo com dois itens")
    class GivenSavedOrderWithItems {

        private Long orderId;

        @BeforeEach
        void saveOrderAndClearContext() {
            // Given: um operador e um pedido com 2 itens
            User user = userRepository.save(new User("Operador", "op@foody.com", "hash"));
            Order order = new Order(user, "Maria", "Rua A, 10");
            order.addItem("Pizza", 2);
            order.addItem("Refrigerante", 1);
            orderId = orderRepository.save(order).getId();

            // Força o INSERT e descarta o cache de 1º nível,
            // para a leitura abaixo vir de fato do banco.
            em.flush();
            em.clear();
        }

        @Test
        @DisplayName("quando recarregado, então nasce com status RECEBIDO")
        void shouldStartWithReceivedStatus() {
            // When
            Order found = orderRepository.findWithItemsById(orderId).orElseThrow();

            // Then
            assertThat(found.getStatus()).isEqualTo(OrderStatus.RECEBIDO);
        }

        @Test
        @DisplayName("quando recarregado, então as datas de criação e atualização são preenchidas")
        void shouldHaveCreationAndUpdateTimestamps() {
            // When
            Order found = orderRepository.findWithItemsById(orderId).orElseThrow();

            // Then: valida o mapeamento de LocalDateTime no SQLite
            assertThat(found.getCreatedAt()).isNotNull();
            assertThat(found.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("quando recarregado, então traz os 2 itens com nome e quantidade")
        void shouldLoadItemsTogetherWithOrder() {
            // When
            Order found = orderRepository.findWithItemsById(orderId).orElseThrow();

            // Then
            assertThat(found.getItems())
                    .extracting("productName", "quantity")
                    .containsExactlyInAnyOrder(
                            org.assertj.core.groups.Tuple.tuple("Pizza", 2),
                            org.assertj.core.groups.Tuple.tuple("Refrigerante", 1));
        }
    }
}