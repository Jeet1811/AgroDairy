package com.agrodairy.animal.repository;

import com.agrodairy.animal.entity.MilkProductionRecord;
import com.agrodairy.animal.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface MilkProductionRecordRepository extends JpaRepository<MilkProductionRecord, UUID>,
        JpaSpecificationExecutor<MilkProductionRecord> {

    boolean existsByAnimalId(UUID animalId);

    List<MilkProductionRecord> findByAnimalIdAndSessionAndProductionDateBetweenOrderByProductionDateAsc(
            UUID animalId, Session session, LocalDate from, LocalDate to);
}
