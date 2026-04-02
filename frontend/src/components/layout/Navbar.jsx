import React, { useContext, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AuthContext } from '../../context/AuthContext';
import { ThemeContext } from '../../context/ThemeContext';
import { motion } from 'framer-motion';
import ProfileDropdown from './ProfileDropdown';
import './Navbar.css';
import './ProfileDropdown.css';

const Navbar = () => {
    const { user, logout } = useContext(AuthContext);
    const { theme, toggleTheme } = useContext(ThemeContext);
    const navigate = useNavigate();
    const [profileOpen, setProfileOpen] = useState(false);

    const handleLogout = () => {
        logout();
        navigate('/');
    };

    const getDashboardPath = () => {
        if (!user) return '/login';
        if (user.role === 'ADMIN') return '/admin';
        if (user.role === 'RESTAURANT_OWNER') return '/restaurant-panel';
        if (user.role === 'RESTAURANT_STAFF') return '/staff-panel';
        return '/dashboard';
    };

    return (
        <nav className="flocko-nav">
            <Link to="/" className="flocko-logo">
                Flo<span>cko</span>
            </Link>

            <ul className="flocko-nav-links">
                <li><Link to="/">Restaurants</Link></li>
                {user && <li><Link to="/orders">Orders</Link></li>}
                {user && <li><Link to="/wallet">Wallet</Link></li>}
                {user && <li><Link to="/cart">🛒 Cart</Link></li>}
                {user && <li><Link to={getDashboardPath()}>Dashboard</Link></li>}
            </ul>

            <div className="flocko-nav-cta">
                <motion.button
                    className="flocko-theme-toggle"
                    onClick={toggleTheme}
                    whileHover={{ scale: 1.1 }}
                    whileTap={{ scale: 0.9 }}
                    title={theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
                >
                    {theme === 'dark' ? '☀️' : '🌙'}
                </motion.button>
                {user ? (
                    <>
                        <div className="profile-dropdown-wrapper">
                            <motion.div
                                className="pd-trigger"
                                onClick={() => setProfileOpen(!profileOpen)}
                                whileHover={{ scale: 1.03 }}
                                whileTap={{ scale: 0.97 }}
                            >
                                <span className="flocko-user-greeting">
                                    👋 {user.firstName || 'User'}
                                </span>
                                <span className={`pd-trigger-arrow ${profileOpen ? 'open' : ''}`}>▼</span>
                            </motion.div>
                            <ProfileDropdown open={profileOpen} onClose={() => setProfileOpen(false)} />
                        </div>
                        <motion.button
                            className="flocko-btn flocko-btn-outline"
                            onClick={handleLogout}
                            whileHover={{ scale: 1.03 }}
                            whileTap={{ scale: 0.97 }}
                        >
                            Log out
                        </motion.button>
                    </>
                ) : (
                    <>
                        <Link to="/login">
                            <motion.button
                                className="flocko-btn flocko-btn-outline"
                                whileHover={{ scale: 1.03 }}
                                whileTap={{ scale: 0.97 }}
                            >
                                Log in
                            </motion.button>
                        </Link>
                        <Link to="/signup">
                            <motion.button
                                className="flocko-btn flocko-btn-primary"
                                whileHover={{ scale: 1.03, y: -1 }}
                                whileTap={{ scale: 0.97 }}
                            >
                                Sign up
                            </motion.button>
                        </Link>
                    </>
                )}
            </div>
        </nav>
    );
};

export default Navbar;
