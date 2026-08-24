package com.kobe.warehouse.service.declaration_ca;

import com.kobe.warehouse.domain.Rayon;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.TiersPayantCategorie;
import com.kobe.warehouse.repository.RayonRepository;
import com.kobe.warehouse.repository.TiersPayantRepository;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.declaration_ca.dto.ExclusionItemDTO;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class ExclusionReferentielServiceImpl implements ExclusionReferentielService {

    /**
     * Catégories écartées de la liste des tiers-payants : elles ne portent pas de chiffre
     * d'affaires tiers-payant à exclure — le carnet est un crédit client, le dépôt un stock
     * avancé.
     */
    private static final EnumSet<TiersPayantCategorie> CATEGORIES_HORS_EXCLUSION = EnumSet.of(
        TiersPayantCategorie.CARNET,
        TiersPayantCategorie.DEPOT
    );

    private final RayonRepository rayonRepository;
    private final TiersPayantRepository tiersPayantRepository;
    private final StorageService storageService;

    public ExclusionReferentielServiceImpl(
        RayonRepository rayonRepository,
        TiersPayantRepository tiersPayantRepository,
        StorageService storageService
    ) {
        this.rayonRepository = rayonRepository;
        this.tiersPayantRepository = tiersPayantRepository;
        this.storageService = storageService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExclusionItemDTO> listerRayons(Boolean exclus, String search, Pageable pageable) {
        Integer storageId = storageService.getDefaultConnectedUserMainStorage().getId();
        Specification<Rayon> specification = rayonRepository.specialisationStorage(storageId)
            .and(rayonRepository.notSansEmplacement());
        if (exclus != null) {
            specification = specification.and(rayonRepository.isExclude(exclus));
        }
        if (StringUtils.hasLength(search)) {
            specification = specification.and(
                rayonRepository.specialisationQueryString(search.toUpperCase() + "%")
            );
        }
        return rayonRepository
            .findAll(specification, trier(pageable, "libelle"))
            .map(rayon -> new ExclusionItemDTO(rayon.getId(), rayon.getCode(), rayon.getLibelle(),
                rayon.isExclude()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExclusionItemDTO> listerTiersPayants(Boolean exclus, String search,
        Pageable pageable) {
        Specification<TiersPayant> specification = tiersPayantRepository
            .isActif()
            .and(tiersPayantRepository.notEqual(TiersPayantCategorie.ASSURANCE));
        if (exclus != null) {
            specification = specification.and(tiersPayantRepository.isBeExclude(exclus));
        }
        if (StringUtils.hasLength(search)) {
            specification = specification.and(
                tiersPayantRepository.specialisationQueryString(search.toUpperCase() + "%")
            );
        }
        return tiersPayantRepository
            .findAll(specification, trier(pageable, "fullName"))
            .map(tiersPayant ->
                new ExclusionItemDTO(
                    tiersPayant.getId(),
                    tiersPayant.getCodeOrganisme(),
                    tiersPayant.getFullName(),
                    tiersPayant.isBeExclude()
                )
            );
    }

    /**
     * Impose l'ordre alphabétique quand l'appelant n'en a demandé aucun.
     *
     * <p>Sans tri explicite, PostgreSQL ne garantit pas l'ordre entre deux pages : une même ligne
     * pourrait apparaître deux fois, ou aucune.
     */
    private Pageable trier(Pageable pageable, String champParDefaut) {
        if (pageable.getSort().isSorted()) {
            return pageable;
        }
        return PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            Sort.by(Sort.Direction.ASC, champParDefaut)
        );
    }

    @Override
    public int majExclusionRayons(Set<Integer> ids, boolean exclure) {
        List<Rayon> rayons = rayonRepository.findAllById(ids);
        int modifies = 0;
        for (Rayon rayon : rayons) {
            if (rayon.isExclude() != exclure) {
                rayon.setExclude(exclure);
                modifies++;
            }
        }
        rayonRepository.saveAll(rayons);
        return modifies;
    }

    @Override
    public int majExclusionTiersPayants(Set<Integer> ids, boolean exclure) {
        List<TiersPayant> tiersPayants = tiersPayantRepository.findAllById(ids);
        int modifies = 0;
        for (TiersPayant tiersPayant : tiersPayants) {
            if (tiersPayant.isBeExclude() != exclure) {
                tiersPayant.setBeExclude(exclure);
                modifies++;
            }
        }
        tiersPayantRepository.saveAll(tiersPayants);
        return modifies;
    }
}
