import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import Login from './components/auth/Login';
import Signup from './components/auth/Signup';
import Unauthorized from './components/auth/Unauthorized';
import Navbar from './components/layout/Navbar';
import HomePage from './components/public/HomePage';
import RestaurantPage from './components/public/RestaurantPage';
import { ROLES } from './utils/constants';

// Simple placeholder components for demonstration
const CustomerDashboard = () => <h1>Customer Home</h1>;
const AdminPanel = () => <h1>Admin Dashboard</h1>;
const RestaurantPanel = () => <h1>Restaurant Management</h1>;

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
                <WithNavbar><AdminPanel /></WithNavbar>
              </ProtectedRoute>
            } 
          />

          <Route 
            path="/restaurant-panel"
            element={
              <ProtectedRoute allowedRoles={[ROLES.RESTAURANT_OWNER]}>
                <WithNavbar><RestaurantPanel /></WithNavbar>
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