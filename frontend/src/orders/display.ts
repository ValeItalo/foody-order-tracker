import type { ChipProps } from '@mui/material';
import type { Order, OrderStatus } from '../api/types';

export const STATUS_LABEL: Record<OrderStatus, string> = {
    RECEBIDO: 'Recebido',
    EM_PREPARO: 'Em preparo',
    SAIU_PARA_ENTREGA: 'Saiu para entrega',
    ENTREGUE: 'Entregue',
    CANCELADO: 'Cancelado',
};

export const STATUS_COLOR: Record<OrderStatus, ChipProps['color']> = {
    RECEBIDO: 'warning',
    EM_PREPARO: 'info',
    SAIU_PARA_ENTREGA: 'secondary',
    ENTREGUE: 'success',
    CANCELADO: 'default',
};

export function formatDateTime(iso: string): string {
    return new Date(iso).toLocaleString('pt-BR', {
        dateStyle: 'short',
        timeStyle: 'short',
    });
}

export function itemsLabel(order: Order): string {
    const total = order.items.reduce((sum, item) => sum + item.quantity, 0);
    return total === 1 ? '1 item' : `${total} itens`;
}