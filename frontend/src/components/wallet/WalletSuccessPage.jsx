import { useEffect, useState, useCallback } from 'react';
import { CheckCircle, ArrowRight, Wallet } from 'lucide-react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { motion } from 'framer-motion';
import { toast } from '../ui/Toast';

const formatBalance = (value) => {
    const numeric = Number(value ?? 0);
    if (Number.isNaN(numeric)) return '0.00';
    return numeric.toLocaleString('en-IN', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
    });
};

const REDIRECT_DELAY = 4000; // ms before auto-redirect

const WalletSuccessPage = () => {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const [countdown, setCountdown] = useState(Math.ceil(REDIRECT_DELAY / 1000));

    const amount = searchParams.get('amount');
    const balance = searchParams.get('balance');

    // Show toast on mount
    useEffect(() => {
        if (amount && balance) {
            toast.success(
                `₹${formatBalance(amount)} deposited successfully to your wallet. Balance: ₹${formatBalance(balance)}`
            );
        } else {
            toast.success('Wallet topped up successfully!');
        }
    }, [amount, balance]);

    // Auto-redirect countdown
    useEffect(() => {
        const timer = setTimeout(() => {
            navigate('/wallet', { replace: true });
        }, REDIRECT_DELAY);

        const interval = setInterval(() => {
            setCountdown((prev) => (prev > 1 ? prev - 1 : 0));
        }, 1000);

        return () => {
            clearTimeout(timer);
            clearInterval(interval);
        };
    }, [navigate]);

    const handleGoNow = useCallback(() => {
        navigate('/wallet', { replace: true });
    }, [navigate]);

    return (
        <div className="mx-auto flex min-h-[75vh] max-w-xl items-center justify-center px-4 py-10">
            <motion.div
                className="w-full rounded-2xl border border-emerald-200 bg-white p-8 text-center shadow-sm dark:border-emerald-900 dark:bg-slate-800"
                initial={{ opacity: 0, y: 30, scale: 0.97 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                transition={{ type: 'spring', stiffness: 300, damping: 25 }}
            >
                {/* Animated checkmark */}
                <motion.div
                    initial={{ scale: 0, rotate: -45 }}
                    animate={{ scale: 1, rotate: 0 }}
                    transition={{ type: 'spring', stiffness: 260, damping: 20, delay: 0.15 }}
                >
                    <CheckCircle className="mx-auto h-20 w-20 text-emerald-500" strokeWidth={1.8} />
                </motion.div>

                <motion.h1
                    className="mt-4 text-3xl font-bold text-slate-900 dark:text-slate-100"
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: 0.25 }}
                >
                    Payment Successful!
                </motion.h1>

                <motion.p
                    className="mt-2 text-slate-600 dark:text-slate-300"
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    transition={{ delay: 0.35 }}
                >
                    Your wallet has been topped up successfully
                </motion.p>

                {/* Amount deposited */}
                {amount && (
                    <motion.div
                        className="mt-5 inline-flex items-center gap-2 rounded-lg bg-emerald-50 px-4 py-2 dark:bg-emerald-900/20"
                        initial={{ opacity: 0, scale: 0.9 }}
                        animate={{ opacity: 1, scale: 1 }}
                        transition={{ delay: 0.4 }}
                    >
                        <Wallet size={18} className="text-emerald-600 dark:text-emerald-400" />
                        <span className="text-lg font-semibold text-emerald-700 dark:text-emerald-300">
                            +₹{formatBalance(amount)}
                        </span>
                    </motion.div>
                )}

                {/* New balance */}
                {balance && (
                    <motion.p
                        className="mt-3 text-lg font-semibold text-emerald-600 dark:text-emerald-400"
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        transition={{ delay: 0.5 }}
                    >
                        New Balance: ₹{formatBalance(balance)}
                    </motion.p>
                )}

                {/* CTA button with countdown */}
                <motion.button
                    type="button"
                    onClick={handleGoNow}
                    className="mt-7 inline-flex items-center justify-center gap-2 rounded-lg bg-emerald-600 px-5 py-2.5 font-medium text-white transition hover:bg-emerald-700"
                    whileHover={{ scale: 1.03 }}
                    whileTap={{ scale: 0.97 }}
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: 0.55 }}
                >
                    Go to Wallet
                    <ArrowRight size={16} />
                </motion.button>

                <motion.p
                    className="mt-3 text-xs text-slate-400 dark:text-slate-500"
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    transition={{ delay: 0.65 }}
                >
                    Redirecting in {countdown}s…
                </motion.p>
            </motion.div>
        </div>
    );
};

export default WalletSuccessPage;
