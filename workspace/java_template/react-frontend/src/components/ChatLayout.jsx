import React, { useState, useEffect } from 'react';
import { useSelector } from 'react-redux';
import { Box, CssBaseline, AppBar, Toolbar, Typography, IconButton, Avatar, Menu, MenuItem, Drawer, Divider } from '@mui/material';
import { Menu as MenuIcon, Logout, Person, Settings } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { logout } from '../redux/slices/authSlice';
import GroupList from './GroupList';
import GroupChat from './GroupChat';
import websocketService from '../services/websocketService';

const drawerWidth = 320;

const ChatLayout = () => {
  const [mobileOpen, setMobileOpen] = useState(false);
  const [selectedGroup, setSelectedGroup] = useState(null);
  const [anchorEl, setAnchorEl] = useState(null);
  const [unreadMessages, setUnreadMessages] = useState({});
  
  const { user } = useSelector(state => state.auth);
  const dispatch = useDispatch();
  const navigate = useNavigate();
  
  useEffect(() => {
    if (user) {
      // Initialize WebSocket connection
      websocketService.connect(user.token);
      
      // Listen for new messages to update unread count
      websocketService.onMessage((event) => {
        const data = JSON.parse(event.data);
        
        if (data.type === 'NEW_MESSAGE' && data.groupId) {
          if (!selectedGroup || selectedGroup.id !== data.groupId) {
            setUnreadMessages(prev => ({
              ...prev,
              [data.groupId]: (prev[data.groupId] || 0) + 1
            }));
          }
        }
      });
      
      return () => {
        websocketService.disconnect();
      };
    }
  }, [user, selectedGroup]);

  const handleDrawerToggle = () => {
    setMobileOpen(!mobileOpen);
  };

  const handleUserMenuOpen = (event) => {
    setAnchorEl(event.currentTarget);
  };

  const handleUserMenuClose = () => {
    setAnchorEl(null);
  };

  const handleGroupSelect = (group) => {
    setSelectedGroup(group);
    // Clear unread count for this group
    setUnreadMessages(prev => ({
      ...prev,
      [group.id]: 0
    }));
    
    // Close drawer on mobile after selection
    if (mobileOpen) {
      setMobileOpen(false);
    }
  };

  const handleLogout = () => {
    handleUserMenuClose();
    websocketService.disconnect();
    dispatch(logout());
    navigate('/login');
  };

  const handleProfile = () => {
    handleUserMenuClose();
    navigate('/profile');
  };

  const handleSettings = () => {
    handleUserMenuClose();
    navigate('/settings');
  };

  const userMenu = (
    <Menu
      anchorEl={anchorEl}
      open={Boolean(anchorEl)}
      onClose={handleUserMenuClose}
      anchorOrigin={{
        vertical: 'bottom',
        horizontal: 'right',
      }}
      transformOrigin={{
        vertical: 'top',
        horizontal: 'right',
      }}
    >
      <MenuItem onClick={handleProfile}>
        <Person sx={{ mr: 1 }} /> Profile
      </MenuItem>
      <MenuItem onClick={handleSettings}>
        <Settings sx={{ mr: 1 }} /> Settings
      </MenuItem>
      <Divider />
      <MenuItem onClick={handleLogout}>
        <Logout sx={{ mr: 1 }} /> Logout
      </MenuItem>
    </Menu>
  );

  return (
    <Box sx={{ display: 'flex', height: '100vh' }}>
      <CssBaseline />
      <AppBar 
        position="fixed" 
        sx={{ 
          zIndex: (theme) => theme.zIndex.drawer + 1,
          backgroundColor: '#2c3e50' 
        }}
      >
        <Toolbar>
          <IconButton
            color="inherit"
            aria-label="open drawer"
            edge="start"
            onClick={handleDrawerToggle}
            sx={{ mr: 2, display: { sm: 'none' } }}
          >
            <MenuIcon />
          </IconButton>
          <Typography variant="h6" noWrap component="div" sx={{ flexGrow: 1 }}>
            Private Domain Chat
          </Typography>
          {user && (
            <Box sx={{ display: 'flex', alignItems: 'center' }}>
              <Typography variant="body2" sx={{ mr: 1, display: { xs: 'none', sm: 'block' } }}>
                {user.username}
              </Typography>
              <IconButton 
                onClick={handleUserMenuOpen}
                size="small"
                sx={{ ml: 2 }}
                aria-controls="user-menu"
                aria-haspopup="true"
              >
                <Avatar 
                  alt={user.username}
                  src={user.avatar || ''} 
                  sx={{ width: 32, height: 32 }}
                />
              </IconButton>
              {userMenu}
            </Box>
          )}
        </Toolbar>
      </AppBar>
      <Box
        component="nav"
        sx={{ width: { sm: drawerWidth }, flexShrink: { sm: 0 } }}
      >
        <Drawer
          variant="temporary"
          open={mobileOpen}
          onClose={handleDrawerToggle}
          ModalProps={{
            keepMounted: true, // Better mobile performance
          }}
          sx={{
            display: { xs: 'block', sm: 'none' },
            '& .MuiDrawer-paper': { boxSizing: 'border-box', width: drawerWidth },
          }}
        >
          <Toolbar />
          <GroupList 
            selectedGroup={selectedGroup} 
            onSelectGroup={handleGroupSelect} 
            unreadMessages={unreadMessages}
          />
        </Drawer>
        <Drawer
          variant="permanent"
          sx={{
            display: { xs: 'none', sm: 'block' },
            '& .MuiDrawer-paper': { boxSizing: 'border-box', width: drawerWidth },
          }}
          open
        >
          <Toolbar />
          <GroupList 
            selectedGroup={selectedGroup} 
            onSelectGroup={handleGroupSelect}
            unreadMessages={unreadMessages}
          />
        </Drawer>
      </Box>
      <Box
        component="main"
        sx={{ flexGrow: 1, p: 0, width: { sm: `calc(100% - ${drawerWidth}px)` }, display: 'flex', flexDirection: 'column' }}
      >
        <Toolbar />
        {selectedGroup ? (
          <GroupChat group={selectedGroup} />
        ) : (
          <Box sx={{ 
            display: 'flex', 
            justifyContent: 'center', 
            alignItems: 'center', 
            height: '100%',
            backgroundColor: '#f5f7f9'
          }}>
            <Typography variant="h6" color="text.secondary">
              Select a group to start chatting
            </Typography>
          </Box>
        )}
      </Box>
    </Box>
  );
};

export default ChatLayout;