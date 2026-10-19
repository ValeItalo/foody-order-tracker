import { Button, Container, Typography } from '@mui/material'

export default function App() {
  return (
    <Container sx={{ py: 4 }}>
      <Typography variant="h4" component="h1" gutterBottom>
        Foody Order Tracker
      </Typography>
      <Button variant="contained">Teste</Button>
    </Container>
  )
}