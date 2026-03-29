
import React, { useState, useEffect, useContext } from 'react';
import { Link } from 'react-router-dom';
import { AuthContext } from '../../context/AuthContext';
import {
    getUserProfile,
    getAddresses, addAddress, updateAddress, setDefaultAddress, deleteAddress,
    getFavourites, removeFavourite, getMyMessSubscriptions,
} from '../../api/userService';
import { getMyOrders } from '../../api/orderService';
import { getMyReviews } from '../../api/reviewService';
import { motion } from 'framer-motion';
import './CustomerDashboard.css';

const cuisineEmoji = {
    INDIAN: '🍛', CHINESE: '🥡', ITALIAN: '🍕',
    MEXICAN: '🌮', CONTINENTAL: '🍽️', AMERICAN: '🍔',
};

const STATUS_STYLES = {
    CREATED:          { bg: '#DBEAFE', color: '#1D4ED8', label: 'Created' },
    CONFIRMED:        { bg: '#DCFCE7', color: '#16A34A', label: 'Confirmed' },
    PREPARING:        { bg: '#FEF3C7', color: '#B45309', label: 'Preparing' },
    OUT_FOR_DELIVERY: { bg: '#FFF7ED', color: '#C2410C', label: 'Out for Delivery' },
    DELIVERED:        { bg: '#DCFCE7', color: '#15803D', label: 'Delivered' },
    CANCELLED:        { bg: '#FEE2E2', color: '#DC2626', label: 'Cancelled' },
};

const TABS = ['overview', 'favourites', 'addresses', 'messPlans', 'reviews'];
const TAB_LABELS = {
    overview: '📊 Overview',
    favourites: '❤️ Favourites',
    addresses: '📍 Addresses',
    messPlans: '🍱 My Mess Plans',
    reviews: '📝 Reviews',
};

const sortReviewsByRecent = (reviews) => [...reviews].sort(
    (a, b) => new Date(b?.postedAt || 0).getTime() - new Date(a?.postedAt || 0).getTime()
);

const normalizeSubscriptionsPayload = (payload) => {
    if (Array.isArray(payload)) return payload;
    if (Array.isArray(payload?.content)) return payload.content;
    if (Array.isArray(payload?.data)) return payload.data;
    if (Array.isArray(payload?.subscriptions)) return payload.subscriptions;
    return [];
};

