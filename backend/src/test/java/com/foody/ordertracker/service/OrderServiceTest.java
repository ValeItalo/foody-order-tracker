package com.foody.ordertracker.service;

import com.foody.ordertracker.dto.CreateOrderRequest;
import com.foody.ordertracker.dto.OrderItemRequest;
import com.foody.ordertracker.entity.Order;
import com.foody.ordertracker.entity.OrderStatus;
import com.foody.ordertracker.entity.User;
import com.foody.ordertracker.exception.OrderNotFoundException;
import com.foody.ordertracker.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    private final User user = new User("Maria", "maria@teste.com", "hash");

    @Test
    void createSavesOrderAsReceivedWithItemsAndCreator() {
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        CreateOrderRequest request = new CreateOrderRequest(
                "  Joao  ", "  Rua A, 10  ",
                List.of(new OrderItemRequest("  Pizza  ", 2), new OrderItemRequest("Suco", 1)));

        Order result = orderService.create(request, user);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        Order saved = captor.getValue();
        assertThat(saved).isSameAs(result);
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.RECEBIDO);
        assertThat(saved.getCreatedBy()).isSameAs(user);
        assertThat(saved.getCustomerName()).isEqualTo("Joao");
        assertThat(saved.getDeliveryAddress()).isEqualTo("Rua A, 10");
        assertThat(saved.getItems()).hasSize(2);
        assertThat(saved.getItems().get(0).getProductName()).isEqualTo("Pizza");
        assertThat(saved.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(saved.getItems().get(1).getProductName()).isEqualTo("Suco");
    }

    @Test
    void findAllWithoutStatusUsesUnfilteredQuery() {
        Order order = new Order(user, "Joao", "Rua A");
        when(orderRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(order));

        List<Order> result = orderService.findAll(null);

        assertThat(result).containsExactly(order);
        verify(orderRepository, never()).findByStatusOrderByCreatedAtDesc(any());
    }

    @Test
    void findAllWithStatusUsesFilteredQuery() {
        Order order = new Order(user, "Joao", "Rua A");
        when(orderRepository.findByStatusOrderByCreatedAtDesc(OrderStatus.RECEBIDO))
                .thenReturn(List.of(order));

        List<Order> result = orderService.findAll(OrderStatus.RECEBIDO);

        assertThat(result).containsExactly(order);
        verify(orderRepository, never()).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void findByIdReturnsOrderWhenItExists() {
        Order order = new Order(user, "Joao", "Rua A");
        when(orderRepository.findWithItemsById(1L)).thenReturn(Optional.of(order));

        assertThat(orderService.findById(1L)).isSameAs(order);
    }

    @Test
    void findByIdThrowsWhenOrderDoesNotExist() {
        when(orderRepository.findWithItemsById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.findById(99L))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("99");
    }
}