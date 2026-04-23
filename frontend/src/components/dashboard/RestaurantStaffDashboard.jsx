import React, { useEffect, useMemo, useState } from 'react';
import { getRestaurantOrders, updateOrderStatus } from '../../api/orderService';
import { updateKitchenStatus } from '../../api/kitchenService';
import OrderChatPanel from '../orders/OrderChatPanel';
import './RestaurantStaffDashboard.css';

const STATUS_LABELS = {
    CREATED: 'Created',
    CONFIRMED: 'Confirmed',
    PREPARING: 'Preparing',
    OUT_FOR_DELIVERY: 'Out for Delivery',
    DELIVERED: 'Delivered',
    CANCELLED: 'Cancelled',
};

const STATUS_STYLES = {
    CREATED: { bg: '#DBEAFE', color: '#1D4ED8' },
    CONFIRMED: { bg: '#DCFCE7', color: '#16A34A' },
    PREPARING: { bg: '#FEF3C7', color: '#B45309' },
    OUT_FOR_DELIVERY: { bg: '#FFF7ED', color: '#C2410C' },
    DELIVERED: { bg: '#DCFCE7', color: '#15803D' },
    CANCELLED: { bg: '#FEE2E2', color: '#DC2626' },
};

const VALID_TRANSITIONS = {
    CREATED: ['CONFIRMED', 'CANCELLED'],
    CONFIRMED: ['PREPARING', 'CANCELLED'],
    PREPARING: ['OUT_FOR_DELIVERY'],
    OUT_FOR_DELIVERY: ['DELIVERED'],
    DELIVERED: [],
    CANCELLED: [],
};

const KITCHEN_LOADS = ['LOW', 'MEDIUM', 'HIGH'];

