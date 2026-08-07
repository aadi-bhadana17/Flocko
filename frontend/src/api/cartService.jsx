import api from './axiosConfig';

const CART_BASE = '/api/cart';

export const getCart = async () => {
    const response = await api.get(CART_BASE);
    return response.data;
};

export const addToCart = async (foodId, quantity, addonIds = []) => {
    const response = await api.post(CART_BASE, { foodId, quantity, addonIds });
    return response.data;
};

export const updateCartItemQuantity = async (cartItemId, quantity) => {
    const response = await api.put(`${CART_BASE}/${cartItemId}`, { quantity });
    return response.data;
};

export const removeCartItem = async (cartItemId) => {
    const response = await api.delete(`${CART_BASE}/${cartItemId}`);
    return response.data;
};

export const clearCart = async () => {
    const response = await api.delete(CART_BASE);
    return response.data;
};

