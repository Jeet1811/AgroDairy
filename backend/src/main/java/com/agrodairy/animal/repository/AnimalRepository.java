package com.agrodairy.animal.repository;

import com.agrodairy.animal.entity.Animal;
import com.agrodairy.animal.entity.AnimalStatus;
import com.agrodairy.animal.entity.AnimalType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface AnimalRepository extends JpaRepository<Animal, UUID> {

    @Query("select a from Animal a where (:status is null or a.status = :status) and (:type is null or a.type = :type)")
    Page<Animal> search(@Param("status") AnimalStatus status, @Param("type") AnimalType type, Pageable pageable);

    boolean existsByTag(String tag);
}
