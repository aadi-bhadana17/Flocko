import React, { useContext, useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AuthContext } from '../../context/AuthContext';
import { ThemeContext } from '../../context/ThemeContext';
import { motion } from 'framer-motion';
import ProfileDropdown from './ProfileDropdown';
import { getUserProfile } from '../../api/userService';
import { claimTestCredit } from '../../api/walletService';
import './Navbar.css';
import './ProfileDropdown.css';

const Navbar = () => {
    const { user, logout } = useContext(AuthContext);
    const { theme, toggleTheme } = useContext(ThemeContext);
    const navigate = useNavigate();
    const [profileOpen, setProfileOpen] = useState(false);

    const [testCreditClaimed, setTestCreditClaimed] = useState(false);
    const [testCreditLoading, setTestCreditLoading] = useState(false);
    const [testCreditMsg, setTestCreditMsg] = useState('');

    useEffect(() => {
        if (user) {
            getUserProfile()
                .then(profile => setTestCreditClaimed(profile.testCreditClaimed || false))
                .catch(() => {});
        } else {
            setTestCreditClaimed(false);
            setTestCreditMsg('');
        }
    }, [user]);

    const handleClaimTestCredit = async () => {
        setTestCreditLoading(true);
        setTestCreditMsg('');
        try {
            const result = await claimTestCredit();
            setTestCreditClaimed(true);
            setTestCreditMsg(result.message || '₹2000 added!');
        } catch (err) {
            const msg = err?.response?.data?.error || 'Could not claim.';
            setTestCreditMsg(msg);
        } finally {
            setTestCreditLoading(false);
        }
    };

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
        <>
            <nav className="flocko-nav">
                <Link to="/" className="flocko-logo">
                    <img src="/logo.png" alt="Flocko" className="flocko-logo-img" />
                    <span className="flocko-logo-text">Flo<span>cko</span></span>
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

            {/* Test Credit Floating Square — shown below navbar top-right, only if not claimed */}
            {user && !testCreditClaimed && (
                <div className="test-credit-float">
                    <button
                        className="test-credit-square-btn"
                        onClick={handleClaimTestCredit}
                        disabled={testCreditLoading}
                        title="Add ₹2000 into your wallet for testing (one-time)"
                    >
                        {testCreditLoading ? (
                            <span className="tc-spinner" />
                        ) : (
                            <span className="tc-icon">💰</span>
                        )}
                        <span className="tc-label">
                            {testCreditLoading ? 'Adding...' : 'Add ₹2000\nfor testing'}
                        </span>
                    </button>
                    {testCreditMsg && (
                        <div className="tc-feedback tc-feedback--error">{testCreditMsg}</div>
                    )}
                </div>
            )}
            {user && testCreditClaimed && testCreditMsg && (
                <div className="test-credit-float">
                    <div className="tc-feedback tc-feedback--success">✅ {testCreditMsg}</div>
                </div>
            )}
        </>
    );
};

export default Navbar;
