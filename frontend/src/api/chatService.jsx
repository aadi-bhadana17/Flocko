import api from './axiosConfig';

export const getOrderChatMessages = async (orderId) => {
    const response = await api.get(`/api/order/${orderId}/chat`);
    return response.data;
};

export const sendOrderChatMessage = async (orderId, message) => {
    const response = await api.post(`/api/order/${orderId}/chat`, { message });
    return response.data;
};

