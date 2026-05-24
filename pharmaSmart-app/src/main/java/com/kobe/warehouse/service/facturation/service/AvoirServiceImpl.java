package com.kobe.warehouse.service.facturation.service;

import com.kobe.warehouse.domain.AvoirLine;
import com.kobe.warehouse.domain.AvoirTiersPayant;
import com.kobe.warehouse.domain.FactureItemId;
import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.GroupeTiersPayant;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.AvoirStatut;
import com.kobe.warehouse.repository.AvoirTiersPayantRepository;
import com.kobe.warehouse.repository.FactureTiersPayantRepository;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.facturation.dto.AvoirCommand;
import com.kobe.warehouse.service.facturation.dto.AvoirDto;
import com.kobe.warehouse.service.facturation.dto.AvoirLineDto;
import com.kobe.warehouse.service.facturation.dto.AvoirSearchParams;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional
public class AvoirServiceImpl implements AvoirService {

    private final AvoirTiersPayantRepository avoirRepository;
    private final FactureTiersPayantRepository factureTiersPayantRepository;
    private final StorageService storageService;
    private final AvoirPdfService avoirPdfService;
    private final AvoirExcelService avoirExcelService;

    public AvoirServiceImpl(
        AvoirTiersPayantRepository avoirRepository,
        FactureTiersPayantRepository factureTiersPayantRepository,
        StorageService storageService,
        AvoirPdfService avoirPdfService,
        AvoirExcelService avoirExcelService
    ) {
        this.avoirRepository = avoirRepository;
        this.factureTiersPayantRepository = factureTiersPayantRepository;
        this.storageService = storageService;
        this.avoirPdfService = avoirPdfService;
        this.avoirExcelService = avoirExcelService;
    }

    @Override
    public AvoirDto creerAvoir(AvoirCommand command) {
        FactureTiersPayant factureTiersPayant = factureTiersPayantRepository
            .getReferenceById(new FactureItemId(command.factureId(), command.factureDate()));

        int nextNum = avoirRepository.findMaxNumeroAvoir() + 1;
        String numAvoir = "AV-" + Year.now().getValue() + "_" + String.format("%04d", nextNum);

        AvoirTiersPayant avoir = new AvoirTiersPayant();
        avoir.setNumAvoir(numAvoir);
        avoir.setFactureTiersPayant(factureTiersPayant);
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

        return toDto(avoirRepository.save(avoir));
    }

    @Override
    public AvoirDto emettre(Long avoirId) {
        AvoirTiersPayant avoir = avoirRepository.getReferenceById(avoirId);
        if (avoir.getStatut() != AvoirStatut.DRAFT) {
            throw new IllegalStateException(
                "L'avoir doit être en statut DRAFT pour être émis. Statut actuel: " + avoir.getStatut());
        }
        avoir.setStatut(AvoirStatut.EMIS);
        return toDto(avoirRepository.save(avoir));
    }

    @Override
    public void imputer(Long avoirId, Long factureId, LocalDate factureDate) {
        AvoirTiersPayant avoir = avoirRepository.getReferenceById(avoirId);
        if (avoir.getStatut() != AvoirStatut.EMIS) {
            throw new IllegalStateException(
                "L'avoir doit être en statut EMIS pour être imputé. Statut actuel: " + avoir.getStatut());
        }
        avoir.setStatut(AvoirStatut.IMPUTE);
        avoirRepository.save(avoir);
    }

    @Override
    public void annuler(Long avoirId, String motif) {
        AvoirTiersPayant avoir = avoirRepository.getReferenceById(avoirId);
        if (avoir.getStatut() == AvoirStatut.IMPUTE) {
            throw new IllegalStateException("Impossible d'annuler un avoir déjà imputé.");
        }
        if (avoir.getStatut() == AvoirStatut.ANNULE) {
            throw new IllegalStateException("L'avoir est déjà annulé.");
        }
        avoir.setStatut(AvoirStatut.ANNULE);
        if (StringUtils.hasText(motif)) {
            avoir.setMotif(motif);
        }
        avoirRepository.save(avoir);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AvoirDto> findAll(AvoirSearchParams params, Pageable pageable) {
        List<AvoirStatut> statuts = !CollectionUtils.isEmpty(params.statuts())
            ? params.statuts()
            : List.of(AvoirStatut.values());

        LocalDate start = params.startDate() != null ? params.startDate() : LocalDate.now().minusMonths(6);
        LocalDate end = params.endDate() != null ? params.endDate() : LocalDate.now();
        String numAvoir = StringUtils.hasText(params.numAvoir()) ? "%" + params.numAvoir().toLowerCase() + "%" : null;

        Page<AvoirTiersPayant> page;
        if (params.tiersPayantId() != null) {
            page = avoirRepository.searchByTiersPayant(params.tiersPayantId(), start, end, statuts, numAvoir, pageable);
        } else {
            page = avoirRepository.searchAll(start, end, statuts, numAvoir, pageable);
        }

        return page.map(this::toDto);
    }

    @Override
    public byte[] exportPdf(Long avoirId) {
        return avoirPdfService.generatePdf(avoirId);
    }

    @Override
    public byte[] exportExcel(AvoirSearchParams params) {
        try {
            return avoirExcelService.exportToExcel(params);
        } catch (Exception e) {
            throw new RuntimeException("Erreur export Excel avoirs", e);
        }
    }

    @Override
    public byte[] exportListPdf(AvoirSearchParams params) {
        return avoirPdfService.generateListPdf(params);
    }

    private AvoirDto toDto(AvoirTiersPayant avoir) {
        FactureTiersPayant factureTiersPayant = avoir.getFactureTiersPayant();
        TiersPayant tiersPayant = factureTiersPayant.getTiersPayant();
        GroupeTiersPayant groupeTiersPayant = Objects.isNull(tiersPayant)
            ? factureTiersPayant.getGroupeTiersPayant()
            : null;
        List<AvoirLineDto> lineDtos = avoir.getLignes() != null
            ? avoir.getLignes().stream()
                .map(l -> new AvoirLineDto(l.getId(), l.getSaleLineId(), l.getSaleLineDate(), l.getMontantAvoir(), l.getMotifRejet()))
                .collect(Collectors.toList())
            : Collections.emptyList();
        FactureItemId factureItemId = factureTiersPayant.getId();
        return new AvoirDto(
            avoir.getId(),
            avoir.getNumAvoir(),
            factureItemId.getId(),
            factureItemId.getInvoiceDate(),
            factureTiersPayant.getNumFacture(),
            avoir.getMontantAvoir(),
            avoir.getMontantTva(),
            avoir.getMontantHt(),
            avoir.getMotif(),
            avoir.getAvoirDate(),
            avoir.getStatut(),
            tiersPayant != null ? tiersPayant.getId() : groupeTiersPayant.getId(),
            tiersPayant != null ? tiersPayant.getFullName() : groupeTiersPayant.getName(),
            lineDtos
        );
    }
}
