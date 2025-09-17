package com.kobe.warehouse.repository;

import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.service.dto.projection.AchatTiersPayant;
import com.kobe.warehouse.service.facturation.dto.TiersPayantDossierFactureDto;
import com.kobe.warehouse.service.tiers_payant.TiersPayantAchat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public interface ThirdPartySaleLineCustomRepository {
    List<TiersPayantAchat> fetchAchatTiersPayant(Specification<ThirdPartySaleLine> specification, Pageable pageable);

    Page<TiersPayantDossierFactureDto> fetch(Specification<ThirdPartySaleLine> specification, Pageable pageable);

    Page<TiersPayantDossierFactureDto> fetchGroup(Specification<ThirdPartySaleLine> specification, Pageable pageable);

    Page<AchatTiersPayant> fetchAchatsTiersPayant(
        Specification<ThirdPartySaleLine> specification,
        Pageable pageable
    );
}
