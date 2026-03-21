import React, { useContext, useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getOrder, cancelOrder } from '../../api/orderService';
import { AuthContext } from '../../context/AuthContext';
import OrderChatPanel from './OrderChatPanel';
import { motion } from 'framer-motion';
import './OrderDetailPage.css';

const STATUS_COLORS = {
    CREATED: { bg: '#DBEAFE', color: '#1D4ED8', label: 'Created' },
    CONFIRMED: { bg: '#DCFCE7', color: '#16A34A', label: 'Confirmed' },
    PREPARING: { bg: '#FEF3C7', color: '#B45309', label: 'Preparing' },
    OUT_FOR_DELIVERY: { bg: '#FFF7ED', color: '#C2410C', label: 'Out for Delivery' },
    DELIVERED: { bg: '#DCFCE7', color: '#15803D', label: 'Delivered' },
    CANCELLED: { bg: '#FEE2E2', color: '#DC2626', label: 'Cancelled' },
};

const STATUS_STEPS = ['CREATED', 'CONFIRMED', 'PREPARING', 'OUT_FOR_DELIVERY', 'DELIVERED'];

const OrderDetailPage = () => {
    const { orderId } = useParams();
    const { user } = useContext(AuthContext);
    const [order, setOrder] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [cancelling, setCancelling] = useState(false);
    const [cancelMsg, setCancelMsg] = useState('');

    useEffect(() => {
        fetchOrder();
    }, [orderId]);

    const fetchOrder = async () => {
        setLoading(true);
        setError('');
        try {
            const data = await getOrder(orderId);
            setOrder(data);
        } catch (err) {
            setError('Unable to load order details.');
        } finally {
            setLoading(false);
        }
    };

    const handleCancel = async () => {
        if (!window.confirm('Are you sure you want to cancel this order?')) return;
        setCancelling(true);
        setCancelMsg('');
        try {
            const msg = await cancelOrder(orderId);
            setCancelMsg(typeof msg === 'string' ? msg : 'Order cancelled.');
            fetchOrder();
        } catch (err) {
            setCancelMsg(err.response?.data?.message || err.response?.data || 'Unable to cancel order.');
        } finally {
            setCancelling(false);
        }
    };

    const formatDate = (dateStr) => {
        if (!dateStr) return '';
        const date = new Date(dateStr);
        return date.toLocaleDateString('en-IN', {
            day: 'numeric', month: 'short', year: 'numeric',
            hour: '2-digit', minute: '2-digit'
        });
    };

    const canCancel = order && (order.orderStatus === 'CREATED' || order.orderStatus === 'CONFIRMED');
    const isSpecialOrder = Boolean(order?.isSpecial);
    const isChatSendAllowedStatus = order && !['CANCELLED', 'DELIVERED'].includes(order.orderStatus);
    const canSendChat = isSpecialOrder && isChatSendAllowedStatus && ['CUSTOMER', 'RESTAURANT_STAFF'].includes(user?.role);
    const sendDisabledReason = !isChatSendAllowedStatus
        ? 'Chat is read-only once the order is cancelled or delivered.'
        : 'Only customers and restaurant staff can send chat messages.';

    const currentStepIndex = order ? STATUS_STEPS.indexOf(order.orderStatus) : -1;
    const isCancelled = order?.orderStatus === 'CANCELLED';

    if (loading) {
        return (
            <div className="od-page">
                <div className="od-loading">
                    <motion.div
                        className="od-loading-icon"
                        animate={{ rotate: 360 }}
                        transition={{ duration: 1.5, repeat: Infinity, ease: 'linear' }}
                    >
                        📋
                    </motion.div>
                    <p>Loading order details...</p>
                </div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="od-page">
                <div className="od-container">
                    <div className="od-error-container">
                        <span className="od-error-icon">⚠️</span>
                        <h3>{error}</h3>
                        <Link to="/orders" className="od-back-link">← Back to orders</Link>
                    </div>
                </div>
            </div>
        );
    }

    const status = STATUS_COLORS[order?.orderStatus] || STATUS_COLORS.CREATED;

    return (
        <div className="od-page">
            <div className="od-container">
                <Link to="/orders" className="od-back-link">← All Orders</Link>

                <motion.div
                    className="od-header"
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                >
                    <div className="od-header-top">
                        <div>
                            <h1 className="od-title">Order #{order?.orderId}</h1>
                            <span className="od-date">{formatDate(order?.createdAt)}</span>
                        </div>
                        <span
                            className="od-status-badge"
                            style={{ background: status.bg, color: status.color }}
                        >
                            {status.label}
                        </span>
                    </div>
                </motion.div>

                {/* Status Tracker */}
                {!isCancelled && currentStepIndex >= 0 && (
                    <motion.div
                        className="od-tracker"
                        initial={{ opacity: 0, y: 15 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ delay: 0.1 }}
                    >
                        <div className="od-tracker-steps">
                            {STATUS_STEPS.map((step, idx) => {
                                const stepStatus = STATUS_COLORS[step];
                                const isActive = idx <= currentStepIndex;
                                return (
                                    <div
                                        key={step}
                                        className={`od-step ${isActive ? 'active' : ''} ${idx === currentStepIndex ? 'current' : ''}`}
                                    >
                                        <div className="od-step-dot" />
                                        <span className="od-step-label">{stepStatus.label}</span>
                                        {idx < STATUS_STEPS.length - 1 && (
                                            <div className={`od-step-line ${idx < currentStepIndex ? 'filled' : ''}`} />
                                        )}
                                    </div>
                                );
                            })}
                        </div>
                    </motion.div>
                )}

                {cancelMsg && (
                    <motion.div
                        className="od-cancel-msg"
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                    >
                        {cancelMsg}
                    </motion.div>
                )}

                <div className="od-layout">
                    {/* Items */}
                    <motion.div
                        className="od-card"
                        initial={{ opacity: 0, y: 15 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ delay: 0.15 }}
                    >
                        <h2 className="od-card-title">
                            🍽️ {order?.restaurant?.restaurantName || 'Restaurant'}
                        </h2>

                        <div className="od-items">
                            {order?.orderItems?.map(item => (
                                <div key={item.orderItemId} className="od-item">
                                    <div className="od-item-info">
                                        <span className="od-item-name">{item.foodName}</span>
                                        <span className="od-item-qty">× {item.quantity}</span>
                                        {item.addons?.length > 0 && (
                                            <div className="od-item-addons">
                                                {item.addons.map(a => (
                                                    <span key={a.addonId} className="od-addon-chip">
                                                        + {a.addonName}
                                                    </span>
                                                ))}
                                            </div>
                                        )}
                                    </div>
                                    <span className="od-item-price">₹{Number(item.itemTotal).toFixed(0)}</span>
                                </div>
                            ))}
                        </div>

                        <hr className="od-divider" />

                        <div className="od-total-row">
                            <span>Total ({order?.totalQuantity} items)</span>
                            <span className="od-total-amount">₹{Number(order?.totalPrice || 0).toFixed(0)}</span>
                        </div>

                        <div className="od-payment-row">
                            <span>Payment</span>
                            <span className={`od-payment-badge ${order?.paymentStatus?.toLowerCase()}`}>
                                {order?.paymentStatus}
                            </span>
                        </div>
                    </motion.div>

                    {/* Delivery Address */}
                    <motion.div
                        className="od-card"
                        initial={{ opacity: 0, y: 15 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ delay: 0.2 }}
                    >
                        <h2 className="od-card-title">📍 Delivery Address</h2>
                        {order?.address ? (
                            <div className="od-address">
                                <span className="od-address-line">
                                    {order.address.buildingNo}, {order.address.street}
                                </span>
                                <span className="od-address-sub">
                                    {order.address.city} - {order.address.pincode}
                                </span>
                                {order.address.landmark && (
                                    <span className="od-address-landmark">
                                        Near: {order.address.landmark}
                                    </span>
                                )}
                            </div>
                        ) : (
                            <p className="od-no-address">Address not available</p>
                        )}

                        {/* Cancel Button */}
                        {canCancel && (
                            <button
                                className="od-cancel-btn"
                                onClick={handleCancel}
                                disabled={cancelling}
                            >
                                {cancelling ? 'Cancelling...' : '✕ Cancel Order'}
                            </button>
                        )}
                    </motion.div>

                    {isSpecialOrder && (
                        <motion.div
                            initial={{ opacity: 0, y: 15 }}
                            animate={{ opacity: 1, y: 0 }}
                            transition={{ delay: 0.25 }}
                        >
                            <OrderChatPanel
                                orderId={order.orderId}
                                canSend={canSendChat}
                                sendDisabledReason={sendDisabledReason}
                                pollMs={4000}
                            />
                        </motion.div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default OrderDetailPage;

