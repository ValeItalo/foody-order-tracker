package com.foody.ordertracker.exception;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(Long id) {
        super("Pedido não encontrado: " + id);
    }
}