package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.service.stock.dto.LotPerimeValeurSum;
import org.springframework.data.jpa.domain.Specification;

public interface ProduitCustomRepository {
    LotPerimeValeurSum fetchPerimeSum(Specification<Produit> specification);
}
