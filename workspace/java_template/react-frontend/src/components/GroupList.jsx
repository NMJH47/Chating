import React, { useState, useEffect } from 'react';
import { useSelector } from 'react-redux';
import { Box, List, ListItem, ListItemAvatar, ListItemText, Avatar, Typography, Fab, Dialog, DialogTitle, DialogContent, DialogActions, TextField, Button, Badge, Divider, ListItemButton, CircularProgress } from '@mui/material';
import { Add as AddIcon, Group as GroupIcon, Public as PublicIcon, Lock as LockIcon } from '@mui/icons-material';
import groupService from '../services/groupService';

const GroupList = ({ selectedGroup, onSelectGroup, unreadMessages }) => {
  const [groups, setGroups] = useState([]);
  const [publicGroups, setPublicGroups] = useState([]);
  const [loading, setLoading] = useState(false);
  const [openCreateDialog, setOpenCreateDialog] = useState(false);
  const [openJoinDialog, setOpenJoinDialog] = useState(false);
  const [newGroupData, setNewGroupData] = useState({
    name: '',
    description: '',
    isPrivate: false
  });
  const [errors, setErrors] = useState({});
  
  const { user } = useSelector(state => state.auth);

  useEffect(() => {
    if (user) {
      loadUserGroups();
    }
  }, [user]);

  const loadUserGroups = async () => {
    setLoading(true);
    try {
      const userGroups = await groupService.getUserGroups();
      setGroups(userGroups);
      
      // Select first group if none is selected
      if (userGroups.length > 0 && !selectedGroup) {
        onSelectGroup(userGroups[0]);
      }
    } catch (error) {
      console.error('Failed to load groups:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadPublicGroups = async () => {
    try {
      const publicGroupsData = await groupService.getPublicGroups(0, 20);
      // Filter out groups the user is already a member of
      const groupIds = groups.map(g => g.id);
      const filteredPublicGroups = publicGroupsData.content.filter(
        group => !groupIds.includes(group.id)
      );
      setPublicGroups(filteredPublicGroups);
    } catch (error) {
      console.error('Failed to load public groups:', error);
    }
  };

  const handleOpenCreateDialog = () => {
    setNewGroupData({
      name: '',
      description: '',
      isPrivate: false
    });
    setErrors({});
    setOpenCreateDialog(true);
  };

  const handleOpenJoinDialog = () => {
    loadPublicGroups();
    setOpenJoinDialog(true);
  };

  const handleCloseCreateDialog = () => {
    setOpenCreateDialog(false);
  };

  const handleCloseJoinDialog = () => {
    setOpenJoinDialog(false);
  };

  const handleCreateInputChange = (e) => {
    const { name, value, type, checked } = e.target;
    setNewGroupData({
      ...newGroupData,
      [name]: type === 'checkbox' ? checked : value
    });
    
    // Clear error for field being edited
    if (errors[name]) {
      setErrors({
        ...errors,
        [name]: ''
      });
    }
  };

  const validateGroupData = () => {
    const newErrors = {};
    if (!newGroupData.name.trim()) {
      newErrors.name = 'Group name is required';
    } else if (newGroupData.name.length < 3) {
      newErrors.name = 'Group name must be at least 3 characters';
    }
    
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleCreateGroup = async () => {
    if (!validateGroupData()) return;
    
    try {
      const createdGroup = await groupService.createGroup(newGroupData);
      setGroups([...groups, createdGroup]);
      onSelectGroup(createdGroup);
      handleCloseCreateDialog();
    } catch (error) {
      console.error('Failed to create group:', error);
      setErrors({
        ...errors,
        submit: error.response?.data?.message || 'Failed to create group'
      });
    }
  };

  const handleJoinGroup = async (groupId) => {
    try {
      await groupService.joinGroup(groupId);
      handleCloseJoinDialog();
      await loadUserGroups(); // Reload user groups to include the newly joined group
    } catch (error) {
      console.error('Failed to join group:', error);
    }
  };

  const handleGroupClick = (group) => {
    onSelectGroup(group);
  };

  const renderGroupIcon = (group) => {
    if (group.avatar) {
      return <Avatar alt={group.name} src={group.avatar} />;
    }
    return group.isPrivate ? 
      <Avatar><LockIcon /></Avatar> : 
      <Avatar><PublicIcon /></Avatar>;
  };

  if (loading && groups.length === 0) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', p: 3 }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
      <Box sx={{ p: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="h6" component="div">
          Your Groups
        </Typography>
        <div>
          <Button 
            variant="outlined" 
            size="small" 
            onClick={handleOpenJoinDialog}
            sx={{ mr: 1 }}
          >
            Join
          </Button>
          <Button 
            variant="contained" 
            size="small"
            onClick={handleOpenCreateDialog}
          >
            Create
          </Button>
        </div>
      </Box>
      <Divider />
      <Box sx={{ overflow: 'auto', flexGrow: 1 }}>
        <List>
          {groups.map((group) => (
            <ListItemButton 
              key={group.id}
              selected={selectedGroup && selectedGroup.id === group.id}
              onClick={() => handleGroupClick(group)}
              sx={{
                '&.Mui-selected': {
                  backgroundColor: 'rgba(25, 118, 210, 0.1)',
                },
                '&.Mui-selected:hover': {
                  backgroundColor: 'rgba(25, 118, 210, 0.15)',
                }
              }}
            >
              <ListItemAvatar>
                {renderGroupIcon(group)}
              </ListItemAvatar>
              <ListItemText 
                primary={
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <Typography variant="subtitle1" noWrap>{group.name}</Typography>
                    {(unreadMessages[group.id] > 0) && (
                      <Badge 
                        badgeContent={unreadMessages[group.id]} 
                        color="primary"
                        sx={{ ml: 1 }}
                      />
                    )}
                  </Box>
                }
                secondary={
                  <Typography
                    variant="body2"
                    color="text.secondary"
                    noWrap
                  >
                    {group.description || "No description"}
                  </Typography>
                }
              />
            </ListItemButton>
          ))}
          {groups.length === 0 && (
            <ListItem>
              <ListItemText 
                primary="No groups yet"
                secondary="Create or join a group to get started"
              />
            </ListItem>
          )}
        </List>
      </Box>

      {/* Create Group Dialog */}
      <Dialog open={openCreateDialog} onClose={handleCloseCreateDialog} maxWidth="sm" fullWidth>
        <DialogTitle>Create New Group</DialogTitle>
        <DialogContent>
          {errors.submit && (
            <Typography color="error" variant="body2" sx={{ mb: 2 }}>
              {errors.submit}
            </Typography>
          )}
          <TextField
            autoFocus
            margin="dense"
            id="name"
            name="name"
            label="Group Name"
            type="text"
            fullWidth
            variant="outlined"
            value={newGroupData.name}
            onChange={handleCreateInputChange}
            error={!!errors.name}
            helperText={errors.name}
            sx={{ mb: 2 }}
          />
          <TextField
            margin="dense"
            id="description"
            name="description"
            label="Description"
            type="text"
            fullWidth
            variant="outlined"
            value={newGroupData.description}
            onChange={handleCreateInputChange}
            multiline
            rows={3}
            sx={{ mb: 2 }}
          />
          <Box sx={{ display: 'flex', alignItems: 'center' }}>
            <Typography variant="body1" sx={{ mr: 2 }}>Privacy:</Typography>
            <Button
              variant={newGroupData.isPrivate ? "outlined" : "contained"}
              onClick={() => setNewGroupData({...newGroupData, isPrivate: false})}
              startIcon={<PublicIcon />}
              sx={{ mr: 1 }}
            >
              Public
            </Button>
            <Button
              variant={newGroupData.isPrivate ? "contained" : "outlined"}
              onClick={() => setNewGroupData({...newGroupData, isPrivate: true})}
              startIcon={<LockIcon />}
            >
              Private
            </Button>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseCreateDialog}>Cancel</Button>
          <Button onClick={handleCreateGroup} variant="contained">Create</Button>
        </DialogActions>
      </Dialog>

      {/* Join Public Groups Dialog */}
      <Dialog open={openJoinDialog} onClose={handleCloseJoinDialog} maxWidth="sm" fullWidth>
        <DialogTitle>Join Public Groups</DialogTitle>
        <DialogContent>
          <List>
            {publicGroups.length > 0 ? (
              publicGroups.map((group) => (
                <ListItem key={group.id} divider>
                  <ListItemAvatar>
                    {group.avatar ? 
                      <Avatar alt={group.name} src={group.avatar} /> : 
                      <Avatar><GroupIcon /></Avatar>
                    }
                  </ListItemAvatar>
                  <ListItemText
                    primary={group.name}
                    secondary={group.description || "No description"}
                  />
                  <Button 
                    variant="contained" 
                    size="small"
                    onClick={() => handleJoinGroup(group.id)}
                  >
                    Join
                  </Button>
                </ListItem>
              ))
            ) : (
              <ListItem>
                <ListItemText primary="No public groups available" />
              </ListItem>
            )}
          </List>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseJoinDialog}>Close</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default GroupList;