package com.agrodairy.analytics;

import com.agrodairy.analytics.dto.DashboardResponse;
import com.agrodairy.analytics.service.AnalyticsService;
import com.agrodairy.animal.entity.Animal;
import com.agrodairy.animal.entity.AnimalStatus;
import com.agrodairy.animal.entity.AnimalType;
import com.agrodairy.animal.entity.MilkProductionRecord;
import com.agrodairy.animal.entity.Session;
import com.agrodairy.animal.repository.AnimalRepository;
import com.agrodairy.animal.repository.MilkProductionRecordRepository;
import com.agrodairy.auth.entity.Role;
import com.agrodairy.auth.entity.User;
import com.agrodairy.auth.repository.UserRepository;
import com.agrodairy.inventory.entity.BatchStatus;
import com.agrodairy.inventory.entity.InventoryBatch;
import com.agrodairy.inventory.repository.InventoryBatchRepository;
import com.agrodairy.order.entity.Order;
import com.agrodairy.order.entity.OrderItem;
import com.agrodairy.order.entity.OrderStatus;
import com.agrodairy.order.entity.PaymentStatus;
import com.agrodairy.order.repository.OrderItemRepository;
import com.agrodairy.order.repository.OrderRepository;
import com.agrodairy.product.entity.CategoryKind;
import com.agrodairy.product.entity.Product;
import com.agrodairy.product.entity.ProductCategory;
import com.agrodairy.product.repository.ProductCategoryRepository;
import com.agrodairy.product.repository.ProductRepository;
import com.agrodairy.subscription.entity.Subscription;
import com.agrodairy.subscription.entity.SubscriptionFrequency;
import com.agrodairy.subscription.entity.SubscriptionStatus;
import com.agrodairy.subscription.repository.SubscriptionRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class AnalyticsServiceTest {

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
    private AnalyticsService analyticsService;

    @Autowired
    private AnimalRepository animalRepository;

    @Autowired
    private MilkProductionRecordRepository productionRecordRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private InventoryBatchRepository inventoryBatchRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductCategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        inventoryBatchRepository.deleteAll();
        subscriptionRepository.deleteAll();
        productionRecordRepository.deleteAll();
        animalRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    private User newCustomer() {
        User user = User.builder()
                .email("customer-" + UUID.randomUUID() + "@example.com")
                .passwordHash(passwordEncoder.encode("supersecret1"))
                .fullName("Test Customer")
                .role(Role.CUSTOMER)
                .active(true)
                .build();
        return userRepository.save(user);
    }

    @Test
    void dashboardReturnsCorrectAggregatesAgainstSeededData() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Animal milkingCow = Animal.builder().tag("A-" + suffix).type(AnimalType.COW)
                .status(AnimalStatus.MILKING).build();
        Animal soldCow = Animal.builder().tag("B-" + suffix).type(AnimalType.COW)
                .status(AnimalStatus.SOLD).build();
        animalRepository.save(milkingCow);
        animalRepository.save(soldCow);

        productionRecordRepository.save(MilkProductionRecord.builder()
                .animal(milkingCow).productionDate(LocalDate.now()).session(Session.MORNING)
                .quantityLitres(new BigDecimal("12.50")).build());
        productionRecordRepository.save(MilkProductionRecord.builder()
                .animal(milkingCow).productionDate(LocalDate.now()).session(Session.EVENING)
                .quantityLitres(new BigDecimal("8.00")).build());

        ProductCategory category = categoryRepository.save(
                ProductCategory.builder().name("Cow Milk").kind(CategoryKind.DAIRY).build());
        Product productA = productRepository.save(Product.builder()
                .category(category).name("Product A").price(new BigDecimal("25.00")).unit("pack").active(true).build());
        Product productB = productRepository.save(Product.builder()
                .category(category).name("Product B").price(new BigDecimal("10.00")).unit("bottle").active(true).build());

        subscriptionRepository.save(Subscription.builder()
                .user(newCustomer()).product(productA).quantity(1).frequency(SubscriptionFrequency.DAILY)
                .startDate(LocalDate.now().minusDays(10)).status(SubscriptionStatus.ACTIVE).build());
        subscriptionRepository.save(Subscription.builder()
                .user(newCustomer()).product(productA).quantity(1).frequency(SubscriptionFrequency.DAILY)
                .startDate(LocalDate.now().minusDays(10)).status(SubscriptionStatus.PAUSED).build());

        Order delivered = orderRepository.save(Order.builder()
                .user(newCustomer()).status(OrderStatus.DELIVERED).totalAmount(new BigDecimal("100.00"))
                .paymentStatus(PaymentStatus.PAID).deliveryAddress("1 Main St").build());
        Order pending = orderRepository.save(Order.builder()
                .user(newCustomer()).status(OrderStatus.PENDING).totalAmount(new BigDecimal("50.00"))
                .paymentStatus(PaymentStatus.PENDING).deliveryAddress("2 Main St").build());
        orderRepository.save(Order.builder()
                .user(newCustomer()).status(OrderStatus.CANCELLED).totalAmount(new BigDecimal("30.00"))
                .paymentStatus(PaymentStatus.PENDING).deliveryAddress("3 Main St").build());

        orderItemRepository.save(OrderItem.builder()
                .order(delivered).product(productA).quantity(4).unitPrice(productA.getPrice()).build());
        orderItemRepository.save(OrderItem.builder()
                .order(pending).product(productB).quantity(1).unitPrice(productB.getPrice()).build());

        inventoryBatchRepository.save(InventoryBatch.builder()
                .product(productA).batchCode("BATCH-EXP-" + suffix)
                .manufactureDate(LocalDate.now().minusDays(5)).expiryDate(LocalDate.now().plusDays(2))
                .quantityProduced(10).quantityAvailable(10).quantitySold(0).status(BatchStatus.ACTIVE).build());
        inventoryBatchRepository.save(InventoryBatch.builder()
                .product(productB).batchCode("BATCH-FAR-" + suffix)
                .manufactureDate(LocalDate.now().minusDays(5)).expiryDate(LocalDate.now().plusDays(30))
                .quantityProduced(20).quantityAvailable(20).quantitySold(0).status(BatchStatus.ACTIVE).build());

        DashboardResponse dashboard = analyticsService.dashboard();

        assertThat(dashboard.todayMilkProductionLitres()).isEqualByComparingTo("20.50");
        assertThat(dashboard.activeAnimals()).isEqualTo(1);
        assertThat(dashboard.activeSubscriptions()).isEqualTo(1);
        assertThat(dashboard.monthRevenue()).isEqualByComparingTo("150.00");
        assertThat(dashboard.totalOrders()).isEqualTo(2);
        assertThat(dashboard.avgOrderValue()).isEqualByComparingTo("75.00");
        assertThat(dashboard.inventoryValueTotal()).isEqualByComparingTo(
                new BigDecimal("25.00").multiply(BigDecimal.TEN).add(new BigDecimal("10.00").multiply(new BigDecimal("20"))));
        assertThat(dashboard.productsExpiringSoon()).isEqualTo(1);
        assertThat(dashboard.topSellingProducts()).hasSize(2);
        assertThat(dashboard.topSellingProducts().get(0).productId()).isEqualTo(productA.getId());
        assertThat(dashboard.topSellingProducts().get(0).unitsSold()).isEqualTo(4);
        assertThat(dashboard.revenueTrend()).hasSize(30);
        assertThat(dashboard.revenueTrend().get(29).amount()).isEqualByComparingTo("150.00");
        assertThat(dashboard.productionTrend()).hasSize(30);
        assertThat(dashboard.productionTrend().get(29).litres()).isEqualByComparingTo("20.50");
    }
}
