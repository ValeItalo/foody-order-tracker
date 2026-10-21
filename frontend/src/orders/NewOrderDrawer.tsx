import { useState } from 'react';
import type { FormEvent } from 'react';
import {
  Alert,
  Box,
  Button,
  Drawer,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { ApiError } from '../api/client';
import { createOrder } from '../api/orders';
import type { Order } from '../api/types';
import { useAuth } from '../auth/AuthContext';

interface ItemForm {
  key: string;
  productName: string;
  quantity: string; // string porque o campo de texto trabalha com texto
}

const newItem = (): ItemForm => ({
  key: crypto.randomUUID(),
  productName: '',
  quantity: '1',
});

const isValidQuantity = (value: string) =>
  Number.isInteger(Number(value)) && Number(value) >= 1;

interface Props {
  open: boolean;
  onClose: () => void;
  onCreated: (order: Order) => void;
}

export default function NewOrderDrawer({ open, onClose, onCreated }: Props) {
  const { signOut } = useAuth();
  const [customerName, setCustomerName] = useState('');
  const [deliveryAddress, setDeliveryAddress] = useState('');
  const [items, setItems] = useState<ItemForm[]>(() => [newItem()]);
  const [submitted, setSubmitted] = useState(false);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const formValid =
    customerName.trim() !== '' &&
    deliveryAddress.trim() !== '' &&
    items.length > 0 &&
    items.every(
      (item) => item.productName.trim() !== '' && isValidQuantity(item.quantity),
    );

  function reset() {
    setCustomerName('');
    setDeliveryAddress('');
    setItems([newItem()]);
    setSubmitted(false);
    setError(null);
  }

  function handleClose() {
    if (sending) return;
    reset();
    onClose();
  }

  function updateItem(
    key: string,
    field: 'productName' | 'quantity',
    value: string,
  ) {
    setItems((prev) =>
      prev.map((item) => (item.key === key ? { ...item, [field]: value } : item)),
    );
  }

  function removeItem(key: string) {
    setItems((prev) => prev.filter((item) => item.key !== key));
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setSubmitted(true);
    if (!formValid) return;

    setSending(true);
    setError(null);
    try {
      const created = await createOrder({
        customerName: customerName.trim(),
        deliveryAddress: deliveryAddress.trim(),
        items: items.map((item) => ({
          productName: item.productName.trim(),
          quantity: Number(item.quantity),
        })),
      });
      onCreated(created);
      reset();
      onClose();
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        signOut();
        return;
      }
      setError(err instanceof Error ? err.message : 'Erro inesperado.');
    } finally {
      setSending(false);
    }
  }

  return (
    <Drawer anchor="right" open={open} onClose={handleClose}>
      <Box
        component="form"
        onSubmit={handleSubmit}
        noValidate
        sx={{
          width: { xs: '100vw', sm: 420 },
          p: 3,
          display: 'flex',
          flexDirection: 'column',
          gap: 2,
        }}
      >
        <Typography variant="h6" component="h2">
          Novo pedido
        </Typography>

        {error && <Alert severity="error">{error}</Alert>}

        <TextField
          label="Nome do cliente"
          value={customerName}
          onChange={(e) => setCustomerName(e.target.value)}
          error={submitted && customerName.trim() === ''}
          helperText={
            submitted && customerName.trim() === ''
              ? 'Informe o nome do cliente'
              : ' '
          }
          required
        />
        <TextField
          label="Endereço de entrega"
          value={deliveryAddress}
          onChange={(e) => setDeliveryAddress(e.target.value)}
          error={submitted && deliveryAddress.trim() === ''}
          helperText={
            submitted && deliveryAddress.trim() === ''
              ? 'Informe o endereço de entrega'
              : ' '
          }
          required
        />

        <Typography variant="subtitle1">Itens do pedido</Typography>

        {items.map((item) => (
          <Stack
            key={item.key}
            direction="row"
            spacing={1}
            sx={{ alignItems: 'flex-start' }}
          >
            <TextField
              label="Item"
              value={item.productName}
              onChange={(e) => updateItem(item.key, 'productName', e.target.value)}
              error={submitted && item.productName.trim() === ''}
              helperText={
                submitted && item.productName.trim() === ''
                  ? 'Informe o item'
                  : ' '
              }
              sx={{ flexGrow: 1 }}
            />
            <TextField
              label="Qtd"
              value={item.quantity}
              onChange={(e) => updateItem(item.key, 'quantity', e.target.value)}
              error={submitted && !isValidQuantity(item.quantity)}
              helperText={
                submitted && !isValidQuantity(item.quantity) ? 'Mín. 1' : ' '
              }
              slotProps={{ htmlInput: { inputMode: 'numeric' } }}
              sx={{ width: 90 }}
            />
            <Button
              color="error"
              onClick={() => removeItem(item.key)}
              disabled={items.length === 1}
              sx={{ mt: 1 }}
            >
              Remover
            </Button>
          </Stack>
        ))}

        <Button onClick={() => setItems((prev) => [...prev, newItem()])}>
          Adicionar item
        </Button>

        <Stack
          direction="row"
          spacing={1}
          sx={{ justifyContent: 'flex-end' }}
        >
          <Button onClick={handleClose} disabled={sending}>
            Cancelar
          </Button>
          <Button type="submit" variant="contained" disabled={sending}>
            {sending ? 'Criando...' : 'Criar pedido'}
          </Button>
        </Stack>
      </Box>
    </Drawer>
  );
}