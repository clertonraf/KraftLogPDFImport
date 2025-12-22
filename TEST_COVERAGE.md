# Test Suite Documentation

## Overview
This project includes comprehensive test coverage with **79 test methods** across **10 test classes** covering all components of the application, including unit tests and integration tests.

## Test Coverage

### Unit Tests (72 tests)

#### DTO Tests (15 tests)
- **ExerciseCreateRequestTest** (8 tests)
  - Validation testing for `@NotBlank` constraint on name field
  - Builder pattern testing
  - Constructor testing (NoArgs, AllArgs)
  - Setters and getters validation
  - Optional fields handling

- **ParsedExerciseDataTest** (7 tests)
  - Builder pattern testing
  - Constructor testing
  - Data manipulation (setters/getters)
  - Equals, hashCode, and toString methods
  - Null value handling

### Configuration Tests (18 tests)
- **KraftLogApiPropertiesTest** (5 tests)
  - Properties configuration
  - Auth inner class testing
  - Default values validation
  - Complete configuration testing

- **MuscleGroupMappingConfigTest** (13 tests)
  - YAML configuration loading
  - File not found handling
  - Empty/null path handling
  - Case-insensitive muscle group mapping
  - Null value handling in YAML
  - Muscle group headers retrieval

### Service Tests (17 tests)
- **PdfParserServiceTest** (5 tests)
  - Non-existent file handling
  - Null file parameter handling
  - Constructor validation
  - Service initialization
  - Configuration usage verification

- **ExerciseImportServiceTest** (12 tests)
  - Successful import scenarios
  - Partial failure handling
  - Complete failure scenarios  
  - Exception handling
  - Empty PDF handling
  - Muscle group mapping (with and without)
  - ImportResult state management
  - ImportFailure getters

### Controller Tests (12 tests)
- **ImportControllerTest** (12 tests)
  - Successful PDF import
  - Import with partial failures
  - Empty file rejection
  - Non-PDF file rejection
  - IOException handling
  - IllegalArgumentException handling
  - Health check endpoint
  - Null filename handling
  - Case-insensitive file extension
  - Special characters in filename
  - All failed imports
  - Temp file cleanup verification

### Client Tests (7 tests)
- **KraftLogApiClientTest** (7 tests)
  - Client instantiation
  - Null properties handling
  - Exercise request validation
  - API properties configuration
  - Multiple client instances
  - Empty base URL handling

### Application Tests (2 tests)
- **KraftLogPdfImportApplicationTest** (2 tests)
  - Spring context loading
  - Main method execution

---

### Integration Tests (7 tests)

- **KraftLogApiIntegrationTest** (7 tests)
  - Full end-to-end integration with KraftLog API
  - Uses WireMock to simulate API responses
  - Tests HTTP communication, authentication, and error handling
  
  **Test Scenarios:**
  1. **Successful Exercise Import** - Verifies complete import flow
  2. **API Authentication Failure** - Tests 401 Unauthorized handling
  3. **API Server Errors** - Tests 500 Internal Server Error handling
  4. **Partial Success** - Tests mixed success/failure scenarios
  5. **API Timeout** - Tests timeout handling (30s delay)
  6. **Request Format Validation** - Verifies JSON payload structure
  7. **End-to-End REST Endpoint** - Tests full HTTP request/response cycle

## Test Statistics
- **Total Test Files**: 10
- **Total Test Methods**: 79
  - **Unit Tests**: 72
  - **Integration Tests**: 7
- **Tests Passing (without Mockito)**: 42
- **Tests with Mockito (requires Java ≤ 21)**: 30
- **Integration Tests (requires WireMock)**: 7

## Running Tests

### Full Test Suite
```bash
mvn test
```

### Run Only Unit Tests
```bash
mvn test -DexcludeGroups=integration
```

### Run Only Integration Tests
```bash
mvn test -Dtest=*IntegrationTest
```

### Run Specific Test Class
```bash
mvn test -Dtest=ExerciseCreateRequestTest
mvn test -Dtest=KraftLogApiIntegrationTest
```

### Run with Coverage Report
```bash
mvn test jacoco:report
```

## Integration Test Details

### Technology Stack
- **WireMock 3.3.1**: HTTP mocking framework
- **Spring Boot Test**: Integration test support
- **TestRestTemplate**: HTTP client for testing
- **Random Port**: Tests use random ports to avoid conflicts

### Configuration
Integration tests use a separate profile (`integration`) with:
- Random server port
- WireMock server on dynamic port
- Test-specific credentials
- In-memory configuration

### Test Data
Tests use:
- Temporary PDF files created programmatically
- Mock HTTP responses from WireMock
- Realistic exercise data with muscle groups
- YouTube video links

## Known Issues

### Java 25 Compatibility
The project is currently running on Java 25, which has compatibility issues with Mockito 5.x. The tests that don't use Mockito (42 tests) pass successfully, but tests using mocking frameworks encounter issues.

**Resolution Options:**
1. **Recommended**: Use Java 21 LTS for full test compatibility
2. Wait for Mockito to release Java 25 compatible version
3. Refactor mocked tests to use real objects or test doubles

### To Run All Tests Successfully
Use Java 21:
```bash
sdk use java 21.0.x-tem  # if using SDKMan
# or
export JAVA_HOME=/path/to/java21
mvn clean test
```

## Test Coverage by Package

| Package | Classes | Test Classes | Test Methods |
|---------|---------|--------------|--------------|
| dto | 2 | 2 | 15 |
| config | 2 | 2 | 18 |
| service | 2 | 2 | 17 |
| controller | 1 | 1 | 12 |
| client | 1 | 1 | 7 |
| application | 1 | 1 | 2 |
| **Total** | **9** | **9** | **72** |

## Test Types

- **Unit Tests**: 70 tests - Testing individual components in isolation
- **Integration Tests**: 2 tests - Testing Spring application context loading

## Dependencies

Test dependencies include:
- Spring Boot Test Starter
- JUnit 5 (Jupiter)
- Mockito 5.15.2
- Byte Buddy 1.15.11 (for Mockito)
- Jakarta Validation API
- Hamcrest (via Spring Test)

## Best Practices Followed

1. **Arrange-Act-Assert Pattern**: All tests follow the AAA pattern
2. **Descriptive Test Names**: Test methods have clear, descriptive names
3. **One Assertion Per Test**: Most tests focus on a single behavior
4. **Test Isolation**: Tests don't depend on each other
5. **Mocking External Dependencies**: Services are tested with mocked dependencies
6. **Edge Case Testing**: Null values, empty inputs, and error conditions are tested
7. **Coverage of Happy and Sad Paths**: Both successful and failure scenarios are covered

## Future Improvements

1. Add integration tests for end-to-end PDF import workflow
2. Add performance tests for large PDF files
3. Add tests for concurrent import operations
4. Implement mutation testing to verify test quality
5. Add contract tests for API client
6. Increase code coverage to 90%+ with JaCoCo reports
