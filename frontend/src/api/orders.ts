import { request } from './client';
import type { CreateOrderRequest, Order, OrderStatus } from './types';

export function listOrders(status?: OrderStatus): Promise<Order[]> {
  const query = status ? `?status=${status}` : '';
  return request<Order[]>(`/orders${query}`);
}

export function createOrder(data: CreateOrderRequest): Promise<Order> {
  return request<Order>('/orders', {
    method: 'POST',
    body: JSON.stringify(data),
  });
}

export function updateOrderStatus(
  id: Order['id'],
  status: OrderStatus,
): Promise<Order> {
  return request<Order>(`/orders/${id}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status }),
  });
}