package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.ModelFacture;
import com.kobe.warehouse.domain.enumeration.OrdreTrisFacture;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.TiersPayantStatut;
import com.kobe.warehouse.repository.ClientTiersPayantRepository;
import com.kobe.warehouse.repository.ThirdPartySaleLineRepository;
import com.kobe.warehouse.repository.TiersPayantRepository;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.TiersPayantService;
import com.kobe.warehouse.service.dto.Pair;
import com.kobe.warehouse.service.dto.TiersPayantDto;
import com.kobe.warehouse.service.dto.VenteRecordParamDTO;
import com.kobe.warehouse.service.dto.projection.AchatTiersPayant;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.stat.CommonStatService;
import com.kobe.warehouse.service.tiers_payant.TiersPayantAchat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TiersPayantServiceImpl implements TiersPayantService, CommonStatService {

    private final Logger log = LoggerFactory.getLogger(TiersPayantServiceImpl.class);
    private final TiersPayantRepository tiersPayantRepository;
    private final StorageService storageService;
    private final ClientTiersPayantRepository clientTiersPayantRepository;
    private final ThirdPartySaleLineRepository thirdPartySaleLineRepository;

    public TiersPayantServiceImpl(
        TiersPayantRepository tiersPayantRepository,
        StorageService storageService,
        ClientTiersPayantRepository clientTiersPayantRepository,
        ThirdPartySaleLineRepository thirdPartySaleLineRepository
    ) {
        this.tiersPayantRepository = tiersPayantRepository;
        this.storageService = storageService;
        this.clientTiersPayantRepository = clientTiersPayantRepository;
        this.thirdPartySaleLineRepository = thirdPartySaleLineRepository;
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
        tiersPayant.setUser(storageService.getUser());
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
        tiersPayant.setUser(storageService.getUser());
        return fromEntity(tiersPayantRepository.save(tiersPayant));
    }

    @Override
    public void desable(Integer id) {
        var tp = tiersPayantRepository.getReferenceById(id);
        tp.setStatut(TiersPayantStatut.DISABLED).setUser(storageService.getUser());
        tp.setUpdated(LocalDateTime.now());
        tiersPayantRepository.save(tp);
    }

    @Override
    public void delete(Integer id) throws GenericError {
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

    @Override
    public Page<AchatTiersPayant> fetchAchatTiersPayant(LocalDate fromDate, LocalDate toDate, String search, Pageable pageable) {
        return this.thirdPartySaleLineRepository.fetchAchatsTiersPayant(
                buildThirdPartySaleLineSpecification(fromDate, toDate, search),
                pageable
            );
    }

    @Override
    public List<TiersPayantAchat> fetchAchatTiersPayant(VenteRecordParamDTO venteRecordParam) {
        return this.thirdPartySaleLineRepository.fetchAchatTiersPayant(
                buildThirdPartySaleLineSpecification(venteRecordParam),
                Pageable.ofSize(venteRecordParam.getLimit())
            );
    }

    private Specification<ThirdPartySaleLine> buildThirdPartySaleLineSpecification(VenteRecordParamDTO venteRecordParam) {
        org.apache.commons.lang3.tuple.Pair<LocalDate, LocalDate> periode = buildPeriode(venteRecordParam);
        Specification<ThirdPartySaleLine> specification = this.thirdPartySaleLineRepository.canceledCriteria();
        return specification.and(buildThirdPartySaleLineSpecification(periode.getLeft(), periode.getRight(), null));
    }

    private Specification<ThirdPartySaleLine> buildThirdPartySaleLineSpecification(LocalDate fromDate, LocalDate toDate, String search) {
        Specification<ThirdPartySaleLine> specification = this.thirdPartySaleLineRepository.periodeCriteria(fromDate, toDate);
        specification = specification.and(this.thirdPartySaleLineRepository.saleStatutsCriteria(SalesStatut.getStatutForFacturation()));

        if (org.springframework.util.StringUtils.hasText(search)) {
            specification = specification.and(this.thirdPartySaleLineRepository.filterBySearchTerm(search));
        }
        return specification;
    }
}
