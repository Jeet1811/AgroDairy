package com.agrodairy.subscription.scheduler;

import com.agrodairy.auth.entity.Role;
import com.agrodairy.auth.entity.User;
import com.agrodairy.auth.repository.UserRepository;
import com.agrodairy.delivery.repository.DeliveryRepository;
import com.agrodairy.product.entity.CategoryKind;
import com.agrodairy.product.entity.Product;
import com.agrodairy.product.entity.ProductCategory;
import com.agrodairy.product.repository.ProductCategoryRepository;
import com.agrodairy.product.repository.ProductRepository;
import com.agrodairy.subscription.entity.Subscription;
import com.agrodairy.subscription.entity.SubscriptionFrequency;
import com.agrodairy.subscription.entity.SubscriptionSkipDate;
import com.agrodairy.subscription.entity.SubscriptionStatus;
import com.agrodairy.subscription.repository.SubscriptionRepository;
import com.agrodairy.subscription.repository.SubscriptionSkipDateRepository;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class DailyDeliveryGeneratorJobTest {

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
    private DailyDeliveryGeneratorJob job;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductCategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private SubscriptionSkipDateRepository skipDateRepository;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Product product;

    @BeforeEach
    void seedProduct() {
        deliveryRepository.deleteAll();
        skipDateRepository.deleteAll();
        subscriptionRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        userRepository.deleteAll();

        ProductCategory category = ProductCategory.builder().name("Cow Milk").kind(CategoryKind.DAIRY).build();
        categoryRepository.save(category);
        product = Product.builder()
                .category(category)
                .name("Cow Milk 1L")
                .price(new BigDecimal("60.00"))
                .unit("bottle")
                .active(true)
                .build();
        productRepository.save(product);
    }

    private User newCustomer(String emailPrefix) {
        User user = User.builder()
                .email(emailPrefix + "-" + java.util.UUID.randomUUID() + "@example.com")
                .passwordHash(passwordEncoder.encode("supersecret1"))
                .fullName("Test Customer")
                .role(Role.CUSTOMER)
                .active(true)
                .build();
        return userRepository.save(user);
    }

    @Test
    void generatesDeliveriesForDailyAndMatchingCustomSubscriptionsAndIsIdempotent() {
        LocalDate targetDate = LocalDate.now().plusDays(10);
        int targetIsoDay = targetDate.getDayOfWeek().getValue();
        int nonMatchingIsoDay = (targetIsoDay % 7) + 1;

        Subscription daily = Subscription.builder()
                .user(newCustomer("daily"))
                .product(product)
                .quantity(2)
                .frequency(SubscriptionFrequency.DAILY)
                .startDate(targetDate.minusDays(5))
                .status(SubscriptionStatus.ACTIVE)
                .build();
        subscriptionRepository.save(daily);

        Subscription matchingCustom = Subscription.builder()
                .user(newCustomer("custom-match"))
                .product(product)
                .quantity(1)
                .frequency(SubscriptionFrequency.CUSTOM)
                .weekdays(String.valueOf(targetIsoDay))
                .startDate(targetDate.minusDays(5))
                .status(SubscriptionStatus.ACTIVE)
                .build();
        subscriptionRepository.save(matchingCustom);

        Subscription nonMatchingCustom = Subscription.builder()
                .user(newCustomer("custom-nomatch"))
                .product(product)
                .quantity(1)
                .frequency(SubscriptionFrequency.CUSTOM)
                .weekdays(String.valueOf(nonMatchingIsoDay))
                .startDate(targetDate.minusDays(5))
                .status(SubscriptionStatus.ACTIVE)
                .build();
        subscriptionRepository.save(nonMatchingCustom);

        Subscription pausedDaily = Subscription.builder()
                .user(newCustomer("paused"))
                .product(product)
                .quantity(1)
                .frequency(SubscriptionFrequency.DAILY)
                .startDate(targetDate.minusDays(5))
                .status(SubscriptionStatus.PAUSED)
                .build();
        subscriptionRepository.save(pausedDaily);

        job.generateDeliveriesFor(targetDate);

        assertThat(deliveryRepository.existsBySubscriptionIdAndDeliveryDate(daily.getId(), targetDate)).isTrue();
        assertThat(deliveryRepository.existsBySubscriptionIdAndDeliveryDate(matchingCustom.getId(), targetDate)).isTrue();
        assertThat(deliveryRepository.existsBySubscriptionIdAndDeliveryDate(nonMatchingCustom.getId(), targetDate)).isFalse();
        assertThat(deliveryRepository.existsBySubscriptionIdAndDeliveryDate(pausedDaily.getId(), targetDate)).isFalse();
        assertThat(deliveryRepository.findByDeliveryDate(targetDate)).hasSize(2);

        // Re-running for the same date must not create duplicates.
        job.generateDeliveriesFor(targetDate);
        assertThat(deliveryRepository.findByDeliveryDate(targetDate)).hasSize(2);
    }

    @Test
    void skipDateExcludesAnOtherwiseEligibleSubscription() {
        LocalDate targetDate = LocalDate.now().plusDays(10);

        Subscription daily = Subscription.builder()
                .user(newCustomer("skip-test"))
                .product(product)
                .quantity(1)
                .frequency(SubscriptionFrequency.DAILY)
                .startDate(targetDate.minusDays(5))
                .status(SubscriptionStatus.ACTIVE)
                .build();
        subscriptionRepository.save(daily);
        skipDateRepository.save(SubscriptionSkipDate.builder().subscription(daily).skipDate(targetDate).build());

        job.generateDeliveriesFor(targetDate);

        assertThat(deliveryRepository.existsBySubscriptionIdAndDeliveryDate(daily.getId(), targetDate)).isFalse();
    }

    @Test
    void alternateDayFrequencyOnlyGeneratesOnEvenOffsetsFromStartDate() {
        LocalDate startDate = LocalDate.now().plusDays(1);
        Subscription alternate = Subscription.builder()
                .user(newCustomer("alt-day"))
                .product(product)
                .quantity(1)
                .frequency(SubscriptionFrequency.ALTERNATE_DAY)
                .startDate(startDate)
                .status(SubscriptionStatus.ACTIVE)
                .build();
        subscriptionRepository.save(alternate);

        job.generateDeliveriesFor(startDate);
        job.generateDeliveriesFor(startDate.plusDays(1));
        job.generateDeliveriesFor(startDate.plusDays(2));

        assertThat(deliveryRepository.existsBySubscriptionIdAndDeliveryDate(alternate.getId(), startDate)).isTrue();
        assertThat(deliveryRepository.existsBySubscriptionIdAndDeliveryDate(alternate.getId(), startDate.plusDays(1))).isFalse();
        assertThat(deliveryRepository.existsBySubscriptionIdAndDeliveryDate(alternate.getId(), startDate.plusDays(2))).isTrue();
    }
}
