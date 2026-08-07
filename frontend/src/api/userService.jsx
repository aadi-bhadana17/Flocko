import api from './axiosConfig';

const USER_BASE = '/api/users';

export const getUserProfile = async () => {
    const response = await api.get(`${USER_BASE}/profile`, { params: { _t: Date.now() } });
    return response.data;
};

export const updateProfile = async (profileData) => {
    const response = await api.put(`${USER_BASE}/profile`, profileData);
    return response.data;
};

export const changePassword = async (passwordData) => {
    const response = await api.put(`${USER_BASE}/change-password`, passwordData);
    return response.data;
};

export const submitRoleChangeRequest = async (requestData) => {
    const response = await api.post(`${USER_BASE}/role-change-request`, requestData);
    return response.data;
};

export const getStaffRestaurant = async () => {
    const response = await api.get(`${USER_BASE}/staff-restaurant`);
    return response.data;
};

export const getAddresses = async () => {
    const response = await api.get(`${USER_BASE}/addresses`);
    return response.data;
};

export const addAddress = async (addressData) => {
    const response = await api.post(`${USER_BASE}/addresses`, addressData);
    return response.data;
};

export const updateAddress = async (addressId, addressData) => {
    const response = await api.put(`${USER_BASE}/addresses/${addressId}`, addressData);
    return response.data;
};

export const setDefaultAddress = async (addressId) => {
    const response = await api.put(`${USER_BASE}/addresses/${addressId}/default`);
    return response.data;
};

export const deleteAddress = async (addressId) => {
    const response = await api.delete(`${USER_BASE}/addresses/${addressId}`);
    return response.data;
};

// ── Favourite Restaurants ──
export const getFavourites = async () => {
    const response = await api.get(`${USER_BASE}/favourites`);
    return response.data;
};

export const addFavourite = async (restaurantId) => {
    const response = await api.post(`${USER_BASE}/favourites/${restaurantId}`);
    return response.data;
};

export const removeFavourite = async (restaurantId) => {
    const response = await api.delete(`${USER_BASE}/favourites/${restaurantId}`);
    return response.data;
};

// ── Mess Plan Subscriptions ──
export const subscribeToMessPlan = async (messPlanId) => {
    const response = await api.post(`${USER_BASE}/mess-plans/${messPlanId}/subscribe`);
    return response.data;
};

export const getMyMessSubscriptions = async (active) => {
    const params = {};
    if (typeof active === 'boolean') params.active = active;

    const response = await api.get(`${USER_BASE}/mess-plans/subscriptions`, { params });
    return response.data;
};

