package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.service.stock.dto.LotPerimeValeurSum;
import org.springframework.data.jpa.domain.Specification;

public interface LotCustomRepository {
    LotPerimeValeurSum fetchPerimeSum(Specification<Lot> specification);
}
