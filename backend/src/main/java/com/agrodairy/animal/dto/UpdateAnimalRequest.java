package com.agrodairy.animal.dto;

import com.agrodairy.animal.entity.AnimalStatus;
import com.agrodairy.animal.entity.AnimalType;

import java.time.LocalDate;

/** Partial update — any field left null is left unchanged on the existing Animal. */
public record UpdateAnimalRequest(
        String tag,
        AnimalType type,
        String breed,
        LocalDate dateOfBirth,
        AnimalStatus status,
        String notes
) {}
