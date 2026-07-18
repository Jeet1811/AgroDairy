package com.agrodairy.analytics.service;

import com.agrodairy.analytics.dto.DashboardResponse;
import com.agrodairy.analytics.dto.ProductionPoint;
import com.agrodairy.analytics.dto.RevenuePoint;
import com.agrodairy.analytics.dto.TopSellingProductResponse;
import com.agrodairy.animal.entity.AnimalStatus;
import com.agrodairy.animal.repository.AnimalRepository;
import com.agrodairy.animal.repository.MilkProductionRecordRepository;
import com.agrodairy.inventory.entity.BatchStatus;
import com.agrodairy.inventory.repository.InventoryBatchRepository;
import com.agrodairy.order.entity.OrderStatus;
import com.agrodairy.order.repository.OrderItemRepository;
import com.agrodairy.order.repository.OrderRepository;
import com.agrodairy.subscription.entity.SubscriptionStatus;
import com.agrodairy.subscription.repository.SubscriptionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsService {

    private static final int TREND_DAYS = 30;
    private static final int TOP_PRODUCTS_LIMIT = 5;
    /** Same threshold as §11.1's read-computed inventory alerts. */
    private static final int EXPIRING_SOON_THRESHOLD_DAYS = 3;
    /** Animals no longer on the farm are excluded; every other status counts as "active" for this dashboard metric. */
    private static final List<AnimalStatus> INACTIVE_ANIMAL_STATUSES = List.of(AnimalStatus.SOLD, AnimalStatus.DECEASED);

    private final MilkProductionRecordRepository productionRecordRepository;
    private final AnimalRepository animalRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final InventoryBatchRepository inventoryBatchRepository;

    public AnalyticsService(MilkProductionRecordRepository productionRecordRepository,
                             AnimalRepository animalRepository,
                             SubscriptionRepository subscriptionRepository,
                             OrderRepository orderRepository,
                             OrderItemRepository orderItemRepository,
                             InventoryBatchRepository inventoryBatchRepository) {
        this.productionRecordRepository = productionRecordRepository;
        this.animalRepository = animalRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.inventoryBatchRepository = inventoryBatchRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        LocalDate today = LocalDate.now();

        BigDecimal todayMilkProductionLitres = productionRecordRepository.sumQuantityByProductionDate(today);
        long activeAnimals = animalRepository.countByStatusNotIn(INACTIVE_ANIMAL_STATUSES);
        long activeSubscriptions = subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE);

        Instant monthStart = today.withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        BigDecimal monthRevenue = orderRepository.sumRevenueSince(OrderStatus.CANCELLED, monthStart);
        long totalOrders = orderRepository.countByStatusNot(OrderStatus.CANCELLED);
        BigDecimal totalRevenueAllTime = orderRepository.sumRevenueSince(OrderStatus.CANCELLED, Instant.EPOCH);
        BigDecimal avgOrderValue = totalOrders > 0
                ? totalRevenueAllTime.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal inventoryValueTotal = inventoryBatchRepository.sumInventoryValue(BatchStatus.ACTIVE);
        LocalDate expiringCutoff = today.plusDays(EXPIRING_SOON_THRESHOLD_DAYS);
        long productsExpiringSoon = inventoryBatchRepository.findExpiringSoon(BatchStatus.ACTIVE, expiringCutoff).stream()
                .map(batch -> batch.getProduct().getId())
                .distinct()
                .count();

        List<TopSellingProductResponse> topSellingProducts = orderItemRepository
                .findTopSellingProducts(OrderStatus.CANCELLED, PageRequest.of(0, TOP_PRODUCTS_LIMIT))
                .stream()
                .map(p -> new TopSellingProductResponse(p.getProductId(), p.getName(), p.getUnitsSold()))
                .toList();

        LocalDate trendStart = today.minusDays(TREND_DAYS - 1);
        List<RevenuePoint> revenueTrend = buildRevenueTrend(trendStart, today);
        List<ProductionPoint> productionTrend = buildProductionTrend(trendStart, today);

        return new DashboardResponse(todayMilkProductionLitres, activeAnimals, activeSubscriptions, monthRevenue,
                totalOrders, avgOrderValue, inventoryValueTotal, productsExpiringSoon, topSellingProducts,
                revenueTrend, productionTrend);
    }

    private List<RevenuePoint> buildRevenueTrend(LocalDate start, LocalDate today) {
        Instant startInstant = start.atStartOfDay(ZoneOffset.UTC).toInstant();
        Map<LocalDate, BigDecimal> byDate = new HashMap<>();
        for (Object[] row : orderRepository.findDailyRevenueSince(OrderStatus.CANCELLED.name(), startInstant)) {
            byDate.put(toLocalDate(row[0]), (BigDecimal) row[1]);
        }
        List<RevenuePoint> trend = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(today); date = date.plusDays(1)) {
            trend.add(new RevenuePoint(date, byDate.getOrDefault(date, BigDecimal.ZERO)));
        }
        return trend;
    }

    private List<ProductionPoint> buildProductionTrend(LocalDate start, LocalDate today) {
        Map<LocalDate, BigDecimal> byDate = new HashMap<>();
        for (Object[] row : productionRecordRepository.findDailyProductionSince(start)) {
            byDate.put(toLocalDate(row[0]), (BigDecimal) row[1]);
        }
        List<ProductionPoint> trend = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(today); date = date.plusDays(1)) {
            trend.add(new ProductionPoint(date, byDate.getOrDefault(date, BigDecimal.ZERO)));
        }
        return trend;
    }

    private static LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate();
        }
        throw new IllegalStateException("Unexpected date type from native query: " + value.getClass());
    }
}
