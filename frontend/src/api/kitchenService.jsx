import api from './axiosConfig';

export const updateKitchenStatus = async (restaurantId, kitchenLoadIndicator) => {
    const response = await api.put(`/api/restaurant/${restaurantId}/kitchen/status`, { kitchenLoadIndicator });
    return response.data;
};

