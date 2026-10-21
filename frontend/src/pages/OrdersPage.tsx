import { useEffect, useState } from 'react';
import {
  Alert,
  AppBar,
  Box,
  Button,
  Chip,
  CircularProgress,
  Container,
  Paper,
  Tab,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tabs,
  Toolbar,
  Typography,
} from '@mui/material';
import { ApiError } from '../api/client';
import { listOrders } from '../api/orders';
import { ORDER_STATUSES } from '../api/types';
import type { Order, OrderStatus } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import {
  STATUS_COLOR,
  STATUS_LABEL,
  formatDateTime,
  itemsLabel,
} from '../orders/display';
import NewOrderDrawer from '../orders/NewOrderDrawer';
import OrderDetailsDrawer from '../orders/OrderDetailsDrawer';

const POLL_INTERVAL_MS = 10_000;
type Filter = 'TODOS' | OrderStatus;

export default function OrdersPage() {
  const { signOut } = useAuth();
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filter, setFilter] = useState<Filter>('TODOS');
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [selectedId, setSelectedId] = useState<Order['id'] | null>(null);
  const selectedOrder = orders.find((order) => order.id === selectedId) ?? null;

useEffect(() => {
  let cancelled = false;

  async function load(isFirstLoad: boolean) {
    try {
      const data = await listOrders();
      if (cancelled) return;
      setOrders(data);
      setError(null);
    } catch (err: unknown) {
      if (err instanceof ApiError && err.status === 401) {
        signOut(); // token expirado ou inválido: volta ao login
        return;
      }
      if (!cancelled) {
        setError(err instanceof Error ? err.message : 'Erro inesperado.');
      }
    } finally {
      if (!cancelled && isFirstLoad) setLoading(false);
    }
  }

  void load(true);
  const timer = setInterval(() => void load(false), POLL_INTERVAL_MS);

  return () => {
    cancelled = true;
    clearInterval(timer);
  };
}, [signOut]);

  const countOf = (status: OrderStatus) =>
    orders.filter((order) => order.status === status).length;

  const visible =
    filter === 'TODOS'
      ? orders
      : orders.filter((order) => order.status === filter);

  return (
    <>
      <AppBar position="static" color="primary" elevation={0}>
        <Toolbar>
          <Typography variant="h6" sx={{ flexGrow: 1 }}>
            Foody Tracker
          </Typography>
          <Button color="inherit" onClick={signOut}>
            Sair
          </Button>
        </Toolbar>
      </AppBar>

      <Container maxWidth="lg" sx={{ py: 3 }}>
        <Box
  sx={{
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    mb: 2,
  }}
>
  <Typography variant="h5" component="h1">
    Pedidos
  </Typography>
  <Button variant="contained" onClick={() => setDrawerOpen(true)}>
    Novo pedido
  </Button>
</Box>

        {error && (
          <Alert severity="error" sx={{ mb: 2 }}>
            {error}
          </Alert>
        )}

        {loading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
            <CircularProgress />
          </Box>
        ) : (
          <>
            <Tabs
              value={filter}
              onChange={(_, value: Filter) => setFilter(value)}
              variant="scrollable"
              scrollButtons="auto"
              sx={{ mb: 2 }}
            >
              <Tab value="TODOS" label={`Todos ${orders.length}`} />
              {ORDER_STATUSES.map((status) => (
                <Tab
                  key={status}
                  value={status}
                  label={`${STATUS_LABEL[status]} ${countOf(status)}`}
                />
              ))}
            </Tabs>

            <TableContainer component={Paper} variant="outlined">
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell>Pedido</TableCell>
                    <TableCell>Cliente</TableCell>
                    <TableCell>Endereço</TableCell>
                    <TableCell>Itens</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Criado em</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {visible.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={6} align="center">
                        Nenhum pedido encontrado.
                      </TableCell>
                    </TableRow>
                  ) : (
                    visible.map((order) => (
                      <TableRow
                        key={order.id}
                        hover
                        onClick={() => setSelectedId(order.id)}
                        sx={{ cursor: 'pointer' }}
                        >
                        <TableCell>#{order.id}</TableCell>
                        <TableCell>{order.customerName}</TableCell>
                        <TableCell>{order.deliveryAddress}</TableCell>
                        <TableCell>{itemsLabel(order)}</TableCell>
                        <TableCell>
                          <Chip
                            size="small"
                            label={STATUS_LABEL[order.status]}
                            color={STATUS_COLOR[order.status]}
                          />
                        </TableCell>
                        <TableCell>{formatDateTime(order.createdAt)}</TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          </>
        )}
      </Container>
      <NewOrderDrawer
  open={drawerOpen}
  onClose={() => setDrawerOpen(false)}
  onCreated={(order) => setOrders((prev) => [order, ...prev])}
/>
<OrderDetailsDrawer
  order={selectedOrder}
  onClose={() => setSelectedId(null)}
  onUpdated={(updated) =>
    setOrders((prev) =>
      prev.map((order) => (order.id === updated.id ? updated : order)),
    )
  }
/>
    </>
  );
}