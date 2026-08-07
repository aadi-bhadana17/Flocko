import api from './axiosConfig';

export const loginApi = async (credentials) => {
    // credentials matches LoginRequest
    const response = await api.post('/auth/login', credentials);

    if(response.data.token) {
        localStorage.setItem('user', JSON.stringify(response.data))
    }

    return response.data; // LoginAuthResponse
};

export const signupApi = async (userData) => {
    // userData matches SignupRequest
    const response = await api.post('/auth/signup', userData);
    return response.data; // LoginAuthResponse
};