import React from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import './Auth.css';

const Unauthorized = () => {
    return (
        <motion.div
            className="auth-container"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
        >
            {/* Animated Background Shapes */}
            <div className="auth-bg-shapes">
                <div className="floating-shape"></div>
                <div className="floating-shape"></div>
                <div className="floating-shape"></div>
                <div className="floating-shape"></div>
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
                initial={{ opacity: 0, y: 60, scale: 0.95 }}
                animate={{
                    opacity: 1,
                    y: 0,
                    scale: 1,
                    transition: { type: "spring", stiffness: 100, damping: 15 }
                }}
            >
                <motion.div className="auth-card-glass" style={{ textAlign: 'center' }}>
                    {/* Glow Effects */}
                    <div className="glow-effect top-right"></div>
                    <div className="glow-effect bottom-left"></div>

                    {/* Error Icon */}
                    <motion.div
                        style={{ fontSize: '5rem', marginBottom: '20px' }}
                        animate={{
                            scale: [1, 1.1, 1],
                            rotate: [0, -5, 5, 0]
                        }}
                        transition={{
                            duration: 2,
                            repeat: Infinity,
                            ease: "easeInOut"
                        }}
                    >
                        🚫
                    </motion.div>

                    {/* Title */}
                    <motion.h1
                        style={{
                            fontFamily: "'Fraunces', serif",
                            fontSize: '3rem',
                            fontWeight: '900',
                            background: 'linear-gradient(135deg, #EF4444, #DC2626)',
                            WebkitBackgroundClip: 'text',
                            WebkitTextFillColor: 'transparent',
                            marginBottom: '10px'
                        }}
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ delay: 0.2 }}
                    >
                        403
                    </motion.h1>

                    <motion.h2
                        style={{
                            fontFamily: "'Fraunces', serif",
                            fontSize: '1.5rem',
                            fontWeight: '700',
                            color: '#1F2937',
                            marginBottom: '15px'
                        }}
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        transition={{ delay: 0.3 }}
                    >
                        Access Denied
                    </motion.h2>

                    <motion.p
                        style={{
                            color: '#6B7280',
                            fontSize: '1rem',
                            marginBottom: '30px',
                            lineHeight: '1.6'
                        }}
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        transition={{ delay: 0.4 }}
                    >
                        Oops! You don't have permission to access this page.
                        Please check your credentials or contact support.
                    </motion.p>

                    {/* Action Buttons */}
                    <motion.div
                        style={{ display: 'flex', gap: '12px', justifyContent: 'center', flexWrap: 'wrap' }}
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ delay: 0.5 }}
                    >
                        <Link to="/">
                            <motion.button
                                className="auth-submit-btn"
                                style={{ padding: '14px 28px' }}
                                whileHover={{ scale: 1.05 }}
                                whileTap={{ scale: 0.95 }}
                            >
                                <span className="btn-content">
                                    🏠 Go Home
                                </span>
                            </motion.button>
                        </Link>
                        <Link to="/login">
                            <motion.button
                                className="social-btn"
                                style={{ padding: '14px 28px' }}
                                whileHover={{ scale: 1.05, y: -2 }}
                                whileTap={{ scale: 0.95 }}
                            >
                                🔐 Sign In
                            </motion.button>
                        </Link>
                    </motion.div>
                </motion.div>
            </motion.div>
        </motion.div>
    );
};

export default Unauthorized;