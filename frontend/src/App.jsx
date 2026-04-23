import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import Login from './components/auth/Login';
import Signup from './components/auth/Signup';
import Unauthorized from './components/auth/Unauthorized';
import Navbar from './components/layout/Navbar';
import HomePage from './components/public/HomePage';
import RestaurantPage from './components/public/RestaurantPage';
import CartPage from './components/cart/CartPage';
import SharedCartPage from './components/cart/SharedCartPage';
import OrdersPage from './components/orders/OrdersPage';
import OrderDetailPage from './components/orders/OrderDetailPage';
import GroupDealDetailPage from './components/deals/GroupDealDetailPage';
import RestaurantDashboard from './components/dashboard/RestaurantDashboard';
import RestaurantStaffDashboard from './components/dashboard/RestaurantStaffDashboard';
import AdminDashboard from './components/dashboard/AdminDashboard';
import CustomerDashboard from './components/dashboard/CustomerDashboard';
import WalletPage from './components/wallet/WalletPage.jsx';
import WalletSuccessPage from './components/wallet/WalletSuccessPage.jsx';
import WalletCancelPage from './components/wallet/WalletCancelPage.jsx';
import { ToastContainer } from './components/ui/Toast';
import { ROLES } from './utils/constants';


// Layout with Navbar
const WithNavbar = ({ children }) => (
  <>
    <Navbar />
    {children}
  </>
);

function App() {
  return (
    <AuthProvider>
      <ToastContainer />
      <Router>
        <Routes>
          {/* Auth Routes (no navbar) */}
          <Route path="/login" element={<Login />} />
          <Route path="/signup" element={<Signup />} />
          <Route path="/unauthorized" element={<Unauthorized />} />

          {/* Public Routes (with navbar) */}
          <Route path="/" element={<WithNavbar><HomePage /></WithNavbar>} />
          <Route path="/restaurant/:id" element={<WithNavbar><RestaurantPage /></WithNavbar>} />

          {/* Protected Routes based on USER_ROLE */}
          <Route 
            path="/dashboard" 
            element={
              <ProtectedRoute allowedRoles={[ROLES.CUSTOMER]}>
                <WithNavbar><CustomerDashboard /></WithNavbar>
              </ProtectedRoute>
            } 
          />
          
          <Route 
            path="/admin" 
            element={
              <ProtectedRoute allowedRoles={[ROLES.ADMIN]}>
                <WithNavbar><AdminDashboard /></WithNavbar>
              </ProtectedRoute>
            } 
          />

          <Route 
            path="/restaurant-panel"
            element={
              <ProtectedRoute allowedRoles={[ROLES.RESTAURANT_OWNER]}>
                <WithNavbar><RestaurantDashboard /></WithNavbar>
              </ProtectedRoute>
            } 
          />

          <Route
            path="/staff-panel"
            element={
              <ProtectedRoute allowedRoles={[ROLES.RESTAURANT_STAFF]}>
                <WithNavbar><RestaurantStaffDashboard /></WithNavbar>
              </ProtectedRoute>
            }
          />

          {/* Cart & Orders (any logged-in user) */}
          <Route
            path="/cart"
            element={
              <ProtectedRoute allowedRoles={[ROLES.CUSTOMER, ROLES.ADMIN, ROLES.RESTAURANT_OWNER, ROLES.RESTAURANT_STAFF]}>
                <WithNavbar><CartPage /></WithNavbar>
              </ProtectedRoute>
            }
          />
          <Route
            path="/shared-cart"
            element={
              <ProtectedRoute allowedRoles={[ROLES.CUSTOMER, ROLES.ADMIN, ROLES.RESTAURANT_OWNER, ROLES.RESTAURANT_STAFF]}>
                <WithNavbar><SharedCartPage /></WithNavbar>
              </ProtectedRoute>
            }
          />
          <Route
            path="/orders"
            element={
              <ProtectedRoute allowedRoles={[ROLES.CUSTOMER, ROLES.ADMIN, ROLES.RESTAURANT_OWNER, ROLES.RESTAURANT_STAFF]}>
                <WithNavbar><OrdersPage /></WithNavbar>
              </ProtectedRoute>
            }
          />
          <Route
            path="/orders/:orderId"
            element={
              <ProtectedRoute allowedRoles={[ROLES.CUSTOMER, ROLES.ADMIN, ROLES.RESTAURANT_OWNER, ROLES.RESTAURANT_STAFF]}>
                <WithNavbar><OrderDetailPage /></WithNavbar>
              </ProtectedRoute>
            }
          />
          <Route
            path="/restaurants/:restaurantId/group-deals/:dealId"
            element={
              <ProtectedRoute allowedRoles={[ROLES.CUSTOMER, ROLES.ADMIN, ROLES.RESTAURANT_OWNER, ROLES.RESTAURANT_STAFF]}>
                <WithNavbar><GroupDealDetailPage /></WithNavbar>
              </ProtectedRoute>
            }
          />

          {/* Wallet Routes (any logged-in user) */}
          <Route
            path="/wallet"
            element={
              <ProtectedRoute allowedRoles={[ROLES.CUSTOMER, ROLES.ADMIN, ROLES.RESTAURANT_OWNER, ROLES.RESTAURANT_STAFF]}>
                <WithNavbar><WalletPage /></WithNavbar>
              </ProtectedRoute>
            }
          />
          <Route
            path="/wallet/success"
            element={
              <ProtectedRoute allowedRoles={[ROLES.CUSTOMER, ROLES.ADMIN, ROLES.RESTAURANT_OWNER, ROLES.RESTAURANT_STAFF]}>
                <WithNavbar><WalletSuccessPage /></WithNavbar>
              </ProtectedRoute>
            }
          />
          <Route
            path="/wallet/cancel"
            element={
              <ProtectedRoute allowedRoles={[ROLES.CUSTOMER, ROLES.ADMIN, ROLES.RESTAURANT_OWNER, ROLES.RESTAURANT_STAFF]}>
                <WithNavbar><WalletCancelPage /></WithNavbar>
              </ProtectedRoute>
            }
          />

          {/* Catch-all → Home */}
          <Route path="*" element={<Navigate to="/" />} />
        </Routes>
      </Router>
    </AuthProvider>
  );
}

export default App;