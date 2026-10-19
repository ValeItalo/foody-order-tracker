import { request } from './client';
import type { Order, OrderStatus } from './types';

export function listOrders(status?: OrderStatus): Promise<Order[]> {
  const query = status ? `?status=${status}` : '';
  return request<Order[]>(`/orders${query}`);
}