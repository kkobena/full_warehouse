package com.kobe.warehouse.service.facturation.service;

import com.kobe.warehouse.domain.AvoirLine;
import com.kobe.warehouse.domain.AvoirTiersPayant;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.AvoirStatut;
import com.kobe.warehouse.repository.AvoirTiersPayantRepository;
import com.kobe.warehouse.repository.TiersPayantRepository;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.facturation.dto.AvoirCommand;
import com.kobe.warehouse.service.facturation.dto.AvoirDto;
import com.kobe.warehouse.service.facturation.dto.AvoirLineDto;
import com.kobe.warehouse.service.facturation.dto.AvoirSearchParams;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
@Transactional
public class AvoirServiceImpl implements AvoirService {

    private final AvoirTiersPayantRepository avoirRepository;
    private final TiersPayantRepository tiersPayantRepository;
    private final StorageService storageService;

    public AvoirServiceImpl(
        AvoirTiersPayantRepository avoirRepository,
        TiersPayantRepository tiersPayantRepository,
        StorageService storageService
    ) {
        this.avoirRepository = avoirRepository;
        this.tiersPayantRepository = tiersPayantRepository;
        this.storageService = storageService;
    }

    @Override
    public AvoirDto creerAvoir(AvoirCommand command) {
        TiersPayant tiersPayant = tiersPayantRepository.getReferenceById(command.tiersPayantId());

        int nextNum = avoirRepository.findMaxNumeroAvoir() + 1;
        String numAvoir = "AV-" + Year.now().getValue() + "_" + String.format("%04d", nextNum);

        AvoirTiersPayant avoir = new AvoirTiersPayant();
        avoir.setNumAvoir(numAvoir);
        avoir.setFactureOrigineId(command.factureId());
        avoir.setFactureOrigineDate(command.factureDate());
        avoir.setTiersPayant(tiersPayant);
        avoir.setMontantAvoir(command.montantAvoir() != null ? command.montantAvoir() : BigDecimal.ZERO);
        avoir.setMontantTva(command.montantTva() != null ? command.montantTva() : BigDecimal.ZERO);
        avoir.setMontantHt(command.montantHt() != null ? command.montantHt() : BigDecimal.ZERO);
        avoir.setMotif(command.motif());
        avoir.setAvoirDate(LocalDate.now());
        avoir.setStatut(AvoirStatut.DRAFT);
        avoir.setUser(storageService.getUser());

        if (!CollectionUtils.isEmpty(command.lignes())) {
            List<AvoirLine> lines = command.lignes().stream()
                .map(dto -> {
                    AvoirLine line = new AvoirLine();
                    line.setAvoir(avoir);
                    line.setSaleLineId(dto.saleLineId());
                    line.setSaleLineDate(dto.saleLineDate());
                    line.setMontantAvoir(dto.montantAvoir() != null ? dto.montantAvoir() : BigDecimal.ZERO);
                    line.setMotifRejet(dto.motifRejet());
                    return line;
                })
                .collect(Collectors.toList());
            avoir.setLignes(lines);
        }

        AvoirTiersPayant saved = avoirRepository.save(avoir);
        return toDto(saved);
    }

    @Override
    public AvoirDto emettre(Long avoirId) {
        AvoirTiersPayant avoir = avoirRepository.getReferenceById(avoirId);
        if (avoir.getStatut() != AvoirStatut.DRAFT) {
            throw new IllegalStateException("L'avoir doit être en statut DRAFT pour être émis. Statut actuel: " + avoir.getStatut());
        }
        avoir.setStatut(AvoirStatut.EMIS);
        return toDto(avoirRepository.save(avoir));
    }

    @Override
    public void imputer(Long avoirId, Long factureId, LocalDate factureDate) {
        AvoirTiersPayant avoir = avoirRepository.getReferenceById(avoirId);
        if (avoir.getStatut() != AvoirStatut.EMIS) {
            throw new IllegalStateException("L'avoir doit être en statut EMIS pour être imputé. Statut actuel: " + avoir.getStatut());
        }
        avoir.setStatut(AvoirStatut.IMPUTE);
        avoirRepository.save(avoir);
    }

    @Override
    public void annuler(Long avoirId) {
        AvoirTiersPayant avoir = avoirRepository.getReferenceById(avoirId);
        if (avoir.getStatut() == AvoirStatut.IMPUTE) {
            throw new IllegalStateException("Impossible d'annuler un avoir déjà imputé.");
        }
        if (avoir.getStatut() == AvoirStatut.ANNULE) {
            throw new IllegalStateException("L'avoir est déjà annulé.");
        }
        avoir.setStatut(AvoirStatut.ANNULE);
        avoirRepository.save(avoir);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AvoirDto> findAll(AvoirSearchParams params, Pageable pageable) {
        List<AvoirStatut> statuts = !CollectionUtils.isEmpty(params.statuts())
            ? params.statuts()
            : List.of(AvoirStatut.values());

        LocalDate start = params.startDate() != null ? params.startDate() : LocalDate.of(2000, 1, 1);
        LocalDate end = params.endDate() != null ? params.endDate() : LocalDate.now().plusYears(10);

        Page<AvoirTiersPayant> page;
        if (params.tiersPayantId() != null) {
            page = avoirRepository.findByTiersPayantIdAndAvoirDateBetweenAndStatutIn(
                params.tiersPayantId(), start, end, statuts, pageable
            );
        } else {
            page = avoirRepository.findByAvoirDateBetweenAndStatutIn(start, end, statuts, pageable);
        }

        List<AvoirDto> content = page.getContent().stream().map(this::toDto).collect(Collectors.toList());
        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    @Override
    public byte[] exportPdf(Long avoirId) {
        return new byte[0];
    }

    private AvoirDto toDto(AvoirTiersPayant avoir) {
        TiersPayant tp = avoir.getTiersPayant();
        List<AvoirLineDto> lineDtos = avoir.getLignes() != null
            ? avoir.getLignes().stream()
                .map(l -> new AvoirLineDto(l.getId(), l.getSaleLineId(), l.getSaleLineDate(), l.getMontantAvoir(), l.getMotifRejet()))
                .collect(Collectors.toList())
            : Collections.emptyList();

        return new AvoirDto(
            avoir.getId(),
            avoir.getNumAvoir(),
            avoir.getFactureOrigineId(),
            avoir.getFactureOrigineDate(),
            avoir.getMontantAvoir(),
            avoir.getMontantTva(),
            avoir.getMontantHt(),
            avoir.getMotif(),
            avoir.getAvoirDate(),
            avoir.getStatut(),
            tp != null ? tp.getId() : null,
            tp != null ? tp.getFullName() : null,
            lineDtos
        );
    }
}
