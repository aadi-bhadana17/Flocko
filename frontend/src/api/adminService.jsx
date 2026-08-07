import api from './axiosConfig';

const ADMIN_BASE = '/api/admin';
const RESTAURANT_BASE = '/api/restaurants';
const ORDER_BASE = '/api/order';

// ── Role Change Requests ──
export const getRoleRequests = async () => {
    const response = await api.get(`${ADMIN_BASE}/role-requests`);
    return response.data;
};

export const updateRoleRequest = async (requestId, action) => {
    const response = await api.put(`${ADMIN_BASE}/role-requests/${requestId}`, { action });
    return response.data;
};

// ── Users (Admin only) ──
export const getAllUsers = async () => {
    const response = await api.get(`${ADMIN_BASE}/users`);
    return response.data;
};

export const getUserById = async (userId) => {
    const response = await api.get(`${ADMIN_BASE}/users/${userId}`);
    return response.data;
};

export const restrictUser = async (userId, durationInDays, reason) => {
    const response = await api.put(`${ADMIN_BASE}/users/${userId}/restrict`, { durationInDays, reason });
    return response.data;
};

export const unrestrictUser = async (userId) => {
    const response = await api.put(`${ADMIN_BASE}/users/${userId}/unrestrict`);
    return response.data;
};

export const blockUser = async (userId) => {
    const response = await api.put(`${ADMIN_BASE}/users/${userId}/block`);
    return response.data;
};

export const unblockUser = async (userId) => {
    const response = await api.put(`${ADMIN_BASE}/users/${userId}/unblock`);
    return response.data;
};

export const deleteUser = async (userId) => {
    const response = await api.put(`${ADMIN_BASE}/users/${userId}/delete`);
    return response.data;
};

// ── Restaurants (Admin can view all, update, toggle status, suspend/activate) ──
export const getAllRestaurants = async () => {
    const response = await api.get(RESTAURANT_BASE);
    return response.data;
};

export const updateRestaurant = async (id, data) => {
    const response = await api.put(`${RESTAURANT_BASE}/${id}`, data);
    return response.data;
};

export const updateRestaurantStatus = async (id, open) => {
    const response = await api.patch(`${RESTAURANT_BASE}/${id}/status`, { open });
    return response.data;
};

export const suspendRestaurant = async (restaurantId) => {
    const response = await api.put(`${ADMIN_BASE}/restaurants/${restaurantId}/suspend`);
    return response.data;
};

export const activateRestaurant = async (restaurantId) => {
    const response = await api.put(`${ADMIN_BASE}/restaurants/${restaurantId}/activate`);
    return response.data;
};

// ── Orders (Admin only) ──
export const getAllOrders = async () => {
    const response = await api.get(`${ADMIN_BASE}/orders`);
    return response.data;
};

export const getAdminRestaurantOrders = async (restaurantId) => {
    const response = await api.get(`${ORDER_BASE}/restaurant/${restaurantId}`);
    return response.data;
};

