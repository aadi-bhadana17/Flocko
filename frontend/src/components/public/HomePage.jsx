import React, { useState, useEffect, useContext, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { getRestaurants, searchRestaurants, getPublicGroupDeals } from '../../api/publicService';
import { getFavourites, addFavourite, removeFavourite } from '../../api/userService';
import { AuthContext } from '../../context/AuthContext';
import { motion, AnimatePresence } from 'framer-motion';
import './HomePage.css';

const CUISINE_TYPES = ['ALL', 'INDIAN', 'CHINESE', 'ITALIAN', 'MEXICAN', 'CONTINENTAL', 'AMERICAN'];

const cuisineEmoji = {
    INDIAN: '🍛',
    CHINESE: '🥡',
    ITALIAN: '🍕',
    MEXICAN: '🌮',
    CONTINENTAL: '🍽️',
    AMERICAN: '🍔',
};

const OWNER_GREETINGS = [
    "Your rating depends on today.",
    "Customers are judging your menu.",
    "Hope the orders keep coming.",
    "Welcome back. Time to impress the customers."
];

const ADMIN_GREETINGS = [
    "Hi Boss, the platform is yours.",
    "Welcome back, Boss. Everything is under control.",
    "Hey Boss, time to run the show.",
    "Boss mode activated. Let's go.",
];

const DEAL_STATUS_LABELS = {
    CREATED: 'Scheduled',
    VOTING: 'Voting',
    CONFIRMATION_WINDOW: 'Confirmation',
    FULFILLED: 'Fulfilled',
    EXPIRED: 'Expired',
};

const dealStatusClass = (status) => {
    if (status === 'VOTING') return 'deal-badge-voting';
    if (status === 'CONFIRMATION_WINDOW') return 'deal-badge-confirmation';
    if (status === 'FULFILLED') return 'deal-badge-fulfilled';
    return 'deal-badge-expired';
};

const getDealTargetTime = (deal) => {
    if (deal?.status === 'VOTING') return deal.endTime;
    if (deal?.status === 'CONFIRMATION_WINDOW') return deal.confirmationWindowEndTime;
    return null;
};

const formatCountdown = (target, nowMs) => {
    if (!target) return 'Ended';

    const diff = new Date(target).getTime() - nowMs;
    if (diff <= 0) return 'Ended';

    const hours = Math.floor(diff / 3600000);
    const minutes = Math.floor((diff % 3600000) / 60000);
    const seconds = Math.floor((diff % 60000) / 1000);

    return `${hours}h ${minutes}m ${seconds}s`;
};

const formatMoney = (value) => `Rs ${Number(value || 0).toFixed(2)}`;

const getRestaurantLabelFromDeal = (deal, fallback) => {
    return deal?.restaurant?.restaurantName || deal?.restaurant?.name || fallback || 'Restaurant';
};

const getRestaurantImageCandidates = (restaurant) => {
    const listImages = Array.isArray(restaurant?.images) ? restaurant.images : [];
    return [...listImages, restaurant?.image, restaurant?.imageUrl]
        .filter((value) => typeof value === 'string')
        .map((value) => value.trim())
        .filter(Boolean);
};

const pickRandomImage = (images) => {
    if (!images.length) return '';
    return images[Math.floor(Math.random() * images.length)];
};

const HomePage = () => {
    const { user } = useContext(AuthContext);
    const ownerGreeting = useMemo(() => OWNER_GREETINGS[Math.floor(Math.random() * OWNER_GREETINGS.length)], []);
    const adminGreeting = useMemo(() => ADMIN_GREETINGS[Math.floor(Math.random() * ADMIN_GREETINGS.length)], []);
    const [restaurants, setRestaurants] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [selectedCuisine, setSelectedCuisine] = useState('ALL');
    const [showOpenOnly, setShowOpenOnly] = useState(false);
    const [favouriteIds, setFavouriteIds] = useState(new Set());
    const [togglingFav, setTogglingFav] = useState(null);
    const [searchQuery, setSearchQuery] = useState('');
    const [, setSearching] = useState(false);
    const [dealCards, setDealCards] = useState([]);
    const [dealsLoading, setDealsLoading] = useState(false);
    const [dealsError, setDealsError] = useState('');
    const [nowMs, setNowMs] = useState(Date.now());
    const [failedRestaurantImageIds, setFailedRestaurantImageIds] = useState(new Set());

    const restaurantCardImages = useMemo(() => {
        const selectedById = new Map();
        restaurants.forEach((restaurant) => {
            const candidates = getRestaurantImageCandidates(restaurant);
            const selectedImage = pickRandomImage(candidates);
            if (selectedImage) selectedById.set(restaurant.id, selectedImage);
        });
        return selectedById;
    }, [restaurants]);

    useEffect(() => {
        if (user) {
            getFavourites()
                .then(favs => setFavouriteIds(new Set(favs.map(f => f.restaurantId))))
                .catch(() => {});
        }
    }, [user]);

    useEffect(() => {
        if (searchQuery.trim()) return;
        fetchRestaurants();
    }, [selectedCuisine, showOpenOnly, searchQuery]);

    useEffect(() => {
        if (!searchQuery.trim()) return;
        const timeout = setTimeout(() => handleSearch(), 400);
        return () => clearTimeout(timeout);
    }, [searchQuery]);

    useEffect(() => {
        const interval = setInterval(() => setNowMs(Date.now()), 1000);
        return () => clearInterval(interval);
    }, []);

    useEffect(() => {
        setFailedRestaurantImageIds(new Set());
    }, [restaurants]);

    useEffect(() => {
        if (!user || favouriteIds.size === 0) {
            setDealCards([]);
            setDealsError('');
            setDealsLoading(false);
            return;
        }

        const fetchDealsFromFavourites = async () => {
            setDealsLoading(true);
            setDealsError('');

            const restaurantNameMap = new Map(
                restaurants.map((restaurant) => [restaurant.id, restaurant.name])
            );

            const favouriteRestaurantIds = Array.from(favouriteIds);

            try {
                const responses = await Promise.allSettled(
                    favouriteRestaurantIds.map((restaurantId) => getPublicGroupDeals(restaurantId))
                );

                const nextDeals = [];

                responses.forEach((response, index) => {
                    if (response.status !== 'fulfilled') return;

                    const restaurantId = favouriteRestaurantIds[index];
                    const payload = Array.isArray(response.value)
                        ? response.value
                        : Array.isArray(response.value?.content)
                            ? response.value.content
                            : [];

                    payload.forEach((deal) => {
                        nextDeals.push({
                            ...deal,
                            restaurantId,
                            restaurantName: getRestaurantLabelFromDeal(deal, restaurantNameMap.get(restaurantId)),
                        });
                    });
                });

                nextDeals.sort((a, b) => new Date(a.endTime || 0) - new Date(b.endTime || 0));
                setDealCards(nextDeals);
            } catch {
                setDealsError('Unable to load group deals right now.');
                setDealCards([]);
            } finally {
                setDealsLoading(false);
            }
        };

        fetchDealsFromFavourites();
    }, [user, favouriteIds, restaurants]);

    const fetchRestaurants = async () => {
        setLoading(true);
        setError('');
        try {
            const filters = {};
            if (selectedCuisine !== 'ALL') filters.cuisineType = selectedCuisine;
            if (showOpenOnly) filters.isOpen = true;
            const data = await getRestaurants(filters);
            setRestaurants(data);
        } catch (err) {
            setError('Unable to load restaurants. Please try again.');
        } finally {
            setLoading(false);
        }
    };

    const handleSearch = async () => {
        if (!searchQuery.trim()) return;
        setSearching(true);
        setLoading(true);
        setError('');
        try {
            const data = await searchRestaurants(searchQuery.trim());
            setRestaurants(data);
        } catch {
            setError('Search failed. Please try again.');
        } finally {
            setSearching(false);
            setLoading(false);
        }
    };

    const handleToggleFavourite = async (e, restaurantId) => {
        e.preventDefault();
        e.stopPropagation();
        if (!user) return;
        setTogglingFav(restaurantId);
        try {
            if (favouriteIds.has(restaurantId)) {
                await removeFavourite(restaurantId);
                setFavouriteIds(prev => { const next = new Set(prev); next.delete(restaurantId); return next; });
            } else {
                await addFavourite(restaurantId);
                setFavouriteIds(prev => new Set(prev).add(restaurantId));
            }
        } catch { /* silent */ }
        finally { setTogglingFav(null); }
    };

    const handleRestaurantImageError = (restaurantId) => {
        setFailedRestaurantImageIds((prev) => {
            const next = new Set(prev);
            next.add(restaurantId);
            return next;
        });
    };

    return (
        <div className="home-page">
            {/* Hero Section */}
            <section className="home-hero">
                <motion.div
                    className="home-hero-content"
                    initial={{ opacity: 0, y: 30 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.5 }}
                >
                    <div className="hero-tag">
                        <span className="hero-tag-dot"></span>
                        {user
                            ? user.role === 'ADMIN'
                                ? adminGreeting
                                : user.role === 'RESTAURANT_OWNER'
                                    ? ownerGreeting
                                    : `Welcome back, ${user.firstName || 'foodie'}!`
                            : 'Now live in your city'}
                    </div>
                    <h1>
                        Food that <em>feels</em> like home
                    </h1>
                    <p>
                        Order from the best restaurants around you. Fast delivery, real flavors, zero compromise.
                    </p>
                    {/* Search Bar */}
                    <div className="home-search-bar">
                        <span className="home-search-icon">🔍</span>
                        <input
                            type="text"
                            className="home-search-input"
                            placeholder="Search restaurants by name..."
                            value={searchQuery}
                            onChange={e => setSearchQuery(e.target.value)}
                        />
                        {searchQuery && (
                            <button className="home-search-clear" onClick={() => setSearchQuery('')}>✕</button>
                        )}
                    </div>
                    <div className="hero-stats">
                        <div className="hero-stat">
                            <span className="hero-stat-num">1K+</span>
                            <span className="hero-stat-label">Restaurants</span>
                        </div>
                        <div className="hero-stat">
                            <span className="hero-stat-num">30m</span>
                            <span className="hero-stat-label">Avg Delivery</span>
                        </div>
                        <div className="hero-stat">
                            <span className="hero-stat-num">10K</span>
                            <span className="hero-stat-label">Happy Users</span>
                        </div>
                    </div>
                </motion.div>
            </section>

            {/* Filters */}
            <section className="home-section">
                <div className="home-filters">
                    <div className="cuisine-pills">
                        {CUISINE_TYPES.map((type) => (
                            <motion.button
                                key={type}
                                className={`cuisine-pill ${selectedCuisine === type ? 'active' : ''}`}
                                onClick={() => setSelectedCuisine(type)}
                                whileHover={{ scale: 1.05 }}
                                whileTap={{ scale: 0.95 }}
                            >
                                {type !== 'ALL' && <span className="pill-emoji">{cuisineEmoji[type]}</span>}
                                {type.charAt(0) + type.slice(1).toLowerCase()}
                            </motion.button>
                        ))}
                    </div>
                    <label className="open-toggle">
                        <input
                            type="checkbox"
                            checked={showOpenOnly}
                            onChange={(e) => setShowOpenOnly(e.target.checked)}
                        />
                        <span className="toggle-slider"></span>
                        <span className="toggle-label">Open now</span>
                    </label>
                </div>
            </section>

            {/* Restaurant Grid */}
            <section className="home-section home-deals-section">
                <div className="section-header">
                    <h2 className="section-title">Group Deals from Your Favourites</h2>
                    <span className="section-count">{dealCards.length} active</span>
                </div>

                {dealsError && <div className="home-error">{dealsError}</div>}

                {!user ? (
                    <div className="home-empty compact">
                        <h3>Login to unlock deal ads</h3>
                        <p>Group deals are personalised from your favourite restaurants.</p>
                    </div>
                ) : dealsLoading ? (
                    <div className="home-deals-scroll">
                        {[1, 2, 3].map((item) => (
                            <div className="deal-ad-card skeleton" key={item}>
                                <div className="r-line-skeleton wide"></div>
                                <div className="r-line-skeleton"></div>
                                <div className="r-line-skeleton narrow"></div>
                            </div>
                        ))}
                    </div>
                ) : dealCards.length === 0 ? (
                    <div className="home-empty compact">
                        <h3>No active group deals yet</h3>
                        <p>Add favourites and check again for new voting bundles.</p>
                    </div>
                ) : (
                    <div className="home-deals-scroll">
                        {dealCards.map((deal) => {
                            const participation = Number(deal.currentParticipation || 0);
                            const target = Number(deal.targetParticipation || 0);
                            const progress = target > 0 ? Math.min(100, (participation / target) * 100) : 0;
                            const countdown = formatCountdown(getDealTargetTime(deal), nowMs);
                            const foodSummary = (deal.foodList || [])
                                .slice(0, 3)
                                .map((food) => food.foodName || food.name)
                                .filter(Boolean)
                                .join(', ');

                            return (
                                <Link
                                    key={`${deal.restaurantId}-${deal.dealId}`}
                                    className="deal-ad-link"
                                    to={`/restaurants/${deal.restaurantId}/group-deals/${deal.dealId}`}
                                >
                                    <article className="deal-ad-card">
                                        <div className="deal-ad-top">
                                            <h3>{deal.dealName}</h3>
                                            <span className={`deal-badge ${dealStatusClass(deal.status)}`}>
                                                {DEAL_STATUS_LABELS[deal.status] || deal.status}
                                            </span>
                                        </div>

                                        <p className="deal-restaurant-name">{deal.restaurantName}</p>
                                        <p className="deal-food-list">{foodSummary || 'Bundle details available on detail page.'}</p>

                                        <div className="deal-prices">
                                            <span className="deal-original">{formatMoney(deal.originalPrice)}</span>
                                            <strong>{formatMoney(deal.currentPrice)}</strong>
                                        </div>

                                        <div className="deal-progress-head">
                                            <span>{participation}/{target} joined</span>
                                            <span>{progress.toFixed(0)}%</span>
                                        </div>
                                        <div className="deal-progress-track">
                                            <div className="deal-progress-fill" style={{ width: `${progress}%` }} />
                                        </div>

                                        <div className="deal-time-left">Time left: {countdown}</div>
                                    </article>
                                </Link>
                            );
                        })}
                    </div>
                )}
            </section>

            <section className="home-section">
                <div className="section-header">
                    <h2 className="section-title">
                        {searchQuery.trim()
                            ? `Results for "${searchQuery}"`
                            : selectedCuisine === 'ALL' ? 'Popular near you' : `${selectedCuisine.charAt(0) + selectedCuisine.slice(1).toLowerCase()} Restaurants`}
                    </h2>
                    <span className="section-count">
                        {!loading && `${restaurants.length} found`}
                    </span>
                </div>

                {error && (
                    <motion.div
                        className="home-error"
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                    >
                        ⚠️ {error}
                    </motion.div>
                )}

                {loading ? (
                    <div className="home-grid">
                        {[1, 2, 3, 4, 5, 6].map((i) => (
                            <div key={i} className="restaurant-card skeleton">
                                <div className="r-thumb-skeleton"></div>
                                <div className="r-body-skeleton">
                                    <div className="r-line-skeleton wide"></div>
                                    <div className="r-line-skeleton narrow"></div>
                                </div>
                            </div>
                        ))}
                    </div>
                ) : restaurants.length === 0 ? (
                    <motion.div
                        className="home-empty"
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                    >
                        <span className="empty-icon">🍽️</span>
                        <h3>No restaurants found</h3>
                        <p>Try changing your filters or check back later.</p>
                    </motion.div>
                ) : (
                    <motion.div
                        className="home-grid"
                        initial="hidden"
                        animate="visible"
                        variants={{
                            hidden: {},
                            visible: { transition: { staggerChildren: 0.06 } }
                        }}
                    >
                        <AnimatePresence>
                            {restaurants.map((r) => {
                                const selectedImage = restaurantCardImages.get(r.id);
                                const showCardImage = !!selectedImage && !failedRestaurantImageIds.has(r.id);

                                return (
                                <motion.div
                                    key={r.id}
                                    variants={{
                                        hidden: { opacity: 0, y: 20 },
                                        visible: { opacity: 1, y: 0 }
                                    }}
                                    exit={{ opacity: 0, scale: 0.95 }}
                                    layout
                                >
                                    <Link to={`/restaurant/${r.id}`} className="restaurant-card-link">
                                        <div className="restaurant-card">
                                            <div className={`r-thumb ${r.open ? '' : 'closed'}`}>
                                                {showCardImage ? (
                                                    <img
                                                        src={selectedImage}
                                                        alt={`${r.name || 'Restaurant'} cover`}
                                                        className="r-thumb-image"
                                                        loading="lazy"
                                                        onError={() => handleRestaurantImageError(r.id)}
                                                    />
                                                ) : (
                                                    <span className="r-thumb-emoji">
                                                        {cuisineEmoji[r.cuisineType] || '🍽️'}
                                                    </span>
                                                )}
                                                {!r.open && <span className="closed-badge">Closed</span>}
                                                {user && (
                                                    <button
                                                        className={`fav-btn ${favouriteIds.has(r.id) ? 'fav-active' : ''}`}
                                                        onClick={(e) => handleToggleFavourite(e, r.id)}
                                                        disabled={togglingFav === r.id}
                                                        title={favouriteIds.has(r.id) ? 'Remove from favourites' : 'Add to favourites'}
                                                    >
                                                        {favouriteIds.has(r.id) ? '❤️' : '🤍'}
                                                    </button>
                                                )}
                                            </div>
                                            <div className="r-body">
                                                <div className="r-name">{r.name}</div>
                                                <div className="r-meta">
                                                    {r.avgRating && (
                                                        <span className="r-rating">★ {Number(r.avgRating).toFixed(1)}</span>
                                                    )}
                                                    <span className="r-cuisine">
                                                        {r.cuisineType?.charAt(0) + r.cuisineType?.slice(1).toLowerCase()}
                                                    </span>
                                                    {r.address?.city && (
                                                        <span className="r-city">📍 {r.address.city}</span>
                                                    )}
                                                </div>
                                            </div>
                                        </div>
                                    </Link>
                                </motion.div>
                            );})}
                        </AnimatePresence>
                    </motion.div>
                )}
            </section>

            {/* Footer */}
            <footer className="home-footer">
                © 2026 Flocko · Built for learning, designed with care.
            </footer>
        </div>
    );
};

export default HomePage;

