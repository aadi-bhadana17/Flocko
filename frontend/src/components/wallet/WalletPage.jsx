import { useEffect, useMemo, useState } from 'react';
import { getUserProfile } from '../../api/userService';
import { initiateDeposit } from '../../api/walletService';

const MIN_DEPOSIT = 100;
const MAX_DEPOSIT = 5000;

const formatCurrency = (value) => {
    const numeric = Number(value ?? 0);
    if (Number.isNaN(numeric)) return 'Rs 0.00';
    return `Rs ${numeric.toFixed(2)}`;
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
            return;
        }

        if (parsedAmount < MIN_DEPOSIT || parsedAmount > MAX_DEPOSIT) {
            setError(`Amount must be between ${MIN_DEPOSIT} and ${MAX_DEPOSIT}.`);
            return;
        }

        setDepositing(true);
        try {
            const approvalUrl = await initiateDeposit(parsedAmount);
            if (!approvalUrl || !approvalUrl.startsWith('http')) {
                setError('Unable to initiate PayPal deposit right now. Please try again.');
                setDepositing(false);
                return;
            }
            window.location.href = approvalUrl;
        } catch {
            setError('Unable to initiate PayPal deposit right now. Please try again.');
            setDepositing(false);
        }
    };

    return (
        <div className="mx-auto max-w-2xl px-4 py-10">
            <div className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm dark:border-slate-700 dark:bg-slate-800">
                <h1 className="text-2xl font-semibold text-slate-900 dark:text-slate-100">Wallet</h1>
                <p className="mt-2 text-sm text-slate-600 dark:text-slate-300">Add funds securely using PayPal.</p>

                <div className="mt-6 rounded-xl bg-slate-50 p-4 dark:bg-slate-900/40">
                    <p className="text-sm text-slate-500 dark:text-slate-400">Current balance</p>
                    <p className="mt-1 text-3xl font-bold text-emerald-600 dark:text-emerald-400">
                        {loadingBalance ? 'Loading...' : formatCurrency(walletBalance)}
                    </p>
                </div>

                <div className="mt-6 space-y-3">
                    <label className="block text-sm font-medium text-slate-700 dark:text-slate-200" htmlFor="depositAmount">
                        Deposit Amount
                    </label>
                    <input
                        id="depositAmount"
                        type="number"
                        min={MIN_DEPOSIT}
                        max={MAX_DEPOSIT}
                        value={amount}
                        onChange={(event) => setAmount(event.target.value)}
                        className="w-full rounded-lg border border-slate-300 bg-white px-4 py-2 text-slate-900 outline-none focus:border-emerald-500 focus:ring-2 focus:ring-emerald-200 dark:border-slate-600 dark:bg-slate-700 dark:text-slate-100 dark:focus:ring-emerald-700"
                    />
                    <p className="text-xs text-slate-500 dark:text-slate-400">Allowed range: {MIN_DEPOSIT} to {MAX_DEPOSIT}</p>
                </div>

                {error && (
                    <div className="mt-4 rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700 dark:border-red-800 dark:bg-red-900/30 dark:text-red-300">
                        {error}
                    </div>
                )}

                <button
                    type="button"
                    onClick={handleDeposit}
                    disabled={depositing || loadingBalance}
                    className="mt-6 inline-flex w-full items-center justify-center rounded-lg bg-emerald-600 px-4 py-2.5 font-medium text-white transition hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-60"
                >
                    {depositing ? 'Redirecting to PayPal...' : 'Deposit with PayPal'}
                </button>
            </div>
        </div>
    );
};

export default WalletPage;

