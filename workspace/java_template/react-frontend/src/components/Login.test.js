import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { BrowserRouter } from 'react-router-dom';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';
import authReducer from '../redux/slices/authSlice';
import Login from './Login';
import authService from '../services/authService';

// Mock the auth service
jest.mock('../services/authService');

// Create a mock store
const createMockStore = (initialState) => {
  return configureStore({
    reducer: {
      auth: authReducer
    },
    preloadedState: {
      auth: initialState
    }
  });
};

// Common wrapper setup for testing
const renderWithProviders = (ui, { initialState = { user: null, isLoggedIn: false, loading: false, error: null }, ...renderOptions } = {}) => {
  const store = createMockStore(initialState);
  
  return {
    store,
    ...render(
      <Provider store={store}>
        <BrowserRouter>
          {ui}
        </BrowserRouter>
      </Provider>,
      renderOptions
    )
  };
};

describe('Login Component', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('renders login form', () => {
    renderWithProviders(<Login />);
    
    // Check for essential login elements
    expect(screen.getByText(/sign in/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/username/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /login/i })).toBeInTheDocument();
  });

  test('validates form inputs', async () => {
    renderWithProviders(<Login />);
    
    // Submit form without entering data
    const loginButton = screen.getByRole('button', { name: /login/i });
    fireEvent.click(loginButton);
    
    // Check for validation errors
    expect(await screen.findByText(/username is required/i)).toBeInTheDocument();
    expect(await screen.findByText(/password is required/i)).toBeInTheDocument();
  });

  test('displays error message on authentication failure', async () => {
    // Mock the login function to return an error
    authService.login.mockRejectedValue({
      response: {
        data: {
          message: 'Invalid credentials'
        }
      }
    });
    
    renderWithProviders(<Login />);
    
    // Fill in form
    await userEvent.type(screen.getByLabelText(/username/i), 'testuser');
    await userEvent.type(screen.getByLabelText(/password/i), 'wrongpassword');
    
    // Submit form
    const loginButton = screen.getByRole('button', { name: /login/i });
    fireEvent.click(loginButton);
    
    // Check for error message
    expect(await screen.findByText(/invalid credentials/i)).toBeInTheDocument();
  });

  test('handles successful login', async () => {
    // Mock successful login
    const mockUser = { 
      id: 1, 
      username: 'testuser', 
      email: 'test@example.com',
      roles: ['ROLE_USER']
    };
    
    const mockLoginResponse = {
      data: {
        token: 'mock-jwt-token',
        id: mockUser.id,
        username: mockUser.username,
        email: mockUser.email,
        roles: mockUser.roles
      }
    };
    
    authService.login.mockResolvedValue(mockLoginResponse);
    
    // Mock localStorage
    const mockLocalStorage = {};
    jest.spyOn(Storage.prototype, 'setItem').mockImplementation(
      (key, value) => (mockLocalStorage[key] = value)
    );
    
    const { store } = renderWithProviders(<Login />);
    
    // Fill in form
    await userEvent.type(screen.getByLabelText(/username/i), 'testuser');
    await userEvent.type(screen.getByLabelText(/password/i), 'password123');
    
    // Submit form
    const loginButton = screen.getByRole('button', { name: /login/i });
    fireEvent.click(loginButton);
    
    // Wait for login to complete
    await waitFor(() => {
      expect(authService.login).toHaveBeenCalledWith('testuser', 'password123');
      expect(mockLocalStorage['user']).toBeTruthy();
      
      // Check store state was updated
      const state = store.getState();
      expect(state.auth.isLoggedIn).toBeTruthy();
      expect(state.auth.user).toEqual(mockLoginResponse.data);
    });
  });

  test('shows loading state during authentication', async () => {
    // Mock a delayed login to see the loading state
    authService.login.mockImplementation(() => {
      return new Promise(resolve => {
        setTimeout(() => {
          resolve({
            data: {
              token: 'mock-jwt-token',
              id: 1,
              username: 'testuser',
              email: 'test@example.com',
              roles: ['ROLE_USER']
            }
          });
        }, 100);
      });
    });
    
    renderWithProviders(<Login />);
    
    // Fill in form
    await userEvent.type(screen.getByLabelText(/username/i), 'testuser');
    await userEvent.type(screen.getByLabelText(/password/i), 'password123');
    
    // Submit form
    const loginButton = screen.getByRole('button', { name: /login/i });
    fireEvent.click(loginButton);
    
    // Check for loading indicator
    expect(await screen.findByTestId('loading-spinner')).toBeInTheDocument();
    
    // Wait for loading to complete
    await waitFor(() => {
      expect(screen.queryByTestId('loading-spinner')).not.toBeInTheDocument();
    });
  });

  test('navigates to registration page when signup link clicked', async () => {
    renderWithProviders(<Login />);
    
    // Find and click the sign up link
    const signupLink = screen.getByText(/don't have an account\? sign up/i);
    fireEvent.click(signupLink);
    
    // In a real test with react-router, we would check for navigation
    // Here we can verify the link's href attribute
    expect(signupLink.closest('a')).toHaveAttribute('href', '/register');
  });
  
  test('toggles password visibility', async () => {
    renderWithProviders(<Login />);
    
    // Get password input and visibility toggle button
    const passwordInput = screen.getByLabelText(/password/i);
    const visibilityToggle = screen.getByTestId('password-visibility-toggle');
    
    // Initially password should be hidden
    expect(passwordInput).toHaveAttribute('type', 'password');
    
    // Click visibility toggle
    fireEvent.click(visibilityToggle);
    
    // Password should now be visible
    expect(passwordInput).toHaveAttribute('type', 'text');
    
    // Click again to hide
    fireEvent.click(visibilityToggle);
    
    // Password should be hidden again
    expect(passwordInput).toHaveAttribute('type', 'password');
  });
});