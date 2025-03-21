# Private Domain Chat System

## Project Overview

Private Domain Chat System is a secure, feature-rich chat application built with Spring Boot and React. It provides real-time messaging capabilities with robust authentication, group management, and message handling features.

This system is designed for organizations requiring secure internal communications, offering a complete solution with user management, group chats, real-time messaging, and file sharing capabilities.

## Features

### User Management
- **Registration and Authentication**: Secure signup and login with JWT authentication
- **Role-based Access Control**: Admin, moderator, and standard user roles
- **Profile Management**: Update personal information and avatar images

### Group Chat
- **Create and Join Groups**: Create private/public chat groups and join existing ones
- **Member Management**: Add/remove members and assign roles within groups
- **Group Settings**: Configure privacy settings and group information

### Messaging
- **Real-time Communication**: WebSocket-based instant messaging
- **Message Types**: Support for text messages and file attachments
- **Message History**: Persistent message storage with pagination
- **Read Status**: Track message delivery and read status

### Security
- **JWT Authentication**: Secure token-based authentication
- **Password Encryption**: Bcrypt password hashing
- **Private Groups**: Invite-only access for sensitive conversations

### Additional Features
- **File Sharing**: Upload and share files within group conversations
- **User Status**: Display online/offline status
- **Typing Indicators**: Show when users are typing messages
- **Search**: Search message history and groups

## Technology Stack

### Backend
- **Java 11+** with **Spring Boot**
- **Spring Security** with **JWT** for authentication
- **Spring WebSocket** for real-time messaging
- **Spring Data JPA** with **MyBatis Plus** for data access
- **PostgreSQL** for user data and groups
- **MongoDB** for message storage
- **Redis** for caching and WebSocket session management

### Frontend
- **React** with functional components and hooks
- **Redux Toolkit** for state management
- **Material-UI** for component styling
- **Axios** for HTTP requests
- **SockJS/STOMP** for WebSocket communication

## Setup Instructions

### Prerequisites
- JDK 11 or higher
- Node.js 14.x or higher
- npm or yarn
- Docker (for running databases)
- Git

### Backend Setup

1. **Clone the repository**
   

2. **Start databases with Docker**
   Create a `docker-compose.yml` file in your project root with the following content:
   ```yaml
   version: '3.8'

   services:
     postgres:
       image: postgres:13
       container_name: private-chat-postgres
       ports:
         - "5432:5432"
       environment:
         - POSTGRES_USER=postgres
         - POSTGRES_PASSWORD=postgres
         - POSTGRES_DB=private_chat
       volumes:
         - postgres-data:/var/lib/postgresql/data
       restart: unless-stopped

     mongodb:
       image: mongo:4.4
       container_name: private-chat-mongodb
       ports:
         - "27017:27017"
       environment:
         - MONGO_INITDB_ROOT_USERNAME=mongodb
         - MONGO_INITDB_ROOT_PASSWORD=mongodb
         - MONGO_INITDB_DATABASE=private_chat_messages
       volumes:
         - mongo-data:/data/db
       restart: unless-stopped

     redis:
       image: redis:6
       container_name: private-chat-redis
       ports:
         - "6379:6379"
       volumes:
         - redis-data:/data
       restart: unless-stopped

   volumes:
     postgres-data:
     mongo-data:
     redis-data:
   ```

   Run the Docker containers:
   

3. **Configure application properties**
   Edit `src/main/resources/application-dev.properties` if needed (defaults should work with the Docker setup).

4. **Build and run the backend**
   

   The backend will start on http://localhost:8080

### Frontend Setup

1. **Navigate to the frontend directory**
   

2. **Install dependencies**
   

3. **Create environment variables**
   Create a `.env` file in the frontend directory:
   ```
   REACT_APP_API_BASE_URL=http://localhost:8080/api
   REACT_APP_WS_BASE_URL=ws://localhost:8080/ws
   ```

4. **Run the frontend**
   

   The frontend will start on http://localhost:3000

## Usage Guide

### Getting Started

1. **Register an account** using the sign-up page (or use the default admin account)
   - Default admin credentials: username: `admin`, password: `admin123`

2. **Login** to access the chat interface

3. **Create or join a group**
   - Click "Create" to start a new group
   - Click "Join" to browse public groups

4. **Start messaging**
   - Select a group from the left sidebar
   - Type messages in the input field at the bottom
   - Use the attachment button to share files

### Group Management

1. **Create a group**
   - Set a name, description, and privacy setting
   - Private groups require invitations to join
   - Public groups can be joined by anyone

2. **Invite members**
   - Open group settings
   - Use the "Add Member" option
   - Enter username and select role

3. **Manage members**
   - View members in the group sidebar
   - Promote/demote members (if you're an admin)
   - Remove members (if you have permission)

### Messaging Features

1. **Send messages**
   - Text messages: Type and press Enter or click Send
   - Attachments: Click the attachment icon, select files

2. **Message history**
   - Scroll up to view older messages
   - Messages are grouped by date

3. **Message status**
   - Delivered: Message received by server
   - Read: Message seen by recipients

## Architecture Overview

### System Architecture
The application follows a microservices-inspired design with clear component separation:

1. **Authentication Service**
   - Handles user registration, login, and JWT token management
   - Enforces role-based access control

2. **User Service**
   - Manages user profiles, settings, and preferences
   - Handles avatar uploads and user information

3. **Group Service**
   - Manages group creation, membership, and settings
   - Controls access rules based on privacy settings

4. **Message Service**
   - Processes and stores messages
   - Handles file attachments and message delivery

5. **WebSocket Service**
   - Manages real-time message delivery
   - Tracks online status and typing indicators

### Data Flow
1. User authentication via JWT
2. Authorized API requests for data operations
3. WebSocket connection for real-time updates
4. Persistent storage in PostgreSQL and MongoDB
5. Caching with Redis for performance

### Security Design
1. Authentication with JWT tokens
2. Password encryption with BCrypt
3. Role-based access control
4. Secure WebSocket connections
5. Input validation and sanitization

## API Documentation

The API documentation is available via Swagger UI once the application is running:
http://localhost:8080/swagger-ui/

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Spring Boot Team for the robust backend framework
- React Team for the flexible frontend library
- The open-source community for various libraries and tools used in this project