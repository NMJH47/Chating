# Private Domain Chat System Testing Guide

This document provides a comprehensive guide to testing the Private Domain Chat System, including setup instructions, test execution, and troubleshooting tips.

## Table of Contents

1. [Testing Strategy Overview](#testing-strategy-overview)
2. [Test Environment Setup](#test-environment-setup)
3. [Running Tests](#running-tests)
4. [Unit Testing](#unit-testing)
5. [Integration Testing](#integration-testing)
6. [WebSocket Testing](#websocket-testing)
7. [API Testing with Postman](#api-testing-with-postman)
8. [End-to-End Testing](#end-to-end-testing)
9. [Frontend Testing](#frontend-testing)
10. [Test Coverage](#test-coverage)
11. [CI/CD Integration](#cicd-integration)
12. [Troubleshooting](#troubleshooting)

## Testing Strategy Overview

Our testing approach follows a pyramid model, with a solid foundation of unit tests complemented by integration tests, API tests, and end-to-end tests. The strategy ensures:

- **Comprehensive Coverage**: Testing across all layers of the application
- **Fast Feedback**: Quick unit tests for faster development cycles
- **Realistic Scenarios**: End-to-end tests that simulate real user flows
- **Isolated Component Testing**: Each system component is tested in isolation
- **Integration Validation**: Ensuring components work together correctly

## Test Environment Setup

### Prerequisites

- JDK 11 or higher
- Maven 3.6 or higher
- Docker (for containerized testing)
- Chrome browser (for Selenium tests)
- Node.js and npm (for frontend tests)
- Postman (for API testing)

### Database Setup

The test suite uses H2 in-memory database by default for unit and integration tests. No additional setup is required.

For end-to-end tests with PostgreSQL:



### Test Properties

Configure test-specific properties in `src/test/resources/application-test.properties`. Key configurations:

- JWT test secret key
- In-memory database connection
- Test user credentials
- WebSocket test configurations

## Running Tests

### All Tests

Use the provided shell script to run all tests:



### Specific Test Categories



## Unit Testing

Unit tests are available for:

- Controllers (`*ControllerTest.java`)
- Services (`*ServiceTest.java`)
- Mappers (`*MapperTest.java`)
- Model validation

### Example Unit Test Execution



## Integration Testing

Integration tests validate the interaction between components:

- Database integration (`*RepositoryTest.java`, `*MapperTest.java`)
- Authentication flow (`AuthControllerIntegrationTest.java`)
- Service-repository integration

### Example Integration Test Execution



## WebSocket Testing

WebSocket functionality is tested at multiple levels:

- Unit tests for message handling
- Integration tests for connection establishment
- End-to-end tests for complete WebSocket communication

### Example WebSocket Test Execution



## API Testing with Postman

A comprehensive Postman collection is provided for manual and automated API testing:

1. Import `PrivateDomainChat_API_Tests.json` into Postman
2. Set up environment variables:
   - `base_url`: Base URL for the API (default: `http://localhost:8080`)
3. Run the collection:
   - Manually through Postman UI
   - Automated via Newman:



## End-to-End Testing

End-to-end tests simulate real user flows using Selenium WebDriver:

- User registration and authentication
- Group creation and management
- Message sending and receiving
- WebSocket interaction

### Example E2E Test Execution



## Frontend Testing

Frontend React components are tested using Jest and React Testing Library:

### Setup



### Example Component Test

Login component test (`Login.test.js`) validates:
- Form rendering
- Input validation
- Authentication success/failure
- Loading states
- Navigation

## Test Coverage

Generate and view test coverage reports:



Coverage goals:
- Service layer: 80%+
- Repository layer: 70%+
- Controller layer: 80%+
- UI components: 70%+

## CI/CD Integration

The test suite is integrated with CI/CD pipelines:

### GitHub Actions Example
