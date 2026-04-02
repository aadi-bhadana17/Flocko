import React, { useState, useEffect, useMemo } from 'react';
import {
    getRoleRequests, updateRoleRequest,
    getAllRestaurants, updateRestaurant, updateRestaurantStatus,
    getAdminRestaurantOrders, getAllOrders,
    getAllUsers, getUserById, restrictUser, unrestrictUser, blockUser, unblockUser, deleteUser,
    suspendRestaurant, activateRestaurant,
} from '../../api/adminService';
import { motion, AnimatePresence } from 'framer-motion';
import './AdminDashboard.css';

const TABS = ['users', 'requests', 'restaurants', 'orders'];
const TAB_LABELS = { users: '👥 Users', requests: '📋 Role Requests', restaurants: '🏪 Restaurants', orders: '📦 Orders' };

const REQUEST_STATUS_STYLES = {
    PENDING:  { bg: '#FEF3C7', color: '#B45309' },
    APPROVED: { bg: '#DCFCE7', color: '#16A34A' },
    REJECTED: { bg: '#FEE2E2', color: '#DC2626' },
};

const ACCOUNT_STATUS_STYLES = {
    ACTIVE:     { bg: '#DCFCE7', color: '#16A34A' },
    RESTRICTED: { bg: '#FEF3C7', color: '#B45309' },
    BLOCKED:    { bg: '#FEE2E2', color: '#DC2626' },
    DELETED:    { bg: '#F3F4F6', color: '#6B7280' },
};

const ORDER_STATUS_LABELS = {
    CREATED: 'Created', CONFIRMED: 'Confirmed', PREPARING: 'Preparing',
    OUT_FOR_DELIVERY: 'Out for Delivery', DELIVERED: 'Delivered', CANCELLED: 'Cancelled',
};

const ORDER_STATUS_STYLES = {
    CREATED:          { bg: '#DBEAFE', color: '#1D4ED8' },
    CONFIRMED:        { bg: '#DCFCE7', color: '#16A34A' },
    PREPARING:        { bg: '#FEF3C7', color: '#B45309' },
    OUT_FOR_DELIVERY: { bg: '#FFF7ED', color: '#C2410C' },
    DELIVERED:        { bg: '#DCFCE7', color: '#15803D' },
    CANCELLED:        { bg: '#FEE2E2', color: '#DC2626' },
};

const ADMIN_GREETINGS = [
    "Hi Boss, ready to run things?",
    "Welcome back, Boss. The empire awaits.",
    "Hey Boss, let's keep this ship sailing.",
    "Good to see you, Boss. Time to manage.",
];

