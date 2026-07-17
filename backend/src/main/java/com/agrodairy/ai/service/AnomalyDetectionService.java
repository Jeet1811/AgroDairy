package com.agrodairy.ai.service;

import com.agrodairy.ai.entity.AnomalyAlert;
import com.agrodairy.ai.entity.Severity;
import com.agrodairy.ai.repository.AnomalyAlertRepository;
import com.agrodairy.animal.entity.MilkProductionRecord;
import com.agrodairy.animal.repository.MilkProductionRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class AnomalyDetectionService {

    private static final int LOOKBACK_DAYS = 21;
    private static final int MIN_DATA_POINTS = 7;
    /** Sentinel stored when stddev is 0 — a real z-score is undefined (division by zero), and z_score is NOT NULL. */
    private static final BigDecimal UNDEFINED_ZSCORE_SENTINEL = new BigDecimal("99.99");
    private static final BigDecimal ZERO_STDDEV_MIN_DEVIATION = BigDecimal.ONE;

    private final MilkProductionRecordRepository productionRecordRepository;
    private final AnomalyAlertRepository anomalyAlertRepository;

    public AnomalyDetectionService(MilkProductionRecordRepository productionRecordRepository,
                                    AnomalyAlertRepository anomalyAlertRepository) {
        this.productionRecordRepository = productionRecordRepository;
        this.anomalyAlertRepository = anomalyAlertRepository;
    }

    @Transactional
    public void checkForAnomaly(MilkProductionRecord justInserted) {
        List<MilkProductionRecord> history = productionRecordRepository
                .findByAnimalIdAndSessionAndProductionDateBetweenOrderByProductionDateAsc(
                        justInserted.getAnimal().getId(),
                        justInserted.getSession(),
                        justInserted.getProductionDate().minusDays(LOOKBACK_DAYS),
                        justInserted.getProductionDate().minusDays(1));

        if (history.size() < MIN_DATA_POINTS) {
            return;
        }

        double[] values = history.stream().mapToDouble(r -> r.getQuantityLitres().doubleValue()).toArray();
        double mean = average(values);
        double stddev = populationStdDev(values, mean);
        double actual = justInserted.getQuantityLitres().doubleValue();

        if (actual >= mean) {
            return;
        }

        Severity severity;
        BigDecimal zScore;
        if (stddev == 0.0) {
            BigDecimal deviation = round(mean - actual);
            if (deviation.compareTo(ZERO_STDDEV_MIN_DEVIATION) < 0) {
                return;
            }
            severity = Severity.HIGH;
            zScore = UNDEFINED_ZSCORE_SENTINEL;
        } else {
            double z = (actual - mean) / stddev;
            double absZ = Math.abs(z);
            if (absZ < 2.0) {
                return;
            }
            severity = absZ < 3.0 ? Severity.LOW : absZ < 4.0 ? Severity.MEDIUM : Severity.HIGH;
            zScore = round(z);
        }

        AnomalyAlert alert = AnomalyAlert.builder()
                .animal(justInserted.getAnimal())
                .productionDate(justInserted.getProductionDate())
                .baselineMean(round(mean))
                .baselineStddev(round(stddev))
                .actualValue(round(actual))
                .zScore(zScore)
                .severity(severity)
                .acknowledged(false)
                .build();
        anomalyAlertRepository.save(alert);
    }

    private static double average(double[] values) {
        double sum = 0;
        for (double v : values) {
            sum += v;
        }
        return sum / values.length;
    }

    private static double populationStdDev(double[] values, double mean) {
        double sumSquaredDeviations = 0;
        for (double v : values) {
            double deviation = v - mean;
            sumSquaredDeviations += deviation * deviation;
        }
        return Math.sqrt(sumSquaredDeviations / values.length);
    }

    private static BigDecimal round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
