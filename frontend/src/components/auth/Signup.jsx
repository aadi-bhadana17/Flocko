import React, { useState, useContext, useMemo } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { signupApi } from '../../api/authService';
import { AuthContext } from '../../context/AuthContext';
import { motion, AnimatePresence } from 'framer-motion';
import './Auth.css';

// Animation variants
const pageVariants = {
    initial: { opacity: 0 },
    animate: {
        opacity: 1,
        transition: { duration: 0.6 }
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

const staggerContainer = {
    initial: {},
    animate: {
        transition: { staggerChildren: 0.08 }
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

// Password strength calculator
const getPasswordStrength = (password) => {
    if (!password) return { strength: 0, label: '' };

    let score = 0;
    if (password.length >= 8) score++;
    if (password.length >= 12) score++;
    if (/[a-z]/.test(password) && /[A-Z]/.test(password)) score++;
    if (/\d/.test(password)) score++;
    if (/[!@#$%^&*(),.?":{}|<>]/.test(password)) score++;

    if (score <= 1) return { strength: 1, label: 'weak' };
    if (score === 2) return { strength: 2, label: 'fair' };
    if (score === 3) return { strength: 3, label: 'good' };
    return { strength: 4, label: 'strong' };
};

const Signup = () => {
    const [formData, setFormData] = useState({
        firstName: '', lastName: '', email: '', password: '', confirmPassword: ''
    });
    const [loading, setLoading] = useState(false);
    const [msg, setMsg] = useState({ type: '', text: '' });
    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);
    const [focusedField, setFocusedField] = useState('');
    const [agreedToTerms, setAgreedToTerms] = useState(false);
    const { login } = useContext(AuthContext);
    const navigate = useNavigate();

    const passwordStrength = useMemo(() => getPasswordStrength(formData.password), [formData.password]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        
        if (formData.password !== formData.confirmPassword) {
            return setMsg({ type: 'error', text: 'Passwords do not match' });
        }

        if (!agreedToTerms) {
            return setMsg({ type: 'error', text: 'Please agree to the terms and conditions' });
        }

        setLoading(true);
        setMsg({ type: '', text: '' });

        try {
            const data = await signupApi(formData);
            setMsg({ type: 'success', text: '🎉 Account created successfully! Redirecting...' });

            setTimeout(() => {
                login(data);
                navigate('/');
            }, 2000);
        } catch (err) {
            setMsg({ type: 'error', text: err.response?.data?.message || err.response?.data || "Registration failed" });
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
                            Join the Feast! 🎉
                        </motion.h1>
                        <motion.p
                            className="auth-subtitle"
                            initial={{ opacity: 0 }}
                            animate={{ opacity: 1 }}
                            transition={{ delay: 0.5 }}
                        >
                            Create your account and start ordering delicious food
                        </motion.p>
                    </motion.div>

                    {/* Form */}
                    <motion.form
                        className="auth-form-modern"
                        onSubmit={handleSubmit}
                        variants={staggerContainer}
                        initial="initial"
                        animate="animate"
                    >
                        {/* Name Row */}
                        <motion.div className="input-row" variants={itemVariants}>
                            <div className="input-group">
                                <motion.input
                                    type="text"
                                    className="auth-input"
                                    placeholder="First Name"
                                    value={formData.firstName}
                                    onChange={e => setFormData({...formData, firstName: e.target.value})}
                                    onFocus={() => setFocusedField('firstName')}
                                    onBlur={() => setFocusedField('')}
                                    disabled={loading}
                                    required
                                />
                                <motion.span
                                    className="input-icon"
                                    animate={{
                                        scale: focusedField === 'firstName' ? 1.2 : 1,
                                        color: focusedField === 'firstName' ? '#22C55E' : '#6B7280'
                                    }}
                                >
                                    👤
                                </motion.span>
                            </div>
                            <div className="input-group">
                                <motion.input
                                    type="text"
                                    className="auth-input"
                                    placeholder="Last Name"
                                    value={formData.lastName}
                                    onChange={e => setFormData({...formData, lastName: e.target.value})}
                                    onFocus={() => setFocusedField('lastName')}
                                    onBlur={() => setFocusedField('')}
                                    disabled={loading}
                                />
                                <motion.span
                                    className="input-icon"
                                    animate={{
                                        scale: focusedField === 'lastName' ? 1.2 : 1,
                                        color: focusedField === 'lastName' ? '#22C55E' : '#6B7280'
                                    }}
                                >
                                    👤
                                </motion.span>
                            </div>
                        </motion.div>

                        {/* Email Input */}
                        <motion.div className="input-group" variants={itemVariants}>
                            <motion.input
                                type="email"
                                className="auth-input"
                                placeholder="Email Address"
                                value={formData.email}
                                onChange={e => setFormData({...formData, email: e.target.value})}
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
                        <motion.div className="input-group" variants={itemVariants}>
                            <motion.input
                                type={showPassword ? "text" : "password"}
                                className="auth-input"
                                placeholder="Password (min 8 characters)"
                                value={formData.password}
                                onChange={e => setFormData({...formData, password: e.target.value})}
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

                        {/* Password Strength Indicator */}
                        <AnimatePresence>
                            {formData.password && (
                                <motion.div
                                    className="password-strength"
                                    initial={{ opacity: 0, height: 0 }}
                                    animate={{ opacity: 1, height: 'auto' }}
                                    exit={{ opacity: 0, height: 0 }}
                                >
                                    <div className="strength-bars">
                                        {[1, 2, 3, 4].map((level) => (
                                            <motion.div
                                                key={level}
                                                className={`strength-bar ${passwordStrength.strength >= level ? `active ${passwordStrength.label}` : ''}`}
                                                initial={{ scaleX: 0 }}
                                                animate={{ scaleX: 1 }}
                                                transition={{ delay: level * 0.1 }}
                                            />
                                        ))}
                                    </div>
                                    <motion.span
                                        className={`strength-text ${passwordStrength.label}`}
                                        initial={{ opacity: 0 }}
                                        animate={{ opacity: 1 }}
                                    >
                                        Password strength: {passwordStrength.label}
                                    </motion.span>
                                </motion.div>
                            )}
                        </AnimatePresence>

                        {/* Confirm Password Input */}
                        <motion.div className="input-group" variants={itemVariants}>
                            <motion.input
                                type={showConfirmPassword ? "text" : "password"}
                                className="auth-input"
                                placeholder="Confirm Password"
                                value={formData.confirmPassword}
                                onChange={e => setFormData({...formData, confirmPassword: e.target.value})}
                                onFocus={() => setFocusedField('confirmPassword')}
                                onBlur={() => setFocusedField('')}
                                disabled={loading}
                                required
                            />
                            <motion.span
                                className="input-icon"
                                animate={{
                                    scale: focusedField === 'confirmPassword' ? 1.2 : 1,
                                    color: focusedField === 'confirmPassword' ? '#22C55E' : '#6B7280'
                                }}
                            >
                                🔐
                            </motion.span>
                            <motion.button
                                type="button"
                                className="password-toggle"
                                onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                                whileHover={{ scale: 1.1 }}
                                whileTap={{ scale: 0.9 }}
                            >
                                {showConfirmPassword ? '👁️' : '👁️‍🗨️'}
                            </motion.button>
                        </motion.div>

                        {/* Password Match Indicator */}
                        <AnimatePresence>
                            {formData.confirmPassword && (
                                <motion.div
                                    initial={{ opacity: 0, y: -10 }}
                                    animate={{ opacity: 1, y: 0 }}
                                    exit={{ opacity: 0, y: -10 }}
                                    style={{
                                        fontSize: '0.8rem',
                                        color: formData.password === formData.confirmPassword ? '#10B981' : '#EF4444',
                                        display: 'flex',
                                        alignItems: 'center',
                                        gap: '6px',
                                        marginTop: '-8px'
                                    }}
                                >
                                    {formData.password === formData.confirmPassword ? (
                                        <>✅ Passwords match</>
                                    ) : (
                                        <>❌ Passwords don't match</>
                                    )}
                                </motion.div>
                            )}
                        </AnimatePresence>

                        {/* Terms Checkbox */}
                        <motion.div
                            className="checkbox-group"
                            variants={itemVariants}
                        >
                            <motion.div
                                className={`custom-checkbox ${agreedToTerms ? 'checked' : ''}`}
                                onClick={() => setAgreedToTerms(!agreedToTerms)}
                                whileHover={{ scale: 1.1 }}
                                whileTap={{ scale: 0.9 }}
                            >
                                <motion.span
                                    className="check-icon"
                                    initial={false}
                                    animate={{
                                        scale: agreedToTerms ? 1 : 0,
                                        opacity: agreedToTerms ? 1 : 0
                                    }}
                                >
                                    ✓
                                </motion.span>
                            </motion.div>
                            <span className="checkbox-label" onClick={() => setAgreedToTerms(!agreedToTerms)}>
                                I agree to the <a href="#terms">Terms of Service</a> and <a href="#privacy">Privacy Policy</a>
                            </span>
                        </motion.div>

                        {/* Messages */}
                        <AnimatePresence mode="wait">
                            {msg.text && (
                                <motion.div
                                    className={`auth-message ${msg.type}`}
                                    initial={{ opacity: 0, y: -10, height: 0 }}
                                    animate={{ opacity: 1, y: 0, height: 'auto' }}
                                    exit={{ opacity: 0, y: -10, height: 0 }}
                                    transition={{ duration: 0.3 }}
                                >
                                    <span className="message-icon">
                                        {msg.type === 'error' ? '⚠️' : '✨'}
                                    </span>
                                    {msg.text}
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
                                        Creating Account...
                                    </>
                                ) : (
                                    <>
                                        Create Account
                                        <motion.span
                                            animate={{ x: [0, 5, 0] }}
                                            transition={{ duration: 1.5, repeat: Infinity }}
                                        >
                                            🚀
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
                        <span className="auth-divider-text">or sign up with</span>
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
                            Already have an account?{' '}
                            <Link to="/login" className="auth-footer-link">
                                <motion.span
                                    whileHover={{ color: '#16A34A' }}
                                >
                                    Sign In
                                </motion.span>
                            </Link>
                        </p>
                    </motion.div>
                </motion.div>
            </motion.div>
        </motion.div>
    );
};

export default Signup;

