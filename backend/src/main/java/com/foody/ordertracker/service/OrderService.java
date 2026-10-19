package com.foody.ordertracker.service;

import com.foody.ordertracker.dto.CreateOrderRequest;
import com.foody.ordertracker.dto.OrderItemRequest;
import com.foody.ordertracker.entity.Order;
import com.foody.ordertracker.entity.OrderStatus;
import com.foody.ordertracker.entity.User;
import com.foody.ordertracker.exception.InvalidStatusTransitionException;
import com.foody.ordertracker.exception.OrderNotFoundException;
import com.foody.ordertracker.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional
    public Order create(CreateOrderRequest request, User createdBy) {
        Order order = new Order(
                createdBy,
                request.customerName().trim(),
                request.deliveryAddress().trim()
        );
        for (OrderItemRequest item : request.items()) {
            order.addItem(item.productName().trim(), item.quantity());
        }
        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public List<Order> findAll(OrderStatus status) {
        if (status == null) {
            return orderRepository.findAllByOrderByCreatedAtDesc();
        }
        return orderRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    @Transactional(readOnly = true)
    public Order findById(Long id) {
        return orderRepository.findWithItemsById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }


    @Transactional
    public Order updateStatus(Long id, OrderStatus newStatus) {
        Order order = findById(id);
        if (!order.getStatus().canTransitionTo(newStatus)) {
            throw new InvalidStatusTransitionException(order.getStatus(), newStatus);
        }
        order.updateStatus(newStatus);
        return order;
    }
}