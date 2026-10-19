import { Navigate, Route, Routes } from 'react-router'
import LoginPage from './pages/LoginPage'
import OrdersPage from './pages/OrdersPage'
import ProtectedRoute from './routes/ProtectedRoute'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<ProtectedRoute />}>
        <Route path="/orders" element={<OrdersPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/orders" replace />} />
    </Routes>
  )
}