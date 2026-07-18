package com.agrodairy.ai.service;

import com.agrodairy.ai.dto.AnomalyAlertResponse;
import com.agrodairy.ai.entity.AnomalyAlert;
import com.agrodairy.ai.entity.Severity;
import com.agrodairy.ai.repository.AnomalyAlertRepository;
import com.agrodairy.common.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AnomalyAlertService {

    private final AnomalyAlertRepository anomalyAlertRepository;

    public AnomalyAlertService(AnomalyAlertRepository anomalyAlertRepository) {
        this.anomalyAlertRepository = anomalyAlertRepository;
    }

    @Transactional(readOnly = true)
    public Page<AnomalyAlertResponse> list(Boolean acknowledged, Severity severity, Pageable pageable) {
        Specification<AnomalyAlert> spec = (root, query, cb) -> cb.conjunction();
        if (acknowledged != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("acknowledged"), acknowledged));
        }
        if (severity != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("severity"), severity));
        }
        return anomalyAlertRepository.findAll(spec, pageable).map(AnomalyAlertResponse::from);
    }

    @Transactional
    public AnomalyAlertResponse acknowledge(UUID id) {
        AnomalyAlert alert = anomalyAlertRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Anomaly alert not found"));
        alert.setAcknowledged(true);
        anomalyAlertRepository.save(alert);
        return AnomalyAlertResponse.from(alert);
    }
}
