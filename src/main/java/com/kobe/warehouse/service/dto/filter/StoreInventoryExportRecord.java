package com.kobe.warehouse.service.dto.filter;

import com.kobe.warehouse.service.dto.enumeration.StoreInventoryExportGroupBy;

public record StoreInventoryExportRecord(StoreInventoryExportGroupBy exportGroupBy, StoreInventoryLineFilterRecord filterRecord) {}
