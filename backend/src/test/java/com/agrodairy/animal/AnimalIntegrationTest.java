package com.agrodairy.animal;

import com.agrodairy.animal.repository.AnimalRepository;
import com.agrodairy.animal.repository.MilkProductionRecordRepository;
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
class AnimalIntegrationTest {

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
    private MilkProductionRecordRepository productionRecordRepository;

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String staffAccessToken;

    @BeforeEach
    void seedStaffUser() {
        productionRecordRepository.deleteAll();
        animalRepository.deleteAll();
        userRepository.deleteAll();
        User staff = User.builder()
                .email("staff-" + UUID.randomUUID() + "@example.com")
                .passwordHash(passwordEncoder.encode("supersecret1"))
                .fullName("Staff Member")
                .role(Role.STAFF)
                .active(true)
                .build();
        userRepository.save(staff);

        var loginResponse = restTemplate.postForEntity("/api/auth/login",
                jsonEntity(Map.of("email", staff.getEmail(), "password", "supersecret1")), Map.class);
        staffAccessToken = (String) dataOf(loginResponse.getBody()).get("accessToken");
    }

    @Test
    void createAnimalAndRecordProduction() {
        Map<String, Object> createAnimalBody = Map.of(
                "tag", "COW-" + UUID.randomUUID().toString().substring(0, 8),
                "type", "COW",
                "status", "MILKING");
        var createResponse = restTemplate.exchange("/api/animals", HttpMethod.POST,
                new HttpEntity<>(createAnimalBody, authHeaders()), Map.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> createdAnimal = dataOf(createResponse.getBody());
        assertThat(createdAnimal.get("createdAt")).isNotNull();
        String animalId = (String) createdAnimal.get("id");

        Map<String, Object> productionBody = Map.of(
                "productionDate", "2026-07-15",
                "session", "MORNING",
                "quantityLitres", 20.0);
        var recordResponse = restTemplate.exchange("/api/animals/" + animalId + "/production", HttpMethod.POST,
                new HttpEntity<>(productionBody, authHeaders()), Map.class);
        assertThat(recordResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> recordData = dataOf(recordResponse.getBody());
        assertThat(recordData.get("animalId")).isEqualTo(animalId);
        assertThat(((Number) recordData.get("quantityLitres")).doubleValue()).isEqualTo(20.0);
    }

    @Test
    void duplicateProductionRecordForSameAnimalDateSessionIsRejected() {
        Map<String, Object> createAnimalBody = Map.of(
                "tag", "COW-" + UUID.randomUUID().toString().substring(0, 8),
                "type", "COW",
                "status", "MILKING");
        var createResponse = restTemplate.exchange("/api/animals", HttpMethod.POST,
                new HttpEntity<>(createAnimalBody, authHeaders()), Map.class);
        String animalId = (String) dataOf(createResponse.getBody()).get("id");

        Map<String, Object> productionBody = Map.of(
                "productionDate", "2026-07-15",
                "session", "EVENING",
                "quantityLitres", 18.5);
        var first = restTemplate.exchange("/api/animals/" + animalId + "/production", HttpMethod.POST,
                new HttpEntity<>(productionBody, authHeaders()), Map.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        var duplicate = restTemplate.exchange("/api/animals/" + animalId + "/production", HttpMethod.POST,
                new HttpEntity<>(productionBody, authHeaders()), Map.class);
        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + staffAccessToken);
        return headers;
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
