import api from './axiosConfig';

const WALLET_BASE = '/api/wallet';

export const initiateDeposit = async (amount) => {
    const response = await api.post(`${WALLET_BASE}/deposit`, null, {
        params: { amount },
        responseType: 'text',
    });

    return (response.data || '').toString().trim();
};

