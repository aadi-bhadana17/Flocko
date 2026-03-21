import api from './axiosConfig';

const ORDER_BASE = '/api/order';

export const placeOrder = async (addressId) => {
    const payload = {
        addressId: typeof addressId === 'object' ? addressId.addressId : addressId,
    };

    if (typeof addressId === 'object') {
        if (addressId.scheduledAt) payload.scheduledAt = addressId.scheduledAt;
        if (typeof addressId.isSpecial === 'boolean') payload.isSpecial = addressId.isSpecial;
    }

    const response = await api.post(ORDER_BASE, payload);
    return response.data;
};

export const getMyOrders = async () => {
    const response = await api.get(ORDER_BASE);
    return response.data;
};

export const getOrder = async (orderId) => {
    const response = await api.get(`${ORDER_BASE}/${orderId}`);
    return response.data;
};

export const cancelOrder = async (orderId) => {
    const response = await api.patch(`${ORDER_BASE}/${orderId}`);
    return response.data;
};

export const getRestaurantOrders = async () => {
    const response = await api.get(`${ORDER_BASE}/restaurant`);
    return response.data;
};

export const updateOrderStatus = async (orderId, orderStatus) => {
    const response = await api.patch(`${ORDER_BASE}/${orderId}/status`, { orderStatus });
    return response.data;
};

