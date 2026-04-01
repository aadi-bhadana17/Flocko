import { XCircle } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

const WalletCancelPage = () => {
    const navigate = useNavigate();

    return (
        <div className="mx-auto flex min-h-[75vh] max-w-xl items-center justify-center px-4 py-10">
            <div className="w-full rounded-2xl border border-red-200 bg-white p-8 text-center shadow-sm dark:border-red-900 dark:bg-slate-800">
                <XCircle className="mx-auto h-20 w-20 text-red-500" strokeWidth={1.8} />
                <h1 className="mt-4 text-3xl font-bold text-slate-900 dark:text-slate-100">Payment Cancelled</h1>
                <p className="mt-2 text-slate-600 dark:text-slate-300">No amount was deducted from your account</p>
                <button
                    type="button"
                    onClick={() => navigate('/wallet')}
                    className="mt-7 inline-flex items-center justify-center rounded-lg bg-slate-700 px-5 py-2.5 font-medium text-white transition hover:bg-slate-800 dark:bg-slate-600 dark:hover:bg-slate-500"
                >
                    Try Again
                </button>
            </div>
        </div>
    );
};

export default WalletCancelPage;
