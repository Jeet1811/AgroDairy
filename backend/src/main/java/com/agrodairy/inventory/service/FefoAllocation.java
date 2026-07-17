package com.agrodairy.inventory.service;

import com.agrodairy.inventory.entity.InventoryBatch;

/** One batch's contribution to a FEFO-allocated order line, after quantities have already been deducted. */
public record FefoAllocation(InventoryBatch batch, int quantity) {
}
