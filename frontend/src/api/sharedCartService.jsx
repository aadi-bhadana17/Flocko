import api from './axiosConfig';

const SHARED_CART_BASE = '/api/shared-cart';

export const getSharedCart = async () => {
    const response = await api.get(SHARED_CART_BASE);
    return response.data;
};

export const getCurrentSharedCart = async () => {
    const response = await api.get(`${SHARED_CART_BASE}/current`);
    return response.data;
};

export const createSharedCart = async (restaurantId, hostPaysAll) => {
    const response = await api.post(SHARED_CART_BASE, {
        restaurant: { restaurantId },
        hostPaysAll
    });
    return response.data;
};

export const getSharedCartByCode = async (code) => {
    const response = await api.get(`${SHARED_CART_BASE}/${code}`);
    return response.data;
};

export const joinSharedCart = async (code) => {
    const response = await api.post(`${SHARED_CART_BASE}/${code}/join`);
    return response.data;
};

export const addItemToSharedCart = async (code, payload) => {
    const response = await api.post(`${SHARED_CART_BASE}/${code}/cart`, payload);
    return response.data;
};

export const removeItemFromSharedCart = async (code, cartItemId) => {
    const response = await api.delete(`${SHARED_CART_BASE}/${code}/cart/${cartItemId}`);
    return response.data;
};

export const contributeToSharedCart = async (code, amount) => {
    const response = await api.patch(`${SHARED_CART_BASE}/${code}/contribute`, { amount });
    return response.data;
};

export const checkoutSharedCart = async (code, payload) => {
    const response = await api.post(`${SHARED_CART_BASE}/${code}/checkout`, payload);
    return response.data;
};
