import React, { useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { useSelector } from 'react-redux';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import CssBaseline from '@mui/material/CssBaseline';

// Components
import Login from './components/Login';
import Register from './components/Register';
import ChatLayout from './components/ChatLayout';

// Create theme
const theme = createTheme({
  palette: {
    primary: {
      main: '#2c3e50',
      light: '#3e5060',
      dark: '#1a2530',
    },
    secondary: {
      main: '#2980b9',
    },
    background: {
      default: '#f5f7f9',
    },
  },
  typography: {
    fontFamily: '"Roboto", "Helvetica", "Arial", sans-serif',
    h6: {
      fontWeight: 600,
    },
  },
  components: {
    MuiButton: {
      styleOverrides: {
        root: {
          textTransform: 'none',
          borderRadius: 8,
        },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: {
          borderRadius: 8,
        },
      },
    },
  },
});

// Protected route component
const ProtectedRoute = ({ children }) => {
  const { isLoggedIn } = useSelector(state => state.auth);
  
  if (!isLoggedIn) {
    return <Navigate to="/login" replace />;
  }
  
  return children;
};

function App() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <Router>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route 
            path="/chat/*" 
            element={
              <ProtectedRoute>
                <ChatLayout />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/" 
            element={<Navigate to="/chat" replace />} 
          />
          <Route 
            path="*" 
            element={
              <div style={{ 
                display: 'flex', 
                justifyContent: 'center', 
                alignItems: 'center', 
                height: '100vh',
                flexDirection: 'column'
              }}>
                <h1>404</h1>
                <p>Page not found</p>
              </div>
            } 
          />
        </Routes>
      </Router>
    </ThemeProvider>
  );
}

export default App;