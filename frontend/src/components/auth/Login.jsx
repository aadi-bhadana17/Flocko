import React, { useState, useContext } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { loginApi } from '../../api/authService';
import { AuthContext } from '../../context/AuthContext';
import { motion, AnimatePresence } from 'framer-motion';
import './Auth.css';

// Animation variants
const pageVariants = {
    initial: { opacity: 0 },
    animate: {
        opacity: 1,
        transition: { duration: 0.6, staggerChildren: 0.1 }
    },
    exit: { opacity: 0, transition: { duration: 0.3 } }
};

const cardVariants = {
    initial: { opacity: 0, y: 60, scale: 0.95 },
    animate: {
        opacity: 1,
        y: 0,
        scale: 1,
        transition: {
            type: "spring",
            stiffness: 100,
            damping: 15,
            delay: 0.2
        }
    }
};

const itemVariants = {
    initial: { opacity: 0, x: -30 },
    animate: {
        opacity: 1,
        x: 0,
        transition: { type: "spring", stiffness: 100 }
    }
};

const logoVariants = {
    initial: { scale: 0, rotate: -180 },
    animate: {
        scale: 1,
        rotate: 0,
        transition: { type: "spring", stiffness: 200, delay: 0.3 }
    }
};

const floatingVariants = {
    animate: {
        y: [0, -10, 0],
        transition: {
            duration: 3,
            repeat: Infinity,
            ease: "easeInOut"
        }
    }
};

