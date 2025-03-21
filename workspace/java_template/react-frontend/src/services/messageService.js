import axios from 'axios';

const API_URL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080/api';

class MessageService {
  /**
   * Get messages for a specific group with pagination
   * @param {number} groupId - Group ID
   * @param {number} page - Page number (0-based)
   * @param {number} size - Page size
   * @returns {Promise<Object>} - Paginated message list
   */
  async getGroupMessages(groupId, page = 0, size = 30) {
    try {
      const response = await axios.get(`${API_URL}/messages/group/${groupId}`, {
        params: { page, size }
      });
      return response.data;
    } catch (error) {
      console.error('Error getting group messages:', error);
      throw error;
    }
  }

  /**
   * Send a text message to a group
   * @param {number} groupId - Group ID
   * @param {string} content - Message text
   * @returns {Promise<Object>} - Created message
   */
  async sendMessage(groupId, content) {
    try {
      const response = await axios.post(`${API_URL}/messages/group/${groupId}/text`, {
        content
      });
      return response.data;
    } catch (error) {
      console.error('Error sending message:', error);
      throw error;
    }
  }

  /**
   * Send a file attachment to a group
   * @param {number} groupId - Group ID
   * @param {FormData} formData - Form data containing the file
   * @returns {Promise<Object>} - Created message with attachment
   */
  async sendAttachment(groupId, formData) {
    try {
      const response = await axios.post(`${API_URL}/messages/group/${groupId}/attachment`, formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      });
      return response.data;
    } catch (error) {
      console.error('Error sending attachment:', error);
      throw error;
    }
  }

  /**
   * Get a specific message by ID
   * @param {number} messageId - Message ID
   * @returns {Promise<Object>} - Message data
   */
  async getMessage(messageId) {
    try {
      const response = await axios.get(`${API_URL}/messages/${messageId}`);
      return response.data;
    } catch (error) {
      console.error('Error getting message:', error);
      throw error;
    }
  }

  /**
   * Delete a message
   * @param {number} messageId - Message ID
   * @returns {Promise<Object>} - Response data
   */
  async deleteMessage(messageId) {
    try {
      const response = await axios.delete(`${API_URL}/messages/${messageId}`);
      return response.data;
    } catch (error) {
      console.error('Error deleting message:', error);
      throw error;
    }
  }

  /**
   * Edit a text message
   * @param {number} messageId - Message ID
   * @param {string} content - Updated content
   * @returns {Promise<Object>} - Updated message
   */
  async editMessage(messageId, content) {
    try {
      const response = await axios.put(`${API_URL}/messages/${messageId}`, {
        content
      });
      return response.data;
    } catch (error) {
      console.error('Error editing message:', error);
      throw error;
    }
  }

  /**
   * React to a message with an emoji
   * @param {number} messageId - Message ID
   * @param {string} reaction - Emoji reaction
   * @returns {Promise<Object>} - Response data
   */
  async addReaction(messageId, reaction) {
    try {
      const response = await axios.post(`${API_URL}/messages/${messageId}/reactions`, {
        reaction
      });
      return response.data;
    } catch (error) {
      console.error('Error adding reaction:', error);
      throw error;
    }
  }

  /**
   * Remove a reaction from a message
   * @param {number} messageId - Message ID
   * @param {string} reaction - Emoji reaction to remove
   * @returns {Promise<Object>} - Response data
   */
  async removeReaction(messageId, reaction) {
    try {
      const response = await axios.delete(`${API_URL}/messages/${messageId}/reactions`, {
        data: { reaction }
      });
      return response.data;
    } catch (error) {
      console.error('Error removing reaction:', error);
      throw error;
    }
  }

  /**
   * Search for messages in a group
   * @param {number} groupId - Group ID
   * @param {string} query - Search query
   * @param {number} page - Page number (0-based)
   * @param {number} size - Page size
   * @returns {Promise<Object>} - Search results
   */
  async searchMessages(groupId, query, page = 0, size = 20) {
    try {
      const response = await axios.get(`${API_URL}/messages/search`, {
        params: {
          groupId,
          query,
          page,
          size
        }
      });
      return response.data;
    } catch (error) {
      console.error('Error searching messages:', error);
      throw error;
    }
  }
}

const messageService = new MessageService();
export default messageService;