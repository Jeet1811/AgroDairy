package com.agrodairy.ai.service;

import com.agrodairy.ai.client.AiServiceClient;
import com.agrodairy.ai.dto.DemandForecastResponse;
import com.agrodairy.ai.entity.ForecastLog;
import com.agrodairy.ai.entity.ForecastType;
import com.agrodairy.ai.repository.ForecastLogRepository;
import com.agrodairy.common.exception.NotFoundException;
import com.agrodairy.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DemandForecastService {

    private final AiServiceClient aiServiceClient;
    private final ForecastLogRepository forecastLogRepository;
    private final ProductRepository productRepository;

    public DemandForecastService(AiServiceClient aiServiceClient,
                                  ForecastLogRepository forecastLogRepository,
                                  ProductRepository productRepository) {
        this.aiServiceClient = aiServiceClient;
        this.forecastLogRepository = forecastLogRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public DemandForecastResponse forecast(UUID productId, int horizonDays) {
        if (!productRepository.existsById(productId)) {
            throw new NotFoundException("Product not found");
        }

        DemandForecastResponse response = aiServiceClient.predictDemand(productId, horizonDays);

        response.predictions().forEach(prediction -> {
            ForecastLog log = ForecastLog.builder()
                    .forecastType(ForecastType.DEMAND)
                    .targetId(productId)
                    .targetDate(prediction.date())
                    .predictedValue(prediction.predictedQuantity())
                    .modelVersion(response.modelVersion())
                    .build();
            forecastLogRepository.save(log);
        });

        return response;
    }
}
