import api from './axiosConfig';

const PUBLIC_BASE = '/public';

// Cache resolved menu payloads by restaurant id.
const menuCache = {};
// Track in-flight menu requests to avoid duplicate concurrent calls.
const menuRequestCache = {};

export const getRestaurants = async (filters = {}) => {
    const params = new URLSearchParams();
    if (filters.city) params.append('city', filters.city);
    if (filters.cuisineType) params.append('cuisineType', filters.cuisineType);
    if (filters.isOpen !== undefined) params.append('isOpen', filters.isOpen);

    const response = await api.get(`${PUBLIC_BASE}/restaurants`, { params });
    return response.data;
};

export const getRestaurantById = async (id) => {
    const response = await api.get(`${PUBLIC_BASE}/restaurants/${id}`);
    return response.data;
};

export const getRestaurantMenu = async (id) => {
    const cacheKey = String(id);

    if (menuCache[cacheKey]) {
        return menuCache[cacheKey];
    }

    if (menuRequestCache[cacheKey]) {
        return menuRequestCache[cacheKey];
    }

    menuRequestCache[cacheKey] = api
        .get(`${PUBLIC_BASE}/restaurants/${id}/menu`)
        .then((response) => {
            menuCache[cacheKey] = response.data;
            return response.data;
        })
        .finally(() => {
            delete menuRequestCache[cacheKey];
        });

    return menuRequestCache[cacheKey];
};

export const clearRestaurantMenuCache = (id) => {
    if (id === undefined || id === null) {
        Object.keys(menuCache).forEach((key) => delete menuCache[key]);
        Object.keys(menuRequestCache).forEach((key) => delete menuRequestCache[key]);
        return;
    }

    const cacheKey = String(id);
    delete menuCache[cacheKey];
    delete menuRequestCache[cacheKey];
};

export const searchRestaurants = async (query) => {
    const response = await api.get(`${PUBLIC_BASE}/restaurants/search`, { params: { q: query } });
    return response.data;
};

export const getPublicGroupDeals = async (restaurantId) => {
    const response = await api.get(`${PUBLIC_BASE}/restaurants/${restaurantId}/group-deals`);
    return response.data;
};

