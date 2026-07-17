package com.agrodairy.ai.repository;

import com.agrodairy.ai.entity.AnomalyAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AnomalyAlertRepository extends JpaRepository<AnomalyAlert, UUID> {
}
