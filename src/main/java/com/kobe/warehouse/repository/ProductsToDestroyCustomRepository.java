package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.ProductsToDestroy;
import com.kobe.warehouse.service.product_to_destroy.dto.ProductToDestroySumDTO;
import org.springframework.data.jpa.domain.Specification;

public interface ProductsToDestroyCustomRepository {
    ProductToDestroySumDTO getSum(Specification<ProductsToDestroy> specification);
}
