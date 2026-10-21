package com.foody.ordertracker.entity;

public enum OrderStatus {
    RECEBIDO, EM_PREPARO, SAIU_PARA_ENTREGA, ENTREGUE, CANCELADO;

    public boolean canTransitionTo(OrderStatus target) {
        return switch (this) {
            case RECEBIDO -> target == EM_PREPARO || target == CANCELADO;
            case EM_PREPARO -> target == SAIU_PARA_ENTREGA || target == CANCELADO;
            case SAIU_PARA_ENTREGA -> target == ENTREGUE || target == CANCELADO;
            case ENTREGUE, CANCELADO -> false;
        };
    }
}