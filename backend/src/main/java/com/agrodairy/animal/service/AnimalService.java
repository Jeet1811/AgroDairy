package com.agrodairy.animal.service;

import com.agrodairy.ai.service.AnomalyDetectionService;
import com.agrodairy.animal.dto.AnimalResponse;
import com.agrodairy.animal.dto.CreateAnimalRequest;
import com.agrodairy.animal.dto.CreateProductionRecordRequest;
import com.agrodairy.animal.dto.ProductionRecordResponse;
import com.agrodairy.animal.dto.UpdateAnimalRequest;
import com.agrodairy.animal.entity.Animal;
import com.agrodairy.animal.entity.AnimalStatus;
import com.agrodairy.animal.entity.AnimalType;
import com.agrodairy.animal.entity.MilkProductionRecord;
import com.agrodairy.animal.repository.AnimalRepository;
import com.agrodairy.animal.repository.MilkProductionRecordRepository;
import com.agrodairy.auth.entity.User;
import com.agrodairy.auth.repository.UserRepository;
import com.agrodairy.common.exception.ApiException;
import com.agrodairy.common.exception.NotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class AnimalService {

    private final AnimalRepository animalRepository;
    private final MilkProductionRecordRepository productionRecordRepository;
    private final UserRepository userRepository;
    private final AnomalyDetectionService anomalyDetectionService;

    public AnimalService(AnimalRepository animalRepository,
                          MilkProductionRecordRepository productionRecordRepository,
                          UserRepository userRepository,
                          AnomalyDetectionService anomalyDetectionService) {
        this.animalRepository = animalRepository;
        this.productionRecordRepository = productionRecordRepository;
        this.userRepository = userRepository;
        this.anomalyDetectionService = anomalyDetectionService;
    }

    public Page<AnimalResponse> list(AnimalStatus status, AnimalType type, Pageable pageable) {
        return animalRepository.search(status, type, pageable).map(AnimalResponse::from);
    }

    public AnimalResponse get(UUID id) {
        return AnimalResponse.from(findAnimalOrThrow(id));
    }

    @Transactional
    public AnimalResponse create(CreateAnimalRequest request) {
        if (animalRepository.existsByTag(request.tag())) {
            throw new ApiException(HttpStatus.CONFLICT, "CONFLICT", "An animal with this tag already exists");
        }
        Animal animal = Animal.builder()
                .tag(request.tag())
                .type(request.type())
                .breed(request.breed())
                .dateOfBirth(request.dateOfBirth())
                .status(request.status())
                .notes(request.notes())
                .build();
        animalRepository.saveAndFlush(animal);
        return AnimalResponse.from(animal);
    }

    @Transactional
    public AnimalResponse update(UUID id, UpdateAnimalRequest request) {
        Animal animal = findAnimalOrThrow(id);
        if (request.tag() != null) {
            animal.setTag(request.tag());
        }
        if (request.type() != null) {
            animal.setType(request.type());
        }
        if (request.breed() != null) {
            animal.setBreed(request.breed());
        }
        if (request.dateOfBirth() != null) {
            animal.setDateOfBirth(request.dateOfBirth());
        }
        if (request.status() != null) {
            animal.setStatus(request.status());
        }
        if (request.notes() != null) {
            animal.setNotes(request.notes());
        }
        animalRepository.saveAndFlush(animal);
        return AnimalResponse.from(animal);
    }

    @Transactional
    public void delete(UUID id) {
        Animal animal = findAnimalOrThrow(id);
        if (productionRecordRepository.existsByAnimalId(id)) {
            throw new ApiException(HttpStatus.CONFLICT, "CONFLICT",
                    "Cannot delete an animal with existing production records; set status to SOLD or DECEASED instead");
        }
        animalRepository.delete(animal);
    }

    public Page<ProductionRecordResponse> listProduction(UUID animalId, LocalDate from, LocalDate to, Pageable pageable) {
        findAnimalOrThrow(animalId);
        return productionRecordRepository.search(animalId, from, to, pageable).map(ProductionRecordResponse::from);
    }

    @Transactional
    public ProductionRecordResponse recordProduction(UUID animalId, CreateProductionRecordRequest request, UUID recordedByUserId) {
        Animal animal = findAnimalOrThrow(animalId);
        User recordedBy = recordedByUserId != null ? userRepository.findById(recordedByUserId).orElse(null) : null;

        MilkProductionRecord record = MilkProductionRecord.builder()
                .animal(animal)
                .productionDate(request.productionDate())
                .session(request.session())
                .quantityLitres(request.quantityLitres())
                .recordedBy(recordedBy)
                .build();
        try {
            productionRecordRepository.saveAndFlush(record);
        } catch (DataIntegrityViolationException e) {
            throw new ApiException(HttpStatus.CONFLICT, "CONFLICT",
                    "A production record already exists for this animal, date, and session");
        }

        anomalyDetectionService.checkForAnomaly(record);
        return ProductionRecordResponse.from(record);
    }

    private Animal findAnimalOrThrow(UUID id) {
        return animalRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Animal not found"));
    }
}
