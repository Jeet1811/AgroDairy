import { Navigate, Route, Routes } from 'react-router-dom'
import { ToastProvider } from '@/context/ToastContext'
import { ProtectedRoute } from '@/routes/ProtectedRoute'
import { PublicLayout } from '@/components/layout/PublicLayout'
import { CustomerLayout } from '@/components/layout/CustomerLayout'
import { AdminLayout } from '@/components/layout/AdminLayout'

import LandingPage from '@/pages/public/LandingPage'
import LoginPage from '@/pages/public/LoginPage'
import RegisterPage from '@/pages/public/RegisterPage'
import ProductsListPage from '@/pages/public/ProductsListPage'
import ProductDetailPage from '@/pages/public/ProductDetailPage'

import CartPage from '@/pages/customer/CartPage'
import CheckoutPage from '@/pages/customer/CheckoutPage'
import OrdersPage from '@/pages/customer/OrdersPage'
import OrderDetailPage from '@/pages/customer/OrderDetailPage'
import SubscriptionsPage from '@/pages/customer/SubscriptionsPage'
import SubscriptionDetailPage from '@/pages/customer/SubscriptionDetailPage'
import AccountPage from '@/pages/customer/AccountPage'

import AdminDashboardPage from '@/pages/admin/AdminDashboardPage'
import AdminAnimalsPage from '@/pages/admin/AdminAnimalsPage'
import AdminAnimalDetailPage from '@/pages/admin/AdminAnimalDetailPage'
import AdminProductsPage from '@/pages/admin/AdminProductsPage'
import AdminInventoryPage from '@/pages/admin/AdminInventoryPage'
import AdminOrdersPage from '@/pages/admin/AdminOrdersPage'
import AdminSubscriptionsPage from '@/pages/admin/AdminSubscriptionsPage'
import AdminDeliveriesPage from '@/pages/admin/AdminDeliveriesPage'
import AdminAnomaliesPage from '@/pages/admin/AdminAnomaliesPage'
import AdminForecastPage from '@/pages/admin/AdminForecastPage'
import AdminUsersPage from '@/pages/admin/AdminUsersPage'

function App() {
  return (
    <ToastProvider>
      <Routes>
        <Route element={<PublicLayout />}>
          <Route path="/" element={<LandingPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
          <Route path="/products" element={<ProductsListPage />} />
          <Route path="/products/:id" element={<ProductDetailPage />} />
        </Route>

        <Route element={<ProtectedRoute allowedRoles={['CUSTOMER']} />}>
          <Route element={<CustomerLayout />}>
            <Route path="/cart" element={<CartPage />} />
            <Route path="/checkout" element={<CheckoutPage />} />
            <Route path="/orders" element={<OrdersPage />} />
            <Route path="/orders/:id" element={<OrderDetailPage />} />
            <Route path="/subscriptions" element={<SubscriptionsPage />} />
            <Route path="/subscriptions/:id" element={<SubscriptionDetailPage />} />
            <Route path="/account" element={<AccountPage />} />
          </Route>
        </Route>

        <Route element={<ProtectedRoute allowedRoles={['STAFF', 'ADMIN']} />}>
          <Route element={<AdminLayout />} path="/admin">
            <Route index element={<AdminDashboardPage />} />
            <Route path="animals" element={<AdminAnimalsPage />} />
            <Route path="animals/:id" element={<AdminAnimalDetailPage />} />
            <Route path="products" element={<AdminProductsPage />} />
            <Route path="inventory" element={<AdminInventoryPage />} />
            <Route path="orders" element={<AdminOrdersPage />} />
            <Route path="subscriptions" element={<AdminSubscriptionsPage />} />
            <Route path="deliveries" element={<AdminDeliveriesPage />} />
            <Route path="anomalies" element={<AdminAnomaliesPage />} />
            <Route path="forecast" element={<AdminForecastPage />} />
            <Route element={<ProtectedRoute allowedRoles={['ADMIN']} />}>
              <Route path="users" element={<AdminUsersPage />} />
            </Route>
          </Route>
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </ToastProvider>
  )
}

export default App
