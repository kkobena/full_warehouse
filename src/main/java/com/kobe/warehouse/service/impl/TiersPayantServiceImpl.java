package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.ModelFacture;
import com.kobe.warehouse.domain.enumeration.OrdreTrisFacture;
import com.kobe.warehouse.domain.enumeration.TiersPayantStatut;
import com.kobe.warehouse.repository.ClientTiersPayantRepository;
import com.kobe.warehouse.repository.TiersPayantRepository;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.TiersPayantService;
import com.kobe.warehouse.service.dto.Pair;
import com.kobe.warehouse.service.dto.TiersPayantDto;
import com.kobe.warehouse.service.errors.GenericError;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TiersPayantServiceImpl implements TiersPayantService {

    private final Logger log = LoggerFactory.getLogger(TiersPayantServiceImpl.class);
    private final TiersPayantRepository tiersPayantRepository;
    private final StorageService storageService;
    private final ClientTiersPayantRepository clientTiersPayantRepository;

    public TiersPayantServiceImpl(
        TiersPayantRepository tiersPayantRepository,
        StorageService storageService,
        ClientTiersPayantRepository clientTiersPayantRepository
    ) {
        this.tiersPayantRepository = tiersPayantRepository;
        this.storageService = storageService;
        this.clientTiersPayantRepository = clientTiersPayantRepository;
    }

    @Override
    public TiersPayantDto createFromDto(TiersPayantDto dto) throws GenericError {
        if (StringUtils.isNotEmpty(dto.getCodeOrganisme())) {
            Optional<TiersPayant> tiersPayantOp = tiersPayantRepository.findOneByNameOrFullNameOrCodeOrganisme(
                dto.getName(),
                dto.getFullName(),
                dto.getCodeOrganisme()
            );
            if (tiersPayantOp.isPresent()) {
                throw new GenericError(
                    "Il existe dejà  un tiers-payant avec soit avec le même nom ou le code orgasisme",
                    "tiersPayantExistant"
                );
            }
        } else {
            Optional<TiersPayant> tiersPayantOp = tiersPayantRepository.findOneByNameOrFullName(dto.getName(), dto.getFullName());
            if (tiersPayantOp.isPresent()) {
                throw new GenericError("Il existe dejà  un tiers-payant avec soit avec le même nom ", "tiersPayantExistant");
            }
        }

        TiersPayant tiersPayant = entityFromDto(dto, new TiersPayant().setCreated(LocalDateTime.now()));
        tiersPayant.setUpdatedBy(storageService.getUser());
        tiersPayant = tiersPayantRepository.save(tiersPayant);
        return fromEntity(tiersPayant);
    }

    @Override
    public TiersPayantDto updateFromDto(TiersPayantDto dto) throws GenericError {
        TiersPayant tiersPayant = tiersPayantRepository.getReferenceById(dto.getId());
        if (StringUtils.isNotEmpty(dto.getCodeOrganisme())) {
            Optional<TiersPayant> tiersPayantOp = tiersPayantRepository.findOneByNameOrFullNameOrCodeOrganisme(
                dto.getName(),
                dto.getFullName(),
                dto.getCodeOrganisme()
            );
            if (tiersPayantOp.isPresent() && !Objects.equals(tiersPayant.getId(), tiersPayantOp.get().getId())) {
                throw new GenericError(
                    "Il existe dejà  un tiers-payant avec soit avec le même nom ou le code orgasisme",
                    "tiersPayantExistant"
                );
            }
        } else {
            Optional<TiersPayant> tiersPayantOp = tiersPayantRepository.findOneByNameOrFullName(dto.getName(), dto.getFullName());
            if (tiersPayantOp.isPresent() && !Objects.equals(tiersPayant.getId(), tiersPayantOp.get().getId())) {
                throw new GenericError("Il existe dejà  un tiers-payant avec soit avec le même nom ", "tiersPayantExistant");
            }
        }

        tiersPayant = entityFromDto(dto, tiersPayant);
        tiersPayant.setUpdatedBy(storageService.getUser());
        return fromEntity(tiersPayantRepository.save(tiersPayant));
    }

    @Override
    public void desable(Long id) {
        tiersPayantRepository.save(
            tiersPayantRepository
                .getReferenceById(id)
                .setStatut(TiersPayantStatut.DISABLED)
                .setUpdated(LocalDateTime.now())
                .setUpdatedBy(storageService.getUser())
        );
    }

    @Override
    public void delete(Long id) throws GenericError {
        try {
            clientTiersPayantRepository.deleteAll(clientTiersPayantRepository.findAllByTiersPayantId(id));
            tiersPayantRepository.deleteById(id);
        } catch (Exception e) {
            log.error("delete {}", e);
            throw new GenericError("Il y'a client associés à ce tiers-payant", "tiersPayantClientsAssocies");
        }
    }

    @Override
    public List<Pair> getModelFacture() {
        return Stream.of(ModelFacture.values()).map(modelFacture -> new Pair(modelFacture.getValue(), modelFacture.getLibelle())).toList();
    }

    @Override
    public List<Pair> getOrdreTrisFacture() {
        return Stream.of(OrdreTrisFacture.values()).map(modelFacture -> new Pair(modelFacture.name(), modelFacture.getLibelle())).toList();
    }
}
