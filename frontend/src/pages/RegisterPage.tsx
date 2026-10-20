import { useState, type FormEvent } from 'react'
import { Link as RouterLink, Navigate, useNavigate } from 'react-router'
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
import { register } from '../api/auth'
import { useAuth } from '../auth/AuthContext'

const EMAIL_PATTERN = /^\S+@\S+\.\S+$/

export default function RegisterPage() {
  const { isAuthenticated } = useAuth()
  const navigate = useNavigate()
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [submitted, setSubmitted] = useState(false)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  if (isAuthenticated) {
    return <Navigate to="/orders" replace />
  }

  const nameError =
    name.trim() === ''
      ? 'Informe seu nome'
      : name.trim().length > 100
        ? 'No máximo 100 caracteres'
        : ''
  const emailError = !EMAIL_PATTERN.test(email.trim())
    ? 'Informe um e-mail válido'
    : email.trim().length > 150
      ? 'No máximo 150 caracteres'
      : ''
  const passwordError =
    password.length < 8 || password.length > 72
      ? 'A senha deve ter entre 8 e 72 caracteres'
      : ''
  const formValid = !nameError && !emailError && !passwordError

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setSubmitted(true)
    setError('')
    if (!formValid) return

    setLoading(true)
    try {
      await register({
        name: name.trim(),
        email: email.trim(),
        password,
      })
      navigate('/login', { replace: true, state: { registered: true } })
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
        Crie sua conta para acompanhar os pedidos.
      </Typography>
      <Box component="form" onSubmit={handleSubmit} noValidate>
        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}
        <TextField
          label="Nome"
          autoComplete="name"
          value={name}
          onChange={(event) => setName(event.target.value)}
          error={submitted && nameError !== ''}
          helperText={submitted && nameError !== '' ? nameError : ' '}
          fullWidth
          required
          margin="dense"
        />
        <TextField
          label="E-mail"
          type="email"
          autoComplete="email"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          error={submitted && emailError !== ''}
          helperText={submitted && emailError !== '' ? emailError : ' '}
          fullWidth
          required
          margin="dense"
        />
        <TextField
          label="Senha"
          type="password"
          autoComplete="new-password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          error={submitted && passwordError !== ''}
          helperText={
            submitted && passwordError !== ''
              ? passwordError
              : 'Entre 8 e 72 caracteres'
          }
          fullWidth
          required
          margin="dense"
        />
        <Button
          type="submit"
          variant="contained"
          fullWidth
          disabled={loading}
          sx={{ mt: 2 }}
        >
          {loading ? 'Cadastrando...' : 'Cadastrar'}
        </Button>
        <Typography sx={{ mt: 2 }} align="center">
          Já tem conta?{' '}
          <Link component={RouterLink} to="/login">
            Entrar
          </Link>
        </Typography>
      </Box>
    </Container>
  )
}