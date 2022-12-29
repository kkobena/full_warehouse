package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.TiersPayantStatut;
import com.kobe.warehouse.repository.TiersPayantRepository;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.TiersPayantService;
import com.kobe.warehouse.service.dto.TiersPayantDto;
import com.kobe.warehouse.web.rest.errors.GenericError;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@Transactional
public class TiersPayantServiceImpl implements TiersPayantService {
    private final Logger log = LoggerFactory.getLogger(TiersPayantServiceImpl.class);
    private final TiersPayantRepository tiersPayantRepository;
    private final StorageService storageService;

    public TiersPayantServiceImpl(TiersPayantRepository tiersPayantRepository, StorageService storageService) {
        this.tiersPayantRepository = tiersPayantRepository;
        this.storageService = storageService;
    }

    @Override
    public TiersPayantDto createFromDto(TiersPayantDto dto) throws GenericError {
        if (StringUtils.isNotEmpty(dto.getCodeOrganisme())) {
            Optional<TiersPayant> tiersPayantOp = tiersPayantRepository.findOneByNameOrFullNameOrCodeOrganisme(dto.getName(), dto.getFullName(), dto.getCodeOrganisme());
            if (tiersPayantOp.isPresent())
                throw new GenericError("tierspayant", "Il existe dejà  un tiers-payant avec soit avec le même nom ou le code orgasisme", "tiersPayantExistant");
        } else {
            Optional<TiersPayant> tiersPayantOp = tiersPayantRepository.findOneByNameOrFullName(dto.getName(), dto.getFullName());
            if (tiersPayantOp.isPresent())
                throw new GenericError("tierspayant", "Il existe dejà  un tiers-payant avec soit avec le même nom ", "tiersPayantExistant");
        }

        TiersPayant tiersPayant = entityFromDto(dto);
        tiersPayant.setUpdatedBy(storageService.getUser());
        tiersPayant = tiersPayantRepository.save(tiersPayant);
        return fromEntity(tiersPayant);
    }

    @Override
    public TiersPayantDto updateFromDto(TiersPayantDto dto) throws GenericError {
        TiersPayant tiersPayant = tiersPayantRepository.getReferenceById(dto.getId());
        if (StringUtils.isNotEmpty(dto.getCodeOrganisme())) {
            Optional<TiersPayant> tiersPayantOp = tiersPayantRepository.findOneByNameOrFullNameOrCodeOrganisme(dto.getName(), dto.getFullName(), dto.getCodeOrganisme());
            if (tiersPayantOp.isPresent() && tiersPayant.getId() != tiersPayantOp.get().getId())
                throw new GenericError("tierspayant", "Il existe dejà  un tiers-payant avec soit avec le même nom ou le code orgasisme", "tiersPayantExistant");
        } else {
            Optional<TiersPayant> tiersPayantOp = tiersPayantRepository.findOneByNameOrFullName(dto.getName(), dto.getFullName());
            if (tiersPayantOp.isPresent() && tiersPayant.getId() != tiersPayantOp.get().getId())
                throw new GenericError("tierspayant", "Il existe dejà  un tiers-payant avec soit avec le même nom ", "tiersPayantExistant");
        }

        tiersPayant = entityFromDto(dto, tiersPayant);
        tiersPayant.setUpdatedBy(storageService.getUser());
        tiersPayant = tiersPayantRepository.save(tiersPayant);
        return fromEntity(tiersPayant);
    }

    @Override
    public void desable(Long id) {
        tiersPayantRepository.save(tiersPayantRepository.getReferenceById(id).setStatut(TiersPayantStatut.DISABLED).setUpdated(Instant.now()).setUpdatedBy(storageService.getUser()));
    }

    @Override
    public void delete(Long id) throws GenericError {
        try {
            tiersPayantRepository.deleteById(id);
        } catch (Exception e) {
            log.debug("delete {}", e);
            throw new GenericError("tierspayant", "Il y'a client associés à ce tiers-payant", "tiersPayantClientsAssocies");
        }

    }
}
