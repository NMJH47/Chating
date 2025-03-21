#!/bin/bash

# Run tests script for Private Domain Chat System
# This script executes different types of tests for the chat application

# Set environment variables for testing
export SPRING_PROFILES_ACTIVE=test

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}Starting Private Domain Chat System Tests${NC}"

# Navigate to the project directory
# Uncomment if you need to change directory
# cd /path/to/java_template

# Clean and package the project
echo -e "${YELLOW}Building the project...${NC}"
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
    echo -e "${RED}Build failed!${NC}"
    exit 1
fi
echo -e "${GREEN}Build successful!${NC}"

# Run unit tests
echo -e "${YELLOW}Running unit tests...${NC}"
mvn test
if [ $? -ne 0 ]; then
    echo -e "${RED}Unit tests failed!${NC}"
    exit 1
fi
echo -e "${GREEN}Unit tests passed!${NC}"

# Run integration tests (using a specific tag)
echo -e "${YELLOW}Running integration tests...${NC}"
mvn test -Dtest=*IntegrationTest
if [ $? -ne 0 ]; then
    echo -e "${RED}Integration tests failed!${NC}"
    exit 1
fi
echo -e "${GREEN}Integration tests passed!${NC}"

# Run end-to-end tests (using a specific tag)
echo -e "${YELLOW}Running end-to-end tests...${NC}"
mvn test -Dtest=*E2ETest
if [ $? -ne 0 ]; then
    echo -e "${RED}End-to-end tests failed!${NC}"
    exit 1
fi
echo -e "${GREEN}End-to-end tests passed!${NC}"

# Generate test coverage report
echo -e "${YELLOW}Generating test coverage report...${NC}"
mvn jacoco:report
echo -e "${GREEN}Test coverage report generated in target/site/jacoco/index.html${NC}"

# Run Postman tests (if Newman is installed)
if command -v newman &> /dev/null; then
    echo -e "${YELLOW}Running Postman API tests...${NC}"
    newman run postman/PrivateDomainChat_API_Tests.json -e postman/local-env.json
    if [ $? -ne 0 ]; then
        echo -e "${RED}Postman tests failed!${NC}"
        exit 1
    fi
    echo -e "${GREEN}Postman tests passed!${NC}"
else
    echo -e "${YELLOW}Newman not installed, skipping Postman tests${NC}"
fi

# Summary
echo -e "${GREEN}==========================${NC}"
echo -e "${GREEN}All tests completed successfully!${NC}"
echo -e "${GREEN}==========================${NC}"