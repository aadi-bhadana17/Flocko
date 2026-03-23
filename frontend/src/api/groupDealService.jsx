import api from './axiosConfig';

const BASE = '/api/restaurants';

export const getGroupDealsByRestaurant = async (restaurantId) => {
    const response = await api.get(`${BASE}/${restaurantId}/group-deals`);
    return response.data;
};

export const getGroupDealDetail = async (restaurantId, dealId) => {
    const response = await api.get(`${BASE}/${restaurantId}/group-deals/${dealId}`);
    return response.data;
};

export const createGroupDeal = async (restaurantId, data) => {
    const response = await api.post(`${BASE}/${restaurantId}/group-deals`, data);
    return response.data;
};

export const deleteGroupDeal = async (restaurantId, dealId) => {
    const response = await api.delete(`${BASE}/${restaurantId}/group-deals/${dealId}`);
    return response.data;
};

export const participateInGroupDeal = async (restaurantId, dealId, data) => {
    const response = await api.post(`${BASE}/${restaurantId}/group-deals/${dealId}/participate`, data);
    return response.data;
};

export const withdrawFromGroupDeal = async (restaurantId, dealId, participationId) => {
    const response = await api.put(`${BASE}/${restaurantId}/group-deals/${dealId}/participate/${participationId}`);
    return response.data;
};

export const getGroupDealParticipationsByUser = async (restaurantId, dealId) => {
    const response = await api.get(`${BASE}/${restaurantId}/group-deals/${dealId}/participate`);
    return response.data;
};

export const getGroupDealParticipationsByDeal = async (restaurantId, dealId) => {
    const response = await api.get(`${BASE}/${restaurantId}/group-deals/${dealId}/participations`);
    return response.data;
};

