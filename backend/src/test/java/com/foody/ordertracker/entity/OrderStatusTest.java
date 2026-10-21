package com.foody.ordertracker.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class OrderStatusTest {

    @ParameterizedTest(name = "{0} -> {1} e permitido")
    @DisplayName("Transicoes permitidas")
    @CsvSource({
            "RECEBIDO, EM_PREPARO",
            "EM_PREPARO, SAIU_PARA_ENTREGA",
            "SAIU_PARA_ENTREGA, ENTREGUE",
            "RECEBIDO, CANCELADO",
            "EM_PREPARO, CANCELADO",
            "SAIU_PARA_ENTREGA, CANCELADO"
    })
    void allowsValidTransitions(OrderStatus from, OrderStatus to) {
        assertThat(from.canTransitionTo(to)).isTrue();
    }

    @ParameterizedTest(name = "{0} -> {1} e proibido (pulo)")
    @DisplayName("Pulos de etapa sao proibidos")
    @CsvSource({
            "RECEBIDO, SAIU_PARA_ENTREGA",
            "RECEBIDO, ENTREGUE",
            "EM_PREPARO, ENTREGUE"
    })
    void rejectsSkippingSteps(OrderStatus from, OrderStatus to) {
        assertThat(from.canTransitionTo(to)).isFalse();
    }

    @ParameterizedTest(name = "{0} -> {1} e proibido (retorno)")
    @DisplayName("Retornos a etapas anteriores sao proibidos")
    @CsvSource({
            "EM_PREPARO, RECEBIDO",
            "SAIU_PARA_ENTREGA, RECEBIDO",
            "SAIU_PARA_ENTREGA, EM_PREPARO"
    })
    void rejectsGoingBack(OrderStatus from, OrderStatus to) {
        assertThat(from.canTransitionTo(to)).isFalse();
    }

    @ParameterizedTest(name = "{0} -> {0} e proibido (repeticao)")
    @DisplayName("Repetir o mesmo status e proibido")
    @EnumSource(OrderStatus.class)
    void rejectsSameStatus(OrderStatus status) {
        assertThat(status.canTransitionTo(status)).isFalse();
    }

    @ParameterizedTest(name = "{0} -> {1} e proibido (status final)")
    @DisplayName("Status finais nao tem saida")
    @CsvSource({
            "ENTREGUE, RECEBIDO",
            "ENTREGUE, EM_PREPARO",
            "ENTREGUE, SAIU_PARA_ENTREGA",
            "ENTREGUE, CANCELADO",
            "CANCELADO, RECEBIDO",
            "CANCELADO, EM_PREPARO",
            "CANCELADO, SAIU_PARA_ENTREGA",
            "CANCELADO, ENTREGUE"
    })
    void rejectsLeavingFinalStatus(OrderStatus from, OrderStatus to) {
        assertThat(from.canTransitionTo(to)).isFalse();
    }
}