const RestaurantStaffDashboard = () => {
    const [activeTab, setActiveTab] = useState('orders');
    const [orders, setOrders] = useState([]);
    const [orderFilter, setOrderFilter] = useState('ALL');
    const [loading, setLoading] = useState(true);
    const [updatingId, setUpdatingId] = useState(null);
    const [expandedChatOrderId, setExpandedChatOrderId] = useState(null);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');

    const [kitchenLoadIndicator, setKitchenLoadIndicator] = useState('MEDIUM');
    const [kitchenBusy, setKitchenBusy] = useState(false);
    const [kitchenResponse, setKitchenResponse] = useState(null);

    const flash = (msg) => {
        setSuccess(msg);
        setTimeout(() => setSuccess(''), 2500);
    };

    const flashError = (err) => {
        const msg = err?.response?.data?.message || err?.response?.data || 'Something went wrong.';
        setError(typeof msg === 'string' ? msg : 'Something went wrong.');
        setTimeout(() => setError(''), 3500);
    };

    const fetchRestaurantOrders = async () => {
        setLoading(true);
        try {
            const data = await getRestaurantOrders();
            setOrders(data);
        } catch (err) {
            flashError(err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchRestaurantOrders();
    }, []);

    const filteredOrders = useMemo(() => {
        return orderFilter === 'ALL' ? orders : orders.filter((o) => o.orderStatus === orderFilter);
    }, [orders, orderFilter]);

    const kitchenRestaurantId = useMemo(() => {
        const id = orders?.[0]?.restaurant?.restaurantId;
        return Number.isFinite(Number(id)) ? Number(id) : null;
    }, [orders]);

    const formatDate = (dateStr) => {
        if (!dateStr) return '';
        return new Date(dateStr).toLocaleDateString('en-IN', {
            day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit'
        });
    };

    const handleStatusChange = async (orderId, newStatus) => {
        setUpdatingId(orderId);
        try {
            await updateOrderStatus(orderId, newStatus);
            flash(`Order #${orderId} moved to ${STATUS_LABELS[newStatus]}.`);
            fetchRestaurantOrders();
        } catch (err) {
            flashError(err);
        } finally {
            setUpdatingId(null);
        }
    };

    const handleKitchenUpdate = async (e) => {
        e.preventDefault();

        if (!kitchenRestaurantId) {
            setError('Restaurant context not found. Refresh orders first.');
            setTimeout(() => setError(''), 3000);
            return;
        }

        setKitchenBusy(true);
        try {
            const response = await updateKitchenStatus(kitchenRestaurantId, kitchenLoadIndicator);
            setKitchenResponse(response);
            flash('Kitchen load updated.');
        } catch (err) {
            flashError(err);
        } finally {
            setKitchenBusy(false);
        }
    };

    if (loading) {
        return (
            <div className="sd-page">
                <div className="sd-container">
                    <div className="sd-page-header">
                        <div className="skeleton skeleton-title" style={{ width: '220px' }}></div>
                        <div className="skeleton skeleton-text" style={{ width: '260px' }}></div>
                    </div>

                    <div className="sd-tabs">
                        <div className="skeleton" style={{ width: '140px', height: '38px', borderRadius: '10px' }}></div>
                        <div className="skeleton" style={{ width: '180px', height: '38px', borderRadius: '10px' }}></div>
                    </div>

                    <div>
                        {[1, 2, 3].map((item) => (
                            <div key={item} className="skeleton" style={{ height: '84px', marginBottom: '10px', borderRadius: '12px' }}></div>
                        ))}
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="sd-page">
            <div className="sd-container">
                <div className="sd-page-header">
                    <h1>Staff Dashboard</h1>
                    <span>Handle incoming orders and kitchen load</span>
                </div>

                {error && <div className="sd-toast sd-toast-error">{error}</div>}
                {success && <div className="sd-toast sd-toast-success">{success}</div>}

                <div className="sd-tabs">
                    <button className={`sd-tab ${activeTab === 'orders' ? 'active' : ''}`} onClick={() => setActiveTab('orders')}>
                        📦 Orders
                    </button>
                    <button className={`sd-tab ${activeTab === 'kitchen' ? 'active' : ''}`} onClick={() => setActiveTab('kitchen')}>
                        👨‍🍳 Kitchen Controller
                    </button>
                </div>

                {activeTab === 'orders' && (
                    <div>
                        <div className="sd-tab-header">
                            <h3>Restaurant Orders ({orders.length})</h3>
                            <button className="sd-btn sd-btn-outline" onClick={fetchRestaurantOrders}>↻ Refresh</button>
                        </div>

                        <div className="sd-order-filters">
                            {['ALL', 'CREATED', 'CONFIRMED', 'PREPARING', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED'].map((status) => (
                                <button
                                    key={status}
                                    className={`sd-order-filter ${orderFilter === status ? 'active' : ''}`}
                                    onClick={() => setOrderFilter(status)}
                                >
                                    {status === 'ALL' ? 'All' : STATUS_LABELS[status]}
                                </button>
                            ))}
                        </div>

                        {filteredOrders.length === 0 ? (
                            <div className="sd-empty"><p>No orders for selected filter.</p></div>
                        ) : (
                            <div className="sd-list">
                                {filteredOrders.map((order) => {
                                    const style = STATUS_STYLES[order.orderStatus] || STATUS_STYLES.CREATED;
                                    const nextStatuses = VALID_TRANSITIONS[order.orderStatus] || [];
                                    const isSpecialOrder = Boolean(order.isSpecial);
                                    const isChatOpen = expandedChatOrderId === order.orderId;
                                    const canSendChat = !['CANCELLED', 'DELIVERED'].includes(order.orderStatus);
                                    return (
                                        <div key={order.orderId} className="sd-order-card">
                                            <div className="sd-order-top">
                                                <div>
                                                    <span className="sd-order-id">Order #{order.orderId}</span>
                                                    <span className="sd-order-date">{formatDate(order.createdAt)}</span>
                                                </div>
                                                <span className="sd-order-badge" style={{ background: style.bg, color: style.color }}>
                                                    {STATUS_LABELS[order.orderStatus]}
                                                </span>
                                            </div>

                                            <div className="sd-order-row">
                                                <span>🍽️ {order.restaurant?.restaurantName || 'Restaurant'}</span>
                                                <span>👤 {order.user?.firstName || 'Customer'} {order.user?.lastName || ''}</span>
                                            </div>

                                            <div className="sd-order-items">
                                                {order.orderItems?.map((item) => (
                                                    <span key={item.orderItemId} className="sd-order-item-chip">
                                                        {item.foodName} × {item.quantity}
                                                    </span>
                                                ))}
                                            </div>

                                            <div className="sd-order-bottom">
                                                <span className="sd-order-total">₹{Number(order.totalPrice).toFixed(0)}</span>
                                                <div className="sd-order-status-actions">
                                                    {isSpecialOrder && (
                                                        <button
                                                            className="sd-btn sd-btn-sm sd-btn-outline"
                                                            onClick={() => setExpandedChatOrderId((prev) => prev === order.orderId ? null : order.orderId)}
                                                        >
                                                            {isChatOpen ? 'Hide Chat' : 'Open Chat'}
                                                        </button>
                                                    )}
                                                    {nextStatuses.length > 0 && (
                                                        <>
                                                        {nextStatuses.map((status) => (
                                                            <button
                                                                key={status}
                                                                className={`sd-btn sd-btn-sm ${status === 'CANCELLED' ? 'sd-btn-danger' : 'sd-btn-primary'}`}
                                                                disabled={updatingId === order.orderId}
                                                                onClick={() => handleStatusChange(order.orderId, status)}
                                                            >
                                                                {updatingId === order.orderId ? '...' : STATUS_LABELS[status]}
                                                            </button>
                                                        ))}
                                                        </>
                                                    )}
                                                </div>
                                            </div>

                                            {isSpecialOrder && isChatOpen && (
                                                <div className="sd-chat-wrap">
                                                    <OrderChatPanel
                                                        orderId={order.orderId}
                                                        canSend={canSendChat}
                                                        sendDisabledReason="Chat is read-only once the order is cancelled or delivered."
                                                        pollMs={4000}
                                                        title={`Order #${order.orderId} Chat`}
                                                    />
                                                </div>
                                            )}
                                        </div>
                                    );
                                })}
                            </div>
                        )}
                    </div>
                )}

                {activeTab === 'kitchen' && (
                    <div className="sd-kitchen-wrap">
                        <form className="sd-kitchen-form" onSubmit={handleKitchenUpdate}>
                            <h3>Kitchen Controller</h3>
                            <p>Set the kitchen load to help the platform manage order pressure.</p>
                            <p className="sd-kitchen-context">
                                Restaurant ID: <strong>{kitchenRestaurantId || 'Not detected yet'}</strong>
                            </p>

                            <label>Kitchen Load</label>
                            <select
                                value={kitchenLoadIndicator}
                                onChange={(e) => setKitchenLoadIndicator(e.target.value)}
                            >
                                {KITCHEN_LOADS.map((load) => (
                                    <option key={load} value={load}>{load}</option>
                                ))}
                            </select>

                            <button type="submit" className="sd-btn sd-btn-primary" disabled={kitchenBusy}>
                                {kitchenBusy ? 'Updating...' : 'Update Kitchen Status'}
                            </button>
                        </form>

                        {kitchenResponse && (
                            <div className="sd-kitchen-card">
                                <h4>{kitchenResponse.restaurantName}</h4>
                                <div className="sd-kitchen-meta">
                                    <span>Restaurant ID: {kitchenResponse.restaurantId}</span>
                                    <span>Current Orders: {kitchenResponse.currentOrders}</span>
                                </div>
                                <span className="sd-kitchen-badge">Load: {kitchenResponse.kitchenLoadStatus}</span>
                                <p>{kitchenResponse.message}</p>
                            </div>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
};

export default RestaurantStaffDashboard;

