package com.kobe.warehouse.service.product_to_destroy.dto;

import java.util.List;

public record ProductsToDestroyPayload(Long magasinId, List<ProductToDestroyPayload> products) {}
