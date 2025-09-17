package com.kobe.warehouse.service;

import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.TiersPayantCategorie;
import com.kobe.warehouse.domain.enumeration.TiersPayantStatut;
import com.kobe.warehouse.repository.TiersPayantRepository;
import com.kobe.warehouse.service.dto.TiersPayantDto;
import com.kobe.warehouse.service.dto.TiersPayantMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class TiersPayantDataService implements TiersPayantMapper {

    private final TiersPayantRepository tiersPayantRepository;

    public TiersPayantDataService(TiersPayantRepository tiersPayantRepository) {
        this.tiersPayantRepository = tiersPayantRepository;
    }

    public Page<TiersPayantDto> list(
        String search,
        String categorie,
        TiersPayantStatut statut,
        Long groupeTiersPayantId,
        Pageable pageable
    ) {
        Specification<TiersPayant> specification = this.tiersPayantRepository.specialisationStatut(statut);
        if (StringUtils.hasLength(search)) {
            specification = specification.and(this.tiersPayantRepository.specialisationQueryString(search + "%"));
        }
        if (groupeTiersPayantId != null) {
            specification = specification.and(this.tiersPayantRepository.specialisationByGroup(groupeTiersPayantId));
        }
        TiersPayantCategorie tiersPayantCategorie ;
        if (StringUtils.hasLength(categorie) && !categorie.equals(EntityConstant.TOUT)) {
            tiersPayantCategorie = TiersPayantCategorie.valueOf(categorie);
            specification = specification.and(this.tiersPayantRepository.specialisationCategorie(tiersPayantCategorie));
        }
        Pageable page = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "fullName"));
        return this.tiersPayantRepository.findAll(specification, page).map(this::fromEntity);
    }
}
