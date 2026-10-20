import type { OrderStatus } from '../api/types';

const NEXT_STATUS: Partial<Record<OrderStatus, OrderStatus>> = {
    RECEBIDO: 'EM_PREPARO',
    EM_PREPARO: 'SAIU_PARA_ENTREGA',
    SAIU_PARA_ENTREGA: 'ENTREGUE',
};

export function nextStatus(status: OrderStatus): OrderStatus | undefined {
    return NEXT_STATUS[status];
}

export function canCancel(status: OrderStatus): boolean {
    return status !== 'ENTREGUE' && status !== 'CANCELADO';
}