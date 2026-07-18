package com.agrodairy.inventory.scheduler;

import com.agrodairy.auth.entity.Role;
import com.agrodairy.auth.entity.User;
import com.agrodairy.auth.repository.UserRepository;
import com.agrodairy.inventory.entity.BatchStatus;
import com.agrodairy.inventory.entity.InventoryBatch;
import com.agrodairy.inventory.repository.InventoryBatchRepository;
import com.agrodairy.notification.entity.NotificationType;
import com.agrodairy.notification.repository.NotificationRepository;
import com.agrodairy.product.entity.CategoryKind;
import com.agrodairy.product.entity.Product;
import com.agrodairy.product.entity.ProductCategory;
import com.agrodairy.product.repository.ProductCategoryRepository;
import com.agrodairy.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class ExpiringBatchNotificationJobTest {

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
    private ExpiringBatchNotificationJob job;

    @Autowired
    private InventoryBatchRepository batchRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductCategoryRepository categoryRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Product product;

    @BeforeEach
    void seedData() {
        notificationRepository.deleteAll();
        batchRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        User staff = User.builder()
                .email("staff-" + java.util.UUID.randomUUID() + "@example.com")
                .passwordHash(passwordEncoder.encode("supersecret1"))
                .fullName("Staff Member")
                .role(Role.STAFF)
                .active(true)
                .build();
        userRepository.save(staff);

        ProductCategory category = categoryRepository.save(
                ProductCategory.builder().name("Cow Milk").kind(CategoryKind.DAIRY).build());
        product = productRepository.save(Product.builder()
                .category(category).name("Cow Milk 1L").price(new BigDecimal("60.00")).unit("bottle").active(true).build());
    }

    @Test
    void notifiesOnceForAnExpiringBatchAndIsIdempotentOnRerun() {
        InventoryBatch expiringBatch = batchRepository.save(InventoryBatch.builder()
                .product(product).batchCode("EXPIRING-1")
                .manufactureDate(LocalDate.now().minusDays(5)).expiryDate(LocalDate.now().plusDays(2))
                .quantityProduced(5).quantityAvailable(5).quantitySold(0).status(BatchStatus.ACTIVE).build());
        batchRepository.save(InventoryBatch.builder()
                .product(product).batchCode("FAR-1")
                .manufactureDate(LocalDate.now().minusDays(5)).expiryDate(LocalDate.now().plusDays(30))
                .quantityProduced(5).quantityAvailable(5).quantitySold(0).status(BatchStatus.ACTIVE).build());

        job.checkExpiringBatchesAsOf(LocalDate.now());

        List<com.agrodairy.notification.entity.Notification> notifications = notificationRepository.findAll();
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getType()).isEqualTo(NotificationType.INVENTORY_EXPIRY);

        InventoryBatch reloaded = batchRepository.findById(expiringBatch.getId()).orElseThrow();
        assertThat(reloaded.getExpiryNotifiedAt()).isNotNull();

        // Re-running the same day must not notify again for the already-notified batch.
        job.checkExpiringBatchesAsOf(LocalDate.now());
        assertThat(notificationRepository.findAll()).hasSize(1);
    }
}
