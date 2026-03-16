import axios from 'axios';

const API_BASE_URL = '/api';

const api = axios.create({
    baseURL: API_BASE_URL,
    withCredentials: true,
    headers: {
        'Content-Type': 'application/json',
    },
});

// Auth APIs
export const authAPI = {
    register: (userData) => api.post('/auth/register', userData),
    login: (credentials) => api.post('/auth/login', credentials),
    logout: () => api.post('/auth/logout'),
    getCurrentUser: () => api.get('/auth/me', { params: { t: new Date().getTime() } }),
};

// Movie APIs
export const movieAPI = {
    getAll: (params) => api.get('/movies', { params }),
    getRecent: () => api.get('/movies/recent'),
    getById: (id) => api.get(`/movies/${id}`),
    search: (query) => api.get('/movies/search/query', { params: { q: query } }),
    getByGenre: (genre) => api.get(`/movies/genre/${genre}`),
    getByYear: (startYear, endYear) => api.get(`/movies/year/${startYear}/${endYear}`),
    getRandom: (limit = 20) => api.get('/movies/random/selection', { params: { limit } }),
    getGenres: () => api.get('/movies/meta/genres'),
    getRecentlyWatched: (userId, limit = 10) => api.get(`/movies/user/${userId}/recent`, { params: { limit } }),

    // Comments
    getComments: (movieId) => api.get(`/movies/${movieId}/comments`),
    addComment: (movieId, userId, commentText) => api.post(`/movies/${movieId}/comments`, { userId, commentText }),
    editComment: (movieId, commentId, userId, commentText) => api.put(`/movies/${movieId}/comments/${commentId}`, { userId, commentText }),
    deleteComment: (movieId, commentId, userId) => api.delete(`/movies/${movieId}/comments/${commentId}`, { data: { userId } }),

    // Ratings
    rateMovie: (movieId, userId, rating) => api.post(`/movies/${movieId}/rating`, { userId, rating }),
    getAverageRating: (movieId) => api.get(`/movies/${movieId}/average-rating`),

    // Watch history
    markAsWatched: (movieId, userId) => api.post(`/movies/${movieId}/watch`, { userId }),
};

// List APIs
export const listAPI = {
    getUserLists: (userId) => api.get(`/lists/user/${userId}`),
    createList: (userId, listName) => api.post('/lists', { userId, listName }),
    addMovieToList: (listId, movieId) => api.post(`/lists/${listId}/movies`, { movieId }),
    removeMovieFromList: (listId, movieId) => api.delete(`/lists/${listId}/movies/${movieId}`),
    deleteList: (listId) => api.delete(`/lists/${listId}`),
    renameList: (listId, listName) => api.put(`/lists/${listId}`, { listName }),
};

// Friend APIs
export const friendAPI = {
    getFriends: (userId) => api.get(`/friends/${userId}`),
    sendFriendRequest: (senderId, receiverId) => api.post('/friends/request', { senderId, receiverId }),
    getPendingRequests: (userId) => api.get(`/friends/requests/${userId}`),
    acceptRequest: (requestId) => api.post(`/friends/accept/${requestId}`),
    rejectRequest: (requestId) => api.post(`/friends/reject/${requestId}`),
    cancelRequest: (requestId) => api.delete(`/friends/request/${requestId}`),
    checkStatus: (userId, targetId) => api.get(`/friends/check/${userId}/${targetId}`),
    removeFriend: (userId, friendId) => api.delete(`/friends/${friendId}`, { data: { userId } }),
};

// User APIs
export const userAPI = {
    getUser: (id) => api.get(`/users/${id}`),
    getFavorites: (userId) => api.get(`/users/${userId}/favorites`),
    addToFavorites: (userId, movieId) => api.post(`/users/${userId}/favorites`, { movie_id: movieId }),
    removeFromFavorites: (userId, movieId) => api.delete(`/users/${userId}/favorites/${movieId}`),
};

// Upload APIs
export const uploadAPI = {
    uploadProfilePicture: (formData) => api.post('/upload/profile-picture', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
    }),
};

// Chat APIs
export const chatAPI = {
    getOrCreateChat: (user1Id, user2Id) => api.post('/chat/get-or-create', { user1Id, user2Id }),
    getUserChats: (userId) => api.get(`/chat/user/${userId}`),
    getChatMessages: (chatId, limit = 100) => api.get(`/chat/${chatId}/messages`, { params: { limit } }),
    sendMessage: (chatId, senderId, messageText) => api.post(`/chat/${chatId}/messages`, { chatId, senderId, messageText }),
};

// Recommendations APIs
export const recommendationAPI = {
    generateForUser: (userId) => api.post(`/recommendations/generate/${userId}`),
};

export default api;
