import api from './axiosConfig';

const PUBLIC_BASE = '/public';

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
    const response = await api.get(`${PUBLIC_BASE}/restaurants/${id}/menu`);
    return response.data;
};

