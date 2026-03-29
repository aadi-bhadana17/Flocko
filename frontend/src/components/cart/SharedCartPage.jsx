import React, { useEffect, useMemo, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { motion } from 'framer-motion';
import { getRestaurantMenu, getRestaurants } from '../../api/publicService';
import { getAddresses } from '../../api/userService';
import {
    addItemToSharedCart,
    checkoutSharedCart,
    contributeToSharedCart,
    createSharedCart,
    getCurrentSharedCart,
    getSharedCartByCode,
    joinSharedCart,
    removeItemFromSharedCart
} from '../../api/sharedCartService';
import './SharedCartPage.css';

const getErrorText = (err, fallback) => {
    if (typeof err?.response?.data === 'string') return err.response.data;
    return err?.response?.data?.message || fallback;
};

const normalizeRestaurant = (restaurant) => ({
    restaurantId: restaurant?.restaurantId ?? restaurant?.id,
    restaurantName: restaurant?.restaurantName ?? restaurant?.name ?? 'Restaurant'
});

const SharedCartPage = () => {
    const location = useLocation();

    const [sharedCart, setSharedCart] = useState(null);
    const [loading, setLoading] = useState(true);
    const [working, setWorking] = useState(false);
    const [error, setError] = useState('');
    const [message, setMessage] = useState('');

    const [restaurants, setRestaurants] = useState([]);
    const [createRestaurantId, setCreateRestaurantId] = useState('');
    const [hostPaysAll, setHostPaysAll] = useState(true);

    const [codeInput, setCodeInput] = useState('');
    const [contributionAmount, setContributionAmount] = useState('');
    const [addingItem, setAddingItem] = useState(false);
    const [removingItemId, setRemovingItemId] = useState(null);

    const [menu, setMenu] = useState(null);
    const [menuLoading, setMenuLoading] = useState(false);
    const [activeCategoryId, setActiveCategoryId] = useState('');
    const [selectedFoodId, setSelectedFoodId] = useState('');
    const [selectedAddonIds, setSelectedAddonIds] = useState([]);
    const [itemQuantity, setItemQuantity] = useState(1);

    const [addresses, setAddresses] = useState([]);
    const [selectedAddress, setSelectedAddress] = useState('');
    const [scheduledAt, setScheduledAt] = useState('');
    const [isSpecial, setIsSpecial] = useState(false);

    const remainingAmount = useMemo(() => {
        if (!sharedCart) return 0;
        const total = Number(sharedCart.totalPrice || 0);
        const paid = Number(sharedCart.amountPaid || 0);
        return Math.max(total - paid, 0);
    }, [sharedCart]);

    const viewerIsHost = Boolean(sharedCart?.viewerIsHost);
    const viewerIsMember = Boolean(sharedCart?.viewerIsMember);

    const hydrateCurrentCart = async () => {
        try {
            const cart = await getCurrentSharedCart();
            setSharedCart(cart);
            setCodeInput(cart.joinCode || '');
        } catch (err) {
            if (err?.response?.status !== 404) {
                throw err;
            }
        }
    };

    useEffect(() => {
        const preselectedRestaurant = normalizeRestaurant(location.state?.restaurant);
        if (preselectedRestaurant.restaurantId) {
            setCreateRestaurantId(String(preselectedRestaurant.restaurantId));
        }
    }, [location.state]);

    useEffect(() => {
        const loadInitialData = async () => {
            setLoading(true);
            setError('');
            try {
                const [restaurantData] = await Promise.all([
                    getRestaurants(),
                    hydrateCurrentCart()
                ]);
                setRestaurants(Array.isArray(restaurantData) ? restaurantData : []);
            } catch (err) {
                setError(getErrorText(err, 'Unable to load shared cart details.'));
            } finally {
                setLoading(false);
            }
        };

        loadInitialData();
    }, []);

    useEffect(() => {
        const loadAddressesForHost = async () => {
            if (!viewerIsHost || !sharedCart || remainingAmount <= 0) return;
            try {
                const addressData = await getAddresses();
                setAddresses(addressData);
                if (addressData?.length > 0) {
                    const defaultAddress = addressData.find(a => a.default);
                    setSelectedAddress(String(defaultAddress?.addressId || addressData[0].addressId));
                }
            } catch {
                // Keep checkout visible even if address loading fails.
            }
        };

        loadAddressesForHost();
    }, [viewerIsHost, sharedCart, remainingAmount]);

    useEffect(() => {
        const loadMenuForSharedCart = async () => {
            const restaurantId = sharedCart?.restaurant?.restaurantId;
            if (!restaurantId) {
                setMenu(null);
                setActiveCategoryId('');
                setSelectedFoodId('');
                setSelectedAddonIds([]);
                return;
            }

            setMenuLoading(true);
            try {
                const menuData = await getRestaurantMenu(restaurantId);
                setMenu(menuData);

                const firstCategory = menuData?.categories?.[0];
                setActiveCategoryId(firstCategory?.categoryId ? String(firstCategory.categoryId) : '');

                const firstFood = firstCategory?.foods?.find((food) => food.available !== false);
                setSelectedFoodId(firstFood?.foodId ? String(firstFood.foodId) : '');
                setSelectedAddonIds([]);
            } catch {
                setMenu(null);
            } finally {
                setMenuLoading(false);
            }
        };

        loadMenuForSharedCart();
    }, [sharedCart?.restaurant?.restaurantId]);

    const updateCartByCode = async (code) => {
        const nextCart = await getSharedCartByCode(code);
        setSharedCart(nextCart);
        return nextCart;
    };

    const refreshSharedCartByCode = async (code) => {
        if (!code) return;
        const refreshed = await getSharedCartByCode(code);
        setSharedCart(refreshed);
        return refreshed;
    };

    const handleCreate = async () => {
        if (!createRestaurantId) {
            setError('Please choose a restaurant to create a shared cart.');
            return;
        }

        setWorking(true);
        setError('');
        setMessage('');
        try {
            const created = await createSharedCart(Number(createRestaurantId), hostPaysAll);
            setSharedCart(created);
            setCodeInput(created.joinCode || '');
            await refreshSharedCartByCode(created.joinCode);
            setMessage('Shared cart is ready. Share the code with your friends.');
        } catch (err) {
            setError(getErrorText(err, 'Unable to create shared cart.'));
        } finally {
            setWorking(false);
        }
    };

    const handleViewByCode = async () => {
        if (!codeInput.trim()) {
            setError('Please enter a shared cart code.');
            return;
        }

        const normalizedCode = codeInput.trim().toUpperCase();
        setWorking(true);
        setError('');
        setMessage('');
        try {
            await updateCartByCode(normalizedCode);
            setCodeInput(normalizedCode);
            setMessage('Shared cart loaded successfully.');
        } catch (err) {
            setError(getErrorText(err, 'Unable to find shared cart with this code.'));
        } finally {
            setWorking(false);
        }
    };

    const handleJoin = async () => {
        if (!codeInput.trim()) {
            setError('Please enter a shared cart code to join.');
            return;
        }

        const normalizedCode = codeInput.trim().toUpperCase();
        setWorking(true);
        setError('');
        setMessage('');
        try {
            const joinMessage = await joinSharedCart(normalizedCode);
            await updateCartByCode(normalizedCode);
            setCodeInput(normalizedCode);
            await refreshSharedCartByCode(normalizedCode);
            setMessage(typeof joinMessage === 'string' ? joinMessage : 'Joined shared cart successfully.');
        } catch (err) {
            setError(getErrorText(err, 'Unable to join this shared cart.'));
        } finally {
            setWorking(false);
        }
    };

    const handleContribute = async () => {
        if (!sharedCart?.joinCode) return;

        const amount = Number(contributionAmount);
        if (!amount || amount <= 0) {
            setError('Please enter a valid amount to contribute.');
            return;
        }

        setWorking(true);
        setError('');
        setMessage('');
        try {
            const updated = await contributeToSharedCart(sharedCart.joinCode, amount);
            setSharedCart(updated);
            await refreshSharedCartByCode(sharedCart.joinCode);
            setContributionAmount('');
            setMessage('Contribution added successfully.');
        } catch (err) {
            setError(getErrorText(err, 'Unable to add contribution.'));
        } finally {
            setWorking(false);
        }
    };

    const handleCheckout = async () => {
        if (!viewerIsHost || !sharedCart?.joinCode) return;
        if (!selectedAddress) {
            setError('Please select a delivery address for checkout.');
            return;
        }

        setWorking(true);
        setError('');
        setMessage('');
        try {
            const response = await checkoutSharedCart(sharedCart.joinCode, {
                addressId: Number(selectedAddress),
                ...(scheduledAt ? { scheduledAt } : {}),
                ...(isSpecial ? { isSpecial: true } : {})
            });

            setMessage(typeof response === 'string' ? response : 'Shared cart checked out successfully.');
            const refreshed = await getSharedCartByCode(sharedCart.joinCode);
            setSharedCart(refreshed);
        } catch (err) {
            setError(getErrorText(err, 'Unable to checkout shared cart.'));
        } finally {
            setWorking(false);
        }
    };

    const handleCopyCode = async () => {
        if (!sharedCart?.joinCode) return;
        try {
            await navigator.clipboard.writeText(sharedCart.joinCode);
            setMessage('Join code copied to clipboard.');
        } catch {
            setMessage(`Join code: ${sharedCart.joinCode}`);
        }
    };

    const handleManualRefresh = async () => {
        setWorking(true);
        setError('');
        setMessage('');
        try {
            if (sharedCart?.joinCode) {
                await refreshSharedCartByCode(sharedCart.joinCode);
            } else {
                const current = await getCurrentSharedCart();
                setSharedCart(current);
                setCodeInput(current.joinCode || '');
            }
            setMessage('Shared cart refreshed.');
        } catch (err) {
            setError(getErrorText(err, 'Unable to refresh shared cart right now.'));
        } finally {
            setWorking(false);
        }
    };

    const activeCategory = useMemo(() => {
        if (!menu?.categories?.length) return null;
        return menu.categories.find((cat) => String(cat.categoryId) === String(activeCategoryId)) || menu.categories[0];
    }, [menu, activeCategoryId]);

    const activeFoods = useMemo(() => activeCategory?.foods || [], [activeCategory]);
    const activeFood = useMemo(() => {
        if (!activeFoods.length) return null;
        return activeFoods.find((food) => String(food.foodId) === String(selectedFoodId)) || activeFoods[0];
    }, [activeFoods, selectedFoodId]);

    const availableAddons = useMemo(() => {
        return (activeCategory?.availableAddons || []).filter((addon) => addon.available !== false);
    }, [activeCategory]);

    const handleCategoryChange = (categoryId) => {
        const nextCategory = menu?.categories?.find((cat) => String(cat.categoryId) === String(categoryId));
        const firstFood = nextCategory?.foods?.find((food) => food.available !== false) || nextCategory?.foods?.[0];
        setActiveCategoryId(String(categoryId));
        setSelectedFoodId(firstFood?.foodId ? String(firstFood.foodId) : '');
        setSelectedAddonIds([]);
    };

    const handleToggleAddon = (addonId) => {
        setSelectedAddonIds((prev) => {
            if (prev.includes(addonId)) return prev.filter((id) => id !== addonId);
            return [...prev, addonId];
        });
    };

    const handleAddItem = async () => {
        if (!sharedCart?.joinCode || !activeFood?.foodId) {
            setError('Please choose a food item before adding to shared cart.');
            return;
        }
        if (!itemQuantity || itemQuantity < 1) {
            setError('Quantity must be at least 1.');
            return;
        }

        setAddingItem(true);
        setError('');
        setMessage('');
        try {
            const updated = await addItemToSharedCart(sharedCart.joinCode, {
                foodId: Number(activeFood.foodId),
                quantity: Number(itemQuantity),
                addonIds: selectedAddonIds
            });
            setSharedCart(updated);
            await refreshSharedCartByCode(sharedCart.joinCode);
            setMessage('Item added to shared cart.');
        } catch (err) {
            setError(getErrorText(err, 'Unable to add item to shared cart.'));
        } finally {
            setAddingItem(false);
        }
    };

    const handleRemoveItem = async (cartItemId) => {
        if (!sharedCart?.joinCode || !cartItemId) return;

        setRemovingItemId(cartItemId);
        setError('');
        setMessage('');
        try {
            const updated = await removeItemFromSharedCart(sharedCart.joinCode, cartItemId);
            setSharedCart(updated);
            await refreshSharedCartByCode(sharedCart.joinCode);
            setMessage('Item removed from shared cart.');
        } catch (err) {
            setError(getErrorText(err, 'Unable to remove this item. You can only remove your own items.'));
        } finally {
            setRemovingItemId(null);
        }
    };

    const getMemberName = (member) => {
        return member?.user?.userName || member?.user?.fullName || 'Member';
    };

    const selectedRestaurantLabel = useMemo(() => {
        if (!createRestaurantId) return '';
        const match = restaurants.find(r => String(r.restaurantId ?? r.id) === String(createRestaurantId));
        return match?.restaurantName ?? match?.name ?? '';
    }, [restaurants, createRestaurantId]);

    if (loading) {
        return (
            <div className="shared-cart-page">
                <div className="shared-cart-container">
                    <div className="shared-cart-loading">Loading shared cart...</div>
                </div>
            </div>
        );
    }

    return (
        <div className="shared-cart-page">
            <div className="shared-cart-container">
                <Link to="/cart" className="shared-cart-back-link">← Back to cart</Link>

                <div className="shared-cart-header-row">
                    <h1 className="shared-cart-title">Shared Cart</h1>
                    <p className="shared-cart-subtitle">Create, join and manage cart splitting with your group.</p>
                </div>

                {error && <div className="shared-cart-alert shared-cart-alert-error">{error}</div>}
                {message && <div className="shared-cart-alert shared-cart-alert-success">{message}</div>}

                <div className="shared-cart-grid">
                    <motion.section className="shared-cart-card" initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }}>
                        <h2>Create shared cart</h2>
                        <p>Start a shared cart for one restaurant and invite others using a join code.</p>

                        <label htmlFor="shared-create-restaurant">Restaurant</label>
                        <select
                            id="shared-create-restaurant"
                            value={createRestaurantId}
                            onChange={(e) => setCreateRestaurantId(e.target.value)}
                        >
                            <option value="">Choose restaurant</option>
                            {restaurants.map((restaurant) => {
                                const normalized = normalizeRestaurant(restaurant);
                                if (!normalized.restaurantId) return null;
                                return (
                                    <option key={normalized.restaurantId} value={normalized.restaurantId}>
                                        {normalized.restaurantName}
                                    </option>
                                );
                            })}
                        </select>

                        {selectedRestaurantLabel && (
                            <span className="shared-cart-hint">Selected: {selectedRestaurantLabel}</span>
                        )}

                        <label className="shared-cart-checkbox">
                            <input
                                type="checkbox"
                                checked={hostPaysAll}
                                onChange={(e) => setHostPaysAll(e.target.checked)}
                            />
                            <span>Host pays all</span>
                        </label>

                        <button className="shared-cart-btn shared-cart-btn-primary" onClick={handleCreate} disabled={working}>
                            {working ? 'Please wait...' : 'Create shared cart'}
                        </button>
                    </motion.section>

                    <motion.section className="shared-cart-card" initial={{ opacity: 0, y: 12 }} animate={{ opacity: 1, y: 0 }}>
                        <h2>Join with code</h2>
                        <p>Enter a join code to preview or become a member of a shared cart.</p>

                        <label htmlFor="shared-code-input">Join code</label>
                        <input
                            id="shared-code-input"
                            type="text"
                            value={codeInput}
                            maxLength={6}
                            onChange={(e) => setCodeInput(e.target.value.toUpperCase())}
                            placeholder="ABC123"
                        />

                        <div className="shared-cart-inline-actions">
                            <button className="shared-cart-btn shared-cart-btn-outline" onClick={handleViewByCode} disabled={working}>
                                View
                            </button>
                            <button className="shared-cart-btn shared-cart-btn-primary" onClick={handleJoin} disabled={working}>
                                Join
                            </button>
                        </div>
                    </motion.section>
                </div>

                {sharedCart && (
                    <motion.section className="shared-cart-details" initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
                        <div className="shared-cart-details-header">
                            <h2>Active shared cart</h2>
                            <div className="shared-cart-code-row">
                                <span>Code: <strong>{sharedCart.joinCode}</strong></span>
                                <button className="shared-cart-btn shared-cart-btn-outline shared-cart-btn-sm" onClick={handleCopyCode}>Copy</button>
                                <button
                                    className="shared-cart-btn shared-cart-btn-outline shared-cart-btn-sm"
                                    onClick={handleManualRefresh}
                                    disabled={working}
                                >
                                    {working ? 'Refreshing...' : 'Refresh'}
                                </button>
                            </div>
                        </div>

                        <div className="shared-cart-stat-grid">
                            <div className="shared-cart-stat-box">
                                <span>Restaurant</span>
                                <strong>{sharedCart.restaurant?.restaurantName || 'Restaurant'}</strong>
                            </div>
                            <div className="shared-cart-stat-box">
                                <span>Total</span>
                                <strong>Rs {Number(sharedCart.totalPrice || 0).toFixed(2)}</strong>
                            </div>
                            <div className="shared-cart-stat-box">
                                <span>Paid</span>
                                <strong>Rs {Number(sharedCart.amountPaid || 0).toFixed(2)}</strong>
                            </div>
                            <div className="shared-cart-stat-box">
                                <span>Remaining</span>
                                <strong>Rs {remainingAmount.toFixed(2)}</strong>
                            </div>
                        </div>

                        <h3 className="shared-cart-members-title">Members</h3>
                        <p className="shared-cart-hint">
                            Note: You can remove only the items added from your own member cart.
                        </p>
                        <div className="shared-cart-members-list">
                            {(sharedCart.memberList || []).map((member) => (
                                <div className="shared-cart-member-card" key={member.memberId}>
                                    <div className="shared-cart-member-header">
                                        <strong>{getMemberName(member)}</strong>
                                        <span>Contributed: Rs {Number(member.walletContribution || 0).toFixed(2)}</span>
                                    </div>
                                    {!member.cart?.cartItems?.length ? (
                                        <span className="shared-cart-hint">No items in member cart yet.</span>
                                    ) : (
                                        <ul className="shared-cart-item-list">
                                            {member.cart.cartItems.map((item) => (
                                                <li key={item.cartItemId} className="shared-cart-item-row">
                                                    <span>
                                                        {item.foodName} x {item.quantity} - Rs {Number(item.itemTotal || 0).toFixed(2)}
                                                    </span>
                                                    <button
                                                        className="shared-cart-btn shared-cart-btn-outline shared-cart-btn-sm"
                                                        onClick={() => handleRemoveItem(item.cartItemId)}
                                                        disabled={(!viewerIsMember && !viewerIsHost) || removingItemId === item.cartItemId}
                                                    >
                                                        {removingItemId === item.cartItemId ? 'Removing...' : 'Remove'}
                                                    </button>
                                                </li>
                                            ))}
                                        </ul>
                                    )}
                                </div>
                            ))}
                        </div>

                        {sharedCart.isActive && (viewerIsMember || viewerIsHost) && (
                            <div className="shared-cart-action-panel">
                                <h3>Add items to shared cart</h3>
                                {menuLoading ? (
                                    <p className="shared-cart-hint">Loading menu...</p>
                                ) : !menu?.categories?.length ? (
                                    <p className="shared-cart-hint">Menu is not available for this restaurant right now.</p>
                                ) : (
                                    <>
                                        <label htmlFor="shared-item-category">Category</label>
                                        <select
                                            id="shared-item-category"
                                            value={activeCategoryId || String(activeCategory?.categoryId || '')}
                                            onChange={(e) => handleCategoryChange(e.target.value)}
                                        >
                                            {menu.categories.map((category) => (
                                                <option key={category.categoryId} value={category.categoryId}>
                                                    {category.categoryName}
                                                </option>
                                            ))}
                                        </select>

                                        <label htmlFor="shared-item-food">Food item</label>
                                        <select
                                            id="shared-item-food"
                                            value={selectedFoodId || String(activeFood?.foodId || '')}
                                            onChange={(e) => {
                                                setSelectedFoodId(e.target.value);
                                                setSelectedAddonIds([]);
                                            }}
                                        >
                                            {activeFoods.map((food) => (
                                                <option key={food.foodId} value={food.foodId} disabled={food.available === false}>
                                                    {food.foodName} - Rs {Number(food.foodPrice || 0).toFixed(2)}
                                                    {food.available === false ? ' (Unavailable)' : ''}
                                                </option>
                                            ))}
                                        </select>

                                        <label htmlFor="shared-item-quantity">Quantity</label>
                                        <input
                                            id="shared-item-quantity"
                                            type="number"
                                            min="1"
                                            value={itemQuantity}
                                            onChange={(e) => setItemQuantity(Number(e.target.value || 1))}
                                        />

                                        {availableAddons.length > 0 && (
                                            <div className="shared-cart-addon-list">
                                                <label>Available add-ons (optional)</label>
                                                {availableAddons.map((addon) => (
                                                    <label key={addon.addonId} className="shared-cart-checkbox">
                                                        <input
                                                            type="checkbox"
                                                            checked={selectedAddonIds.includes(addon.addonId)}
                                                            onChange={() => handleToggleAddon(addon.addonId)}
                                                        />
                                                        <span>{addon.addonName} (+Rs {Number(addon.price || 0).toFixed(2)})</span>
                                                    </label>
                                                ))}
                                            </div>
                                        )}

                                        <button
                                            className="shared-cart-btn shared-cart-btn-primary"
                                            onClick={handleAddItem}
                                            disabled={addingItem || activeFood?.available === false}
                                        >
                                            {addingItem ? 'Adding...' : 'Add to shared cart'}
                                        </button>
                                    </>
                                )}
                            </div>
                        )}

                        {!sharedCart.hostPaysAll && viewerIsMember && !viewerIsHost && (
                            <div className="shared-cart-action-panel">
                                <h3>Contribute from wallet</h3>
                                <div className="shared-cart-contribution-warning">
                                    Warning: wallet contributions are non-refundable, even if you remove all your items later.
                                </div>
                                <div className="shared-cart-inline-actions">
                                    <input
                                        type="number"
                                        min="0"
                                        step="0.01"
                                        value={contributionAmount}
                                        onChange={(e) => setContributionAmount(e.target.value)}
                                        placeholder="Amount"
                                    />
                                    <button className="shared-cart-btn shared-cart-btn-primary" onClick={handleContribute} disabled={working}>
                                        Contribute
                                    </button>
                                </div>
                            </div>
                        )}

                        {viewerIsHost && remainingAmount > 0 && (
                            <div className="shared-cart-action-panel">
                                <h3>Host checkout</h3>
                                <label htmlFor="shared-checkout-address">Delivery address</label>
                                <select
                                    id="shared-checkout-address"
                                    value={selectedAddress}
                                    onChange={(e) => setSelectedAddress(e.target.value)}
                                >
                                    <option value="">Choose address</option>
                                    {addresses.map((address) => (
                                        <option key={address.addressId} value={address.addressId}>
                                            {address.buildingNo}, {address.street}, {address.city}
                                        </option>
                                    ))}
                                </select>

                                <label htmlFor="shared-scheduled-at">Schedule delivery (optional)</label>
                                <input
                                    id="shared-scheduled-at"
                                    type="datetime-local"
                                    value={scheduledAt}
                                    onChange={(e) => setScheduledAt(e.target.value)}
                                />

                                <label className="shared-cart-checkbox">
                                    <input
                                        type="checkbox"
                                        checked={isSpecial}
                                        onChange={(e) => setIsSpecial(e.target.checked)}
                                    />
                                    <span>Mark as special order</span>
                                </label>

                                <button className="shared-cart-btn shared-cart-btn-primary" onClick={handleCheckout} disabled={working || !selectedAddress}>
                                    Checkout shared cart
                                </button>
                            </div>
                        )}
                    </motion.section>
                )}
            </div>
        </div>
    );
};

export default SharedCartPage;
