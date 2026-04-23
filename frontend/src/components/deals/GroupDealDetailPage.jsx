import React, { useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { motion } from 'framer-motion';
import { getAddresses } from '../../api/userService';
import {
    getGroupDealDetail,
    getGroupDealParticipationsByDeal,
    getGroupDealParticipationsByUser,
    participateInGroupDeal,
    withdrawFromGroupDeal,
} from '../../api/groupDealService';
import { AuthContext } from '../../context/AuthContext';
import { ROLES } from '../../utils/constants';
import './GroupDealDetailPage.css';

const STATUS_LABELS = {
    CREATED: 'Scheduled',
    VOTING: 'Voting',
    CONFIRMATION_WINDOW: 'Confirmation Window',
    FULFILLED: 'Fulfilled',
    EXPIRED: 'Expired',
};

const statusClass = (status) => {
    if (status === 'CREATED') return 'gdd-status-created';
    if (status === 'VOTING') return 'gdd-status-voting';
    if (status === 'CONFIRMATION_WINDOW') return 'gdd-status-confirmation';
    if (status === 'FULFILLED') return 'gdd-status-fulfilled';
    return 'gdd-status-expired';
};

const formatMoney = (value) => `Rs ${Number(value || 0).toFixed(2)}`;

const extractErrorMessage = (err, fallback) => {
    const payload = err?.response?.data;
    if (typeof payload === 'string') return payload;
    return payload?.message || fallback;
};

const extractAddressId = (address) => {
    return address?.addressId ?? address?.id ?? address?.userAddressId ?? null;
};

const didUserParticipate = (deal) => {
    const direct = deal?.hasParticipated ?? deal?.participated ?? deal?.isParticipating;
    if (typeof direct === 'boolean') return direct;

    const quantity = Number(
        deal?.myParticipationQuantity
        ?? deal?.participationQuantity
        ?? deal?.userParticipationQuantity
        ?? 0
    );

    return quantity > 0;
};

const getCountdownTarget = (deal) => {
    if (!deal) return null;
    if (deal.status === 'VOTING') return deal.endTime;
    if (deal.status === 'CONFIRMATION_WINDOW') return deal.confirmationWindowEndTime;
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

const GroupDealDetailPage = () => {
    const { restaurantId, dealId } = useParams();
    const { user } = useContext(AuthContext);

    const [deal, setDeal] = useState(null);
    const [addresses, setAddresses] = useState([]);
    const [selectedAddressId, setSelectedAddressId] = useState('');
    const [quantity, setQuantity] = useState(1);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [toast, setToast] = useState('');
    const [actionBusy, setActionBusy] = useState(false);
    const [hasParticipated, setHasParticipated] = useState(false);
    const [nowMs, setNowMs] = useState(Date.now());
    const [participations, setParticipations] = useState([]);
    const [participationsLoading, setParticipationsLoading] = useState(false);
    const [participationsError, setParticipationsError] = useState('');

    const isCustomer = user?.role === ROLES.CUSTOMER;
    const isOwner = user?.role === ROLES.RESTAURANT_OWNER;

    const activeParticipation = useMemo(() => {
        if (!participations.length) return null;
        const confirmed = participations.filter((item) => (item?.isConfirmed ?? item?.confirmed) !== false);
        if (!confirmed.length) return null;
        return [...confirmed].sort((a, b) => Number(b?.participantId || 0) - Number(a?.participantId || 0))[0];
    }, [participations]);

    const fetchDeal = useCallback(async () => {
        setLoading(true);
        setError('');

        try {
            const data = await getGroupDealDetail(restaurantId, dealId);
            setDeal(data);
            setHasParticipated(didUserParticipate(data));
        } catch (err) {
            setError(extractErrorMessage(err, 'Unable to load this deal.'));
        } finally {
            setLoading(false);
        }
    }, [restaurantId, dealId]);

    useEffect(() => {
        fetchDeal();
    }, [fetchDeal]);

    useEffect(() => {
        const interval = setInterval(() => setNowMs(Date.now()), 1000);
        return () => clearInterval(interval);
    }, []);

    useEffect(() => {
        if (!isCustomer) {
            setAddresses([]);
            setSelectedAddressId('');
            return;
        }

        getAddresses()
            .then((data) => {
                const list = Array.isArray(data) ? data : [];
                setAddresses(list);
                const defaultAddress = list.find((address) => address.defaultAddress || address.isDefault);
                const firstAddress = list[0];
                setSelectedAddressId(
                    extractAddressId(defaultAddress) || extractAddressId(firstAddress) || ''
                );
            })
            .catch(() => {
                setAddresses([]);
                setSelectedAddressId('');
            });
    }, [isCustomer]);

    useEffect(() => {
        if (!isOwner && !isCustomer) {
            setParticipations([]);
            setParticipationsError('');
            setParticipationsLoading(false);
            return;
        }

        const fetchParticipations = async () => {
            setParticipationsLoading(true);
            setParticipationsError('');
            try {
                const payload = isOwner
                    ? await getGroupDealParticipationsByDeal(restaurantId, dealId)
                    : await getGroupDealParticipationsByUser(restaurantId, dealId);
                setParticipations(Array.isArray(payload) ? payload : []);
            } catch (err) {
                setParticipations([]);
                const msg = extractErrorMessage(err, 'Unable to load participants.');
                if (isCustomer && /didn't participate/i.test(msg)) {
                    setParticipationsError('');
                    return;
                }
                setParticipationsError(msg);
            } finally {
                setParticipationsLoading(false);
            }
        };

        fetchParticipations();
    }, [isOwner, isCustomer, restaurantId, dealId]);

    useEffect(() => {
        if (!isCustomer) return;
        setHasParticipated(Boolean(activeParticipation));
    }, [activeParticipation, isCustomer]);

    const progressPercent = useMemo(() => {
        if (!deal?.targetParticipation) return 0;
        const raw = (Number(deal.currentParticipation || 0) / Number(deal.targetParticipation)) * 100;
        return Math.max(0, Math.min(100, raw));
    }, [deal]);

    const countdownTarget = getCountdownTarget(deal);
    const countdownText = formatCountdown(countdownTarget, nowMs);

    const handleParticipate = async (e) => {
        e.preventDefault();

        if (!isCustomer) {
            setToast('Only customers can participate in a deal.');
            return;
        }

        if (!selectedAddressId) {
            setToast('Please select a delivery address before payment.');
            return;
        }

        if (Number(quantity) <= 0) {
            setToast('Quantity must be at least 1.');
            return;
        }

        setActionBusy(true);
        setToast('');

        try {
            await participateInGroupDeal(restaurantId, dealId, {
                quantity: Number(quantity),
                addressId: Number(selectedAddressId),
            });
            setHasParticipated(true);
            setToast('Payment mocked and participation confirmed.');
            await Promise.all([
                fetchDeal(),
                getGroupDealParticipationsByUser(restaurantId, dealId).then((payload) => {
                    setParticipations(Array.isArray(payload) ? payload : []);
                    setParticipationsError('');
                }).catch((err) => {
                    const msg = extractErrorMessage(err, 'Unable to load participants.');
                    if (/didn't participate/i.test(msg)) {
                        setParticipations([]);
                        setParticipationsError('');
                        return;
                    }
                    setParticipationsError(msg);
                }),
            ]);
        } catch (err) {
            setToast(extractErrorMessage(err, 'Unable to participate right now.'));
        } finally {
            setActionBusy(false);
        }
    };

    const handleWithdraw = async () => {
        if (!activeParticipation?.participantId) {
            setToast('No active participation found to withdraw.');
            return;
        }

        setActionBusy(true);
        setToast('');

        try {
            await withdrawFromGroupDeal(restaurantId, dealId, activeParticipation.participantId);
            setHasParticipated(false);
            setToast('You have withdrawn from this group deal.');
            await Promise.all([
                fetchDeal(),
                getGroupDealParticipationsByUser(restaurantId, dealId).then((payload) => {
                    setParticipations(Array.isArray(payload) ? payload : []);
                    setParticipationsError('');
                }).catch((err) => {
                    const msg = extractErrorMessage(err, 'Unable to load participants.');
                    if (/didn't participate/i.test(msg)) {
                        setParticipations([]);
                        setParticipationsError('');
                        return;
                    }
                    setParticipationsError(msg);
                }),
            ]);
        } catch (err) {
            setToast(extractErrorMessage(err, 'Unable to withdraw right now.'));
        } finally {
            setActionBusy(false);
        }
    };

    if (loading) {
        return (
            <div className="gdd-page">
                <div className="gdd-container gdd-loading">Loading group deal...</div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="gdd-page">
                <div className="gdd-container">
                    <div className="gdd-error">{error}</div>
                    <Link className="gdd-back" to={`/restaurant/${restaurantId}`}>Back to Restaurant</Link>
                </div>
            </div>
        );
    }

    return (
        <div className="gdd-page">
            <div className="gdd-container">
                <Link className="gdd-back" to={`/restaurant/${restaurantId}`}>Back to Restaurant</Link>

                <motion.div
                    className="gdd-hero"
                    initial={{ opacity: 0, y: 12 }}
                    animate={{ opacity: 1, y: 0 }}
                >
                    <div className="gdd-hero-top">
                        <div>
                            <h1>{deal?.dealName || 'Group Deal'}</h1>
                            <p>{deal?.restaurant?.restaurantName || deal?.restaurant?.name || 'Restaurant'}</p>
                        </div>
                        <span className={`gdd-status ${statusClass(deal?.status)}`}>
                            {STATUS_LABELS[deal?.status] || deal?.status || 'Unknown'}
                        </span>
                    </div>

                    <div className="gdd-price-row">
                        <div>
                            <span className="gdd-label">Original</span>
                            <strong>{formatMoney(deal?.originalPrice)}</strong>
                        </div>
                        <div>
                            <span className="gdd-label">Current Deal Price</span>
                            <strong>{formatMoney(deal?.currentPrice)}</strong>
                        </div>
                        <div>
                            <span className="gdd-label">Timer</span>
                            <strong>{countdownText}</strong>
                        </div>
                    </div>

                    <div className="gdd-progress-wrap">
                        <div className="gdd-progress-meta">
                            <span>
                                Participants {Number(deal?.currentParticipation || 0)} / {Number(deal?.targetParticipation || 0)}
                            </span>
                            <span>{progressPercent.toFixed(0)}%</span>
                        </div>
                        <div className="gdd-progress-track">
                            <div className="gdd-progress-fill" style={{ width: `${progressPercent}%` }} />
                        </div>
                    </div>
                </motion.div>

                {toast && <div className="gdd-toast">{toast}</div>}

                <div className="gdd-grid">
                    <section className="gdd-card">
                        <h2>Food Bundle</h2>
                        {deal?.foodList?.length ? (
                            <ul className="gdd-list">
                                {deal.foodList.map((food, index) => (
                                    <li key={food?.foodId ?? food?.id ?? `${food?.foodName}-${index}`}>
                                        <span>{food?.foodName || food?.name || `Food #${food?.foodId ?? food?.id}`}</span>
                                        {food?.quantity ? <strong>x{food.quantity}</strong> : null}
                                    </li>
                                ))}
                            </ul>
                        ) : (
                            <p className="gdd-muted">No bundle details available.</p>
                        )}
                    </section>

                    <section className="gdd-card">
                        <h2>Discount Tiers</h2>
                        {deal?.discountList?.length ? (
                            <table className="gdd-table">
                                <thead>
                                    <tr>
                                        <th>Threshold</th>
                                        <th>Discount</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {deal.discountList.map((tier, index) => (
                                        <tr key={`${tier.thresholdPercent}-${tier.discountPercent}-${index}`}>
                                            <td>{tier.thresholdPercent}%</td>
                                            <td>{tier.discountPercent}%</td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        ) : (
                            <p className="gdd-muted">No discount tiers configured.</p>
                        )}
                    </section>
                </div>

                <section className="gdd-card gdd-actions-card">
                    <h2>Participation</h2>

                    {deal?.status === 'VOTING' && (
                        <form className="gdd-form" onSubmit={handleParticipate}>
                            <label>
                                Quantity
                                <input
                                    type="number"
                                    min="1"
                                    value={quantity}
                                    onChange={(e) => setQuantity(Math.max(1, Number(e.target.value || 1)))}
                                />
                            </label>

                            <label>
                                Delivery Address
                                <select
                                    value={selectedAddressId}
                                    onChange={(e) => setSelectedAddressId(e.target.value)}
                                    disabled={!addresses.length}
                                >
                                    <option value="">Select address</option>
                                    {addresses.map((address) => {
                                        const id = extractAddressId(address);
                                        if (!id) return null;

                                        return (
                                            <option key={id} value={id}>
                                                {`${address.buildingNo || ''} ${address.street || ''}, ${address.city || ''} ${address.pincode || ''}`.trim()}
                                            </option>
                                        );
                                    })}
                                </select>
                            </label>

                            <button
                                type="submit"
                                className="gdd-primary"
                                disabled={actionBusy || !isCustomer || !addresses.length}
                                title={!isCustomer ? 'Only customers can participate.' : ''}
                            >
                                {actionBusy ? 'Processing...' : 'Pay Now (Mocked)'}
                            </button>
                        </form>
                    )}

                    {deal?.status === 'CONFIRMATION_WINDOW' && hasParticipated && (
                        <button className="gdd-danger" onClick={handleWithdraw} disabled={actionBusy}>
                            {actionBusy ? 'Processing...' : 'Withdraw Participation'}
                        </button>
                    )}

                    {deal?.status === 'CONFIRMATION_WINDOW' && !hasParticipated && (
                        <p className="gdd-muted">Withdrawal is available only for customers who already participated.</p>
                    )}

                    {deal?.status === 'FULFILLED' && (
                        <p className="gdd-success">Deal fulfilled. Orders are being auto-placed for confirmed participants.</p>
                    )}

                    {deal?.status === 'CREATED' && (
                        <p className="gdd-muted">This deal is scheduled and hasn't started voting yet. Voting will begin at the start time.</p>
                    )}

                    {deal?.status === 'EXPIRED' && (
                        <p className="gdd-muted">This deal expired before reaching confirmation requirements.</p>
                    )}
                </section>

                {(isOwner || isCustomer) && (
                    <section className="gdd-card gdd-actions-card">
                        <h2>{isOwner ? 'Participants' : 'Your Participation'}</h2>

                        {participationsError && <p className="gdd-muted">{participationsError}</p>}

                        {participationsLoading ? (
                            <p className="gdd-muted">Loading participants...</p>
                        ) : participations.length === 0 ? (
                            <p className="gdd-muted">No participants yet for this deal.</p>
                        ) : (
                            <table className="gdd-table">
                                <thead>
                                    <tr>
                                        <th>User</th>
                                        <th>Quantity</th>
                                        <th>Amount Paid</th>
                                        <th>Payment</th>
                                        <th>Confirmed</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {participations.map((participant, index) => (
                                        <tr key={participant.participantId ?? participant.userId ?? index}>
                                            <td>
                                                {participant.participantName || participant.userId || 'User'}
                                                {participant.participantEmail ? ` (${participant.participantEmail})` : ''}
                                            </td>
                                            <td>{participant.quantity}</td>
                                            <td>{formatMoney(participant.amountPaid)}</td>
                                            <td>{participant.paymentStatus || 'N/A'}</td>
                                            <td>{(participant.isConfirmed ?? participant.confirmed) ? 'Yes' : 'No'}</td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        )}
                    </section>
                )}
            </div>
        </div>
    );
};

export default GroupDealDetailPage;



