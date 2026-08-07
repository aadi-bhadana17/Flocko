import { useEffect, useMemo, useState } from 'react';
import { getUserProfile } from '../../api/userService';
import { initiateDeposit } from '../../api/walletService';
import { Wallet, Loader2 } from 'lucide-react';
import { motion } from 'framer-motion';
import { toast } from '../ui/Toast';

const MIN_DEPOSIT = 100;
const MAX_DEPOSIT = 5000;
const QUICK_AMOUNTS = [100, 200, 500, 1000, 2000, 5000];

const formatCurrency = (value) => {
    const numeric = Number(value ?? 0);
    if (Number.isNaN(numeric)) return '₹0.00';
    return `₹${numeric.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
};

const WalletPage = () => {
    const [walletBalance, setWalletBalance] = useState(0);
    const [amount, setAmount] = useState('100');
    const [loadingBalance, setLoadingBalance] = useState(true);
    const [depositing, setDepositing] = useState(false);
    const [error, setError] = useState('');

    const parsedAmount = useMemo(() => Number(amount), [amount]);

    const fetchWalletBalance = async () => {
        setLoadingBalance(true);
        setError('');
        try {
            const profile = await getUserProfile();
            setWalletBalance(profile?.walletBalance ?? 0);
        } catch {
            setError('Unable to load wallet balance. Please refresh and try again.');
            toast.error('Unable to load wallet balance.');
        } finally {
            setLoadingBalance(false);
        }
    };

    useEffect(() => {
        fetchWalletBalance();
    }, []);

    const handleDeposit = async () => {
        setError('');

        if (Number.isNaN(parsedAmount)) {
            setError('Please enter a valid amount.');
            toast.warning('Please enter a valid amount.');
            return;
        }

        if (parsedAmount < MIN_DEPOSIT || parsedAmount > MAX_DEPOSIT) {
            const msg = `Amount must be between ₹${MIN_DEPOSIT} and ₹${MAX_DEPOSIT}.`;
            setError(msg);
            toast.warning(msg);
            return;
        }

        setDepositing(true);
        try {
            const approvalUrl = await initiateDeposit(parsedAmount);
            if (!approvalUrl || !approvalUrl.startsWith('http')) {
                setError('Unable to initiate PayPal deposit right now. Please try again.');
                toast.error('Unable to initiate deposit. Please try again.');
                setDepositing(false);
                return;
            }
            toast.info('Redirecting to PayPal…');
            // Small delay so the user sees the toast before redirect
            setTimeout(() => {
                window.location.href = approvalUrl;
            }, 600);
        } catch (err) {
            const backendMsg = err?.response?.data?.message || err?.response?.data || '';
            const errorMsg = backendMsg || 'Unable to initiate PayPal deposit right now. Please try again.';
            setError(errorMsg);
            toast.error(errorMsg);
            setDepositing(false);
        }
    };

    return (
        <div className="mx-auto max-w-2xl px-4 py-10">
            <motion.div
                className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-700 dark:bg-slate-800"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ type: 'spring', stiffness: 300, damping: 25 }}
            >
                <div className="flex items-center gap-3">
                    <Wallet className="h-7 w-7 text-emerald-600 dark:text-emerald-400" />
                    <h1 className="text-2xl font-semibold text-slate-900 dark:text-slate-100">Wallet</h1>
                </div>
                <p className="mt-2 text-sm text-slate-600 dark:text-slate-300">Add funds securely using PayPal.</p>

                {/* Balance Card */}
                <motion.div
                    className="mt-6 rounded-xl bg-slate-50 p-4 dark:bg-slate-900/40"
                    initial={{ opacity: 0, scale: 0.97 }}
                    animate={{ opacity: 1, scale: 1 }}
                    transition={{ delay: 0.1 }}
                >
                    <p className="text-sm text-slate-500 dark:text-slate-400">Current balance</p>
                    <p className="mt-1 text-3xl font-bold text-emerald-600 dark:text-emerald-400">
                        {loadingBalance ? (
                            <span className="inline-flex items-center gap-2">
                                <Loader2 size={24} className="animate-spin" />
                                Loading…
                            </span>
                        ) : (
                            formatCurrency(walletBalance)
                        )}
                    </p>
                </motion.div>

                {/* Quick Amount Buttons */}
                <div className="mt-6">
                    <label className="block text-sm font-medium text-slate-700 dark:text-slate-200 mb-2">
                        Quick Select
                    </label>
                    <div className="flex flex-wrap gap-2">
                        {QUICK_AMOUNTS.map((qa) => (
                            <button
                                key={qa}
                                type="button"
                                onClick={() => setAmount(String(qa))}
                                disabled={depositing}
                                className={`rounded-lg border px-3 py-1.5 text-sm font-medium transition
                                    ${parsedAmount === qa
                                        ? 'border-emerald-500 bg-emerald-50 text-emerald-700 dark:border-emerald-600 dark:bg-emerald-900/30 dark:text-emerald-300'
                                        : 'border-slate-200 bg-white text-slate-600 hover:border-emerald-300 hover:bg-emerald-50 dark:border-slate-600 dark:bg-slate-700 dark:text-slate-300 dark:hover:border-emerald-700 dark:hover:bg-emerald-900/20'
                                    }
                                    disabled:cursor-not-allowed disabled:opacity-50`}
                            >
                                ₹{qa}
                            </button>
                        ))}
                    </div>
                </div>

                {/* Amount Input */}
                <div className="mt-4 space-y-2">
                    <label className="block text-sm font-medium text-slate-700 dark:text-slate-200" htmlFor="depositAmount">
                        Or enter custom amount
                    </label>
                    <div className="relative">
                        <span className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 font-medium">₹</span>
                        <input
                            id="depositAmount"
                            type="number"
                            min={MIN_DEPOSIT}
                            max={MAX_DEPOSIT}
                            value={amount}
                            onChange={(event) => setAmount(event.target.value)}
                            disabled={depositing}
                            className="w-full rounded-lg border border-slate-300 bg-white pl-8 pr-4 py-2 text-slate-900 outline-none focus:border-emerald-500 focus:ring-2 focus:ring-emerald-200 dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100 dark:focus:ring-emerald-700 disabled:cursor-not-allowed disabled:opacity-60"
                        />
                    </div>
                    <p className="text-xs text-slate-500 dark:text-slate-400">
                        Allowed range: ₹{MIN_DEPOSIT} – ₹{MAX_DEPOSIT}
                    </p>
                </div>

                {/* Inline Error */}
                {error && (
                    <motion.div
                        className="mt-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700 dark:border-red-800 dark:bg-red-900/30 dark:text-red-300"
                        initial={{ opacity: 0, y: -5 }}
                        animate={{ opacity: 1, y: 0 }}
                    >
                        {error}
                    </motion.div>
                )}

                {/* Deposit Button */}
                <motion.button
                    type="button"
                    onClick={handleDeposit}
                    disabled={depositing || loadingBalance}
                    className="mt-6 inline-flex w-full items-center justify-center gap-2 rounded-lg bg-emerald-600 px-4 py-2.5 font-medium text-white transition hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-60"
                    whileHover={!depositing && !loadingBalance ? { scale: 1.01 } : {}}
                    whileTap={!depositing && !loadingBalance ? { scale: 0.99 } : {}}
                >
                    {depositing ? (
                        <>
                            <Loader2 size={18} className="animate-spin" />
                            Redirecting to PayPal…
                        </>
                    ) : (
                        'Deposit with PayPal'
                    )}
                </motion.button>
            </motion.div>
        </div>
    );
};

export default WalletPage;
