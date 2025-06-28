package com.kobe.warehouse.service.mobile.dto;


import com.kobe.warehouse.service.dto.StoreInventoryLineDTO;

import java.util.List;

public record RayonInventaireDetail(long rayonId, List<StoreInventoryLineDTO> items) {
}
