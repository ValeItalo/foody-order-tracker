import { Button, Container, Typography } from '@mui/material'
import { useAuth } from '../auth/AuthContext'

export default function OrdersPage() {
  const { signOut } = useAuth()

  return (
    <Container sx={{ py: 4 }}>
      <Typography variant="h4" component="h1" gutterBottom>
        Pedidos
      </Typography>
      <Typography color="text.secondary" sx={{ mb: 2 }}>
        A lista de pedidos vem no próximo passo.
      </Typography>
      <Button variant="outlined" onClick={signOut}>
        Sair
      </Button>
    </Container>
  )
}