const Login = () => {
    const [credentials, setCredentials] = useState({ email: '', password: '' });
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [focusedField, setFocusedField] = useState('');
    const { login } = useContext(AuthContext);
    const navigate = useNavigate();

    const hasTempPassword = (data) => data?.isTempPassword === true || data?.tempPassword === true;

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError('');

        try {
            const data = await loginApi(credentials);
            login(data);

            if (hasTempPassword(data)) {
                window.alert('You are using a temporary password. Please change it from your profile settings.');
            }

            if (data.role === 'ADMIN') navigate('/admin');
            else if (data.role === 'RESTAURANT_OWNER') navigate('/restaurant-panel');
            else if (data.role === 'RESTAURANT_STAFF') navigate('/staff-panel');
            else navigate('/');
        } catch (err) {
            setError(err.response?.data?.message || err.response?.data || "Invalid email or password");
        } finally {
            setLoading(false);
        }
    };

    const foodIcons = ['🍕', '🍔', '🌮', '🍜', '🍣', '🥗', '🍰', '☕'];

    return (
        <motion.div 
            className="auth-container"
            variants={pageVariants}
            initial="initial"
            animate="animate"
            exit="exit"
        >
            {/* Animated Background Shapes */}
            <div className="auth-bg-shapes">
                <div className="floating-shape"></div>
                <div className="floating-shape"></div>
                <div className="floating-shape"></div>
                <div className="floating-shape"></div>
            </div>

            {/* Floating Food Icons */}
            <div className="food-icons-bg">
                {foodIcons.map((icon, index) => (
                    <motion.span
                        key={index}
                        className="food-icon"
                        animate={{
                            y: [0, -20, 0],
                            rotate: [0, 10, -10, 0]
                        }}
                        transition={{
                            duration: 4 + index * 0.5,
                            repeat: Infinity,
                            ease: "easeInOut",
                            delay: index * 0.3
                        }}
                    >
                        {icon}
                    </motion.span>
                ))}
            </div>

            {/* Particles */}
            <div className="particles">
                {[...Array(10)].map((_, i) => (
                    <div key={i} className="particle"></div>
                ))}
            </div>

            {/* Auth Card */}
            <motion.div
                className="auth-card-wrapper"
                variants={cardVariants}
            >
                <motion.div className="auth-card-glass">
                    {/* Glow Effects */}
                    <div className="glow-effect top-right"></div>
                    <div className="glow-effect bottom-left"></div>

                    {/* Header */}
                    <motion.div className="auth-header">
                        <motion.div className="auth-logo" variants={floatingVariants} animate="animate">
                            <motion.div
                                className="auth-logo-icon"
                                variants={logoVariants}
                                whileHover={{ rotate: 360, transition: { duration: 0.6 } }}
                            >
                                🍽️
                            </motion.div>
                            <motion.span
                                className="auth-logo-text"
                                initial={{ opacity: 0, x: -20 }}
                                animate={{ opacity: 1, x: 0 }}
                                transition={{ delay: 0.5 }}
                            >
                                Flocko
                            </motion.span>
                        </motion.div>
                        <motion.h1
                            className="auth-title"
                            initial={{ opacity: 0, y: 20 }}
                            animate={{ opacity: 1, y: 0 }}
                            transition={{ delay: 0.4 }}
                        >
                            Welcome Back! 👋
                        </motion.h1>
                        <motion.p
                            className="auth-subtitle"
                            initial={{ opacity: 0 }}
                            animate={{ opacity: 1 }}
                            transition={{ delay: 0.5 }}
                        >
                            Sign in to continue your delicious journey
                        </motion.p>
                    </motion.div>

                    {/* Form */}
                    <motion.form
                        className="auth-form-modern"
                        onSubmit={handleSubmit}
                        initial="initial"
                        animate="animate"
                    >
                        {/* Email Input */}
                        <motion.div
                            className="input-group"
                            variants={itemVariants}
                            whileFocus={{ scale: 1.02 }}
                        >
                            <motion.input
                                type="email"
                                className="auth-input"
                                placeholder="Email Address"
                                value={credentials.email}
                                onChange={e => setCredentials({...credentials, email: e.target.value})}
                                onFocus={() => setFocusedField('email')}
                                onBlur={() => setFocusedField('')}
                                disabled={loading}
                                required
                            />
                            <motion.span
                                className="input-icon"
                                animate={{
                                    scale: focusedField === 'email' ? 1.2 : 1,
                                    color: focusedField === 'email' ? '#22C55E' : '#6B7280'
                                }}
                            >
                                📧
                            </motion.span>
                        </motion.div>

                        {/* Password Input */}
                        <motion.div
                            className="input-group"
                            variants={itemVariants}
                        >
                            <motion.input
                                type={showPassword ? "text" : "password"}
                                className="auth-input"
                                placeholder="Password"
                                value={credentials.password}
                                onChange={e => setCredentials({...credentials, password: e.target.value})}
                                onFocus={() => setFocusedField('password')}
                                onBlur={() => setFocusedField('')}
                                disabled={loading}
                                required
                            />
                            <motion.span
                                className="input-icon"
                                animate={{
                                    scale: focusedField === 'password' ? 1.2 : 1,
                                    color: focusedField === 'password' ? '#22C55E' : '#6B7280'
                                }}
                            >
                                🔒
                            </motion.span>
                            <motion.button
                                type="button"
                                className="password-toggle"
                                onClick={() => setShowPassword(!showPassword)}
                                whileHover={{ scale: 1.1 }}
                                whileTap={{ scale: 0.9 }}
                            >
                                {showPassword ? '👁️' : '👁️‍🗨️'}
                            </motion.button>
                        </motion.div>

                        {/* Error Message */}
                        <AnimatePresence mode="wait">
                            {error && (
                                <motion.div
                                    className="auth-message error"
                                    initial={{ opacity: 0, y: -10, height: 0 }}
                                    animate={{ opacity: 1, y: 0, height: 'auto' }}
                                    exit={{ opacity: 0, y: -10, height: 0 }}
                                    transition={{ duration: 0.3 }}
                                >
                                    <span className="message-icon">⚠️</span>
                                    {error}
                                </motion.div>
                            )}
                        </AnimatePresence>

                        {/* Submit Button */}
                        <motion.button
                            type="submit"
                            className="auth-submit-btn"
                            variants={itemVariants}
                            whileHover={{
                                scale: 1.02,
                                boxShadow: "0 15px 35px rgba(34, 197, 94, 0.4)"
                            }}
                            whileTap={{ scale: 0.98 }}
                            disabled={loading}
                        >
                            <span className="btn-content">
                                {loading ? (
                                    <>
                                        <motion.span
                                            className="loading-spinner"
                                            animate={{ rotate: 360 }}
                                            transition={{ duration: 1, repeat: Infinity, ease: "linear" }}
                                        />
                                        Signing in...
                                    </>
                                ) : (
                                    <>
                                        Sign In
                                        <motion.span
                                            animate={{ x: [0, 5, 0] }}
                                            transition={{ duration: 1.5, repeat: Infinity }}
                                        >
                                            →
                                        </motion.span>
                                    </>
                                )}
                            </span>
                        </motion.button>
                    </motion.form>

                    {/* Divider */}
                    <motion.div
                        className="auth-divider"
                        initial={{ opacity: 0, scaleX: 0 }}
                        animate={{ opacity: 1, scaleX: 1 }}
                        transition={{ delay: 0.7 }}
                    >
                        <span className="auth-divider-line"></span>
                        <span className="auth-divider-text">or continue with</span>
                        <span className="auth-divider-line"></span>
                    </motion.div>

                    {/* Social Login */}
                    <motion.div
                        className="social-login"
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ delay: 0.8 }}
                    >
                        <motion.button
                            type="button"
                            className="social-btn"
                            whileHover={{ scale: 1.05, y: -2 }}
                            whileTap={{ scale: 0.95 }}
                        >
                            <span className="social-btn-icon">🔵</span>
                            Google
                        </motion.button>
                        <motion.button
                            type="button"
                            className="social-btn"
                            whileHover={{ scale: 1.05, y: -2 }}
                            whileTap={{ scale: 0.95 }}
                        >
                            <span className="social-btn-icon">📘</span>
                            Facebook
                        </motion.button>
                    </motion.div>

                    {/* Footer */}
                    <motion.div
                        className="auth-footer"
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        transition={{ delay: 0.9 }}
                    >
                        <p className="auth-footer-text">
                            Don't have an account?{' '}
                            <Link to="/signup" className="auth-footer-link">
                                <motion.span
                                    whileHover={{ color: '#16A34A' }}
                                >
                                    Create Account
                                </motion.span>
                            </Link>
                        </p>
                    </motion.div>
                </motion.div>
            </motion.div>
        </motion.div>
    );
};

export default Login;

