import { request } from './client'
import type { LoginRequest, LoginResponse, RegisterRequest } from './types'

export function login(data: LoginRequest): Promise<LoginResponse> {
    return request<LoginResponse>('/auth/login', {
        method: 'POST',
        body: JSON.stringify(data),
    })
}

export function register(data: RegisterRequest): Promise<void> {
    return request<void>('/auth/register', {
        method: 'POST',
        body: JSON.stringify(data),
    })
}