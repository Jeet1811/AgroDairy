package com.agrodairy.auth;

import com.agrodairy.auth.entity.Role;
import com.agrodairy.auth.entity.User;
import com.agrodairy.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Testcontainers
class AuthIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("app.jwt.secret", () -> "test-secret-not-for-prod-use-only");
        registry.add("app.ai-service.internal-api-key", () -> "test-internal-key");
        registry.add("app.admin-seed.email", () -> "");
        registry.add("app.admin-seed.password", () -> "");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void registerLoginRefreshRotationHappyPath() {
        String email = uniqueEmail();
        Map<String, Object> registerBody = Map.of(
                "email", email,
                "password", "supersecret1",
                "fullName", "Test User");

        var registerResponse = restTemplate.postForEntity("/api/auth/register", jsonEntity(registerBody), Map.class);
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> registerData = dataOf(registerResponse.getBody());
        assertThat(registerData.get("accessToken")).isNotNull();
        String firstRefreshToken = (String) registerData.get("refreshToken");
        Map<String, Object> user = (Map<String, Object>) registerData.get("user");
        assertThat(user.get("role")).isEqualTo("CUSTOMER");
        assertThat(user.get("createdAt")).isNotNull();

        var loginResponse = restTemplate.postForEntity("/api/auth/login",
                jsonEntity(Map.of("email", email, "password", "supersecret1")), Map.class);
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        var refreshResponse = restTemplate.postForEntity("/api/auth/refresh",
                jsonEntity(Map.of("refreshToken", firstRefreshToken)), Map.class);
        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> refreshData = dataOf(refreshResponse.getBody());
        String rotatedRefreshToken = (String) refreshData.get("refreshToken");
        assertThat(rotatedRefreshToken).isNotEqualTo(firstRefreshToken);

        // Reusing the original (now-rotated/revoked) refresh token must be rejected.
        var reuseResponse = restTemplate.postForEntity("/api/auth/refresh",
                jsonEntity(Map.of("refreshToken", firstRefreshToken)), Map.class);
        assertThat(reuseResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void meRequiresValidToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer not-a-real-token");
        var invalidResponse = restTemplate.exchange("/api/auth/me", HttpMethod.GET,
                new HttpEntity<>(headers), Map.class);
        assertThat(invalidResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        var noHeaderResponse = restTemplate.getForEntity("/api/auth/me", Map.class);
        assertThat(noHeaderResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void customerIsBlockedFromAdminOnlyStaffEndpoint() {
        String email = uniqueEmail();
        var registerResponse = restTemplate.postForEntity("/api/auth/register",
                jsonEntity(Map.of("email", email, "password", "supersecret1", "fullName", "Customer User")), Map.class);
        Map<String, Object> registerData = dataOf(registerResponse.getBody());
        String customerAccessToken = (String) registerData.get("accessToken");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + customerAccessToken);
        Map<String, Object> staffBody = Map.of(
                "email", uniqueEmail(), "password", "supersecret1", "fullName", "New Staff", "role", "STAFF");
        var response = restTemplate.exchange("/api/auth/staff", HttpMethod.POST,
                new HttpEntity<>(staffBody, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void adminCanCreateStaffAccount() {
        User admin = User.builder()
                .email(uniqueEmail())
                .passwordHash(passwordEncoder.encode("supersecret1"))
                .fullName("Seed Admin")
                .role(Role.ADMIN)
                .active(true)
                .build();
        userRepository.save(admin);

        var loginResponse = restTemplate.postForEntity("/api/auth/login",
                jsonEntity(Map.of("email", admin.getEmail(), "password", "supersecret1")), Map.class);
        Map<String, Object> loginData = dataOf(loginResponse.getBody());
        String adminAccessToken = (String) loginData.get("accessToken");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + adminAccessToken);
        Map<String, Object> staffBody = Map.of(
                "email", uniqueEmail(), "password", "supersecret1", "fullName", "New Staff", "role", "STAFF");
        var response = restTemplate.exchange("/api/auth/staff", HttpMethod.POST,
                new HttpEntity<>(staffBody, headers), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> dataOf(Map body) {
        return (Map<String, Object>) body.get("data");
    }

    private static HttpEntity<Map<String, Object>> jsonEntity(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
