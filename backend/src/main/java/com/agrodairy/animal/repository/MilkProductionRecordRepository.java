package com.agrodairy.animal.repository;

import com.agrodairy.animal.entity.MilkProductionRecord;
import com.agrodairy.animal.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface MilkProductionRecordRepository extends JpaRepository<MilkProductionRecord, UUID>,
        JpaSpecificationExecutor<MilkProductionRecord> {

    boolean existsByAnimalId(UUID animalId);

    List<MilkProductionRecord> findByAnimalIdAndSessionAndProductionDateBetweenOrderByProductionDateAsc(
            UUID animalId, Session session, LocalDate from, LocalDate to);

    @Query("select coalesce(sum(r.quantityLitres), 0) from MilkProductionRecord r where r.productionDate = :date")
    BigDecimal sumQuantityByProductionDate(@Param("date") LocalDate date);

    @Query(value = "SELECT production_date AS d, SUM(quantity_litres) AS litres FROM milk_production_records "
            + "WHERE production_date >= :start GROUP BY production_date ORDER BY production_date", nativeQuery = true)
    List<Object[]> findDailyProductionSince(@Param("start") LocalDate start);
}
