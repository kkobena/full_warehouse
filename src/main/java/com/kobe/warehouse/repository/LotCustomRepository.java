package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.service.stock.dto.LotPerimeValeurTotal;
import org.springframework.data.jpa.domain.Specification;

public interface LotCustomRepository {
    LotPerimeValeurTotal fetchPerimeSum(Specification<Lot> specification);
}
