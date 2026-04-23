import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { getMyOrders } from '../../api/orderService';
import { sortOrdersByMostRecent } from '../../utils/orderUtils';
import { motion, AnimatePresence } from 'framer-motion';
import './OrdersPage.css';

const STATUS_COLORS = {
    CREATED: { bg: '#DBEAFE', color: '#1D4ED8', label: 'Created' },
    CONFIRMED: { bg: '#DCFCE7', color: '#16A34A', label: 'Confirmed' },
    PREPARING: { bg: '#FEF3C7', color: '#B45309', label: 'Preparing' },
    OUT_FOR_DELIVERY: { bg: '#FFF7ED', color: '#C2410C', label: 'Out for Delivery' },
    DELIVERED: { bg: '#DCFCE7', color: '#15803D', label: 'Delivered' },
    CANCELLED: { bg: '#FEE2E2', color: '#DC2626', label: 'Cancelled' },
};

const OrdersPage = () => {
    const [orders, setOrders] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [filter, setFilter] = useState('ALL');

    useEffect(() => {
        fetchOrders();
    }, []);

    const fetchOrders = async () => {
        setLoading(true);
        setError('');
        try {
            const data = await getMyOrders();
            setOrders(sortOrdersByMostRecent(data));
        } catch (err) {
            setError('Unable to load orders. Please try again.');
        } finally {
            setLoading(false);
        }
    };

    const filteredOrders = filter === 'ALL'
        ? orders
        : orders.filter(o => o.orderStatus === filter);

    const formatDate = (dateStr) => {
        if (!dateStr) return '';
        const date = new Date(dateStr);
        return date.toLocaleDateString('en-IN', {
            day: 'numeric',
            month: 'short',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    };

    if (loading) {
        return (
            <div className="orders-page">
                <div className="orders-container">
                    <div className="orders-header">
                        <div className="skeleton skeleton-title" style={{ width: '180px' }}></div>
                        <div className="skeleton skeleton-text" style={{ width: '90px' }}></div>
                    </div>

                    <div className="orders-filters">
                        {[1, 2, 3, 4].map((item) => (
                            <div key={item} className="skeleton" style={{ width: '90px', height: '30px', borderRadius: '999px' }}></div>
                        ))}
                    </div>

                    {[1, 2, 3].map((item) => (
                        <div key={item} className="order-card" style={{ marginBottom: '12px' }}>
                            <div className="order-card-top">
                                <div style={{ flex: 1 }}>
                                    <div className="skeleton skeleton-text" style={{ width: '45%' }}></div>
                                    <div className="skeleton skeleton-text" style={{ width: '30%' }}></div>
                                </div>
                                <div className="skeleton" style={{ width: '90px', height: '26px', borderRadius: '999px' }}></div>
                            </div>
                            <div className="skeleton skeleton-text" style={{ width: '70%' }}></div>
                            <div className="order-card-bottom">
                                <div className="skeleton skeleton-text" style={{ width: '120px' }}></div>
                                <div className="skeleton skeleton-text" style={{ width: '60px' }}></div>
                            </div>
                        </div>
                    ))}
                </div>
            </div>
        );
    }

    return (
        <div className="orders-page">
            <div className="orders-container">
                <div className="orders-header">
                    <h1 className="orders-title">My Orders</h1>
                    <span className="orders-count">{orders.length} order{orders.length !== 1 ? 's' : ''}</span>
                </div>

                {error && (
                    <motion.div
                        className="orders-error"
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                    >
                        ⚠️ {error}
                    </motion.div>
                )}

                {/* Filters */}
                <div className="orders-filters">
                    {['ALL', 'CREATED', 'CONFIRMED', 'PREPARING', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED'].map(f => (
                        <button
                            key={f}
                            className={`orders-filter-btn ${filter === f ? 'active' : ''}`}
                            onClick={() => setFilter(f)}
                        >
                            {f === 'ALL' ? 'All' : (STATUS_COLORS[f]?.label || f)}
                        </button>
                    ))}
                </div>

                {orders.length === 0 ? (
                    <motion.div
                        className="orders-empty"
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                    >
                        <span className="orders-empty-icon">📦</span>
                        <h2>No orders yet</h2>
                        <p>Your order history will appear here after your first order.</p>
                        <Link to="/" className="orders-btn orders-btn-primary">Browse Restaurants</Link>
                    </motion.div>
                ) : filteredOrders.length === 0 ? (
                    <motion.div
                        className="orders-empty"
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                    >
                        <p>No orders with status "{STATUS_COLORS[filter]?.label || filter}"</p>
                    </motion.div>
                ) : (
                    <motion.div
                        key={filter}
                        className="orders-list"
                        initial="hidden"
                        animate="visible"
                        variants={{
                            hidden: {},
                            visible: { transition: { staggerChildren: 0.05 } }
                        }}
                    >
                        <AnimatePresence>
                            {filteredOrders.map(order => {
                                const status = STATUS_COLORS[order.orderStatus] || STATUS_COLORS.CREATED;
                                return (
                                    <motion.div
                                        key={order.orderId}
                                        variants={{
                                            hidden: { opacity: 0, y: 15 },
                                            visible: { opacity: 1, y: 0 }
                                        }}
                                        exit={{ opacity: 0, y: -10 }}
                                        layout
                                    >
                                        <Link to={`/orders/${order.orderId}`} className="order-card-link">
                                            <div className="order-card">
                                                <div className="order-card-top">
                                                    <div className="order-card-info">
                                                        <h3 className="order-card-restaurant">
                                                            🍽️ {order.restaurant?.restaurantName || 'Restaurant'}
                                                        </h3>
                                                        <span className="order-card-id">
                                                            Order #{order.orderId}
                                                        </span>
                                                    </div>
                                                    <span
                                                        className="order-status-badge"
                                                        style={{ background: status.bg, color: status.color }}
                                                    >
                                                        {status.label}
                                                    </span>
                                                </div>

                                                <div className="order-card-items">
                                                    {order.orderItems?.slice(0, 3).map(item => (
                                                        <span key={item.orderItemId} className="order-card-item">
                                                            {item.foodName} × {item.quantity}
                                                        </span>
                                                    ))}
                                                    {order.orderItems?.length > 3 && (
                                                        <span className="order-card-more">
                                                            +{order.orderItems.length - 3} more
                                                        </span>
                                                    )}
                                                </div>

                                                <div className="order-card-bottom">
                                                    <span className="order-card-date">
                                                        {formatDate(order.createdAt)}
                                                    </span>
                                                    <span className="order-card-total">
                                                        ₹{Number(order.totalPrice).toFixed(0)}
                                                    </span>
                                                </div>
                                            </div>
                                        </Link>
                                    </motion.div>
                                );
                            })}
                        </AnimatePresence>
                    </motion.div>
                )}
            </div>
        </div>
    );
};

export default OrdersPage;

