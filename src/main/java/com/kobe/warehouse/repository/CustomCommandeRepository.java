package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.service.dto.records.AchatRecord;
import org.springframework.data.jpa.domain.Specification;

public interface CustomCommandeRepository {

    AchatRecord getAchatPeriode(Specification<Commande> specification);
}
