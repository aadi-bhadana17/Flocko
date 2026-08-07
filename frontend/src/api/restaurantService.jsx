import api from './axiosConfig';

const BASE = '/api/restaurants';

// ── Restaurant ──
export const getMyRestaurants = async () => {
    const response = await api.get(`${BASE}/my`);
    return response.data;
};

export const getRestaurantById = async (id) => {
    const response = await api.get(`${BASE}/${id}`);
    return response.data;
};

export const createRestaurant = async (data) => {
    const response = await api.post(BASE, data);
    return response.data;
};

export const updateRestaurant = async (id, data) => {
    const response = await api.put(`${BASE}/${id}`, data);
    return response.data;
};

export const updateRestaurantStatus = async (id, open) => {
    const response = await api.patch(`${BASE}/${id}/status`, { open });
    return response.data;
};

// ── Staff ──
export const getRestaurantStaff = async (restaurantId) => {
    const response = await api.get(`${BASE}/${restaurantId}/staff`);
    return response.data;
};

export const addRestaurantStaff = async (restaurantId, data) => {
    const response = await api.post(`${BASE}/${restaurantId}/staff`, data);
    return response.data;
};

// ── Categories ──
export const getCategories = async (restaurantId) => {
    const response = await api.get(`${BASE}/${restaurantId}/categories`);
    return response.data;
};

export const createCategory = async (restaurantId, data) => {
    const response = await api.post(`${BASE}/${restaurantId}/categories`, data);
    return response.data;
};

export const updateCategory = async (restaurantId, categoryId, data) => {
    const response = await api.put(`${BASE}/${restaurantId}/categories/${categoryId}`, data);
    return response.data;
};

export const deleteCategory = async (restaurantId, categoryId) => {
    const response = await api.delete(`${BASE}/${restaurantId}/categories/${categoryId}`);
    return response.data;
};

// ── Foods ──
export const getFoods = async (restaurantId) => {
    const response = await api.get(`${BASE}/${restaurantId}/foods`);
    return response.data;
};

export const createFood = async (restaurantId, data) => {
    const response = await api.post(`${BASE}/${restaurantId}/foods`, data);
    return response.data;
};

export const updateFood = async (restaurantId, foodId, data) => {
    const response = await api.put(`${BASE}/${restaurantId}/foods/${foodId}`, data);
    return response.data;
};

export const updateFoodStatus = async (restaurantId, foodId, available) => {
    const response = await api.patch(`${BASE}/${restaurantId}/foods/${foodId}`, { available });
    return response.data;
};

export const deleteFood = async (restaurantId, foodId) => {
    const response = await api.delete(`${BASE}/${restaurantId}/foods/${foodId}`);
    return response.data;
};

// ── Addons ──
export const getAddons = async (restaurantId) => {
    const response = await api.get(`${BASE}/${restaurantId}/addons`);
    return response.data;
};

export const createAddon = async (restaurantId, data) => {
    const response = await api.post(`${BASE}/${restaurantId}/addons`, data);
    return response.data;
};

export const updateAddon = async (restaurantId, addonId, data) => {
    const response = await api.put(`${BASE}/${restaurantId}/addons/${addonId}`, data);
    return response.data;
};

export const updateAddonAvailability = async (restaurantId, addonId, available) => {
    const response = await api.patch(`${BASE}/${restaurantId}/addons/${addonId}`, { available });
    return response.data;
};

export const deleteAddon = async (restaurantId, addonId) => {
    const response = await api.delete(`${BASE}/${restaurantId}/addons/${addonId}`);
    return response.data;
};

// ── Category Addons ──
export const getCategoryAddons = async (restaurantId, categoryId) => {
    const response = await api.get(`${BASE}/${restaurantId}/categories/${categoryId}/addons`);
    return response.data;
};

export const appendCategoryAddons = async (restaurantId, categoryId, addonIds) => {
    const response = await api.post(`${BASE}/${restaurantId}/categories/${categoryId}/addons`, { addonIds });
    return response.data;
};

export const removeCategoryAddons = async (restaurantId, categoryId, addonIds) => {
    const response = await api.delete(`${BASE}/${restaurantId}/categories/${categoryId}/addons`, { data: { addonIds } });
    return response.data;
};

// ── Mess Plans ──
export const getMessPlans = async (restaurantId) => {
    const response = await api.get(`${BASE}/${restaurantId}/mess-plans`);
    return response.data;
};

export const createMessPlan = async (restaurantId, data) => {
    const response = await api.post(`${BASE}/${restaurantId}/mess-plans`, data);
    return response.data;
};

export const getMessPlanById = async (restaurantId, messPlanId) => {
    const response = await api.get(`${BASE}/${restaurantId}/mess-plans/${messPlanId}`);
    return response.data;
};

export const updateMessPlan = async (restaurantId, messPlanId, data) => {
    const response = await api.put(`${BASE}/${restaurantId}/mess-plans/${messPlanId}`, data);
    return response.data;
};

export const deleteMessPlan = async (restaurantId, messPlanId) => {
    const response = await api.delete(`${BASE}/${restaurantId}/mess-plans/${messPlanId}`);
    return response.data;
};

