package com.agrodairy.ai.entity;

import com.agrodairy.animal.entity.Animal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "anomaly_alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnomalyAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "animal_id", nullable = false)
    private Animal animal;

    @CreationTimestamp
    @Column(name = "detected_at", nullable = false, updatable = false)
    private Instant detectedAt;

    @Column(name = "production_date", nullable = false)
    private LocalDate productionDate;

    @Column(name = "baseline_mean", nullable = false, precision = 6, scale = 2)
    private BigDecimal baselineMean;

    @Column(name = "baseline_stddev", nullable = false, precision = 6, scale = 2)
    private BigDecimal baselineStddev;

    @Column(name = "actual_value", nullable = false, precision = 6, scale = 2)
    private BigDecimal actualValue;

    @Column(name = "z_score", nullable = false, precision = 6, scale = 2)
    private BigDecimal zScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Severity severity;

    @Column(nullable = false)
    private boolean acknowledged;
}
