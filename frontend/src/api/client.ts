import { getToken } from '../auth/tokenStorage'
import type { ErrorResponse } from './types'

const API_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080'

export class ApiError extends Error {
    status: number

    constructor(status: number, message: string) {
        super(message)
        this.name = 'ApiError'
        this.status = status
    }
}

export async function request<T>(
    path: string,
    options: RequestInit = {},
): Promise<T> {
    const headers = new Headers(options.headers)
    if (options.body) {
        headers.set('Content-Type', 'application/json')
    }
    const token = getToken()
    if (token) {
        headers.set('Authorization', `Bearer ${token}`)
    }

    let response: Response
    try {
        response = await fetch(`${API_URL}${path}`, { ...options, headers })
    } catch {
        throw new ApiError(0, 'Não foi possível conectar ao servidor.')
    }

    if (!response.ok) {
        let message = 'Erro inesperado.'
        try {
            const body = (await response.json()) as ErrorResponse
            message = body.message
        } catch {
            // corpo vazio ou não-JSON: mantém a mensagem genérica
        }
        throw new ApiError(response.status, message)
    }

    const text = await response.text()
    return (text ? JSON.parse(text) : undefined) as T
}