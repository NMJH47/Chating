class WebSocketService {
  constructor() {
    this.socket = null;
    this.isConnected = false;
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
    this.reconnectDelay = 3000; // 3 seconds
    this.listeners = [];
    this.wsBaseUrl = process.env.REACT_APP_WS_BASE_URL || 'ws://localhost:8080/ws';
  }

  /**
   * Connect to WebSocket server
   * @param {string} token - JWT authentication token
   */
  connect(token) {
    if (this.socket && this.isConnected) {
      console.log('WebSocket is already connected');
      return;
    }
    
    try {
      const url = `${this.wsBaseUrl}?token=${token}`;
      this.socket = new WebSocket(url);
      
      this.socket.onopen = () => {
        console.log('WebSocket connection established');
        this.isConnected = true;
        this.reconnectAttempts = 0;
        
        // Send a ping every 30 seconds to keep the connection alive
        this.pingInterval = setInterval(() => {
          if (this.isConnected) {
            this.sendPing();
          }
        }, 30000);
      };
      
      this.socket.onclose = (event) => {
        this.isConnected = false;
        clearInterval(this.pingInterval);
        
        if (!event.wasClean) {
          console.log('WebSocket connection lost, attempting to reconnect...');
          this.attemptReconnect(token);
        } else {
          console.log('WebSocket connection closed cleanly');
        }
      };
      
      this.socket.onerror = (error) => {
        console.error('WebSocket error:', error);
        this.isConnected = false;
      };
      
      this.socket.onmessage = (event) => {
        this.notifyListeners(event);
      };
    } catch (error) {
      console.error('Error connecting to WebSocket server:', error);
      this.isConnected = false;
    }
  }

  /**
   * Send a ping to keep the connection alive
   */
  sendPing() {
    if (this.socket && this.isConnected) {
      try {
        this.socket.send(JSON.stringify({ type: 'PING' }));
      } catch (error) {
        console.error('Error sending ping:', error);
      }
    }
  }

  /**
   * Attempt to reconnect to WebSocket server
   * @param {string} token - JWT authentication token
   */
  attemptReconnect(token) {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('Maximum reconnect attempts reached');
      return;
    }
    
    this.reconnectAttempts += 1;
    
    setTimeout(() => {
      console.log(`Reconnect attempt ${this.reconnectAttempts}...`);
      this.connect(token);
    }, this.reconnectDelay * this.reconnectAttempts); // Exponential backoff
  }

  /**
   * Disconnect from WebSocket server
   */
  disconnect() {
    if (this.socket) {
      clearInterval(this.pingInterval);
      this.socket.close();
      this.socket = null;
      this.isConnected = false;
      console.log('WebSocket disconnected');
    }
  }

  /**
   * Send a message to the WebSocket server
   * @param {Object} data - Message data to send
   * @returns {boolean} - Whether the message was sent
   */
  send(data) {
    if (!this.socket || !this.isConnected) {
      console.error('Cannot send message: WebSocket is not connected');
      return false;
    }
    
    try {
      this.socket.send(JSON.stringify(data));
      return true;
    } catch (error) {
      console.error('Error sending WebSocket message:', error);
      return false;
    }
  }

  /**
   * Add a listener for incoming messages
   * @param {Function} listener - Callback function for message events
   */
  onMessage(listener) {
    if (typeof listener !== 'function') {
      console.error('Listener must be a function');
      return;
    }
    
    this.listeners.push(listener);
  }

  /**
   * Remove a specific message listener
   * @param {Function} listener - Listener to remove
   */
  offMessage(listener) {
    this.listeners = this.listeners.filter(l => l !== listener);
  }

  /**
   * Notify all listeners about a new message
   * @param {Object} event - WebSocket message event
   */
  notifyListeners(event) {
    this.listeners.forEach(listener => {
      try {
        listener(event);
      } catch (error) {
        console.error('Error in WebSocket listener:', error);
      }
    });
  }

  /**
   * Check if the WebSocket is connected
   * @returns {boolean} - Connection status
   */
  isConnected() {
    return this.isConnected;
  }
}

const websocketService = new WebSocketService();
export default websocketService;