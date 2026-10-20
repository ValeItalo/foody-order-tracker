import { useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Chip,
  Divider,
  Drawer,
  Stack,
  Typography,
} from '@mui/material';
import { ApiError } from '../api/client';
import { updateOrderStatus } from '../api/orders';
import type { Order, OrderStatus } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { STATUS_COLOR, STATUS_LABEL, formatDateTime } from './display';
import { canCancel, nextStatus } from './transitions';

interface Props {
  order: Order | null;
  onClose: () => void;
  onUpdated: (order: Order) => void;
}

export default function OrderDetailsDrawer({ order, onClose, onUpdated }: Props) {
  const { signOut } = useAuth();
  const [updating, setUpdating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  function handleClose() {
    if (updating) return;
    setError(null);
    onClose();
  }

  async function changeStatus(status: OrderStatus) {
    if (!order) return;
    setUpdating(true);
    setError(null);
    try {
      const updated = await updateOrderStatus(order.id, status);
      onUpdated(updated);
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        signOut();
        return;
      }
      setError(err instanceof Error ? err.message : 'Erro inesperado.');
    } finally {
      setUpdating(false);
    }
  }

  function handleCancel() {
    if (window.confirm('Cancelar este pedido? Essa ação não pode ser desfeita.')) {
      void changeStatus('CANCELADO');
    }
  }

  const next = order ? nextStatus(order.status) : undefined;

  return (
    <Drawer anchor="right" open={order !== null} onClose={handleClose}>
      {order && (
        <Box
          sx={{
            width: { xs: '100vw', sm: 420 },
            p: 3,
            display: 'flex',
            flexDirection: 'column',
            gap: 2,
          }}
        >
          <Typography variant="h6" component="h2">
            Pedido #{order.id}
          </Typography>

          <Box>
            <Chip
              size="small"
              label={STATUS_LABEL[order.status]}
              color={STATUS_COLOR[order.status]}
            />
          </Box>

          {error && <Alert severity="error">{error}</Alert>}

          <Box>
            <Typography variant="subtitle2">Cliente</Typography>
            <Typography>{order.customerName}</Typography>
          </Box>

          <Box>
            <Typography variant="subtitle2">Endereço de entrega</Typography>
            <Typography>{order.deliveryAddress}</Typography>
          </Box>

          <Box>
            <Typography variant="subtitle2">Itens</Typography>
            {order.items.map((item, index) => (
              <Typography key={index}>
                {item.quantity}x {item.productName}
              </Typography>
            ))}
          </Box>

          <Box>
            <Typography variant="body2" color="text.secondary">
              Criado em {formatDateTime(order.createdAt)}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Atualizado em {formatDateTime(order.updatedAt)}
            </Typography>
          </Box>

          <Divider />

          {next && (
            <Button
              variant="contained"
              disabled={updating}
              onClick={() => void changeStatus(next)}
            >
              {updating ? 'Atualizando...' : `Avançar para ${STATUS_LABEL[next]}`}
            </Button>
          )}

          {canCancel(order.status) && (
            <Button
              variant="outlined"
              color="error"
              disabled={updating}
              onClick={handleCancel}
            >
              Cancelar pedido
            </Button>
          )}

          {!next && !canCancel(order.status) && (
            <Typography color="text.secondary">
              Pedido finalizado: não há mais transições possíveis.
            </Typography>
          )}

          <Stack direction="row" sx={{ justifyContent: 'flex-end' }}>
            <Button onClick={handleClose} disabled={updating}>
              Fechar
            </Button>
          </Stack>
        </Box>
      )}
    </Drawer>
  );
}