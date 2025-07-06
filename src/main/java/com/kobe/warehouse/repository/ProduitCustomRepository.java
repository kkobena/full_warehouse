package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.service.stock.dto.LotPerimeValeurTotal;
import org.springframework.data.jpa.domain.Specification;

public interface ProduitCustomRepository {
    LotPerimeValeurTotal fetchPerimeSum(Specification<Produit> specification);
}
