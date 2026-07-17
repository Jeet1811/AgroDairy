package com.agrodairy.inventory;

import com.agrodairy.auth.entity.Role;
import com.agrodairy.auth.entity.User;
import com.agrodairy.auth.repository.UserRepository;
import com.agrodairy.inventory.repository.InventoryBatchRepository;
import com.agrodairy.product.entity.CategoryKind;
import com.agrodairy.product.entity.Product;
import com.agrodairy.product.entity.ProductCategory;
import com.agrodairy.product.repository.ProductCategoryRepository;
import com.agrodairy.product.repository.ProductRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Testcontainers
class InventoryIntegrationTest {

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
    private InventoryBatchRepository batchRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductCategoryRepository categoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String staffAccessToken;

    @BeforeEach
    void seedStaffUser() {
        batchRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
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
    void alertsEndpointReturnsLowStockAndExpiringSoonWithoutLazyInitError() {
        ProductCategory category = ProductCategory.builder().name("Curd").kind(CategoryKind.DAIRY).build();
        categoryRepository.save(category);
        Product product = Product.builder()
                .category(category)
                .name("Fresh Curd 200g")
                .price(new BigDecimal("25.00"))
                .unit("pack")
                .active(true)
                .build();
        productRepository.save(product);

        Map<String, Object> batchBody = Map.of(
                "productId", product.getId().toString(),
                "batchCode", "CURD-TEST-1",
                "manufactureDate", LocalDate.now().toString(),
                "expiryDate", LocalDate.now().plusDays(2).toString(),
                "quantityProduced", 4);
        var createResponse = restTemplate.exchange("/api/inventory/batches", HttpMethod.POST,
                new HttpEntity<>(batchBody, authHeaders()), Map.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        var alertsResponse = restTemplate.exchange("/api/inventory/alerts", HttpMethod.GET,
                new HttpEntity<>(authHeaders()), Map.class);
        assertThat(alertsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, Object> data = dataOf(alertsResponse.getBody());
        assertThat((java.util.List<?>) data.get("expiringSoon")).hasSize(1);
        assertThat((java.util.List<?>) data.get("lowStock")).hasSize(1);
        assertThat((java.util.List<?>) data.get("outOfStock")).isEmpty();
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + staffAccessToken);
        return headers;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> dataOf(Map body) {
        return (Map<String, Object>) body.get("data");
    }

    private static HttpEntity<Map<String, Object>> jsonEntity(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        return new HttpEntity<>(body, headers);
    }
}
