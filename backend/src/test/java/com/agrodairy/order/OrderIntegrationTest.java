package com.agrodairy.order;

import com.agrodairy.auth.entity.Role;
import com.agrodairy.auth.entity.User;
import com.agrodairy.auth.repository.UserRepository;
import com.agrodairy.inventory.entity.BatchStatus;
import com.agrodairy.inventory.entity.InventoryBatch;
import com.agrodairy.inventory.repository.InventoryBatchRepository;
import com.agrodairy.order.repository.CartItemRepository;
import com.agrodairy.order.repository.CartRepository;
import com.agrodairy.order.repository.OrderItemRepository;
import com.agrodairy.order.repository.OrderRepository;
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
import org.springframework.http.MediaType;
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
class OrderIntegrationTest {

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
    private ProductCategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryBatchRepository batchRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        batchRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void registerBrowseCartOrderDecrementsInventoryAndOrderIsPending() {
        ProductCategory category = ProductCategory.builder().name("Cow Milk").kind(CategoryKind.DAIRY).build();
        categoryRepository.save(category);
        Product product = Product.builder()
                .category(category)
                .name("Cow Milk 1L")
                .price(new BigDecimal("60.00"))
                .unit("bottle")
                .active(true)
                .build();
        productRepository.save(product);
        InventoryBatch batch = InventoryBatch.builder()
                .product(product)
                .batchCode("MILK-E2E-1")
                .manufactureDate(LocalDate.now())
                .expiryDate(LocalDate.now().plusDays(5))
                .quantityProduced(20)
                .quantityAvailable(20)
                .quantitySold(0)
                .status(BatchStatus.ACTIVE)
                .build();
        batchRepository.save(batch);

        String email = "customer-" + UUID.randomUUID() + "@example.com";
        var registerResponse = restTemplate.postForEntity("/api/auth/register",
                jsonEntity(Map.of("email", email, "password", "supersecret1", "fullName", "E2E Customer")), Map.class);
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String accessToken = (String) dataOf(registerResponse.getBody()).get("accessToken");
        HttpHeaders headers = authHeaders(accessToken);

        var browseResponse = restTemplate.exchange("/api/products", HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()), Map.class);
        assertThat(browseResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        var addToCartResponse = restTemplate.exchange("/api/cart/items", HttpMethod.POST,
                new HttpEntity<>(Map.of("productId", product.getId().toString(), "quantity", 5), headers), Map.class);
        assertThat(addToCartResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        var orderResponse = restTemplate.exchange("/api/orders", HttpMethod.POST,
                new HttpEntity<>(Map.of("deliveryAddress", "42 Dairy Lane"), headers), Map.class);
        assertThat(orderResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Map<String, Object> orderData = dataOf(orderResponse.getBody());
        assertThat(orderData.get("status")).isEqualTo("PENDING");
        assertThat(((Number) orderData.get("totalAmount")).doubleValue()).isEqualTo(300.0);

        InventoryBatch reloaded = batchRepository.findById(batch.getId()).orElseThrow();
        assertThat(reloaded.getQuantityAvailable()).isEqualTo(15);
        assertThat(reloaded.getQuantitySold()).isEqualTo(5);

        var cartAfterOrder = restTemplate.exchange("/api/cart", HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        assertThat((java.util.List<?>) dataOf(cartAfterOrder.getBody()).get("items")).isEmpty();
    }

    @Test
    void insufficientStockRejectsOrderWithoutMutatingInventory() {
        ProductCategory category = ProductCategory.builder().name("Cow Milk").kind(CategoryKind.DAIRY).build();
        categoryRepository.save(category);
        Product product = Product.builder()
                .category(category)
                .name("Cow Milk 1L")
                .price(new BigDecimal("60.00"))
                .unit("bottle")
                .active(true)
                .build();
        productRepository.save(product);
        InventoryBatch batch = InventoryBatch.builder()
                .product(product)
                .batchCode("MILK-E2E-2")
                .manufactureDate(LocalDate.now())
                .expiryDate(LocalDate.now().plusDays(5))
                .quantityProduced(3)
                .quantityAvailable(3)
                .quantitySold(0)
                .status(BatchStatus.ACTIVE)
                .build();
        batchRepository.save(batch);

        String email = "customer-" + UUID.randomUUID() + "@example.com";
        var registerResponse = restTemplate.postForEntity("/api/auth/register",
                jsonEntity(Map.of("email", email, "password", "supersecret1", "fullName", "E2E Customer")), Map.class);
        String accessToken = (String) dataOf(registerResponse.getBody()).get("accessToken");
        HttpHeaders headers = authHeaders(accessToken);

        restTemplate.exchange("/api/cart/items", HttpMethod.POST,
                new HttpEntity<>(Map.of("productId", product.getId().toString(), "quantity", 10), headers), Map.class);

        var orderResponse = restTemplate.exchange("/api/orders", HttpMethod.POST,
                new HttpEntity<>(Map.of("deliveryAddress", "42 Dairy Lane"), headers), Map.class);
        assertThat(orderResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        InventoryBatch reloaded = batchRepository.findById(batch.getId()).orElseThrow();
        assertThat(reloaded.getQuantityAvailable()).isEqualTo(3);
    }

    private HttpHeaders authHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + accessToken);
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
