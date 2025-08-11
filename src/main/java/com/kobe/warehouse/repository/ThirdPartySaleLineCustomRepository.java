package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.service.tiers_payant.TiersPayantAchat;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface ThirdPartySaleLineCustomRepository {
    List<TiersPayantAchat> fetchAchatTiersPayant(Specification<ThirdPartySaleLine> specification, Pageable pageable);
}
