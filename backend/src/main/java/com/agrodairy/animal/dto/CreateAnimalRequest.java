package com.agrodairy.animal.dto;

import com.agrodairy.animal.entity.AnimalStatus;
import com.agrodairy.animal.entity.AnimalType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateAnimalRequest(
        @NotBlank String tag,
        @NotNull AnimalType type,
        String breed,
        LocalDate dateOfBirth,
        @NotNull AnimalStatus status,
        String notes
) {}
