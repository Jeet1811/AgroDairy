package com.agrodairy.common.dto;

import org.springframework.data.domain.Page;

public record PageResponse<T>(int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> of(Page<?> page) {
        return new PageResponse<>(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
