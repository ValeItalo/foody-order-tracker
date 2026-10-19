package com.foody.ordertracker.exception;

import com.foody.ordertracker.entity.OrderStatus;

public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(OrderStatus from, OrderStatus to) {
        super("Transição de status inválida: " + from + " -> " + to);
    }
}