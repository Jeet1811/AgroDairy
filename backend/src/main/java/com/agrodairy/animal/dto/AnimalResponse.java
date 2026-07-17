package com.agrodairy.animal.dto;

import com.agrodairy.animal.entity.Animal;
import com.agrodairy.animal.entity.AnimalStatus;
import com.agrodairy.animal.entity.AnimalType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AnimalResponse(
        UUID id,
        String tag,
        AnimalType type,
        String breed,
        LocalDate dateOfBirth,
        AnimalStatus status,
        String lactationStatus,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    public static AnimalResponse from(Animal animal) {
        return new AnimalResponse(
                animal.getId(),
                animal.getTag(),
                animal.getType(),
                animal.getBreed(),
                animal.getDateOfBirth(),
                animal.getStatus(),
                animal.getLactationStatus(),
                animal.getNotes(),
                animal.getCreatedAt(),
                animal.getUpdatedAt());
    }
}
