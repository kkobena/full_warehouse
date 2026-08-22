package com.kobe.warehouse.service.declaration_ca;

import com.kobe.warehouse.domain.Rayon;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.repository.RayonRepository;
import com.kobe.warehouse.repository.TiersPayantRepository;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.declaration_ca.dto.ExclusionItemDTO;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ExclusionReferentielServiceImpl implements ExclusionReferentielService {

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
    public List<ExclusionItemDTO> listerRayons(Boolean exclus) {
        Integer storageId = storageService.getDefaultConnectedUserMainStorage().getId();
        return rayonRepository
            .findAllByStorageIdOrderByLibelle(storageId)
            .stream()
            .filter(rayon -> exclus == null || rayon.isExclude() == exclus)
            .map(rayon -> new ExclusionItemDTO(rayon.getId(), rayon.getCode(), rayon.getLibelle(), rayon.isExclude()))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExclusionItemDTO> listerTiersPayants(Boolean exclus) {
        return tiersPayantRepository
            .findAll()
            .stream()
            .filter(tiersPayant -> exclus == null || tiersPayant.isBeExclude() == exclus)
            .sorted(Comparator.comparing(TiersPayant::getFullName, String.CASE_INSENSITIVE_ORDER))
            .map(tiersPayant ->
                new ExclusionItemDTO(
                    tiersPayant.getId(),
                    tiersPayant.getCodeOrganisme(),
                    tiersPayant.getFullName(),
                    tiersPayant.isBeExclude()
                )
            )
            .toList();
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
