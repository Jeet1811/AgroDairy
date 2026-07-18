package com.agrodairy.analytics.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResponse(
        BigDecimal todayMilkProductionLitres,
        long activeAnimals,
        long activeSubscriptions,
        BigDecimal monthRevenue,
        long totalOrders,
        BigDecimal avgOrderValue,
        BigDecimal inventoryValueTotal,
        long productsExpiringSoon,
        List<TopSellingProductResponse> topSellingProducts,
        List<RevenuePoint> revenueTrend,
        List<ProductionPoint> productionTrend
) {
}
