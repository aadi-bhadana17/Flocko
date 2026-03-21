import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { getCart, updateCartItemQuantity, removeCartItem, clearCart } from '../../api/cartService';
import { getAddresses, addAddress } from '../../api/userService';
import { placeOrder } from '../../api/orderService';
import { motion, AnimatePresence } from 'framer-motion';
import './CartPage.css';

const CartPage = () => {

    const [cart, setCart] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [updatingItem, setUpdatingItem] = useState(null);

    // Checkout state
    const [showCheckout, setShowCheckout] = useState(false);
    const [addresses, setAddresses] = useState([]);
    const [selectedAddress, setSelectedAddress] = useState(null);
    const [loadingAddresses, setLoadingAddresses] = useState(false);
    const [placingOrder, setPlacingOrder] = useState(false);
    const [orderSuccess, setOrderSuccess] = useState(null);
    const [scheduledAt, setScheduledAt] = useState('');
    const [isSpecial, setIsSpecial] = useState(false);

    // New address form
    const [showAddressForm, setShowAddressForm] = useState(false);
    const [newAddress, setNewAddress] = useState({
        buildingNo: '', street: '', city: '', state: '', pincode: '', landmark: ''
    });
    const [addingAddress, setAddingAddress] = useState(false);

    useEffect(() => {
        fetchCart();
    }, []);

    const fetchCart = async () => {
        setLoading(true);
        setError('');
        try {
            const data = await getCart();
            setCart(data);
        } catch (err) {
            if (err.response?.status === 404 || err.response?.data?.includes?.('not found')) {
                setCart(null);
            } else {
                setError('Unable to load cart. Please try again.');
            }
        } finally {
            setLoading(false);
        }
    };

    const handleUpdateQuantity = async (cartItemId, newQuantity) => {
        if (newQuantity < 1) return;
        setUpdatingItem(cartItemId);
        try {
            const data = await updateCartItemQuantity(cartItemId, newQuantity);
            setCart(data);
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to update quantity.');
        } finally {
            setUpdatingItem(null);
        }
    };

    const handleRemoveItem = async (cartItemId) => {
        setUpdatingItem(cartItemId);
        try {
            const data = await removeCartItem(cartItemId);
            setCart(data);
        } catch (err) {
            setError(err.response?.data?.message || 'Failed to remove item.');
        } finally {
            setUpdatingItem(null);
        }
    };

    const handleClearCart = async () => {
        try {
            await clearCart();
            setCart(null);
        } catch (err) {
            setError('Failed to clear cart.');
        }
    };

    const handleCheckout = async () => {
        setShowCheckout(true);
        setLoadingAddresses(true);
        try {
            const data = await getAddresses();
            setAddresses(data);
            const defaultAddr = data.find(a => a.default);
            if (defaultAddr) setSelectedAddress(defaultAddr.addressId);
            else if (data.length > 0) setSelectedAddress(data[0].addressId);
        } catch (err) {
            setError('Failed to load addresses.');
        } finally {
            setLoadingAddresses(false);
        }
    };

    const handleAddAddress = async (e) => {
        e.preventDefault();
        setAddingAddress(true);
        try {
            const created = await addAddress(newAddress);
            setAddresses(prev => [...prev, created]);
            setSelectedAddress(created.addressId);
            setShowAddressForm(false);
            setNewAddress({ buildingNo: '', street: '', city: '', state: '', pincode: '', landmark: '' });
        } catch (err) {
            setError('Failed to add address.');
        } finally {
            setAddingAddress(false);
        }
    };

    const handlePlaceOrder = async () => {
        if (!selectedAddress) {
            setError('Please select a delivery address.');
            return;
        }
        setPlacingOrder(true);
        setError('');
        try {
            const orderPayload = {
                addressId: selectedAddress,
                ...(scheduledAt ? { scheduledAt } : {}),
                ...(isSpecial ? { isSpecial: true } : {})
            };

            const orderData = await placeOrder(orderPayload);
            setOrderSuccess(orderData);
            setCart(null);
        } catch (err) {
            setError(err.response?.data?.message || err.response?.data || 'Failed to place order.');
        } finally {
            setPlacingOrder(false);
        }
    };

    // Order success view
    if (orderSuccess) {
        return (
            <div className="cart-page">
                <div className="cart-container">
                    <motion.div
                        className="cart-success"
                        initial={{ opacity: 0, scale: 0.9 }}
                        animate={{ opacity: 1, scale: 1 }}
                    >
                        <span className="cart-success-icon">🎉</span>
                        <h2>Order Placed!</h2>
                        <p className="cart-success-msg">{orderSuccess.message}</p>
                        <div className="cart-success-details">
                            <span>Order #{orderSuccess.orderId}</span>
                            <span>₹{Number(orderSuccess.totalPrice).toFixed(0)}</span>
                        </div>
                        <div className="cart-success-actions">
                            <Link to={`/orders/${orderSuccess.orderId}`} className="cart-btn cart-btn-primary">
                                View Order
                            </Link>
                            <Link to="/" className="cart-btn cart-btn-outline">
                                Continue Browsing
                            </Link>
                        </div>
                    </motion.div>
                </div>
            </div>
        );
    }

    if (loading) {
        return (
            <div className="cart-page">
                <div className="cart-loading">
                    <motion.div
                        className="cart-loading-icon"
                        animate={{ rotate: 360 }}
                        transition={{ duration: 1.5, repeat: Infinity, ease: 'linear' }}
                    >
                        🛒
                    </motion.div>
                    <p>Loading your cart...</p>
                </div>
            </div>
        );
    }

    const isEmpty = !cart || !cart.cartItems || cart.cartItems.length === 0;

    if (isEmpty && !showCheckout) {
        return (
            <div className="cart-page">
                <div className="cart-container">
                    <motion.div
                        className="cart-empty"
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                    >
                        <span className="cart-empty-icon">🛒</span>
                        <h2>Your cart is empty</h2>
                        <p>Looks like you haven't added any items yet.</p>
                        <Link to="/" className="cart-btn cart-btn-primary">Browse Restaurants</Link>
                    </motion.div>
                </div>
            </div>
        );
    }

    return (
        <div className="cart-page">
            <div className="cart-container">
                <Link to="/" className="cart-back-link">← Continue Shopping</Link>

                <div className="cart-layout">
                    {/* Cart Items */}
                    <motion.div
                        className="cart-items-section"
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                    >
                        <div className="cart-header">
                            <h1 className="cart-title">Your Cart</h1>
                            {cart?.restaurant && (
                                <span className="cart-restaurant-tag">
                                    🍽️ {cart.restaurant.restaurantName}
                                </span>
                            )}
                        </div>

                        {error && (
                            <motion.div
                                className="cart-error"
                                initial={{ opacity: 0 }}
                                animate={{ opacity: 1 }}
                            >
                                ⚠️ {error}
                                <button onClick={() => setError('')} className="cart-error-close">×</button>
                            </motion.div>
                        )}

                        <AnimatePresence>
                            {cart?.cartItems?.map((item) => (
                                <motion.div
                                    key={item.cartItemId}
                                    className="cart-item"
                                    layout
                                    initial={{ opacity: 0, x: -20 }}
                                    animate={{ opacity: 1, x: 0 }}
                                    exit={{ opacity: 0, x: 20, height: 0 }}
                                >
                                    <div className="cart-item-info">
                                        <h3 className="cart-item-name">{item.foodName}</h3>
                                        {item.addons?.length > 0 && (
                                            <div className="cart-item-addons">
                                                {item.addons.map(a => (
                                                    <span key={a.addonId} className="cart-addon-chip">
                                                        + {a.addonName}
                                                    </span>
                                                ))}
                                            </div>
                                        )}
                                    </div>

                                    <div className="cart-item-controls">
                                        <div className="cart-qty-control">
                                            <button
                                                className="cart-qty-btn"
                                                onClick={() => handleUpdateQuantity(item.cartItemId, item.quantity - 1)}
                                                disabled={updatingItem === item.cartItemId || item.quantity <= 1}
                                            >
                                                −
                                            </button>
                                            <span className="cart-qty-value">
                                                {updatingItem === item.cartItemId ? '...' : item.quantity}
                                            </span>
                                            <button
                                                className="cart-qty-btn"
                                                onClick={() => handleUpdateQuantity(item.cartItemId, item.quantity + 1)}
                                                disabled={updatingItem === item.cartItemId}
                                            >
                                                +
                                            </button>
                                        </div>

                                        <span className="cart-item-price">₹{Number(item.itemTotal).toFixed(0)}</span>

                                        <button
                                            className="cart-remove-btn"
                                            onClick={() => handleRemoveItem(item.cartItemId)}
                                            disabled={updatingItem === item.cartItemId}
                                        >
                                            🗑️
                                        </button>
                                    </div>
                                </motion.div>
                            ))}
                        </AnimatePresence>

                        <div className="cart-actions-row">
                            <button className="cart-btn cart-btn-danger" onClick={handleClearCart}>
                                Clear Cart
                            </button>
                        </div>
                    </motion.div>

                    {/* Order Summary / Checkout */}
                    <motion.div
                        className="cart-summary-section"
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ delay: 0.1 }}
                    >
                        <div className="cart-summary-card">
                            <h2 className="cart-summary-title">Order Summary</h2>

                            <div className="cart-summary-row">
                                <span>Items ({cart?.totalQuantity || 0})</span>
                                <span>₹{Number(cart?.totalPrice || 0).toFixed(0)}</span>
                            </div>
                            <div className="cart-summary-row">
                                <span>Delivery</span>
                                <span className="cart-free-tag">FREE</span>
                            </div>

                            <hr className="cart-divider" />

                            <div className="cart-summary-row cart-total-row">
                                <span>Total</span>
                                <span>₹{Number(cart?.totalPrice || 0).toFixed(0)}</span>
                            </div>

                            {!showCheckout ? (
                                <button
                                    className="cart-btn cart-btn-primary cart-btn-full"
                                    onClick={handleCheckout}
                                >
                                    Proceed to Checkout
                                </button>
                            ) : (
                                <div className="cart-checkout-section">
                                    <h3 className="cart-checkout-title">Select Delivery Address</h3>

                                    {loadingAddresses ? (
                                        <p className="cart-loading-text">Loading addresses...</p>
                                    ) : addresses.length === 0 && !showAddressForm ? (
                                        <div className="cart-no-address">
                                            <p>No addresses found. Add one to continue.</p>
                                            <button
                                                className="cart-btn cart-btn-outline cart-btn-full"
                                                onClick={() => setShowAddressForm(true)}
                                            >
                                                + Add Address
                                            </button>
                                        </div>
                                    ) : (
                                        <>
                                            <div className="cart-address-list">
                                                {addresses.map(addr => (
                                                    <div
                                                        key={addr.addressId}
                                                        className={`cart-address-option ${selectedAddress === addr.addressId ? 'selected' : ''}`}
                                                        onClick={() => setSelectedAddress(addr.addressId)}
                                                    >
                                                        <div className="cart-address-radio">
                                                            <div className={`cart-radio ${selectedAddress === addr.addressId ? 'active' : ''}`} />
                                                        </div>
                                                        <div className="cart-address-details">
                                                            <span className="cart-address-line">
                                                                {addr.buildingNo}, {addr.street}
                                                            </span>
                                                            <span className="cart-address-sub">
                                                                {addr.city} - {addr.pincode}
                                                            </span>
                                                            {addr.landmark && (
                                                                <span className="cart-address-landmark">
                                                                    Near: {addr.landmark}
                                                                </span>
                                                            )}
                                                            {addr.default && (
                                                                <span className="cart-default-badge">Default</span>
                                                            )}
                                                        </div>
                                                    </div>
                                                ))}
                                            </div>

                                            {!showAddressForm && (
                                                <button
                                                    className="cart-btn cart-btn-outline cart-btn-full cart-btn-sm"
                                                    onClick={() => setShowAddressForm(true)}
                                                >
                                                    + Add New Address
                                                </button>
                                            )}
                                        </>
                                    )}

                                    {/* New Address Form */}
                                    <AnimatePresence>
                                        {showAddressForm && (
                                            <motion.form
                                                className="cart-address-form"
                                                onSubmit={handleAddAddress}
                                                initial={{ opacity: 0, height: 0 }}
                                                animate={{ opacity: 1, height: 'auto' }}
                                                exit={{ opacity: 0, height: 0 }}
                                            >
                                                <input
                                                    placeholder="Building No."
                                                    value={newAddress.buildingNo}
                                                    onChange={e => setNewAddress(p => ({ ...p, buildingNo: e.target.value }))}
                                                    required
                                                />
                                                <input
                                                    placeholder="Street"
                                                    value={newAddress.street}
                                                    onChange={e => setNewAddress(p => ({ ...p, street: e.target.value }))}
                                                    required
                                                />
                                                <div className="cart-form-row">
                                                    <input
                                                        placeholder="City"
                                                        value={newAddress.city}
                                                        onChange={e => setNewAddress(p => ({ ...p, city: e.target.value }))}
                                                        required
                                                    />
                                                    <input
                                                        placeholder="State"
                                                        value={newAddress.state}
                                                        onChange={e => setNewAddress(p => ({ ...p, state: e.target.value }))}
                                                        required
                                                    />
                                                </div>
                                                <div className="cart-form-row">
                                                    <input
                                                        placeholder="Pincode"
                                                        value={newAddress.pincode}
                                                        onChange={e => setNewAddress(p => ({ ...p, pincode: e.target.value }))}
                                                        required
                                                    />
                                                    <input
                                                        placeholder="Landmark (optional)"
                                                        value={newAddress.landmark}
                                                        onChange={e => setNewAddress(p => ({ ...p, landmark: e.target.value }))}
                                                    />
                                                </div>
                                                <div className="cart-form-actions">
                                                    <button type="submit" className="cart-btn cart-btn-primary" disabled={addingAddress}>
                                                        {addingAddress ? 'Saving...' : 'Save Address'}
                                                    </button>
                                                    <button
                                                        type="button"
                                                        className="cart-btn cart-btn-outline"
                                                        onClick={() => setShowAddressForm(false)}
                                                    >
                                                        Cancel
                                                    </button>
                                                </div>
                                            </motion.form>
                                        )}
                                    </AnimatePresence>

                                    <div className="cart-order-options">
                                        <label className="cart-option-label" htmlFor="scheduledAt">
                                            Schedule delivery (optional)
                                        </label>
                                        <input
                                            id="scheduledAt"
                                            type="datetime-local"
                                            className="cart-option-input"
                                            value={scheduledAt}
                                            onChange={(e) => setScheduledAt(e.target.value)}
                                        />

                                        <label className="cart-special-toggle" htmlFor="isSpecial">
                                            <input
                                                id="isSpecial"
                                                type="checkbox"
                                                checked={isSpecial}
                                                onChange={(e) => setIsSpecial(e.target.checked)}
                                            />
                                            <span>Mark as special order (optional)</span>
                                        </label>
                                    </div>

                                    <button
                                        className="cart-btn cart-btn-primary cart-btn-full cart-place-order-btn"
                                        onClick={handlePlaceOrder}
                                        disabled={placingOrder || !selectedAddress}
                                    >
                                        {placingOrder ? 'Placing Order...' : '🛒 Place Order'}
                                    </button>
                                </div>
                            )}
                        </div>
                    </motion.div>
                </div>
            </div>
        </div>
    );
};

export default CartPage;


