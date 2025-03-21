import React, { useState, useEffect, useRef } from 'react';
import { useSelector } from 'react-redux';
import { 
  Box, 
  Typography, 
  Paper, 
  TextField, 
  IconButton, 
  Avatar, 
  List, 
  ListItem, 
  ListItemText, 
  ListItemAvatar, 
  Divider, 
  CircularProgress, 
  Button,
  Menu,
  MenuItem,
  Badge
} from '@mui/material';
import { 
  Send as SendIcon, 
  AttachFile as AttachFileIcon,
  MoreVert as MoreVertIcon,
  InsertEmoticon as EmojiIcon,
  Info as InfoIcon
} from '@mui/icons-material';
import messageService from '../services/messageService';
import groupService from '../services/groupService';
import websocketService from '../services/websocketService';
import { format } from 'date-fns';

const GroupChat = ({ group }) => {
  const [messages, setMessages] = useState([]);
  const [messageText, setMessageText] = useState('');
  const [loading, setLoading] = useState(false);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [members, setMembers] = useState([]);
  const [showMembers, setShowMembers] = useState(false);
  const [anchorEl, setAnchorEl] = useState(null);
  const [attachments, setAttachments] = useState([]);
  const [uploading, setUploading] = useState(false);
  
  const messagesEndRef = useRef(null);
  const fileInputRef = useRef(null);
  const messageListRef = useRef(null);
  
  const { user } = useSelector(state => state.auth);
  const pageSize = 30;
  
  // Load initial messages when group changes
  useEffect(() => {
    if (group) {
      setMessages([]);
      setPage(0);
      setHasMore(true);
      loadMessages(0, true);
      loadGroupMembers();
      
      // Mark messages as read
      groupService.updateLastRead(group.id);
      
      // Reset message input and attachments
      setMessageText('');
      setAttachments([]);
    }
  }, [group]);
  
  // Scroll to bottom when messages change
  useEffect(() => {
    if (messages.length > 0 && page === 0) {
      scrollToBottom();
    }
  }, [messages, page]);

  // Handle incoming websocket messages
  useEffect(() => {
    if (!group) return;
    
    const handleWebSocketMessage = (event) => {
      const data = JSON.parse(event.data);
      
      if (data.type === 'NEW_MESSAGE' && data.groupId === group.id) {
        const newMessage = {
          id: data.messageId,
          groupId: data.groupId,
          senderId: data.senderId,
          senderName: data.senderName,
          content: data.content,
          type: data.messageType,
          attachments: data.attachments || [],
          timestamp: new Date(data.timestamp),
          status: 'DELIVERED'
        };
        
        setMessages(prev => [...prev, newMessage]);
        
        // Mark as read if we're actively looking at this group
        if (data.senderId !== user.id) {
          groupService.updateLastRead(group.id);
        }
      }
    };
    
    websocketService.onMessage(handleWebSocketMessage);
    
    return () => {
      websocketService.offMessage(handleWebSocketMessage);
    };
  }, [group, user]);
  
  const loadMessages = async (pageNumber, isInitial = false) => {
    if (!group || (loading && !isInitial)) return;
    
    setLoading(true);
    try {
      const response = await messageService.getGroupMessages(group.id, pageNumber, pageSize);
      
      // If we got fewer messages than the page size, there are no more messages
      if (response.content.length < pageSize) {
        setHasMore(false);
      }
      
      // Format messages for display
      const formattedMessages = response.content.map(msg => ({
        id: msg.id,
        groupId: msg.groupId,
        senderId: msg.senderId,
        senderName: msg.senderName,
        content: msg.content,
        type: msg.type,
        attachments: msg.attachments || [],
        timestamp: new Date(msg.timestamp),
        status: msg.status
      }));
      
      // Add messages to state (prepend for older messages)
      if (isInitial) {
        setMessages(formattedMessages.reverse());
      } else {
        setMessages(prev => [...formattedMessages.reverse(), ...prev]);
      }
    } catch (error) {
      console.error('Failed to load messages:', error);
    } finally {
      setLoading(false);
    }
  };
  
  const loadGroupMembers = async () => {
    if (!group) return;
    
    try {
      const membersData = await groupService.getGroupMembers(group.id);
      setMembers(membersData);
    } catch (error) {
      console.error('Failed to load group members:', error);
    }
  };
  
  const loadMoreMessages = () => {
    if (hasMore && !loading) {
      const nextPage = page + 1;
      setPage(nextPage);
      loadMessages(nextPage);
    }
  };
  
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  };
  
  const handleScroll = (e) => {
    const { scrollTop } = e.target;
    
    // Load more messages when scrolled to top (with small threshold)
    if (scrollTop < 50 && hasMore && !loading) {
      loadMoreMessages();
    }
  };
  
  const handleSendMessage = async (e) => {
    e.preventDefault();
    
    if (!messageText.trim() && attachments.length === 0) return;
    
    try {
      if (attachments.length > 0) {
        setUploading(true);
        
        // Handle file uploads one by one
        for (const file of attachments) {
          const formData = new FormData();
          formData.append('file', file);
          
          await messageService.sendAttachment(group.id, formData);
        }
        
        setAttachments([]);
      }
      
      // Send text message if there's text
      if (messageText.trim()) {
        await messageService.sendMessage(group.id, messageText);
      }
      
      setMessageText('');
      setUploading(false);
    } catch (error) {
      console.error('Failed to send message:', error);
      setUploading(false);
    }
  };
  
  const handleOpenMenu = (event) => {
    setAnchorEl(event.currentTarget);
  };
  
  const handleCloseMenu = () => {
    setAnchorEl(null);
  };
  
  const toggleMembersList = () => {
    setShowMembers(!showMembers);
    handleCloseMenu();
  };
  
  const handleAttachFile = () => {
    fileInputRef.current.click();
  };
  
  const handleFileChange = (e) => {
    const files = Array.from(e.target.files);
    setAttachments([...attachments, ...files]);
    
    // Reset file input so the same file can be selected again if needed
    e.target.value = '';
  };
  
  const handleRemoveAttachment = (index) => {
    setAttachments(attachments.filter((_, i) => i !== index));
  };
  
  const formatMessageTime = (timestamp) => {
    return format(timestamp, 'HH:mm');
  };
  
  const formatMessageDate = (timestamp) => {
    return format(timestamp, 'MMM dd, yyyy');
  };
  
  const renderMessage = (message, index, messages) => {
    const isCurrentUser = message.senderId === user.id;
    const showSender = !isCurrentUser && (index === 0 || messages[index - 1].senderId !== message.senderId);
    const showDate = index === 0 || 
      formatMessageDate(message.timestamp) !== formatMessageDate(messages[index - 1].timestamp);
    
    return (
      <React.Fragment key={message.id || `temp-${index}`}>
        {showDate && (
          <Box sx={{ textAlign: 'center', py: 2 }}>
            <Typography variant="caption" sx={{ 
              backgroundColor: 'rgba(0,0,0,0.05)', 
              py: 0.5, 
              px: 2, 
              borderRadius: 10 
            }}>
              {formatMessageDate(message.timestamp)}
            </Typography>
          </Box>
        )}
        
        <Box
          sx={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: isCurrentUser ? 'flex-end' : 'flex-start',
            mb: 1,
          }}
        >
          {showSender && !isCurrentUser && (
            <Typography variant="caption" sx={{ ml: 7, mb: 0.5 }}>
              {message.senderName}
            </Typography>
          )}
          
          <Box sx={{ display: 'flex', alignItems: 'flex-end' }}>
            {!isCurrentUser && showSender && (
              <Avatar 
                sx={{ width: 32, height: 32, mr: 1 }}
                alt={message.senderName}
                src={`/api/users/${message.senderId}/avatar`} 
              />
            )}
            
            {!isCurrentUser && !showSender && <Box sx={{ width: 32, mr: 1 }} />}
            
            <Paper
              elevation={1}
              sx={{
                p: 1.5,
                borderRadius: 2,
                maxWidth: '75%',
                wordBreak: 'break-word',
                backgroundColor: isCurrentUser ? 'primary.light' : 'grey.100',
                color: isCurrentUser ? 'white' : 'inherit',
              }}
            >
              {message.content && (
                <Typography variant="body1">{message.content}</Typography>
              )}
              
              {message.attachments && message.attachments.length > 0 && (
                <Box sx={{ mt: message.content ? 1 : 0 }}>
                  {message.attachments.map((attachment, idx) => {
                    const isImage = /\.(jpeg|jpg|png|gif)$/i.test(attachment.filename);
                    
                    return isImage ? (
                      <Box key={idx} sx={{ mt: 1 }}>
                        <img 
                          src={attachment.url} 
                          alt={attachment.filename} 
                          style={{ 
                            maxWidth: '100%', 
                            maxHeight: 200, 
                            borderRadius: 4 
                          }} 
                        />
                      </Box>
                    ) : (
                      <Button
                        key={idx}
                        variant="outlined"
                        size="small"
                        component="a"
                        href={attachment.url}
                        target="_blank"
                        sx={{ mt: 1, textTransform: 'none' }}
                      >
                        {attachment.filename}
                      </Button>
                    );
                  })}
                </Box>
              )}
              
              <Typography 
                variant="caption" 
                sx={{ 
                  display: 'block', 
                  textAlign: 'right', 
                  mt: 0.5,
                  opacity: 0.7 
                }}
              >
                {formatMessageTime(message.timestamp)}
              </Typography>
            </Paper>
          </Box>
        </Box>
      </React.Fragment>
    );
  };
  
  const renderMembersList = () => {
    if (!showMembers) return null;
    
    return (
      <Box 
        sx={{ 
          width: 280, 
          height: '100%',
          borderLeft: '1px solid',
          borderColor: 'divider',
          display: 'flex',
          flexDirection: 'column',
          bgcolor: 'background.paper'
        }}
      >
        <Box sx={{ 
          p: 2, 
          borderBottom: '1px solid',
          borderColor: 'divider',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center'
        }}>
          <Typography variant="h6">Members</Typography>
          <IconButton size="small" onClick={toggleMembersList}>
            <InfoIcon fontSize="small" />
          </IconButton>
        </Box>
        
        <List sx={{ overflow: 'auto', flexGrow: 1 }}>
          {members.map((member) => (
            <ListItem key={member.userId} divider>
              <ListItemAvatar>
                <Avatar 
                  alt={member.username}
                  src={`/api/users/${member.userId}/avatar`} 
                />
              </ListItemAvatar>
              <ListItemText 
                primary={member.username}
                secondary={member.role.toLowerCase()} 
              />
            </ListItem>
          ))}
        </List>
      </Box>
    );
  };
  
  if (!group) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100%' }}>
        <Typography>Select a group to start chatting</Typography>
      </Box>
    );
  }
  
  return (
    <Box sx={{ display: 'flex', height: '100%' }}>
      <Box sx={{ display: 'flex', flexDirection: 'column', flexGrow: 1, height: '100%' }}>
        {/* Group Header */}
        <Box sx={{ 
          px: 2, 
          py: 1, 
          display: 'flex', 
          alignItems: 'center',
          justifyContent: 'space-between',
          borderBottom: '1px solid',
          borderColor: 'divider',
          bgcolor: 'background.paper'
        }}>
          <Box sx={{ display: 'flex', alignItems: 'center' }}>
            <Avatar
              alt={group.name}
              src={group.avatar || ''}
              sx={{ width: 40, height: 40, mr: 2 }}
            />
            <Box>
              <Typography variant="h6">{group.name}</Typography>
              <Typography variant="caption" color="text.secondary">
                {members.length} members
              </Typography>
            </Box>
          </Box>
          <IconButton onClick={handleOpenMenu}>
            <MoreVertIcon />
          </IconButton>
          <Menu
            anchorEl={anchorEl}
            open={Boolean(anchorEl)}
            onClose={handleCloseMenu}
            anchorOrigin={{
              vertical: 'bottom',
              horizontal: 'right',
            }}
            transformOrigin={{
              vertical: 'top',
              horizontal: 'right',
            }}
          >
            <MenuItem onClick={toggleMembersList}>
              {showMembers ? 'Hide' : 'Show'} Members
            </MenuItem>
          </Menu>
        </Box>
        
        {/* Messages List */}
        <Box
          ref={messageListRef}
          sx={{
            flexGrow: 1,
            overflow: 'auto',
            display: 'flex',
            flexDirection: 'column',
            p: 2,
            bgcolor: '#f5f7f9',
          }}
          onScroll={handleScroll}
        >
          {loading && page > 0 && (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 2 }}>
              <CircularProgress size={30} />
            </Box>
          )}
          
          {messages.map((message, index, array) => renderMessage(message, index, array))}
          
          {loading && page === 0 && (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 2 }}>
              <CircularProgress size={30} />
            </Box>
          )}
          
          {/* Empty state for no messages */}
          {!loading && messages.length === 0 && (
            <Box 
              sx={{ 
                display: 'flex', 
                flexDirection: 'column',
                justifyContent: 'center', 
                alignItems: 'center',
                height: '100%'
              }}
            >
              <Typography variant="h6" color="text.secondary">
                No messages yet
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Be the first to send a message!
              </Typography>
            </Box>
          )}
          
          {/* This element is for auto-scrolling to the bottom of messages */}
          <div ref={messagesEndRef} />
        </Box>
        
        {/* Message Input */}
        <Box
          component="form"
          onSubmit={handleSendMessage}
          sx={{
            p: 2,
            borderTop: '1px solid',
            borderColor: 'divider',
            bgcolor: 'background.paper',
          }}
        >
          {/* Attachment preview */}
          {attachments.length > 0 && (
            <Box sx={{ mb: 2, display: 'flex', flexWrap: 'wrap', gap: 1 }}>
              {attachments.map((file, index) => (
                <Box 
                  key={index}
                  sx={{
                    display: 'flex',
                    alignItems: 'center',
                    bgcolor: 'background.default',
                    borderRadius: 1,
                    p: 0.5,
                    pr: 1
                  }}
                >
                  <IconButton 
                    size="small" 
                    onClick={() => handleRemoveAttachment(index)}
                    sx={{ mr: 0.5 }}
                  >
                    <Badge badgeContent="×" color="error" />
                  </IconButton>
                  <Typography variant="caption" noWrap sx={{ maxWidth: 150 }}>
                    {file.name}
                  </Typography>
                </Box>
              ))}
            </Box>
          )}
          
          {/* Message input field and buttons */}
          <Box sx={{ display: 'flex', alignItems: 'center' }}>
            <IconButton 
              color="primary" 
              component="span"
              onClick={handleAttachFile}
            >
              <AttachFileIcon />
            </IconButton>
            
            {/* Hidden file input */}
            <input
              ref={fileInputRef}
              accept="image/*,.pdf,.doc,.docx,.xls,.xlsx,.txt"
              type="file"
              multiple
              style={{ display: 'none' }}
              onChange={handleFileChange}
            />
            
            <TextField
              fullWidth
              placeholder="Type a message..."
              variant="outlined"
              value={messageText}
              onChange={(e) => setMessageText(e.target.value)}
              disabled={uploading}
              multiline
              maxRows={4}
              sx={{ mx: 1 }}
            />
            
            <IconButton color="primary" disabled={uploading}>
              <EmojiIcon />
            </IconButton>
            
            <IconButton 
              color="primary" 
              type="submit" 
              disabled={(messageText.trim() === '' && attachments.length === 0) || uploading}
            >
              {uploading ? <CircularProgress size={24} /> : <SendIcon />}
            </IconButton>
          </Box>
        </Box>
      </Box>
      
      {/* Members sidebar */}
      {renderMembersList()}
    </Box>
  );
};

export default GroupChat;