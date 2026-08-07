import api from './axiosConfig';

const REVIEW_BASE = '/api/reviews';
const USER_REVIEW_ENDPOINT = '/api/user/reviews';
const CREATE_REVIEW_BASE = '/api/restaurant';

const normalizeReviewList = (payload) => {
    if (Array.isArray(payload)) return payload;
    if (Array.isArray(payload?.content)) return payload.content;
    if (Array.isArray(payload?.data)) return payload.data;
    if (Array.isArray(payload?.reviews)) return payload.reviews;
    return [];
};

export const getRestaurantReviews = async (restaurantId) => {
    const response = await api.get(`${REVIEW_BASE}/restaurant/${restaurantId}`);
    return normalizeReviewList(response.data);
};

export const createRestaurantReview = async (restaurantId, data) => {
    const response = await api.post(`${CREATE_REVIEW_BASE}/${restaurantId}`, data);
    return response.data;
};

export const getMyReviews = async () => {
    try {
        const response = await api.get(USER_REVIEW_ENDPOINT);
        return normalizeReviewList(response.data);
    } catch (err) {
        if (err?.response?.status === 404) {
            return [];
        }
        throw err;
    }
};

