export const ORDER_STATUSES = [
    'RECEBIDO',
    'EM_PREPARO',
    'SAIU_PARA_ENTREGA',
    'ENTREGUE',
    'CANCELADO',
] as const

export type OrderStatus = (typeof ORDER_STATUSES)[number]

export interface OrderItem {
    productName: string
    quantity: number
}

export interface Order {
    id: number
    customerName: string
    deliveryAddress: string
    status: OrderStatus
    createdAt: string
    updatedAt: string
    items: OrderItem[]
}

export interface CreateOrderRequest {
    customerName: string
    deliveryAddress: string
    items: OrderItem[]
}

export interface RegisterRequest {
    name: string
    email: string
    password: string
}

export interface LoginRequest {
    email: string
    password: string
}

export interface LoginResponse {
    token: string
    type: string
}

export interface ErrorResponse {
    status: number
    message: string
    timestamp: string
}