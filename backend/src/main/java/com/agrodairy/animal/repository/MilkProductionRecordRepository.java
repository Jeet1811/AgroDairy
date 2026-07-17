package com.agrodairy.animal.repository;

import com.agrodairy.animal.entity.MilkProductionRecord;
import com.agrodairy.animal.entity.Session;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface MilkProductionRecordRepository extends JpaRepository<MilkProductionRecord, UUID> {

    boolean existsByAnimalId(UUID animalId);

    @Query("select r from MilkProductionRecord r where r.animal.id = :animalId "
            + "and (:from is null or r.productionDate >= :from) "
            + "and (:to is null or r.productionDate <= :to)")
    Page<MilkProductionRecord> search(@Param("animalId") UUID animalId,
                                       @Param("from") LocalDate from,
                                       @Param("to") LocalDate to,
                                       Pageable pageable);

    List<MilkProductionRecord> findByAnimalIdAndSessionAndProductionDateBetweenOrderByProductionDateAsc(
            UUID animalId, Session session, LocalDate from, LocalDate to);
}
