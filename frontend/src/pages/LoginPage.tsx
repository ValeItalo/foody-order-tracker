import { useState, type FormEvent } from 'react'
import { Link as RouterLink, Navigate, useLocation } from 'react-router'
import {
  Alert,
  Box,
  Button,
  Container,
  Link,
  TextField,
  Typography,
} from '@mui/material'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'

export default function LoginPage() {
  const { isAuthenticated, signIn } = useAuth()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const justRegistered =
    (location.state as { registered?: boolean } | null)?.registered === true

  if (isAuthenticated) {
    return <Navigate to="/orders" replace />
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError('')
    setLoading(true)
    try {
      await signIn(email, password)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Erro inesperado.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Container maxWidth="xs" sx={{ py: 8 }}>
      <Typography variant="h4" component="h1" gutterBottom>
        Foody Order Tracker
      </Typography>
      <Typography color="text.secondary" sx={{ mb: 3 }}>
        Entre para acompanhar os pedidos.
      </Typography>
      <Box component="form" onSubmit={handleSubmit} noValidate>
        {justRegistered && !error && (
          <Alert severity="success" sx={{ mb: 2 }}>
            Cadastro realizado! Entre com seu e-mail e senha.
          </Alert>
        )}
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}
        <TextField
          label="E-mail"
          type="email"
          autoComplete="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          fullWidth
          required
          margin="normal"
        />
        <TextField
          label="Senha"
          type="password"
          autoComplete="current-password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          fullWidth
          required
          margin="normal"
        />
        <Button
          type="submit"
          variant="contained"
          fullWidth
          disabled={loading}
          sx={{ mt: 2 }}
        >
          {loading ? 'Entrando...' : 'Entrar'}
        </Button>
        <Typography sx={{ mt: 2 }} align="center">
          Não tem conta?{' '}
          <Link component={RouterLink} to="/register">
            Cadastre-se
          </Link>
        </Typography>
      </Box>
    </Container>
  )
}