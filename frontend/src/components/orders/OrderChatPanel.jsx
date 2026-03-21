import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { getOrderChatMessages, sendOrderChatMessage } from '../../api/chatService';
import { getUserProfile } from '../../api/userService';
import './OrderChatPanel.css';

const normalizeMessages = (payload) => (Array.isArray(payload) ? payload : []);

const getSenderLabel = (sender) => {
    if (!sender) return 'User';
    return sender.userName || sender.name || sender.firstName || sender.email || 'User';
};

const normalize = (value) => String(value || '').trim().toLowerCase();

const CUSTOMER_ROLE = 'CUSTOMER';
const STAFF_ROLE = 'RESTAURANT_STAFF';

const OrderChatPanel = ({
    orderId,
    canSend = true,
    sendDisabledReason = '',
    pollMs = 4000,
    title = 'Special Order Chat',
}) => {
    const [messages, setMessages] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [message, setMessage] = useState('');
    const [sending, setSending] = useState(false);
    const [currentUser, setCurrentUser] = useState({ fullName: '', firstName: '', role: '' });

    const listRef = useRef(null);

    const fetchMessages = useCallback(async (silent = false) => {
        if (!orderId) return;
        if (!silent) setLoading(true);

        try {
            const data = await getOrderChatMessages(orderId);
            setMessages(normalizeMessages(data));
            setError('');
        } catch (err) {
            if (!silent) {
                const msg = err?.response?.data?.message || err?.response?.data || 'Unable to load chat messages.';
                setError(typeof msg === 'string' ? msg : 'Unable to load chat messages.');
            }
        } finally {
            if (!silent) setLoading(false);
        }
    }, [orderId]);

    useEffect(() => {
        fetchMessages();
    }, [fetchMessages]);

    useEffect(() => {
        const fetchCurrentUser = async () => {
            try {
                const profile = await getUserProfile();
                setCurrentUser({
                    fullName: `${profile?.firstName || ''} ${profile?.lastName || ''}`.trim(),
                    firstName: profile?.firstName || '',
                    role: profile?.role || '',
                });
            } catch {
                const stored = JSON.parse(localStorage.getItem('user') || '{}');
                setCurrentUser({
                    fullName: stored?.firstName || '',
                    firstName: stored?.firstName || '',
                    role: stored?.role || '',
                });
            }
        };

        fetchCurrentUser();
    }, []);

    useEffect(() => {
        if (!orderId) return undefined;

        const intervalId = window.setInterval(() => {
            if (!document.hidden) fetchMessages(true);
        }, pollMs);

        return () => window.clearInterval(intervalId);
    }, [orderId, fetchMessages, pollMs]);

    useEffect(() => {
        if (listRef.current) {
            listRef.current.scrollTop = listRef.current.scrollHeight;
        }
    }, [messages.length]);

    const handleSend = async (e) => {
        e.preventDefault();
        const trimmed = message.trim();
        if (!trimmed || !canSend || sending) return;

        setSending(true);
        try {
            const created = await sendOrderChatMessage(orderId, trimmed);
            setMessages((prev) => [...prev, created]);
            setMessage('');
            setError('');
        } catch (err) {
            const msg = err?.response?.data?.message || err?.response?.data || 'Unable to send message.';
            setError(typeof msg === 'string' ? msg : 'Unable to send message.');
        } finally {
            setSending(false);
        }
    };

    const emptyMessageText = useMemo(() => {
        if (loading) return 'Loading messages...';
        return 'No chat messages yet. Start the conversation below.';
    }, [loading]);

    const isOwnMessage = useCallback((item) => {
        const senderName = normalize(getSenderLabel(item?.sender));
        const fullName = normalize(currentUser.fullName);
        const firstName = normalize(currentUser.firstName);

        if (fullName && senderName === fullName) return true;
        return Boolean(firstName) && senderName.startsWith(firstName);
    }, [currentUser.firstName, currentUser.fullName]);

    const senderRole = useCallback((item) => {
        if (isOwnMessage(item)) {
            return currentUser.role === CUSTOMER_ROLE ? CUSTOMER_ROLE : STAFF_ROLE;
        }

        if (currentUser.role === CUSTOMER_ROLE) return STAFF_ROLE;
        if (currentUser.role === STAFF_ROLE) return CUSTOMER_ROLE;
        return '';
    }, [currentUser.role, isOwnMessage]);

    const senderRoleLabel = useCallback((item) => {
        const role = senderRole(item);
        if (role === CUSTOMER_ROLE) return 'Customer';
        if (role === STAFF_ROLE) return 'Staff';
        return 'User';
    }, [senderRole]);

    const bubbleClassName = useCallback((item) => {
        const role = senderRole(item);
        if (role === STAFF_ROLE) return 'ocp-message-staff';
        return 'ocp-message-customer';
    }, [senderRole]);

    return (
        <div className="ocp-card">
            <div className="ocp-header">
                <h3>{title}</h3>
                <button className="ocp-refresh" onClick={() => fetchMessages()} disabled={loading || sending}>
                    Refresh
                </button>
            </div>

            {error && <div className="ocp-error">{error}</div>}

            <div className="ocp-messages" ref={listRef}>
                {messages.length === 0 ? (
                    <div className="ocp-empty">{emptyMessageText}</div>
                ) : (
                    messages.map((item) => (
                        <div className={`ocp-message ${bubbleClassName(item)}`} key={item.messageId}>
                            <div className="ocp-meta">
                                <span className="ocp-sender">
                                    {getSenderLabel(item.sender)} <span className="ocp-role">({senderRoleLabel(item)})</span>
                                </span>
                                <span className="ocp-time">
                                    {item.timestamp ? new Date(item.timestamp).toLocaleString('en-IN', {
                                        day: 'numeric',
                                        month: 'short',
                                        hour: '2-digit',
                                        minute: '2-digit',
                                    }) : ''}
                                </span>
                            </div>
                            <p>{item.message}</p>
                        </div>
                    ))
                )}
            </div>

            <form className="ocp-form" onSubmit={handleSend}>
                <textarea
                    className="ocp-input"
                    rows={3}
                    value={message}
                    onChange={(e) => setMessage(e.target.value)}
                    placeholder="Type your message..."
                    maxLength={500}
                    disabled={!canSend || sending}
                />
                {!canSend && sendDisabledReason && <div className="ocp-hint">{sendDisabledReason}</div>}
                <div className="ocp-actions">
                    <span className="ocp-count">{message.trim().length}/500</span>
                    <button className="ocp-send" type="submit" disabled={!canSend || sending || !message.trim()}>
                        {sending ? 'Sending...' : 'Send'}
                    </button>
                </div>
            </form>
        </div>
    );
};

export default OrderChatPanel;

