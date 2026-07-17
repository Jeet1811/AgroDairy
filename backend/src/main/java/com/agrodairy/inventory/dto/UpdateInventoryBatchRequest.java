package com.agrodairy.inventory.dto;

import com.agrodairy.inventory.entity.BatchStatus;

public record UpdateInventoryBatchRequest(
        BatchStatus status
) {}
