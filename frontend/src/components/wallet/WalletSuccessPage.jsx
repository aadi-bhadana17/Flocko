import { useEffect, useState } from 'react';
import axios from 'axios';
import { CheckCircle } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

const formatBalance = (value) => {
    const numeric = Number(value ?? 0);
    if (Number.isNaN(numeric)) return '0.00';
    return numeric.toLocaleString('en-IN', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
    });
};

const WalletSuccessPage = () => {
    const navigate = useNavigate();
    const [balance, setBalance] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchProfile = async () => {
            try {
                const token = localStorage.getItem('token');
                const baseUrl = import.meta.env.VITE_API_BASE_URL || '';
                const response = await axios.get(`${baseUrl}/api/users/profile`, {
                    headers: {
                        Authorization: `Bearer ${token}`,
                    },
                });
                setBalance(response.data?.walletBalance ?? 0);
            } catch {
                setBalance(0);
            } finally {
                setLoading(false);
            }
        };

        fetchProfile();
    }, []);

    return (
        <div className="mx-auto flex min-h-[75vh] max-w-xl items-center justify-center px-4 py-10">
            <div className="w-full rounded-2xl border border-emerald-200 bg-white p-8 text-center shadow-sm dark:border-emerald-900 dark:bg-slate-800">
                <CheckCircle className="mx-auto h-20 w-20 text-emerald-500" strokeWidth={1.8} />
                <h1 className="mt-4 text-3xl font-bold text-slate-900 dark:text-slate-100">Payment Successful!</h1>
                <p className="mt-2 text-slate-600 dark:text-slate-300">Your wallet has been topped up successfully</p>
                <p className="mt-5 text-lg font-semibold text-emerald-600 dark:text-emerald-400">
                    New Balance: {loading ? 'Loading...' : `₹${formatBalance(balance)}`}
                </p>
                <button
                    type="button"
                    onClick={() => navigate('/wallet')}
                    className="mt-7 inline-flex items-center justify-center rounded-lg bg-emerald-600 px-5 py-2.5 font-medium text-white transition hover:bg-emerald-700"
                >
                    Go to Wallet
                </button>
            </div>
        </div>
    );
};

export default WalletSuccessPage;

