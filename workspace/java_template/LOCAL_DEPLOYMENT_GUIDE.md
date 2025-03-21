# Local Deployment Guide for Private Domain Chat System

This guide will help you set up and run the Private Domain Chat System on your local machine. Follow these step-by-step instructions to deploy both the backend and frontend components, configure the necessary databases, and start using the application.

## Table of Contents

1. [Environment Preparation](#1-environment-preparation)
2. [Database Setup](#2-database-setup)
3. [Backend Setup](#3-backend-setup)
4. [Frontend Setup](#4-frontend-setup)
5. [Running the Application](#5-running-the-application)
6. [Test Accounts & Login](#6-test-accounts--login)
7. [Feature Guide](#7-feature-guide)
8. [Troubleshooting](#8-troubleshooting)

## 1. Environment Preparation

### Required Software

Make sure you have the following software installed on your machine:

- **Java Development Kit (JDK)**: Version 11 or higher
- **Maven**: Latest version
- **Node.js**: Version 14.x or higher
- **npm** or **yarn**: Latest version
- **Docker**: Latest version (for running databases)
- **Git**: Latest version

### Verifying Installations

Run the following commands to verify your installations:

```bash
java -version
mvn -version
node -v
npm -v
docker --version
git --version
```

### Environment Variables

Set the following environment variables if needed:

- `JAVA_HOME`: Points to your JDK installation directory
- `MAVEN_HOME`: Points to your Maven installation directory
- `NODE_HOME`: Points to your Node.js installation directory

## 2. Database Setup

The Private Domain Chat System requires PostgreSQL, MongoDB, and Redis. We'll use Docker to set up these databases.

### Start Docker Containers

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

Run the docker-compose file to start all the databases:

```bash
docker-compose up -d
```

Verify that the containers are running:

```bash
docker ps
```

### Initialize PostgreSQL Database

Connect to the PostgreSQL container and create necessary tables:

```bash
docker exec -it private-chat-postgres psql -U postgres -d private_chat
```

The application will handle database schema creation through Hibernate when it starts for the first time. You can exit the PostgreSQL console with `\q`.

## 3. Backend Setup

### Clone the Repository

If you haven't already, clone the repository:

```bash
git clone <repository-url>
cd private-domain-chat
```

### Configure Application Properties

The application uses different property files for different environments. For local development, edit the `src/main/resources/application-dev.properties` file if needed:

```properties
# Database Configuration - PostgreSQL
spring.datasource.url=jdbc:postgresql://localhost:5432/private_chat
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.datasource.driver-class-name=org.postgresql.Driver

# MongoDB Configuration
spring.data.mongodb.host=localhost
spring.data.mongodb.port=27017
spring.data.mongodb.database=private_chat_messages
spring.data.mongodb.username=mongodb
spring.data.mongodb.password=mongodb

# Redis Configuration
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.password=
```

### Build the Application

Build the application using Maven:

```bash
mvn clean install -DskipTests
```

### Run the Backend

Start the Spring Boot application:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The backend will be available at http://localhost:8080

## 4. Frontend Setup

### Navigate to Frontend Directory

```bash
cd react-frontend
```

### Install Dependencies

```bash
npm install
# OR if you use yarn
yarn install
```

### Configure Environment

Create a `.env` file in the frontend directory with the following content:

```
REACT_APP_API_BASE_URL=http://localhost:8080/api
REACT_APP_WS_BASE_URL=ws://localhost:8080/ws
```

### Run the Frontend

```bash
npm start
# OR if you use yarn
yarn start
```

The frontend application will be available at http://localhost:3000

## 5. Running the Application

### Accessing the Application

Once both backend and frontend are running, you can access the application at:

- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080/api
- **Swagger UI** (if enabled): http://localhost:8080/swagger-ui/

### Health Check

Verify the backend is running correctly by accessing:

```
http://localhost:8080/api/actuator/health
```

## 6. Test Accounts & Login

### Default Admin Account

The system automatically creates an admin account on first startup:

- **Username**: admin
- **Password**: admin123

### Creating a New Account

1. Open the application in your browser: http://localhost:3000
2. Click on "Sign Up" or "Register"
3. Fill in the registration form with your details
4. Submit the form to create a new account
5. Use your credentials to log in

## 7. Feature Guide

### User Management

- **Registration**: Create a new user account
- **Login/Logout**: Authenticate users and manage sessions
- **Profile Management**: Update user information, change password, and upload avatar

### Group Chat Features

- **Create Group**: Create a new chat group with name, description, and privacy settings
- **Join Group**: Join existing public groups
- **Invite Members**: Invite other users to your groups
- **Manage Roles**: Assign different roles to group members (Owner, Admin, Moderator, Member)
- **Leave Group**: Leave a group you no longer want to participate in

### Messaging Features

- **Send Text Messages**: Send text messages in group chats
- **Upload Attachments**: Share files, images, and other media
- **Message History**: View message history with pagination
- **Message Status**: See message delivery and read status

### Real-time Communication

- **WebSocket Connection**: Automatic real-time updates
- **Online Status**: See which users are currently online
- **Typing Indicators**: See when other users are typing

## 8. Troubleshooting

### Common Issues

#### Database Connection Issues

- **Problem**: Cannot connect to PostgreSQL/MongoDB/Redis
- **Solution**: Verify Docker containers are running with `docker ps`, check connection parameters in application properties

#### Backend Fails to Start

- **Problem**: Spring Boot application fails to start
- **Solution**: Check logs for errors, verify database connections, ensure required ports are not in use

#### Frontend Connection Issues

- **Problem**: Frontend cannot connect to backend API
- **Solution**: Verify backend is running, check API URL in frontend environment configuration

#### WebSocket Connection Failures

- **Problem**: Real-time updates not working
- **Solution**: Check browser console for WebSocket errors, verify WebSocket URL, ensure backend WebSocket endpoint is accessible

### Getting Help

If you encounter issues not covered in this guide, please:

1. Check the application logs for detailed error messages
2. Consult the project's GitHub issues or documentation
3. Contact the development team for support

---

© 2025 Private Domain Chat System - All Rights Reserved
