import { useEffect, useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { CheckCircle, XCircle, AlertTriangle, Info, X } from 'lucide-react';
import './Toast.css';

const ICON_MAP = {
    success: CheckCircle,
    error: XCircle,
    warning: AlertTriangle,
    info: Info,
};

const Toast = ({ message, type = 'success', duration = 3000, onClose, visible }) => {
    const [show, setShow] = useState(visible);
    const Icon = ICON_MAP[type] || Info;

    useEffect(() => {
        setShow(visible);
    }, [visible]);

    useEffect(() => {
        if (!show || duration <= 0) return;
        const timer = setTimeout(() => {
            setShow(false);
            onClose?.();
        }, duration);
        return () => clearTimeout(timer);
    }, [show, duration, onClose]);

    const handleClose = () => {
        setShow(false);
        onClose?.();
    };

    return (
        <AnimatePresence>
            {show && (
                <motion.div
                    className={`toast toast--${type}`}
                    initial={{ opacity: 0, y: -40, scale: 0.95 }}
                    animate={{ opacity: 1, y: 0, scale: 1 }}
                    exit={{ opacity: 0, y: -30, scale: 0.95 }}
                    transition={{ type: 'spring', stiffness: 400, damping: 28 }}
                    role="alert"
                    aria-live="assertive"
                >
                    <div className="toast__icon">
                        <Icon size={20} strokeWidth={2.2} />
                    </div>
                    <p className="toast__message">{message}</p>
                    <button
                        className="toast__close"
                        onClick={handleClose}
                        aria-label="Close notification"
                        type="button"
                    >
                        <X size={16} strokeWidth={2.5} />
                    </button>
                </motion.div>
            )}
        </AnimatePresence>
    );
};

/* ── Imperative Toast Manager ─────────────────────────────── */
let _addToast = () => {};

export const toast = {
    success: (message, duration) => _addToast({ message, type: 'success', duration }),
    error: (message, duration) => _addToast({ message, type: 'error', duration }),
    warning: (message, duration) => _addToast({ message, type: 'warning', duration }),
    info: (message, duration) => _addToast({ message, type: 'info', duration }),
};

export const ToastContainer = () => {
    const [toasts, setToasts] = useState([]);

    useEffect(() => {
        _addToast = ({ message, type = 'success', duration = 3000 }) => {
            const id = Date.now() + Math.random();
            setToasts((prev) => [...prev, { id, message, type, duration }]);
        };
        return () => { _addToast = () => {}; };
    }, []);

    const removeToast = (id) => {
        setToasts((prev) => prev.filter((t) => t.id !== id));
    };

    return (
        <div className="toast-container" aria-label="Notifications">
            <AnimatePresence mode="popLayout">
                {toasts.map((t) => (
                    <motion.div
                        key={t.id}
                        className={`toast toast--${t.type}`}
                        initial={{ opacity: 0, y: -40, scale: 0.95 }}
                        animate={{ opacity: 1, y: 0, scale: 1 }}
                        exit={{ opacity: 0, x: 80, scale: 0.9 }}
                        transition={{ type: 'spring', stiffness: 400, damping: 28 }}
                        layout
                        role="alert"
                        aria-live="assertive"
                    >
                        <div className="toast__icon">
                            {(() => {
                                const Icon = ICON_MAP[t.type] || Info;
                                return <Icon size={20} strokeWidth={2.2} />;
                            })()}
                        </div>
                        <p className="toast__message">{t.message}</p>
                        <button
                            className="toast__close"
                            onClick={() => removeToast(t.id)}
                            aria-label="Close notification"
                            type="button"
                        >
                            <X size={16} strokeWidth={2.5} />
                        </button>
                    </motion.div>
                ))}
            </AnimatePresence>
        </div>
    );
};

export default Toast;
