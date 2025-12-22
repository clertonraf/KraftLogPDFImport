package com.kraftlog.pdfimport.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.kraftlog.pdfimport.service.ExerciseImportService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for KraftLog PDF Import with KraftLog API.
 * Uses WireMock to simulate KraftLog API responses.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class KraftLogApiIntegrationTest {

    private static WireMockServer wireMockServer;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ExerciseImportService exerciseImportService;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("kraftlog.api.base-url", () -> "http://localhost:" + wireMockServer.port());
        registry.add("wiremock.server.port", () -> String.valueOf(wireMockServer.port()));
    }

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();
    }

    @Test
    @Order(1)
    @DisplayName("Integration: Should successfully import exercises to KraftLog API")
    void testSuccessfulExerciseImport() throws IOException {
        // Given: Mock KraftLog API successful response
        wireMockServer.stubFor(post(urlEqualTo("/api/exercises"))
                .withBasicAuth("test-user", "test-password")
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":1,\"name\":\"Supino Reto\",\"muscleGroup\":\"Chest\"}")));

        // Create a test PDF file
        File testPdf = createTestPdfFile();

        try {
            // When: Import exercises
            ExerciseImportService.ImportResult result = 
                    exerciseImportService.importExercisesFromPdf(testPdf);

            // Then: Verify import was successful
            assertThat(result).isNotNull();
            assertThat(result.getSuccessCount()).isGreaterThan(0);
            assertThat(result.getFailureCount()).isEqualTo(0);

            // Verify API was called
            wireMockServer.verify(postRequestedFor(urlEqualTo("/api/exercises"))
                    .withBasicAuth(new com.github.tomakehurst.wiremock.matching.BasicCredentials("test-user", "test-password")));
        } finally {
            testPdf.delete();
        }
    }

    @Test
    @Order(2)
    @DisplayName("Integration: Should handle API authentication failure")
    void testApiAuthenticationFailure() throws IOException {
        // Given: Mock KraftLog API authentication failure
        wireMockServer.stubFor(post(urlEqualTo("/api/exercises"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"Unauthorized\"}")));

        File testPdf = createTestPdfFile();

        try {
            // When: Import exercises
            ExerciseImportService.ImportResult result = 
                    exerciseImportService.importExercisesFromPdf(testPdf);

            // Then: All imports should fail
            assertThat(result).isNotNull();
            assertThat(result.getFailureCount()).isGreaterThan(0);
            assertThat(result.getFailures()).isNotEmpty();
        } finally {
            testPdf.delete();
        }
    }

    @Test
    @Order(3)
    @DisplayName("Integration: Should handle API server errors")
    void testApiServerError() throws IOException {
        // Given: Mock KraftLog API server error
        wireMockServer.stubFor(post(urlEqualTo("/api/exercises"))
                .withBasicAuth("test-user", "test-password")
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"Internal Server Error\"}")));

        File testPdf = createTestPdfFile();

        try {
            // When: Import exercises
            ExerciseImportService.ImportResult result = 
                    exerciseImportService.importExercisesFromPdf(testPdf);

            // Then: All imports should fail
            assertThat(result).isNotNull();
            assertThat(result.getFailureCount()).isGreaterThan(0);
        } finally {
            testPdf.delete();
        }
    }

    @Test
    @Order(4)
    @DisplayName("Integration: Should handle partial success (some exercises succeed, some fail)")
    void testPartialSuccess() throws IOException {
        // Given: Mock API with alternating success/failure
        wireMockServer.stubFor(post(urlEqualTo("/api/exercises"))
                .inScenario("Partial Success")
                .whenScenarioStateIs(com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED)
                .withBasicAuth("test-user", "test-password")
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":1,\"name\":\"Exercise 1\"}"))
                .willSetStateTo("FIRST_SUCCESS"));

        wireMockServer.stubFor(post(urlEqualTo("/api/exercises"))
                .inScenario("Partial Success")
                .whenScenarioStateIs("FIRST_SUCCESS")
                .withBasicAuth("test-user", "test-password")
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\":\"Invalid exercise data\"}")));

        File testPdf = createTestPdfWithMultipleExercises();

        try {
            // When: Import exercises
            ExerciseImportService.ImportResult result = 
                    exerciseImportService.importExercisesFromPdf(testPdf);

            // Then: Should have both successes and failures
            assertThat(result).isNotNull();
            assertThat(result.getTotalCount()).isGreaterThan(1);
            // At least one should succeed and one should fail
            assertThat(result.getSuccessCount()).isGreaterThan(0);
            assertThat(result.getFailureCount()).isGreaterThan(0);
        } finally {
            testPdf.delete();
        }
    }

    @Test
    @Order(5)
    @DisplayName("Integration: Should handle API timeout")
    void testApiTimeout() throws IOException {
        // Given: Mock API with slow response
        wireMockServer.stubFor(post(urlEqualTo("/api/exercises"))
                .withBasicAuth("test-user", "test-password")
                .willReturn(aResponse()
                        .withStatus(201)
                        .withFixedDelay(30000) // 30 second delay
                        .withBody("{\"id\":1}")));

        File testPdf = createTestPdfFile();

        try {
            // When: Import exercises (should timeout)
            ExerciseImportService.ImportResult result = 
                    exerciseImportService.importExercisesFromPdf(testPdf);

            // Then: Should fail due to timeout
            assertThat(result).isNotNull();
            assertThat(result.getFailureCount()).isGreaterThan(0);
        } finally {
            testPdf.delete();
        }
    }

    @Test
    @Order(6)
    @DisplayName("Integration: Should correctly format request with muscle groups")
    void testRequestFormatWithMuscleGroups() throws IOException {
        // Given: Mock API and capture request
        wireMockServer.stubFor(post(urlEqualTo("/api/exercises"))
                .withBasicAuth("test-user", "test-password")
                .willReturn(aResponse()
                        .withStatus(201)
                        .withBody("{\"id\":1}")));

        File testPdf = createTestPdfFile();

        try {
            // When: Import exercises
            exerciseImportService.importExercisesFromPdf(testPdf);

            // Then: Verify request format
            wireMockServer.verify(postRequestedFor(urlEqualTo("/api/exercises"))
                    .withHeader("Content-Type", containing("application/json"))
                    .withBasicAuth(new com.github.tomakehurst.wiremock.matching.BasicCredentials("test-user", "test-password"))
                    .withRequestBody(matchingJsonPath("$.name"))
                    .withRequestBody(matchingJsonPath("$.muscleGroup")));
        } finally {
            testPdf.delete();
        }
    }

    @Test
    @Order(7)
    @DisplayName("Integration: End-to-end test via REST endpoint")
    void testEndToEndViaRestEndpoint() throws IOException {
        // Given: Mock KraftLog API
        wireMockServer.stubFor(post(urlEqualTo("/api/exercises"))
                .withBasicAuth("test-user", "test-password")
                .willReturn(aResponse()
                        .withStatus(201)
                        .withBody("{\"id\":1,\"name\":\"Test Exercise\"}")));

        File testPdf = createTestPdfFile();

        try {
            // When: Call the REST endpoint
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new org.springframework.core.io.FileSystemResource(testPdf));

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    "/api/import/pdf",
                    requestEntity,
                    String.class
            );

            // Then: Verify response
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).contains("success");
            assertThat(response.getBody()).contains("successful");

            // Verify API was called
            wireMockServer.verify(atLeast(1), postRequestedFor(urlEqualTo("/api/exercises")));
        } finally {
            testPdf.delete();
        }
    }

    // Helper methods

    private File createTestPdfFile() throws IOException {
        // Create a temporary PDF with simple exercise data
        File tempFile = File.createTempFile("test-exercise-", ".pdf");
        
        // Write a simple text file (for testing purposes)
        String content = """
                PEITO
                Supino Reto
                https://www.youtube.com/watch?v=test123
                """;
        
        Files.write(tempFile.toPath(), content.getBytes());
        return tempFile;
    }

    private File createTestPdfWithMultipleExercises() throws IOException {
        File tempFile = File.createTempFile("test-exercises-multiple-", ".pdf");
        
        String content = """
                PEITO
                Supino Reto
                https://www.youtube.com/watch?v=test123
                
                Supino Inclinado
                https://www.youtube.com/watch?v=test456
                """;
        
        Files.write(tempFile.toPath(), content.getBytes());
        return tempFile;
    }
}
