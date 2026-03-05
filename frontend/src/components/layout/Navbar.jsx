import React, { useContext } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AuthContext } from '../../context/AuthContext';
import { motion } from 'framer-motion';
import './Navbar.css';

const Navbar = () => {
    const { user, logout } = useContext(AuthContext);
    const navigate = useNavigate();

    const handleLogout = () => {
        logout();
        navigate('/');
    };

    const getDashboardPath = () => {
        if (!user) return '/login';
        if (user.role === 'ADMIN') return '/admin';
        if (user.role === 'RESTAURANT_OWNER') return '/restaurant';
        return '/dashboard';
    };

    return (
        <nav className="flocko-nav">
            <Link to="/" className="flocko-logo">
                Flo<span>cko</span>
            </Link>

            <ul className="flocko-nav-links">
                <li><Link to="/">Restaurants</Link></li>
                {user && <li><Link to={getDashboardPath()}>Dashboard</Link></li>}
            </ul>

            <div className="flocko-nav-cta">
                {user ? (
                    <>
                        <motion.span
                            className="flocko-user-greeting"
                            initial={{ opacity: 0, x: 10 }}
                            animate={{ opacity: 1, x: 0 }}
                        >
                            👋 {user.firstName || 'User'}
                        </motion.span>
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

