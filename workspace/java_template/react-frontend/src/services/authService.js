import axios from 'axios';

const API_URL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080/api';

class AuthService {
  async login(username, password) {
    const response = await axios.post(`${API_URL}/auth/signin`, {
      username,
      password
    });
    
    if (response.data.token) {
      localStorage.setItem('user', JSON.stringify(response.data));
      this.setAuthHeader(response.data.token);
    }
    
    return response;
  }
  
  async register(userData) {
    const { username, email, password } = userData;
    return axios.post(`${API_URL}/auth/signup`, {
      username,
      email,
      password
    });
  }
  
  logout() {
    localStorage.removeItem('user');
    this.removeAuthHeader();
  }
  
  getCurrentUser() {
    const userStr = localStorage.getItem('user');
    if (!userStr) return null;
    return JSON.parse(userStr);
  }
  
  isLoggedIn() {
    const user = this.getCurrentUser();
    return !!user && !!user.token;
  }
  
  hasRole(role) {
    const user = this.getCurrentUser();
    return user && user.roles && user.roles.includes(role);
  }
  
  setAuthHeader(token) {
    axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
  }
  
  removeAuthHeader() {
    delete axios.defaults.headers.common['Authorization'];
  }
  
  async updateProfile(userData) {
    const response = await axios.put(`${API_URL}/users/profile`, userData);
    
    // Update stored user data
    const currentUser = this.getCurrentUser();
    if (currentUser) {
      const updatedUser = {
        ...currentUser,
        username: userData.username || currentUser.username,
        email: userData.email || currentUser.email
      };
      localStorage.setItem('user', JSON.stringify(updatedUser));
    }
    
    return response;
  }
  
  async changePassword(oldPassword, newPassword) {
    return axios.put(`${API_URL}/users/password`, {
      oldPassword,
      newPassword
    });
  }
  
  async uploadAvatar(file) {
    const formData = new FormData();
    formData.append('file', file);
    
    const response = await axios.post(`${API_URL}/users/avatar`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    });
    
    // Update avatar in stored user data
    const currentUser = this.getCurrentUser();
    if (currentUser && response.data.avatar) {
      const updatedUser = {
        ...currentUser,
        avatar: response.data.avatar
      };
      localStorage.setItem('user', JSON.stringify(updatedUser));
    }
    
    return response;
  }
  
  // Initialize auth header from localStorage when service is loaded
  initializeAuth() {
    const user = this.getCurrentUser();
    if (user && user.token) {
      this.setAuthHeader(user.token);
    }
  }
}

const authService = new AuthService();
authService.initializeAuth();

export default authService;