import { useEffect } from 'react';
import { XCircle, RotateCcw } from 'lucide-react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { motion } from 'framer-motion';
import { toast } from '../ui/Toast';

const WalletCancelPage = () => {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const errorMessage = searchParams.get('error');

    // Show error toast on mount
    useEffect(() => {
        toast.error(
            errorMessage || 'Payment was cancelled. No amount was deducted from your account.'
        );
    }, [errorMessage]);

    return (
        <div className="mx-auto flex min-h-[75vh] max-w-xl items-center justify-center px-4 py-10">
            <motion.div
                className="w-full rounded-2xl border border-red-200 bg-white p-8 text-center shadow-sm dark:border-red-900 dark:bg-slate-800"
                initial={{ opacity: 0, y: 30, scale: 0.97 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                transition={{ type: 'spring', stiffness: 300, damping: 25 }}
            >
                <motion.div
                    initial={{ scale: 0, rotate: 45 }}
                    animate={{ scale: 1, rotate: 0 }}
                    transition={{ type: 'spring', stiffness: 260, damping: 20, delay: 0.15 }}
                >
                    <XCircle className="mx-auto h-20 w-20 text-red-500" strokeWidth={1.8} />
                </motion.div>

                <motion.h1
                    className="mt-4 text-3xl font-bold text-slate-900 dark:text-slate-100"
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: 0.25 }}
                >
                    Payment Cancelled
                </motion.h1>

                <motion.p
                    className="mt-2 text-slate-600 dark:text-slate-300"
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    transition={{ delay: 0.35 }}
                >
                    {errorMessage || 'No amount was deducted from your account'}
                </motion.p>

                <motion.button
                    type="button"
                    onClick={() => navigate('/wallet')}
                    className="mt-7 inline-flex items-center justify-center gap-2 rounded-lg bg-slate-700 px-5 py-2.5 font-medium text-white transition hover:bg-slate-800 dark:bg-slate-600 dark:hover:bg-slate-500"
                    whileHover={{ scale: 1.03 }}
                    whileTap={{ scale: 0.97 }}
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: 0.45 }}
                >
                    <RotateCcw size={16} />
                    Try Again
                </motion.button>
            </motion.div>
        </div>
    );
};

export default WalletCancelPage;
