#!/bin/bash

# Structurizr Confluence Exporter - Test Script
# This script automates the testing process as required for PR validation

set -e  # Exit on any error

echo "🚀 Structurizr Confluence Exporter - Test Execution"
echo "=================================================="

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Step 1: Clean and compile
echo ""
echo "📦 Step 1: Clean and compile project"
echo "------------------------------------"
mvn clean compile

if [ $? -eq 0 ]; then
    print_status "Project compiled successfully"
else
    print_error "Compilation failed"
    exit 1
fi

# Step 2: Run unit tests (must always pass)
echo ""
echo "🧪 Step 2: Run unit tests (ADF Integration)"
echo "--------------------------------------------"
mvn test -Dtest=AdfIntegrationTest

if [ $? -eq 0 ]; then
    print_status "Unit tests passed - ADF generation working correctly"
else
    print_error "Unit tests failed - Fix ADF generation issues"
    exit 1
fi

# Step 3: Check for integration test credentials
echo ""
echo "🔐 Step 3: Check for integration test credentials"
echo "------------------------------------------------"

if [ -z "$CONFLUENCE_USER" ] || [ -z "$CONFLUENCE_TOKEN" ]; then
    print_warning "Integration test credentials not found"
    echo "To run integration tests, set:"
    echo "  export CONFLUENCE_USER=\"your-email@example.com\""
    echo "  export CONFLUENCE_TOKEN=\"your-confluence-api-token\""
    echo ""
    print_status "Unit tests passed successfully - Ready for PR"
    exit 0
fi

# Step 4: Run integration tests
echo ""
echo "🌐 Step 4: Run integration tests"
echo "--------------------------------"
print_warning "Running integration tests against live Confluence instance"
echo "URL: https://arnaudroubinet.atlassian.net"
echo "Space: Test"

mvn test -Dtest=ConfluenceIntegrationTest

if [ $? -eq 0 ]; then
    print_status "Integration tests passed"
    echo ""
    print_warning "REMINDER: Screenshot required for PR validation!"
    echo "Navigate to: https://arnaudroubinet.atlassian.net/wiki/spaces/Test/pages/10977556"
    echo "Capture screenshot showing updated page content"
else
    print_error "Integration tests failed"
    echo "Check your credentials and network connectivity"
    exit 1
fi

# Step 5: Run specific page update test
echo ""
echo "📄 Step 5: Test specific page update (10977556)"
echo "----------------------------------------------"
mvn test -Dtest=ConfluenceIntegrationTest#shouldUpdateSpecificConfluencePage

if [ $? -eq 0 ]; then
    print_status "Specific page update test passed"
    echo ""
    print_warning "🔥 MANDATORY: Include screenshot in PR"
    echo "URL: https://arnaudroubinet.atlassian.net/wiki/spaces/Test/pages/10977556"
    echo "Screenshot must show:"
    echo "  - Updated page title"
    echo "  - Workspace information"
    echo "  - Recent timestamp"
    echo "  - ADF formatted content"
else
    print_error "Specific page update test failed"
    exit 1
fi

echo ""
print_status "All tests completed successfully! 🎉"
print_warning "Don't forget the screenshot for PR validation!"