const AdminDashboard = () => {
    const greeting = useMemo(() => ADMIN_GREETINGS[Math.floor(Math.random() * ADMIN_GREETINGS.length)], []);

    const [activeTab, setActiveTab] = useState('users');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');

    // Data
    const [requests, setRequests] = useState([]);
    const [restaurants, setRestaurants] = useState([]);
    const [orders, setOrders] = useState([]);
    const [allOrdersList, setAllOrdersList] = useState([]);
    const [users, setUsers] = useState([]);
    const [selectedRestaurantId, setSelectedRestaurantId] = useState(null);
    const [loadingOrders, setLoadingOrders] = useState(false);
    const [orderMode, setOrderMode] = useState('all'); // 'all' or 'restaurant'

    useEffect(() => { fetchInitial(); }, []);

    useEffect(() => {
        if (activeTab === 'users') fetchUsers();
        if (activeTab === 'requests') fetchRequests();
        if (activeTab === 'restaurants') fetchRestaurants();
        if (activeTab === 'orders') fetchRestaurants(); // need restaurant list for selector
    }, [activeTab]);

    // Fetch orders whenever the orders tab is active and mode/restaurant changes
    useEffect(() => {
        if (activeTab === 'orders') {
            if (orderMode === 'all') {
                setLoadingOrders(true);
                fetchAllOrders().finally(() => setLoadingOrders(false));
            } else if (orderMode === 'restaurant' && selectedRestaurantId) {
                setLoadingOrders(true);
                fetchOrders(selectedRestaurantId).finally(() => setLoadingOrders(false));
            }
        }
    }, [activeTab, orderMode, selectedRestaurantId]);

    const flash = (msg) => { setSuccess(msg); setTimeout(() => setSuccess(''), 2500); };
    const flashError = (err) => {
        const msg = err.response?.data?.message || err.response?.data || 'Something went wrong.';
        setError(typeof msg === 'string' ? msg : 'Something went wrong.');
        setTimeout(() => setError(''), 4000);
    };

    const fetchInitial = async () => {
        setLoading(true);
        try {
            const [reqData, restData, userData] = await Promise.all([getRoleRequests(), getAllRestaurants(), getAllUsers()]);
            setRequests(reqData);
            setRestaurants(restData);
            setUsers(userData);
            if (restData.length > 0) setSelectedRestaurantId(restData[0].restaurantId);
        } catch (e) { flashError(e); }
        finally { setLoading(false); }
    };

    const fetchUsers = async () => {
        try { setUsers(await getAllUsers()); }
        catch (e) { flashError(e); }
    };

    const fetchRequests = async () => {
        try { setRequests(await getRoleRequests()); }
        catch (e) { flashError(e); }
    };

    const fetchRestaurants = async () => {
        try {
            const data = await getAllRestaurants();
            setRestaurants(data);
            if (!selectedRestaurantId && data.length > 0) setSelectedRestaurantId(data[0].restaurantId);
        } catch (e) { flashError(e); }
    };

    const fetchOrders = async (restaurantId) => {
        if (!restaurantId) return;
        try { setOrders(await getAdminRestaurantOrders(restaurantId)); }
        catch (e) { flashError(e); }
    };

    const fetchAllOrders = async () => {
        try { setAllOrdersList(await getAllOrders()); }
        catch (e) { flashError(e); }
    };

    // ═══════════════════════════════════════
    //  SUMMARY
    // ═══════════════════════════════════════
    const pendingCount = requests.filter(r => r.requestStatus === 'PENDING').length;
    const totalRestaurants = restaurants.length;
    const openRestaurants = restaurants.filter(r => r.open).length;
    const totalUsers = users.length;
    const activeUsers = users.filter(u => u.status === 'ACTIVE').length;

    // ═══════════════════════════════════════
    //  USERS TAB
    // ═══════════════════════════════════════
    const UsersTab = () => {
        const [filter, setFilter] = useState('ALL');
        const [processingId, setProcessingId] = useState(null);
        const [restrictModal, setRestrictModal] = useState(null);
        const [restrictDays, setRestrictDays] = useState(7);
        const [restrictReason, setRestrictReason] = useState('');
        const [expandedUserId, setExpandedUserId] = useState(null);
        const [expandedUserDetail, setExpandedUserDetail] = useState(null);
        const [loadingDetail, setLoadingDetail] = useState(false);

        const filtered = filter === 'ALL'
            ? users
            : users.filter(u => u.status === filter);

        const formatDate = (dateStr) => {
            if (!dateStr) return '';
            return new Date(dateStr).toLocaleDateString('en-IN', {
                day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
            });
        };

        const handleToggleExpand = async (userId) => {
            if (expandedUserId === userId) {
                setExpandedUserId(null);
                setExpandedUserDetail(null);
                setRestrictModal(null);
                return;
            }
            setExpandedUserId(userId);
            setExpandedUserDetail(null);
            setLoadingDetail(true);
            try {
                const detail = await getUserById(userId);
                setExpandedUserDetail(detail);
            } catch (e) { flashError(e); }
            finally { setLoadingDetail(false); }
        };

        const handleRestrict = async () => {
            if (!restrictModal) return;
            setProcessingId(restrictModal);
            try {
                const msg = await restrictUser(restrictModal, restrictDays, restrictReason);
                flash(typeof msg === 'string' ? msg : 'User restricted.');
                setRestrictModal(null);
                setRestrictDays(7);
                setRestrictReason('');
                setExpandedUserId(null);
                setExpandedUserDetail(null);
                fetchUsers();
            } catch (e) { flashError(e); }
            finally { setProcessingId(null); }
        };

        const handleUnrestrict = async (userId) => {
            setProcessingId(userId);
            try {
                const msg = await unrestrictUser(userId);
                flash(typeof msg === 'string' ? msg : 'User unrestricted.');
                setExpandedUserId(null);
                setExpandedUserDetail(null);
                fetchUsers();
            } catch (e) { flashError(e); }
            finally { setProcessingId(null); }
        };

        const handleBlock = async (userId) => {
            setProcessingId(userId);
            try {
                const msg = await blockUser(userId);
                flash(typeof msg === 'string' ? msg : 'User blocked.');
                setExpandedUserId(null);
                setExpandedUserDetail(null);
                fetchUsers();
            } catch (e) { flashError(e); }
            finally { setProcessingId(null); }
        };

        const handleUnblock = async (userId) => {
            setProcessingId(userId);
            try {
                const msg = await unblockUser(userId);
                flash(typeof msg === 'string' ? msg : 'User unblocked.');
                setExpandedUserId(null);
                setExpandedUserDetail(null);
                fetchUsers();
            } catch (e) { flashError(e); }
            finally { setProcessingId(null); }
        };

        const handleDelete = async (userId) => {
            if (!window.confirm('Are you sure you want to delete this user?')) return;
            setProcessingId(userId);
            try {
                const msg = await deleteUser(userId);
                flash(typeof msg === 'string' ? msg : 'User deleted.');
                setExpandedUserId(null);
                setExpandedUserDetail(null);
                fetchUsers();
            } catch (e) { flashError(e); }
            finally { setProcessingId(null); }
        };

        return (
            <div>
                <div className="ad-tab-header">
                    <h3>All Users ({users.length})</h3>
                    <button className="ad-btn ad-btn-outline ad-btn-sm" onClick={fetchUsers}>↻ Refresh</button>
                </div>

                <div className="ad-filters">
                    {['ALL', 'ACTIVE', 'RESTRICTED', 'BLOCKED', 'DELETED'].map(f => (
                        <button key={f} className={`ad-filter ${filter === f ? 'active' : ''}`} onClick={() => setFilter(f)}>
                            {f === 'ALL' ? `All (${totalUsers})` : `${f.charAt(0) + f.slice(1).toLowerCase()} (${users.filter(u => u.status === f).length})`}
                        </button>
                    ))}
                </div>

                {filtered.length === 0 ? (
                    <div className="ad-empty-small"><p>No {filter !== 'ALL' ? filter.toLowerCase() : ''} users found.</p></div>
                ) : (
                    <div className="ad-request-list">
                        <AnimatePresence>
                            {filtered.map(u => {
                                const style = ACCOUNT_STATUS_STYLES[u.status] || ACCOUNT_STATUS_STYLES.ACTIVE;
                                const isExpanded = expandedUserId === u.id;
                                const detail = isExpanded ? expandedUserDetail : null;
                                return (
                                    <motion.div
                                        key={u.id}
                                        className="ad-request-card"
                                        initial={{ opacity: 0, y: 10 }}
                                        animate={{ opacity: 1, y: 0 }}
                                        exit={{ opacity: 0, y: -10 }}
                                        layout
                                        style={{ cursor: 'pointer' }}
                                        onClick={() => handleToggleExpand(u.id)}
                                    >
                                        {/* Summary row – always visible */}
                                        <div className="ad-request-top">
                                            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                                <span className="ad-request-user">{u.name}</span>
                                                <span className="ad-request-status" style={{ background: style.bg, color: style.color, fontSize: '0.72rem' }}>
                                                    {u.status}
                                                </span>
                                                <span style={{
                                                    width: 8, height: 8, borderRadius: '50%',
                                                    background: u.online ? '#22C55E' : '#D1D5DB', display: 'inline-block', flexShrink: 0
                                                }} title={u.online ? 'Online' : 'Offline'} />
                                            </div>
                                            <div style={{ display: 'flex', gap: '0.4rem', alignItems: 'center' }}>
                                                <span className="ad-request-role">{u.role?.replace('_', ' ')}</span>
                                                <span style={{ fontSize: '0.85rem', color: '#6B7280' }}>{isExpanded ? '▲' : '▼'}</span>
                                            </div>
                                        </div>
                                        <span className="ad-request-email" style={{ display: 'block', marginTop: '0.1rem' }}>{u.email}</span>

                                        {/* Expanded detail panel */}
                                        {isExpanded && (
                                            <motion.div
                                                initial={{ opacity: 0, height: 0 }}
                                                animate={{ opacity: 1, height: 'auto' }}
                                                exit={{ opacity: 0, height: 0 }}
                                                onClick={e => e.stopPropagation()}
                                                style={{
                                                    marginTop: '0.85rem', paddingTop: '0.85rem',
                                                    borderTop: '1px solid var(--border, #E5E7EB)',
                                                }}
                                            >
                                                {loadingDetail ? (
                                                    <p style={{ fontSize: '0.85rem', color: '#6B7280' }}>Loading details...</p>
                                                ) : detail ? (
                                                    <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                                                        {/* User details grid */}
                                                        <div className="ad-user-detail-grid">
                                                            <div className="ad-user-detail-item">
                                                                <span className="ad-user-detail-label">User ID</span>
                                                                <span className="ad-user-detail-value">#{detail.id}</span>
                                                            </div>
                                                            <div className="ad-user-detail-item">
                                                                <span className="ad-user-detail-label">Full Name</span>
                                                                <span className="ad-user-detail-value">{detail.name}</span>
                                                            </div>
                                                            <div className="ad-user-detail-item">
                                                                <span className="ad-user-detail-label">Email</span>
                                                                <span className="ad-user-detail-value">{detail.email}</span>
                                                            </div>
                                                            <div className="ad-user-detail-item">
                                                                <span className="ad-user-detail-label">Role</span>
                                                                <span className="ad-user-detail-value">{detail.role?.replace('_', ' ')}</span>
                                                            </div>
                                                            <div className="ad-user-detail-item">
                                                                <span className="ad-user-detail-label">Status</span>
                                                                <span className="ad-request-status" style={{ background: style.bg, color: style.color }}>
                                                                    {detail.status}
                                                                </span>
                                                            </div>
                                                            <div className="ad-user-detail-item">
                                                                <span className="ad-user-detail-label">Online</span>
                                                                <span className="ad-user-detail-value">{detail.online ? '🟢 Yes' : '⚪ No'}</span>
                                                            </div>
                                                            {detail.restrictedUntil && (
                                                                <div className="ad-user-detail-item" style={{ gridColumn: '1 / -1' }}>
                                                                    <span className="ad-user-detail-label">Restricted Until</span>
                                                                    <span className="ad-user-detail-value" style={{ color: '#B45309' }}>{formatDate(detail.restrictedUntil)}</span>
                                                                </div>
                                                            )}
                                                            {detail.restrictionReason && (
                                                                <div className="ad-user-detail-item" style={{ gridColumn: '1 / -1' }}>
                                                                    <span className="ad-user-detail-label">Restriction Reason</span>
                                                                    <span className="ad-user-detail-value" style={{ color: '#B45309', fontStyle: 'italic' }}>"{detail.restrictionReason}"</span>
                                                                </div>
                                                            )}
                                                        </div>

                                                        {/* Owned restaurants */}
                                                        {detail.ownedRestaurants?.length > 0 && (
                                                            <div style={{ marginTop: '0.25rem' }}>
                                                                <span style={{ fontSize: '0.82rem', fontWeight: 600, color: '#1F2937' }}>🏪 Owned Restaurants</span>
                                                                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.35rem', marginTop: '0.3rem' }}>
                                                                    {detail.ownedRestaurants.map(r => (
                                                                        <span key={r.restaurantId} className="ad-order-item-chip">
                                                                            {r.restaurantName} · {r.cuisineType} {r.avgRating ? `· ★ ${r.avgRating}` : ''}
                                                                        </span>
                                                                    ))}
                                                                </div>
                                                            </div>
                                                        )}

                                                        {/* Action buttons – only here */}
                                                        <div style={{
                                                            display: 'flex', gap: '0.4rem', flexWrap: 'wrap',
                                                            marginTop: '0.5rem', paddingTop: '0.65rem',
                                                            borderTop: '1px solid var(--border, #E5E7EB)'
                                                        }}>
                                                            {detail.status === 'ACTIVE' && (
                                                                <>
                                                                    <button className="ad-btn ad-btn-sm ad-btn-outline"
                                                                        disabled={processingId === detail.id}
                                                                        onClick={() => setRestrictModal(detail.id)}
                                                                    >⏳ Restrict</button>
                                                                    <button className="ad-btn ad-btn-sm ad-btn-reject"
                                                                        disabled={processingId === detail.id}
                                                                        onClick={() => handleBlock(detail.id)}
                                                                    >🚫 Block</button>
                                                                </>
                                                            )}
                                                            {detail.status === 'RESTRICTED' && (
                                                                <>
                                                                    <button className="ad-btn ad-btn-sm ad-btn-approve"
                                                                        disabled={processingId === detail.id}
                                                                        onClick={() => handleUnrestrict(detail.id)}
                                                                    >✓ Unrestrict</button>
                                                                    <button className="ad-btn ad-btn-sm ad-btn-reject"
                                                                        disabled={processingId === detail.id}
                                                                        onClick={() => handleBlock(detail.id)}
                                                                    >🚫 Block</button>
                                                                </>
                                                            )}
                                                            {detail.status === 'BLOCKED' && (
                                                                <button className="ad-btn ad-btn-sm ad-btn-approve"
                                                                    disabled={processingId === detail.id}
                                                                    onClick={() => handleUnblock(detail.id)}
                                                                >✓ Unblock</button>
                                                            )}
                                                            {detail.status !== 'DELETED' && (
                                                                <button className="ad-btn ad-btn-sm ad-btn-danger"
                                                                    disabled={processingId === detail.id}
                                                                    onClick={() => handleDelete(detail.id)}
                                                                >🗑️ Delete</button>
                                                            )}
                                                        </div>

                                                        {/* Restrict inline form */}
                                                        {restrictModal === detail.id && (
                                                            <div style={{
                                                                marginTop: '0.5rem', padding: '1rem',
                                                                background: '#FFFBEB', borderRadius: '10px',
                                                                border: '1px solid #FDE68A',
                                                                display: 'flex', flexDirection: 'column', gap: '0.5rem'
                                                            }}>
                                                                <span style={{ fontWeight: 600, fontSize: '0.9rem', color: '#B45309' }}>
                                                                    Restrict {detail.name}
                                                                </span>
                                                                <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                                                                    <label style={{ fontSize: '0.85rem', color: '#6B7280' }}>Days:</label>
                                                                    <input type="number" min={1} max={365} value={restrictDays}
                                                                        onChange={e => setRestrictDays(parseInt(e.target.value) || 1)}
                                                                        style={{
                                                                            width: '70px', padding: '0.35rem 0.5rem',
                                                                            border: '1.5px solid #E5E7EB', borderRadius: '6px',
                                                                            fontFamily: 'DM Sans', fontSize: '0.85rem'
                                                                        }}
                                                                    />
                                                                </div>
                                                                <input placeholder="Reason for restriction" value={restrictReason}
                                                                    onChange={e => setRestrictReason(e.target.value)}
                                                                    style={{
                                                                        padding: '0.45rem 0.65rem', border: '1.5px solid #E5E7EB',
                                                                        borderRadius: '6px', fontFamily: 'DM Sans', fontSize: '0.85rem'
                                                                    }}
                                                                />
                                                                <div style={{ display: 'flex', gap: '0.4rem' }}>
                                                                    <button className="ad-btn ad-btn-sm ad-btn-approve"
                                                                        disabled={processingId === detail.id}
                                                                        onClick={handleRestrict}
                                                                    >{processingId === detail.id ? '...' : '✓ Confirm'}</button>
                                                                    <button className="ad-btn ad-btn-sm ad-btn-outline"
                                                                        onClick={() => { setRestrictModal(null); setRestrictReason(''); setRestrictDays(7); }}
                                                                    >Cancel</button>
                                                                </div>
                                                            </div>
                                                        )}
                                                    </div>
                                                ) : null}
                                            </motion.div>
                                        )}
                                    </motion.div>
                                );
                            })}
                        </AnimatePresence>
                    </div>
                )}
            </div>
        );
    };

    // ═══════════════════════════════════════
    //  ROLE REQUESTS TAB
    // ═══════════════════════════════════════
    const RequestsTab = () => {
        const [filter, setFilter] = useState('ALL');
        const [processingId, setProcessingId] = useState(null);

        const filtered = filter === 'ALL'
            ? requests
            : requests.filter(r => r.requestStatus === filter);

        const handleAction = async (requestId, action) => {
            setProcessingId(requestId);
            try {
                await updateRoleRequest(requestId, action);
                flash(`Request ${action.toLowerCase()}d!`);
                fetchRequests();
            } catch (e) { flashError(e); }
            finally { setProcessingId(null); }
        };

        const formatDate = (dateStr) => {
            if (!dateStr) return '';
            return new Date(dateStr).toLocaleDateString('en-IN', {
                day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
            });
        };

        return (
            <div>
                <div className="ad-tab-header">
                    <h3>Role Change Requests ({requests.length})</h3>
                    <button className="ad-btn ad-btn-outline ad-btn-sm" onClick={fetchRequests}>↻ Refresh</button>
                </div>

                <div className="ad-filters">
                    {['ALL', 'PENDING', 'APPROVED', 'REJECTED'].map(f => (
                        <button key={f} className={`ad-filter ${filter === f ? 'active' : ''}`} onClick={() => setFilter(f)}>
                            {f === 'ALL' ? 'All' : f.charAt(0) + f.slice(1).toLowerCase()}
                            {f === 'PENDING' && pendingCount > 0 && ` (${pendingCount})`}
                        </button>
                    ))}
                </div>

                {filtered.length === 0 ? (
                    <div className="ad-empty-small"><p>No {filter !== 'ALL' ? filter.toLowerCase() : ''} requests.</p></div>
                ) : (
                    <div className="ad-request-list">
                        <AnimatePresence>
                            {filtered.map(req => {
                                const style = REQUEST_STATUS_STYLES[req.requestStatus] || REQUEST_STATUS_STYLES.PENDING;
                                return (
                                    <motion.div
                                        key={req.requestId}
                                        className="ad-request-card"
                                        initial={{ opacity: 0, y: 10 }}
                                        animate={{ opacity: 1, y: 0 }}
                                        exit={{ opacity: 0, y: -10 }}
                                        layout
                                    >
                                        <div className="ad-request-top">
                                            <div>
                                                <span className="ad-request-user">{req.userName}</span>
                                                <span className="ad-request-email">{req.userEmail}</span>
                                            </div>
                                            <span className="ad-request-role">{req.requestedRole?.replace('_', ' ')}</span>
                                        </div>

                                        {req.requestReason && (
                                            <div className="ad-request-reason">"{req.requestReason}"</div>
                                        )}

                                        <div className="ad-request-meta">
                                            Requested: {formatDate(req.requestedAt)}
                                            {req.respondedAt && <> · Responded: {formatDate(req.respondedAt)}</>}
                                            {req.adminName && <> · By: {req.adminName}</>}
                                        </div>

                                        <div className="ad-request-bottom">
                                            <span
                                                className="ad-request-status"
                                                style={{ background: style.bg, color: style.color }}
                                            >
                                                {req.requestStatus}
                                            </span>

                                            {req.requestStatus === 'PENDING' && (
                                                <div className="ad-request-actions">
                                                    <button
                                                        className="ad-btn ad-btn-sm ad-btn-approve"
                                                        disabled={processingId === req.requestId}
                                                        onClick={() => handleAction(req.requestId, 'approve')}
                                                    >
                                                        {processingId === req.requestId ? '...' : '✓ Approve'}
                                                    </button>
                                                    <button
                                                        className="ad-btn ad-btn-sm ad-btn-reject"
                                                        disabled={processingId === req.requestId}
                                                        onClick={() => handleAction(req.requestId, 'reject')}
                                                    >
                                                        {processingId === req.requestId ? '...' : '✗ Reject'}
                                                    </button>
                                                </div>
                                            )}
                                        </div>
                                    </motion.div>
                                );
                            })}
                        </AnimatePresence>
                    </div>
                )}
            </div>
        );
    };

    // ═══════════════════════════════════════
    //  RESTAURANTS TAB
    // ═══════════════════════════════════════
    const RestaurantsTab = () => {
        const parseImageLines = (value) => value
            .split(/\r?\n/)
            .map(line => line.trim())
            .filter(Boolean);

        const [filter, setFilter] = useState('ALL');
        const [editingId, setEditingId] = useState(null);
        const [editForm, setEditForm] = useState({});
        const [savingEdit, setSavingEdit] = useState(false);
        const [processingId, setProcessingId] = useState(null);
        const [expandedRestId, setExpandedRestId] = useState(null);

        const filtered = filter === 'ALL'
            ? restaurants
            : filter === 'OPEN'
                ? restaurants.filter(r => r.open)
                : filter === 'SUSPENDED'
                    ? restaurants.filter(r => r.restaurantStatus === 'SUSPENDED')
                    : restaurants.filter(r => !r.open);

        const handleToggleStatus = async (restaurant) => {
            try {
                await updateRestaurantStatus(restaurant.restaurantId, !restaurant.open);
                flash(`${restaurant.restaurantName} is now ${!restaurant.open ? 'Open' : 'Closed'}.`);
                fetchRestaurants();
            } catch (e) { flashError(e); }
        };

        const handleSuspend = async (restaurant) => {
            setProcessingId(restaurant.restaurantId);
            try {
                const msg = await suspendRestaurant(restaurant.restaurantId);
                flash(typeof msg === 'string' ? msg : 'Restaurant suspended.');
                setExpandedRestId(null);
                fetchRestaurants();
            } catch (e) { flashError(e); }
            finally { setProcessingId(null); }
        };

        const handleActivate = async (restaurant) => {
            setProcessingId(restaurant.restaurantId);
            try {
                const msg = await activateRestaurant(restaurant.restaurantId);
                flash(typeof msg === 'string' ? msg : 'Restaurant activated.');
                setExpandedRestId(null);
                fetchRestaurants();
            } catch (e) { flashError(e); }
            finally { setProcessingId(null); }
        };

        const startEdit = (r) => {
            setEditForm({
                restaurantName: r.restaurantName || '',
                restaurantDescription: r.restaurantDescription || '',
                cuisineType: r.cuisineType || '',
                openingTime: r.openingTime || '',
                closingTime: r.closingTime || '',
                images: r.images?.length ? r.images : [''],
                address: {
                    street: r.address?.street || '',
                    city: r.address?.city || '',
                    state: r.address?.state || '',
                    pincode: r.address?.pincode || '',
                    country: r.address?.country || '',
                },
                contactInformation: {
                    email: r.contactInformation?.email || '',
                    phone: r.contactInformation?.phone || '',
                },
            });
            setEditingId(r.restaurantId);
        };

        const handleSaveEdit = async (e) => {
            e.preventDefault();
            setSavingEdit(true);
            try {
                await updateRestaurant(editingId, editForm);
                flash('Restaurant updated.');
                setEditingId(null);
                fetchRestaurants();
            } catch (err) { flashError(err); }
            finally { setSavingEdit(false); }
        };

        const handleToggleExpand = (restaurantId) => {
            if (expandedRestId === restaurantId) {
                setExpandedRestId(null);
            } else {
                setExpandedRestId(restaurantId);
                setEditingId(null);
            }
        };

        return (
            <div>
                <div className="ad-tab-header">
                    <h3>All Restaurants ({restaurants.length})</h3>
                    <button className="ad-btn ad-btn-outline ad-btn-sm" onClick={fetchRestaurants}>↻ Refresh</button>
                </div>

                <div className="ad-filters">
                    {['ALL', 'OPEN', 'CLOSED', 'SUSPENDED'].map(f => (
                        <button key={f} className={`ad-filter ${filter === f ? 'active' : ''}`} onClick={() => setFilter(f)}>
                            {f === 'ALL' ? `All (${totalRestaurants})`
                                : f === 'OPEN' ? `Open (${openRestaurants})`
                                : f === 'SUSPENDED' ? `Suspended (${restaurants.filter(r => r.restaurantStatus === 'SUSPENDED').length})`
                                : `Closed (${totalRestaurants - openRestaurants})`}
                        </button>
                    ))}
                </div>

                {filtered.length === 0 ? (
                    <div className="ad-empty-small"><p>No restaurants found.</p></div>
                ) : (
                    <div className="ad-restaurant-list">
                        {filtered.map(r => {
                            const isExpanded = expandedRestId === r.restaurantId;
                            return (
                                <div key={r.restaurantId}>
                                    <div
                                        className="ad-restaurant-card"
                                        style={{ cursor: 'pointer' }}
                                        onClick={() => handleToggleExpand(r.restaurantId)}
                                    >
                                        <div className="ad-restaurant-info">
                                            <div className="ad-restaurant-name">
                                                {r.restaurantName}
                                                <span style={{ fontSize: '0.8rem', fontWeight: 400, color: '#6B7280' }}>#{r.restaurantId}</span>
                                                {r.restaurantStatus === 'SUSPENDED' && (
                                                    <span style={{ fontSize: '0.72rem', fontWeight: 600, color: '#DC2626', background: '#FEE2E2', padding: '0.15rem 0.5rem', borderRadius: '100px', marginLeft: '0.3rem' }}>
                                                        SUSPENDED
                                                    </span>
                                                )}
                                            </div>
                                            <span className="ad-restaurant-sub">
                                                {r.cuisineType} · {r.address?.city || 'N/A'}
                                                {r.openingTime && ` · ${r.openingTime} – ${r.closingTime}`}
                                            </span>
                                        </div>
                                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                                            <span className={`ad-status-toggle ${r.open ? 'open' : 'closed'}`} style={{ pointerEvents: 'none' }}>
                                                {r.open ? '● Open' : '● Closed'}
                                            </span>
                                            <span style={{ fontSize: '0.85rem', color: '#6B7280' }}>{isExpanded ? '▲' : '▼'}</span>
                                        </div>
                                    </div>

                                    {/* Expanded detail panel */}
                                    {isExpanded && (
                                        <motion.div
                                            initial={{ opacity: 0, height: 0 }}
                                            animate={{ opacity: 1, height: 'auto' }}
                                            style={{
                                                background: 'var(--surface, #fff)', border: '1px solid var(--border, #E5E7EB)',
                                                borderRadius: '0 0 14px 14px', borderTop: 'none', padding: '1.15rem 1.25rem',
                                                marginTop: '-0.35rem', marginBottom: '0.65rem',
                                                display: 'flex', flexDirection: 'column', gap: '0.6rem'
                                            }}
                                            onClick={e => e.stopPropagation()}
                                        >
                                            {r.owner && (
                                                <span className="ad-restaurant-owner">
                                                    👤 Owner: {r.owner.firstName || ''} {r.owner.lastName || ''} ({r.owner.email || ''})
                                                </span>
                                            )}
                                            {r.restaurantDescription && (
                                                <span style={{ fontSize: '0.85rem', color: '#6B7280' }}>{r.restaurantDescription}</span>
                                            )}

                                            {/* Action buttons */}
                                            <div style={{ display: 'flex', gap: '0.5rem', flexWrap: 'wrap', paddingTop: '0.5rem', borderTop: '1px solid var(--border, #E5E7EB)' }}>
                                                <button
                                                    className={`ad-status-toggle ${r.open ? 'open' : 'closed'}`}
                                                    onClick={() => handleToggleStatus(r)}
                                                >
                                                    {r.open ? '● Close' : '● Open'}
                                                </button>
                                                {r.restaurantStatus === 'SUSPENDED' ? (
                                                    <button
                                                        className="ad-btn ad-btn-sm ad-btn-approve"
                                                        disabled={processingId === r.restaurantId}
                                                        onClick={() => handleActivate(r)}
                                                    >
                                                        ✓ Activate
                                                    </button>
                                                ) : (
                                                    <button
                                                        className="ad-btn ad-btn-sm ad-btn-reject"
                                                        disabled={processingId === r.restaurantId}
                                                        onClick={() => handleSuspend(r)}
                                                    >
                                                        ⛔ Suspend
                                                    </button>
                                                )}
                                                <button
                                                    className="ad-btn ad-btn-sm ad-btn-outline"
                                                    onClick={() => startEdit(r)}
                                                >
                                                    ✏️ Edit
                                                </button>
                                                <button
                                                    className="ad-btn ad-btn-sm ad-btn-outline"
                                                    onClick={() => {
                                                        setSelectedRestaurantId(r.restaurantId);
                                                        setOrderMode('restaurant');
                                                        setActiveTab('orders');
                                                    }}
                                                >
                                                    📦 Orders
                                                </button>
                                            </div>

                                            {/* Inline Edit Form */}
                                            {editingId === r.restaurantId && (
                                                <form className="ad-edit-form" onSubmit={handleSaveEdit} style={{
                                                    display: 'flex', flexDirection: 'column', gap: '0.6rem', marginTop: '0.25rem',
                                                    paddingTop: '0.75rem', borderTop: '1px dashed var(--border, #E5E7EB)'
                                                }}>
                                                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.6rem' }}>
                                                        <input placeholder="Restaurant Name" value={editForm.restaurantName}
                                                            onChange={e => setEditForm({ ...editForm, restaurantName: e.target.value })} required
                                                            style={{ padding: '0.5rem 0.8rem', border: '1.5px solid var(--border)', borderRadius: '8px', fontFamily: 'DM Sans', fontSize: '0.9rem', outline: 'none' }} />
                                                        <select value={editForm.cuisineType} onChange={e => setEditForm({ ...editForm, cuisineType: e.target.value })}
                                                            style={{ padding: '0.5rem 0.8rem', border: '1.5px solid var(--border)', borderRadius: '8px', fontFamily: 'DM Sans', fontSize: '0.9rem', outline: 'none' }}>
                                                            {['INDIAN','CHINESE','ITALIAN','MEXICAN','CONTINENTAL','AMERICAN'].map(c => (
                                                                <option key={c} value={c}>{c}</option>
                                                            ))}
                                                        </select>
                                                    </div>
                                                    <input placeholder="Description" value={editForm.restaurantDescription}
                                                        onChange={e => setEditForm({ ...editForm, restaurantDescription: e.target.value })}
                                                        style={{ padding: '0.5rem 0.8rem', border: '1.5px solid var(--border)', borderRadius: '8px', fontFamily: 'DM Sans', fontSize: '0.9rem', outline: 'none' }} />
                                                    <textarea
                                                        placeholder="Image URLs (one per line)"
                                                        value={(editForm.images || []).join('\n')}
                                                        onChange={e => setEditForm({ ...editForm, images: parseImageLines(e.target.value) })}
                                                        style={{ padding: '0.5rem 0.8rem', border: '1.5px solid var(--border)', borderRadius: '8px', fontFamily: 'DM Sans', fontSize: '0.9rem', outline: 'none', minHeight: '4.5rem' }}
                                                    />
                                                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.6rem' }}>
                                                        <input type="time" value={editForm.openingTime} onChange={e => setEditForm({ ...editForm, openingTime: e.target.value })}
                                                            style={{ padding: '0.5rem 0.8rem', border: '1.5px solid var(--border)', borderRadius: '8px', fontFamily: 'DM Sans', fontSize: '0.9rem', outline: 'none' }} />
                                                        <input type="time" value={editForm.closingTime} onChange={e => setEditForm({ ...editForm, closingTime: e.target.value })}
                                                            style={{ padding: '0.5rem 0.8rem', border: '1.5px solid var(--border)', borderRadius: '8px', fontFamily: 'DM Sans', fontSize: '0.9rem', outline: 'none' }} />
                                                    </div>
                                                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '0.6rem' }}>
                                                        <input placeholder="City" value={editForm.address?.city || ''}
                                                            onChange={e => setEditForm({ ...editForm, address: { ...editForm.address, city: e.target.value } })}
                                                            style={{ padding: '0.5rem 0.8rem', border: '1.5px solid var(--border)', borderRadius: '8px', fontFamily: 'DM Sans', fontSize: '0.9rem', outline: 'none' }} />
                                                        <input placeholder="Street" value={editForm.address?.street || ''}
                                                            onChange={e => setEditForm({ ...editForm, address: { ...editForm.address, street: e.target.value } })}
                                                            style={{ padding: '0.5rem 0.8rem', border: '1.5px solid var(--border)', borderRadius: '8px', fontFamily: 'DM Sans', fontSize: '0.9rem', outline: 'none' }} />
                                                        <input placeholder="Pincode" value={editForm.address?.pincode || ''}
                                                            onChange={e => setEditForm({ ...editForm, address: { ...editForm.address, pincode: e.target.value } })}
                                                            style={{ padding: '0.5rem 0.8rem', border: '1.5px solid var(--border)', borderRadius: '8px', fontFamily: 'DM Sans', fontSize: '0.9rem', outline: 'none' }} />
                                                    </div>
                                                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.6rem' }}>
                                                        <input placeholder="Contact Email" value={editForm.contactInformation?.email || ''}
                                                            onChange={e => setEditForm({ ...editForm, contactInformation: { ...editForm.contactInformation, email: e.target.value } })}
                                                            style={{ padding: '0.5rem 0.8rem', border: '1.5px solid var(--border)', borderRadius: '8px', fontFamily: 'DM Sans', fontSize: '0.9rem', outline: 'none' }} />
                                                        <input placeholder="Contact Phone" value={editForm.contactInformation?.phone || ''}
                                                            onChange={e => setEditForm({ ...editForm, contactInformation: { ...editForm.contactInformation, phone: e.target.value } })}
                                                            style={{ padding: '0.5rem 0.8rem', border: '1.5px solid var(--border)', borderRadius: '8px', fontFamily: 'DM Sans', fontSize: '0.9rem', outline: 'none' }} />
                                                    </div>
                                                    <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.25rem' }}>
                                                        <button type="submit" className="ad-btn ad-btn-sm ad-btn-primary" disabled={savingEdit}>
                                                            {savingEdit ? 'Saving...' : '✓ Save'}
                                                        </button>
                                                        <button type="button" className="ad-btn ad-btn-sm ad-btn-outline" onClick={() => setEditingId(null)}>Cancel</button>
                                                    </div>
                                                </form>
                                            )}
                                        </motion.div>
                                    )}
                                </div>
                            );
                        })}
                    </div>
                )}
            </div>
        );
    };

    // ═══════════════════════════════════════
    //  ORDERS TAB
    // ═══════════════════════════════════════
    const OrdersTab = () => {
        const [orderFilter, setOrderFilter] = useState('ALL');

        const sourceOrders = orderMode === 'all' ? allOrdersList : orders;
        const filteredOrders = orderFilter === 'ALL'
            ? sourceOrders
            : sourceOrders.filter(o => o.orderStatus === orderFilter);

        const formatDate = (dateStr) => {
            if (!dateStr) return '';
            return new Date(dateStr).toLocaleDateString('en-IN', {
                day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit'
            });
        };

        const handleRefresh = () => {
            setLoadingOrders(true);
            if (orderMode === 'all') {
                fetchAllOrders().finally(() => setLoadingOrders(false));
            } else if (selectedRestaurantId) {
                fetchOrders(selectedRestaurantId).finally(() => setLoadingOrders(false));
            }
        };

        return (
            <div>
                <div className="ad-tab-header">
                    <h3>{orderMode === 'all' ? 'All Platform Orders' : 'Restaurant Orders'}</h3>
                    <button className="ad-btn ad-btn-outline ad-btn-sm" onClick={handleRefresh}>↻ Refresh</button>
                </div>

                {/* Mode toggle */}
                <div className="ad-filters" style={{ marginBottom: '0.75rem' }}>
                    <button
                        className={`ad-filter ${orderMode === 'all' ? 'active' : ''}`}
                        onClick={() => { setOrderMode('all'); setOrderFilter('ALL'); }}
                    >
                        🌐 All Orders
                    </button>
                    <button
                        className={`ad-filter ${orderMode === 'restaurant' ? 'active' : ''}`}
                        onClick={() => { setOrderMode('restaurant'); setOrderFilter('ALL'); }}
                    >
                        🏪 By Restaurant
                    </button>
                </div>

                {/* Restaurant selector (only in restaurant mode) */}
                {orderMode === 'restaurant' && restaurants.length > 0 && (
                    <div className="ad-restaurant-select">
                        <label>Restaurant:</label>
                        <select
                            value={selectedRestaurantId || ''}
                            onChange={e => {
                                const id = parseInt(e.target.value);
                                setSelectedRestaurantId(id);
                                setOrderFilter('ALL');
                            }}
                        >
                            {restaurants.map(r => (
                                <option key={r.restaurantId} value={r.restaurantId}>
                                    {r.restaurantName} (#{r.restaurantId})
                                </option>
                            ))}
                        </select>
                    </div>
                )}

                {/* Status filters */}
                <div className="ad-filters">
                    {['ALL', 'CREATED', 'CONFIRMED', 'PREPARING', 'OUT_FOR_DELIVERY', 'DELIVERED', 'CANCELLED'].map(f => (
                        <button key={f} className={`ad-filter ${orderFilter === f ? 'active' : ''}`} onClick={() => setOrderFilter(f)}>
                            {f === 'ALL' ? 'All' : ORDER_STATUS_LABELS[f]}
                        </button>
                    ))}
                </div>

                {loadingOrders ? (
                    <div className="ad-empty-small"><p>Loading orders...</p></div>
                ) : (orderMode === 'restaurant' && !selectedRestaurantId) ? (
                    <div className="ad-empty-small"><p>Select a restaurant to view orders.</p></div>
                ) : filteredOrders.length === 0 ? (
                    <div className="ad-empty-small"><p>No {orderFilter !== 'ALL' ? ORDER_STATUS_LABELS[orderFilter]?.toLowerCase() : ''} orders{orderMode === 'restaurant' ? ' for this restaurant' : ''}.</p></div>
                ) : (
                    <div className="ad-request-list">
                        {filteredOrders.map(order => {
                            const style = ORDER_STATUS_STYLES[order.orderStatus] || ORDER_STATUS_STYLES.CREATED;
                            return (
                                <div key={order.orderId} className="ad-order-card">
                                    <div className="ad-order-top">
                                        <div>
                                            <span className="ad-order-id">Order #{order.orderId}</span>
                                            <span className="ad-order-date">{formatDate(order.createdAt)}</span>
                                        </div>
                                        <span className="ad-order-badge" style={{ background: style.bg, color: style.color }}>
                                            {ORDER_STATUS_LABELS[order.orderStatus]}
                                        </span>
                                    </div>

                                    {orderMode === 'all' && order.restaurant && (
                                        <div className="ad-order-customer">
                                            🏪 {order.restaurant.restaurantName || 'Restaurant'}
                                        </div>
                                    )}

                                    <div className="ad-order-customer">
                                        👤 {order.user?.firstName || order.user?.name || 'Customer'}
                                    </div>

                                    <div className="ad-order-items">
                                        {order.orderItems?.map(item => (
                                            <span key={item.orderItemId} className="ad-order-item-chip">
                                                {item.foodName} × {item.quantity}
                                            </span>
                                        ))}
                                    </div>

                                    <div className="ad-order-bottom">
                                        <span className="ad-order-total">₹{Number(order.totalPrice).toFixed(0)}</span>
                                        <span className="ad-order-badge" style={{ background: style.bg, color: style.color }}>
                                            {ORDER_STATUS_LABELS[order.orderStatus]}
                                        </span>
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                )}
            </div>
        );
    };

    // ═══════════════════════════════════════
    //  RENDER
    // ═══════════════════════════════════════
    if (loading) {
        return (
            <div className="ad-page">
                <div className="ad-loading">
                    <motion.div animate={{ rotate: 360 }} transition={{ duration: 1.5, repeat: Infinity, ease: 'linear' }} className="ad-loading-icon">🛡️</motion.div>
                    <p>Loading admin panel...</p>
                </div>
            </div>
        );
    }

    return (
        <div className="ad-page">
            <div className="ad-container">
                <div className="ad-page-header">
                    <h1 className="ad-page-title">Admin Dashboard</h1>
                    <span className="ad-page-sub">Platform management</span>
                </div>

                <div className="ad-greeting"><em>{greeting}</em></div>

                {error && <div className="ad-toast ad-toast-error">{error}</div>}
                {success && <div className="ad-toast ad-toast-success">{success}</div>}

                {/* Summary Cards — contextual to active tab */}
                <div className="ad-summary-row">
                    {activeTab === 'users' && (
                        <>
                            <div className="ad-summary-card">
                                <div className="ad-summary-num">{totalUsers}</div>
                                <div className="ad-summary-label">Total Users</div>
                            </div>
                            <div className="ad-summary-card">
                                <div className="ad-summary-num">{activeUsers}</div>
                                <div className="ad-summary-label">Active</div>
                            </div>
                            <div className="ad-summary-card">
                                <div className="ad-summary-num">{users.filter(u => u.status === 'RESTRICTED').length}</div>
                                <div className="ad-summary-label">Restricted</div>
                            </div>
                            <div className="ad-summary-card">
                                <div className="ad-summary-num">{users.filter(u => u.status === 'BLOCKED').length}</div>
                                <div className="ad-summary-label">Blocked</div>
                            </div>
                        </>
                    )}
                    {activeTab === 'requests' && (
                        <>
                            <div className="ad-summary-card">
                                <div className="ad-summary-num">{requests.length}</div>
                                <div className="ad-summary-label">Total Requests</div>
                            </div>
                            <div className="ad-summary-card">
                                <div className="ad-summary-num">{pendingCount}</div>
                                <div className="ad-summary-label">Pending</div>
                            </div>
                            <div className="ad-summary-card">
                                <div className="ad-summary-num">{requests.filter(r => r.requestStatus === 'APPROVED').length}</div>
                                <div className="ad-summary-label">Approved</div>
                            </div>
                            <div className="ad-summary-card">
                                <div className="ad-summary-num">{requests.filter(r => r.requestStatus === 'REJECTED').length}</div>
                                <div className="ad-summary-label">Rejected</div>
                            </div>
                        </>
                    )}
                    {activeTab === 'restaurants' && (
                        <>
                            <div className="ad-summary-card">
                                <div className="ad-summary-num">{totalRestaurants}</div>
                                <div className="ad-summary-label">Total Restaurants</div>
                            </div>
                            <div className="ad-summary-card">
                                <div className="ad-summary-num">{openRestaurants}</div>
                                <div className="ad-summary-label">Open Now</div>
                            </div>
                            <div className="ad-summary-card">
                                <div className="ad-summary-num">{totalRestaurants - openRestaurants}</div>
                                <div className="ad-summary-label">Closed</div>
                            </div>
                            <div className="ad-summary-card">
                                <div className="ad-summary-num">{restaurants.filter(r => r.restaurantStatus === 'SUSPENDED').length}</div>
                                <div className="ad-summary-label">Suspended</div>
                            </div>
                        </>
                    )}
                    {activeTab === 'orders' && (
                        <>
                            <div className="ad-summary-card">
                                <div className="ad-summary-num">{allOrdersList.length}</div>
                                <div className="ad-summary-label">Total Orders</div>
                            </div>
                            <div className="ad-summary-card">
                                <div className="ad-summary-num">{allOrdersList.filter(o => o.orderStatus === 'DELIVERED').length}</div>
                                <div className="ad-summary-label">Delivered</div>
                            </div>
                            <div className="ad-summary-card">
                                <div className="ad-summary-num">{allOrdersList.filter(o => ['CREATED','CONFIRMED','PREPARING','OUT_FOR_DELIVERY'].includes(o.orderStatus)).length}</div>
                                <div className="ad-summary-label">In Progress</div>
                            </div>
                            <div className="ad-summary-card">
                                <div className="ad-summary-num">{allOrdersList.filter(o => o.orderStatus === 'CANCELLED').length}</div>
                                <div className="ad-summary-label">Cancelled</div>
                            </div>
                        </>
                    )}
                </div>

                {/* Tabs */}
                <div className="ad-tabs">
                    {TABS.map(tab => (
                        <button
                            key={tab}
                            className={`ad-tab ${activeTab === tab ? 'active' : ''}`}
                            onClick={() => setActiveTab(tab)}
                        >
                            {TAB_LABELS[tab]}
                        </button>
                    ))}
                </div>

                {/* Tab Content */}
                <div className="ad-tab-content">
                    {activeTab === 'users' && <UsersTab />}
                    {activeTab === 'requests' && <RequestsTab />}
                    {activeTab === 'restaurants' && <RestaurantsTab />}
                    {activeTab === 'orders' && <OrdersTab />}
                </div>
            </div>
        </div>
    );
};

export default AdminDashboard;

