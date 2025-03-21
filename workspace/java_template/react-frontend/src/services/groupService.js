import axios from 'axios';

const API_URL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080/api';

class GroupService {
  /**
   * Get all groups the current user is a member of
   * @returns {Promise<Array>} - List of groups
   */
  async getUserGroups() {
    try {
      const response = await axios.get(`${API_URL}/users/current/groups`);
      return response.data;
    } catch (error) {
      console.error('Error getting user groups:', error);
      throw error;
    }
  }

  /**
   * Get public groups with pagination
   * @param {number} page - Page number (0-based)
   * @param {number} size - Page size
   * @returns {Promise<Object>} - Paginated list of public groups
   */
  async getPublicGroups(page = 0, size = 20) {
    try {
      const response = await axios.get(`${API_URL}/groups/public`, {
        params: { page, size }
      });
      return response.data;
    } catch (error) {
      console.error('Error getting public groups:', error);
      throw error;
    }
  }

  /**
   * Create a new group
   * @param {Object} groupData - Group data to create
   * @returns {Promise<Object>} - Created group
   */
  async createGroup(groupData) {
    try {
      const response = await axios.post(`${API_URL}/groups`, {
        name: groupData.name,
        description: groupData.description,
        isPrivate: groupData.isPrivate,
        maxMembers: groupData.maxMembers || 200
      });
      return response.data;
    } catch (error) {
      console.error('Error creating group:', error);
      throw error;
    }
  }

  /**
   * Join a public group
   * @param {number} groupId - ID of the group to join
   * @returns {Promise<Object>} - Join response
   */
  async joinGroup(groupId) {
    try {
      const response = await axios.post(`${API_URL}/groups/${groupId}/join`);
      return response.data;
    } catch (error) {
      console.error('Error joining group:', error);
      throw error;
    }
  }

  /**
   * Leave a group
   * @param {number} groupId - ID of the group to leave
   * @returns {Promise<Object>} - Leave response
   */
  async leaveGroup(groupId) {
    try {
      const response = await axios.post(`${API_URL}/groups/${groupId}/leave`);
      return response.data;
    } catch (error) {
      console.error('Error leaving group:', error);
      throw error;
    }
  }

  /**
   * Get group by ID
   * @param {number} groupId - Group ID
   * @returns {Promise<Object>} - Group data
   */
  async getGroup(groupId) {
    try {
      const response = await axios.get(`${API_URL}/groups/${groupId}`);
      return response.data;
    } catch (error) {
      console.error('Error getting group:', error);
      throw error;
    }
  }

  /**
   * Get all members of a group
   * @param {number} groupId - Group ID
   * @returns {Promise<Array>} - List of group members
   */
  async getGroupMembers(groupId) {
    try {
      const response = await axios.get(`${API_URL}/groups/${groupId}/members`);
      return response.data;
    } catch (error) {
      console.error('Error getting group members:', error);
      throw error;
    }
  }

  /**
   * Add a member to a group
   * @param {number} groupId - Group ID
   * @param {number} userId - User ID to add
   * @param {string} role - Role to assign (MEMBER, MODERATOR, ADMIN)
   * @returns {Promise<Object>} - Added member data
   */
  async addMember(groupId, userId, role = 'MEMBER') {
    try {
      const response = await axios.post(`${API_URL}/groups/${groupId}/members`, {
        userId,
        role
      });
      return response.data;
    } catch (error) {
      console.error('Error adding member:', error);
      throw error;
    }
  }

  /**
   * Remove a member from a group
   * @param {number} groupId - Group ID
   * @param {number} userId - User ID to remove
   * @returns {Promise<Object>} - Remove response
   */
  async removeMember(groupId, userId) {
    try {
      const response = await axios.delete(`${API_URL}/groups/${groupId}/members/${userId}`);
      return response.data;
    } catch (error) {
      console.error('Error removing member:', error);
      throw error;
    }
  }

  /**
   * Update a member's role in a group
   * @param {number} groupId - Group ID
   * @param {number} userId - User ID
   * @param {string} role - New role (MEMBER, MODERATOR, ADMIN)
   * @returns {Promise<Object>} - Updated member data
   */
  async updateMemberRole(groupId, userId, role) {
    try {
      const response = await axios.put(`${API_URL}/groups/${groupId}/members/${userId}/role`, {
        role
      });
      return response.data;
    } catch (error) {
      console.error('Error updating member role:', error);
      throw error;
    }
  }

  /**
   * Update group details
   * @param {number} groupId - Group ID
   * @param {Object} groupData - Updated group data
   * @returns {Promise<Object>} - Updated group
   */
  async updateGroup(groupId, groupData) {
    try {
      const response = await axios.put(`${API_URL}/groups/${groupId}`, groupData);
      return response.data;
    } catch (error) {
      console.error('Error updating group:', error);
      throw error;
    }
  }

  /**
   * Upload a group avatar
   * @param {number} groupId - Group ID
   * @param {FormData} formData - Form data containing the avatar file
   * @returns {Promise<Object>} - Upload response
   */
  async uploadGroupAvatar(groupId, formData) {
    try {
      const response = await axios.post(`${API_URL}/groups/${groupId}/avatar`, formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      });
      return response.data;
    } catch (error) {
      console.error('Error uploading group avatar:', error);
      throw error;
    }
  }

  /**
   * Delete a group (only owner can do this)
   * @param {number} groupId - Group ID
   * @returns {Promise<Object>} - Delete response
   */
  async deleteGroup(groupId) {
    try {
      const response = await axios.delete(`${API_URL}/groups/${groupId}`);
      return response.data;
    } catch (error) {
      console.error('Error deleting group:', error);
      throw error;
    }
  }

  /**
   * Update the last read timestamp for the current user in a group
   * @param {number} groupId - Group ID
   * @returns {Promise<Object>} - Response data
   */
  async updateLastRead(groupId) {
    try {
      const response = await axios.post(`${API_URL}/groups/${groupId}/lastread`);
      return response.data;
    } catch (error) {
      console.error('Error updating last read timestamp:', error);
      // Don't throw here since this is a background operation
      return null;
    }
  }
}

const groupService = new GroupService();
export default groupService;