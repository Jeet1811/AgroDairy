package com.agrodairy.ai.repository;

import com.agrodairy.ai.entity.ForecastLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ForecastLogRepository extends JpaRepository<ForecastLog, UUID> {
}