const CustomerDashboard = () => {
    const { user } = useContext(AuthContext);

    const [activeTab, setActiveTab] = useState('overview');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');

    const [profile, setProfile] = useState(null);
    const [orders, setOrders] = useState([]);
    const [favourites, setFavourites] = useState([]);
    const [addresses, setAddresses] = useState([]);
    const [messSubscriptions, setMessSubscriptions] = useState([]);
    const [messFilter, setMessFilter] = useState('all');
    const [loadingMess, setLoadingMess] = useState(false);
    const [reviews, setReviews] = useState([]);
    const [loadingReviews, setLoadingReviews] = useState(false);

    useEffect(() => { fetchInitial(); }, []);

    useEffect(() => {
        if (activeTab === 'favourites') fetchFavourites();
        if (activeTab === 'addresses') fetchAddresses();
        if (activeTab === 'messPlans') fetchMessSubscriptions();
        if (activeTab === 'reviews') fetchReviews();
    }, [activeTab]);

    useEffect(() => {
        if (activeTab === 'messPlans') fetchMessSubscriptions();
    }, [messFilter]);

    const flash = (msg) => { setSuccess(msg); setTimeout(() => setSuccess(''), 2500); };
    const flashError = (msg) => { setError(msg); setTimeout(() => setError(''), 4000); };

    const fetchInitial = async () => {
        setLoading(true);
        try {
            const [profileData, ordersData, favsData, addrsData] = await Promise.all([
                getUserProfile(), getMyOrders(), getFavourites(), getAddresses()
            ]);
            setProfile(profileData);
            setOrders(ordersData);
            setFavourites(favsData);
            setAddresses(addrsData);
        } catch { flashError('Failed to load dashboard data.'); }
        finally { setLoading(false); }
    };

    const fetchFavourites = async () => {
        try { setFavourites(await getFavourites()); } catch {}
    };

    const fetchAddresses = async () => {
        try { setAddresses(await getAddresses()); } catch {}
    };

    const fetchMessSubscriptions = async () => {
        setLoadingMess(true);
        try {
            const active = messFilter === 'all' ? undefined : messFilter === 'active';
            const payload = await getMyMessSubscriptions(active);
            setMessSubscriptions(normalizeSubscriptionsPayload(payload));
        } catch {
            flashError('Failed to load mess subscriptions.');
        } finally {
            setLoadingMess(false);
        }
    };

    const fetchReviews = async () => {
        setLoadingReviews(true);
        try {
            const payload = await getMyReviews();
            setReviews(sortReviewsByRecent(payload));
        } catch {
            flashError('Failed to load your reviews.');
        } finally {
            setLoadingReviews(false);
        }
    };

    const handleRemoveFavourite = async (restaurantId) => {
        try {
            await removeFavourite(restaurantId);
            setFavourites(prev => prev.filter(f => f.restaurantId !== restaurantId));
            flash('Removed from favourites.');
        } catch { flashError('Failed to remove.'); }
    };

    // ═══════════════════════════════════════
    //  OVERVIEW TAB
    // ═══════════════════════════════════════
    const OverviewTab = () => {
        const recentOrders = orders.slice(0, 5);
        const activeOrders = orders.filter(o => !['DELIVERED', 'CANCELLED'].includes(o.orderStatus));

        const formatDate = (dateStr) => {
            if (!dateStr) return '';
            return new Date(dateStr).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' });
        };

        return (
            <div>
                {/* Summary */}
                <div className="cd-summary-row">
                    <div className="cd-summary-card">
                        <div className="cd-summary-num">{orders.length}</div>
                        <div className="cd-summary-label">Total Orders</div>
                    </div>
                    <div className="cd-summary-card">
                        <div className="cd-summary-num">{activeOrders.length}</div>
                        <div className="cd-summary-label">Active Orders</div>
                    </div>
                    <div className="cd-summary-card">
                        <div className="cd-summary-num">{favourites.length}</div>
                        <div className="cd-summary-label">Favourites</div>
                    </div>
                    <div className="cd-summary-card">
                        <div className="cd-summary-num">{addresses.length}</div>
                        <div className="cd-summary-label">Addresses</div>
                    </div>
                    <div className="cd-summary-card">
                        <div className="cd-summary-num">{messSubscriptions.length}</div>
                        <div className="cd-summary-label">Mess Plans</div>
                    </div>
                    <div className="cd-summary-card">
                        <div className="cd-summary-num">{reviews.length}</div>
                        <div className="cd-summary-label">Reviews</div>
                    </div>
                </div>

                {/* Recent Orders */}
                <div className="cd-section-header">
                    <h3>Recent Orders</h3>
                    <Link to="/orders" className="cd-btn cd-btn-outline cd-btn-sm">View all →</Link>
                </div>

                {recentOrders.length === 0 ? (
                    <div className="cd-empty">
                        <span className="cd-empty-icon">📦</span>
                        <h3>No orders yet</h3>
                        <p>Your orders will show up here.</p>
                    </div>
                ) : (
                    recentOrders.map(order => {
                        const st = STATUS_STYLES[order.orderStatus] || STATUS_STYLES.CREATED;
                        return (
                            <Link to={`/orders/${order.orderId}`} key={order.orderId} className="cd-order-card">
                                <div className="cd-order-info">
                                    <div className="cd-order-name">🍽️ {order.restaurant?.restaurantName || 'Restaurant'}</div>
                                    <div className="cd-order-sub">#{order.orderId} · {formatDate(order.createdAt)}</div>
                                </div>
                                <span className="cd-order-badge" style={{ background: st.bg, color: st.color }}>{st.label}</span>
                                <span className="cd-order-total">₹{Number(order.totalPrice).toFixed(0)}</span>
                            </Link>
                        );
                    })
                )}
            </div>
        );
    };

    // ═══════════════════════════════════════
    //  MESS PLANS TAB
    // ═══════════════════════════════════════
    const MessPlansTab = () => {
        const formatDate = (dateStr) => {
            if (!dateStr) return '-';
            const dt = new Date(dateStr);
            if (Number.isNaN(dt.getTime())) return dateStr;
            return dt.toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
        };

        return (
            <div>
                <div className="cd-section-header">
                    <h3>My Subscriptions ({messSubscriptions.length})</h3>
                    <div className="cd-mess-filter-wrap">
                        <button
                            className={`cd-chip-btn ${messFilter === 'all' ? 'active' : ''}`}
                            onClick={() => setMessFilter('all')}
                        >
                            All
                        </button>
                        <button
                            className={`cd-chip-btn ${messFilter === 'active' ? 'active' : ''}`}
                            onClick={() => setMessFilter('active')}
                        >
                            Active
                        </button>
                        <button
                            className={`cd-chip-btn ${messFilter === 'inactive' ? 'active' : ''}`}
                            onClick={() => setMessFilter('inactive')}
                        >
                            Inactive
                        </button>
                        <button className="cd-btn cd-btn-outline cd-btn-sm" onClick={fetchMessSubscriptions}>↻ Refresh</button>
                    </div>
                </div>

                {loadingMess ? (
                    <div className="cd-empty"><p>Loading subscriptions...</p></div>
                ) : messSubscriptions.length === 0 ? (
                    <div className="cd-empty">
                        <span className="cd-empty-icon">🍱</span>
                        <h3>No subscriptions found</h3>
                        <p>Subscribe to a restaurant mess plan to see it here.</p>
                        <Link to="/" className="cd-btn cd-btn-primary" style={{ marginTop: '0.75rem' }}>Explore Restaurants</Link>
                    </div>
                ) : (
                    <div className="cd-mess-grid">
                        {messSubscriptions.map((sub) => (
                            <div key={sub.subscriptionId} className="cd-mess-card">
                                <div className="cd-mess-head">
                                    <h4 className="cd-mess-name">{sub.messPlan?.name || 'Mess Plan'}</h4>
                                    <span className={`cd-mess-badge ${sub.active ? 'active' : 'inactive'}`}>
                                        {sub.active ? 'Active' : 'Inactive'}
                                    </span>
                                </div>
                                {sub.messPlan?.description && <p className="cd-mess-desc">{sub.messPlan.description}</p>}
                                <div className="cd-mess-meta-row">
                                    <span>Price: ₹{Number(sub.messPlan?.price || 0).toFixed(0)}</span>
                                </div>
                                <div className="cd-mess-meta-row">
                                    <span>Start: {formatDate(sub.startDate)}</span>
                                    <span>End: {formatDate(sub.endDate)}</span>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        );
    };

    const ReviewsTab = () => {
        const formatDate = (dateStr) => {
            if (!dateStr) return '-';
            const dt = new Date(dateStr);
            if (Number.isNaN(dt.getTime())) return dateStr;
            return dt.toLocaleDateString('en-IN', {
                day: 'numeric',
                month: 'short',
                year: 'numeric',
                hour: '2-digit',
                minute: '2-digit',
            });
        };

        return (
            <div>
                <div className="cd-section-header">
                    <h3>My Reviews ({reviews.length})</h3>
                    <button className="cd-btn cd-btn-outline cd-btn-sm" onClick={fetchReviews}>↻ Refresh</button>
                </div>

                {loadingReviews ? (
                    <div className="cd-empty"><p>Loading reviews...</p></div>
                ) : reviews.length === 0 ? (
                    <div className="cd-empty">
                        <span className="cd-empty-icon">📝</span>
                        <h3>No reviews posted yet</h3>
                        <p>Rate restaurants after your orders to see them here.</p>
                    </div>
                ) : (
                    <div className="cd-review-list">
                        {reviews.map((review) => (
                            <div key={review.reviewId} className="cd-review-card">
                                <div className="cd-review-top">
                                    <h4>{review.restaurant?.restaurantName || 'Restaurant'}</h4>
                                    <span>{formatDate(review.postedAt)}</span>
                                </div>
                                <div className="cd-review-rating">{'★'.repeat(Math.max(0, Math.min(5, review.rating || 0)))} <span>({review.rating}/5)</span></div>
                                {review.comment && <p className="cd-review-comment">{review.comment}</p>}
                            </div>
                        ))}
                    </div>
                )}
            </div>
        );
    };

    // ═══════════════════════════════════════
    //  FAVOURITES TAB
    // ═══════════════════════════════════════
    const FavouritesTab = () => (
        <div>
            <div className="cd-section-header">
                <h3>Favourite Restaurants ({favourites.length})</h3>
                <button className="cd-btn cd-btn-outline cd-btn-sm" onClick={fetchFavourites}>↻ Refresh</button>
            </div>
            {favourites.length === 0 ? (
                <div className="cd-empty">
                    <span className="cd-empty-icon">💛</span>
                    <h3>No favourites yet</h3>
                    <p>Heart a restaurant to save it here.</p>
                    <Link to="/" className="cd-btn cd-btn-primary" style={{ marginTop: '0.75rem' }}>Browse Restaurants</Link>
                </div>
            ) : (
                <div className="cd-fav-grid">
                    {favourites.map(fav => (
                        <Link to={`/restaurant/${fav.restaurantId}`} key={fav.restaurantId} className="cd-fav-card">
                            <span className="cd-fav-emoji">{cuisineEmoji[fav.cuisineType] || '🍽️'}</span>
                            <div className="cd-fav-info">
                                <div className="cd-fav-name">{fav.restaurantName}</div>
                                <div className="cd-fav-meta">
                                    {fav.avgRating && <span>★ {Number(fav.avgRating).toFixed(1)}</span>}
                                    <span>{fav.cuisineType?.charAt(0) + fav.cuisineType?.slice(1).toLowerCase()}</span>
                                </div>
                            </div>
                            <button
                                className="cd-fav-remove"
                                onClick={(e) => { e.preventDefault(); e.stopPropagation(); handleRemoveFavourite(fav.restaurantId); }}
                                title="Remove"
                            >✕</button>
                        </Link>
                    ))}
                </div>
            )}
        </div>
    );

    // ═══════════════════════════════════════
    //  ADDRESSES TAB
    // ═══════════════════════════════════════
    const AddressesTab = () => {
        const [showForm, setShowForm] = useState(false);
        const [editId, setEditId] = useState(null);
        const [form, setForm] = useState({ buildingNo: '', street: '', city: '', state: '', pincode: '', landmark: '' });
        const [saving, setSaving] = useState(false);

        const resetForm = () => {
            setForm({ buildingNo: '', street: '', city: '', state: '', pincode: '', landmark: '' });
            setEditId(null);
            setShowForm(false);
        };

        const startEdit = (addr) => {
            setForm({
                buildingNo: addr.buildingNo || '',
                street: addr.street || '',
                city: addr.city || '',
                state: addr.state || '',
                pincode: addr.pincode || '',
                landmark: addr.landmark || '',
            });
            setEditId(addr.addressId);
            setShowForm(true);
        };

        const handleSubmit = async (e) => {
            e.preventDefault();
            setSaving(true);
            try {
                if (editId) {
                    await updateAddress(editId, form);
                    flash('Address updated.');
                } else {
                    await addAddress(form);
                    flash('Address added.');
                }
                fetchAddresses();
                resetForm();
            } catch (err) {
                flashError(err.response?.data?.message || 'Failed to save address.');
            } finally { setSaving(false); }
        };

        const handleDelete = async (id) => {
            try {
                await deleteAddress(id);
                flash('Address deleted.');
                fetchAddresses();
            } catch { flashError('Failed to delete address.'); }
        };

        const handleSetDefault = async (id) => {
            try {
                await setDefaultAddress(id);
                flash('Default address updated.');
                fetchAddresses();
            } catch { flashError('Failed to set default.'); }
        };

        return (
            <div>
                <div className="cd-section-header">
                    <h3>My Addresses ({addresses.length})</h3>
                    {!showForm && (
                        <button className="cd-btn cd-btn-primary cd-btn-sm" onClick={() => { resetForm(); setShowForm(true); }}>
                            + Add Address
                        </button>
                    )}
                </div>

                {showForm && (
                    <form className="cd-form" onSubmit={handleSubmit}>
                        <div className="cd-form-row">
                            <input placeholder="Building No." value={form.buildingNo} onChange={e => setForm({ ...form, buildingNo: e.target.value })} required />
                            <input placeholder="Street" value={form.street} onChange={e => setForm({ ...form, street: e.target.value })} required />
                        </div>
                        <div className="cd-form-row">
                            <input placeholder="City" value={form.city} onChange={e => setForm({ ...form, city: e.target.value })} required />
                            <input placeholder="State" value={form.state} onChange={e => setForm({ ...form, state: e.target.value })} required />
                        </div>
                        <div className="cd-form-row">
                            <input placeholder="Pincode" value={form.pincode} onChange={e => setForm({ ...form, pincode: e.target.value })} required />
                            <input placeholder="Landmark (optional)" value={form.landmark} onChange={e => setForm({ ...form, landmark: e.target.value })} />
                        </div>
                        <div className="cd-form-actions">
                            <button type="submit" className="cd-btn cd-btn-primary" disabled={saving}>
                                {saving ? 'Saving...' : editId ? 'Update' : 'Save'}
                            </button>
                            <button type="button" className="cd-btn cd-btn-outline" onClick={resetForm}>Cancel</button>
                        </div>
                    </form>
                )}

                {addresses.length === 0 && !showForm ? (
                    <div className="cd-empty">
                        <span className="cd-empty-icon">📍</span>
                        <h3>No addresses</h3>
                        <p>Add your first delivery address.</p>
                    </div>
                ) : (
                    <div className="cd-addr-list">
                        {addresses.map(addr => (
                            <div key={addr.addressId} className="cd-addr-card">
                                <div className="cd-addr-info">
                                    <div className="cd-addr-line">{addr.buildingNo}, {addr.street}</div>
                                    <span className="cd-addr-sub">{addr.city}, {addr.state} – {addr.pincode}</span>
                                    {addr.landmark && <span className="cd-addr-sub">Near: {addr.landmark}</span>}
                                    <div className="cd-addr-badges">
                                        {addr.default && <span className="cd-default-badge">Default</span>}
                                    </div>
                                </div>
                                <div className="cd-addr-actions">
                                    {!addr.default && (
                                        <button className="cd-icon-btn" onClick={() => handleSetDefault(addr.addressId)} title="Set as default">📌</button>
                                    )}
                                    <button className="cd-icon-btn" onClick={() => startEdit(addr)} title="Edit">✏️</button>
                                    <button className="cd-icon-btn danger" onClick={() => handleDelete(addr.addressId)} title="Delete">🗑️</button>
                                </div>
                            </div>
                        ))}
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
            <div className="cd-page">
                <div className="cd-loading">
                    <motion.div animate={{ rotate: 360 }} transition={{ duration: 1.5, repeat: Infinity, ease: 'linear' }} className="cd-loading-icon">🍽️</motion.div>
                    <p>Loading your dashboard...</p>
                </div>
            </div>
        );
    }

    return (
        <div className="cd-page">
            <div className="cd-container">
                <div className="cd-page-header">
                    <h1 className="cd-page-title">Hi, {profile?.firstName || user?.firstName || 'there'} 👋</h1>
                    <span className="cd-page-sub">Your personal space</span>
                </div>

                {error && <div className="cd-toast cd-toast-error">{error}</div>}
                {success && <div className="cd-toast cd-toast-success">{success}</div>}

                {/* Tabs */}
                <div className="cd-tabs">
                    {TABS.map(tab => (
                        <button
                            key={tab}
                            className={`cd-tab ${activeTab === tab ? 'active' : ''}`}
                            onClick={() => setActiveTab(tab)}
                        >
                            {TAB_LABELS[tab]}
                        </button>
                    ))}
                </div>

                <div>
                    {activeTab === 'overview' && <OverviewTab />}
                    {activeTab === 'favourites' && <FavouritesTab />}
                    {activeTab === 'addresses' && <AddressesTab />}
                    {activeTab === 'messPlans' && <MessPlansTab />}
                    {activeTab === 'reviews' && <ReviewsTab />}
                </div>
            </div>
        </div>
    );
};

export default CustomerDashboard;

