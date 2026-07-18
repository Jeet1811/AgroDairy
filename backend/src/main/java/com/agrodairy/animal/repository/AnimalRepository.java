package com.agrodairy.animal.repository;

import com.agrodairy.animal.entity.Animal;
import com.agrodairy.animal.entity.AnimalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface AnimalRepository extends JpaRepository<Animal, UUID>, JpaSpecificationExecutor<Animal> {

    boolean existsByTag(String tag);

    long countByStatusNotIn(List<AnimalStatus> statuses);
}
