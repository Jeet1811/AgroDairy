package com.agrodairy.ai.service;

import com.agrodairy.ai.entity.AnomalyAlert;
import com.agrodairy.ai.entity.Severity;
import com.agrodairy.ai.repository.AnomalyAlertRepository;
import com.agrodairy.animal.entity.Animal;
import com.agrodairy.animal.entity.AnimalStatus;
import com.agrodairy.animal.entity.AnimalType;
import com.agrodairy.animal.entity.MilkProductionRecord;
import com.agrodairy.animal.entity.Session;
import com.agrodairy.animal.repository.MilkProductionRecordRepository;
import com.agrodairy.auth.entity.Role;
import com.agrodairy.notification.entity.NotificationType;
import com.agrodairy.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnomalyDetectionServiceTest {

    @Mock
    private MilkProductionRecordRepository productionRecordRepository;

    @Mock
    private AnomalyAlertRepository anomalyAlertRepository;

    @Mock
    private NotificationService notificationService;

    private AnomalyDetectionService newService() {
        return new AnomalyDetectionService(productionRecordRepository, anomalyAlertRepository, notificationService);
    }

    private static Animal testAnimal() {
        return Animal.builder()
                .id(UUID.randomUUID())
                .tag("COW-001")
                .type(AnimalType.COW)
                .status(AnimalStatus.MILKING)
                .build();
    }

    private static MilkProductionRecord historyRecord(Animal animal, LocalDate date, Session session, double litres) {
        return MilkProductionRecord.builder()
                .id(UUID.randomUUID())
                .animal(animal)
                .productionDate(date)
                .session(session)
                .quantityLitres(BigDecimal.valueOf(litres))
                .build();
    }

    private static MilkProductionRecord todayRecord(Animal animal, LocalDate date, Session session, double litres) {
        return MilkProductionRecord.builder()
                .id(UUID.randomUUID())
                .animal(animal)
                .productionDate(date)
                .session(session)
                .quantityLitres(BigDecimal.valueOf(litres))
                .build();
    }

    /** 14 days alternating 18.0/22.0 litres — population mean 20.00, population stddev exactly 2.00. */
    private static List<MilkProductionRecord> fourteenDayAlternatingHistory(Animal animal, LocalDate today, Session session) {
        List<MilkProductionRecord> history = new ArrayList<>();
        for (int i = 14; i >= 1; i--) {
            double litres = (i % 2 == 0) ? 18.0 : 22.0;
            history.add(historyRecord(animal, today.minusDays(i), session, litres));
        }
        return history;
    }

    @Test
    void knownDropProducesExactZScoreAndSeverity() {
        AnomalyDetectionService anomalyDetectionService = newService();
        Animal animal = testAnimal();
        LocalDate today = LocalDate.of(2026, 7, 15);
        Session session = Session.MORNING;
        List<MilkProductionRecord> history = fourteenDayAlternatingHistory(animal, today, session);

        when(productionRecordRepository.findByAnimalIdAndSessionAndProductionDateBetweenOrderByProductionDateAsc(
                eq(animal.getId()), eq(session), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(history);

        MilkProductionRecord today13Litres = todayRecord(animal, today, session, 13.0);
        anomalyDetectionService.checkForAnomaly(today13Litres);

        ArgumentCaptor<AnomalyAlert> captor = ArgumentCaptor.forClass(AnomalyAlert.class);
        verify(anomalyAlertRepository).save(captor.capture());
        AnomalyAlert alert = captor.getValue();

        assertThat(alert.getBaselineMean()).isEqualByComparingTo("20.00");
        assertThat(alert.getBaselineStddev()).isEqualByComparingTo("2.00");
        assertThat(alert.getActualValue()).isEqualByComparingTo("13.00");
        assertThat(alert.getZScore()).isEqualByComparingTo("-3.50");
        assertThat(alert.getSeverity()).isEqualTo(Severity.MEDIUM);
        assertThat(alert.getAnimal()).isEqualTo(animal);
        assertThat(alert.getProductionDate()).isEqualTo(today);

        verify(notificationService).notifyRoles(eq(List.of(Role.STAFF, Role.ADMIN)), eq(NotificationType.ANOMALY_ALERT), any(String.class));
    }

    @Test
    void fewerThanSevenDataPointsSkipsCheckEntirely() {
        AnomalyDetectionService anomalyDetectionService = newService();
        Animal animal = testAnimal();
        LocalDate today = LocalDate.of(2026, 7, 15);
        Session session = Session.MORNING;
        List<MilkProductionRecord> history = List.of(
                historyRecord(animal, today.minusDays(1), session, 20.0),
                historyRecord(animal, today.minusDays(2), session, 20.0),
                historyRecord(animal, today.minusDays(3), session, 20.0));

        when(productionRecordRepository.findByAnimalIdAndSessionAndProductionDateBetweenOrderByProductionDateAsc(
                eq(animal.getId()), eq(session), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(history);

        anomalyDetectionService.checkForAnomaly(todayRecord(animal, today, session, 5.0));

        verify(anomalyAlertRepository, never()).save(any());
    }

    @Test
    void zeroStddevWithDeviationAtLeastOneLitreIsHighSeverity() {
        AnomalyDetectionService anomalyDetectionService = newService();
        Animal animal = testAnimal();
        LocalDate today = LocalDate.of(2026, 7, 15);
        Session session = Session.EVENING;
        List<MilkProductionRecord> history = new ArrayList<>();
        for (int i = 14; i >= 1; i--) {
            history.add(historyRecord(animal, today.minusDays(i), session, 20.0));
        }

        when(productionRecordRepository.findByAnimalIdAndSessionAndProductionDateBetweenOrderByProductionDateAsc(
                eq(animal.getId()), eq(session), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(history);

        anomalyDetectionService.checkForAnomaly(todayRecord(animal, today, session, 18.0));

        ArgumentCaptor<AnomalyAlert> captor = ArgumentCaptor.forClass(AnomalyAlert.class);
        verify(anomalyAlertRepository).save(captor.capture());
        assertThat(captor.getValue().getSeverity()).isEqualTo(Severity.HIGH);
        assertThat(captor.getValue().getBaselineStddev()).isEqualByComparingTo("0.00");
    }

    @Test
    void zeroStddevWithSmallDeviationDoesNotAlert() {
        AnomalyDetectionService anomalyDetectionService = newService();
        Animal animal = testAnimal();
        LocalDate today = LocalDate.of(2026, 7, 15);
        Session session = Session.EVENING;
        List<MilkProductionRecord> history = new ArrayList<>();
        for (int i = 14; i >= 1; i--) {
            history.add(historyRecord(animal, today.minusDays(i), session, 20.0));
        }

        when(productionRecordRepository.findByAnimalIdAndSessionAndProductionDateBetweenOrderByProductionDateAsc(
                eq(animal.getId()), eq(session), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(history);

        anomalyDetectionService.checkForAnomaly(todayRecord(animal, today, session, 19.5));

        verify(anomalyAlertRepository, never()).save(any());
    }

    @Test
    void increaseInProductionNeverAlerts() {
        AnomalyDetectionService anomalyDetectionService = newService();
        Animal animal = testAnimal();
        LocalDate today = LocalDate.of(2026, 7, 15);
        Session session = Session.MORNING;
        List<MilkProductionRecord> history = fourteenDayAlternatingHistory(animal, today, session);

        when(productionRecordRepository.findByAnimalIdAndSessionAndProductionDateBetweenOrderByProductionDateAsc(
                eq(animal.getId()), eq(session), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(history);

        anomalyDetectionService.checkForAnomaly(todayRecord(animal, today, session, 40.0));

        verify(anomalyAlertRepository, never()).save(any());
    }
}
