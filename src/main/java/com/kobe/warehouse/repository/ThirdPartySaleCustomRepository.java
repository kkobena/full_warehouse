package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.service.tiketz.dto.TicketZCreditProjection;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public interface ThirdPartySaleCustomRepository {
    List<TicketZCreditProjection> getTicketZCreditProjection(Specification<ThirdPartySales> specification);
}
