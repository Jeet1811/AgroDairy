package com.agrodairy.ai.service;

import com.agrodairy.ai.client.AiServiceClient;
import com.agrodairy.ai.dto.DemandForecastResponse;
import com.agrodairy.ai.dto.DemandPrediction;
import com.agrodairy.ai.entity.ForecastLog;
import com.agrodairy.ai.repository.ForecastLogRepository;
import com.agrodairy.common.exception.NotFoundException;
import com.agrodairy.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemandForecastServiceTest {

    @Mock
    private AiServiceClient aiServiceClient;

    @Mock
    private ForecastLogRepository forecastLogRepository;

    @Mock
    private ProductRepository productRepository;

    private DemandForecastService newService() {
        return new DemandForecastService(aiServiceClient, forecastLogRepository, productRepository);
    }

    @Test
    void forecastLogsOnePredictionPerDayAndReturnsTheResponse() {
        DemandForecastService service = newService();
        UUID productId = UUID.randomUUID();
        when(productRepository.existsById(productId)).thenReturn(true);

        DemandForecastResponse aiResponse = new DemandForecastResponse(productId, "demand-xgb-v1", List.of(
                new DemandPrediction(LocalDate.of(2026, 7, 19), new BigDecimal("30.00"), new BigDecimal("27.00"), new BigDecimal("33.00")),
                new DemandPrediction(LocalDate.of(2026, 7, 20), new BigDecimal("40.00"), new BigDecimal("36.00"), new BigDecimal("44.00"))
        ));
        when(aiServiceClient.predictDemand(productId, 2)).thenReturn(aiResponse);

        DemandForecastResponse result = service.forecast(productId, 2);

        assertThat(result).isEqualTo(aiResponse);
        ArgumentCaptor<ForecastLog> captor = ArgumentCaptor.forClass(ForecastLog.class);
        verify(forecastLogRepository, times(2)).save(captor.capture());
        List<ForecastLog> savedLogs = captor.getAllValues();
        assertThat(savedLogs).extracting(ForecastLog::getTargetDate)
                .containsExactly(LocalDate.of(2026, 7, 19), LocalDate.of(2026, 7, 20));
        assertThat(savedLogs).extracting(ForecastLog::getPredictedValue)
                .containsExactly(new BigDecimal("30.00"), new BigDecimal("40.00"));
        assertThat(savedLogs).allMatch(log -> log.getTargetId().equals(productId));
        assertThat(savedLogs).allMatch(log -> log.getModelVersion().equals("demand-xgb-v1"));
    }

    @Test
    void forecastRejectsUnknownProductWithoutCallingAiService() {
        DemandForecastService service = newService();
        UUID productId = UUID.randomUUID();
        when(productRepository.existsById(productId)).thenReturn(false);

        assertThatThrownBy(() -> service.forecast(productId, 7))
                .isInstanceOf(NotFoundException.class);

        verify(aiServiceClient, never()).predictDemand(any(), eq(7));
        verify(forecastLogRepository, never()).save(any());
    }
}
