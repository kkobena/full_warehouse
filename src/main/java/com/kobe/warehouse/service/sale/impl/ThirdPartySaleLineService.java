package com.kobe.warehouse.service.sale.impl;

import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.ThirdPartySaleStatut;
import com.kobe.warehouse.repository.ThirdPartySaleLineRepository;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.id_generator.AssuranceItemIdGeneratorService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ThirdPartySaleLineService {

    private final AssuranceItemIdGeneratorService assuranceItemIdGeneratorService;
    private final ThirdPartySaleLineRepository thirdPartySaleLineRepository;
    private final StorageService storageService;

    public ThirdPartySaleLineService(
        AssuranceItemIdGeneratorService assuranceItemIdGeneratorService,
        ThirdPartySaleLineRepository thirdPartySaleLineRepository,
        StorageService storageService
    ) {
        this.assuranceItemIdGeneratorService = assuranceItemIdGeneratorService;
        this.thirdPartySaleLineRepository = thirdPartySaleLineRepository;
        this.storageService = storageService;
    }

    public List<ThirdPartySaleLine> findAllBySaleId(SaleId saleId) {
        return thirdPartySaleLineRepository.findAllBySaleIdAndSaleSaleDate(saleId.getId(), saleId.getSaleDate());
    }

    public ThirdPartySaleLine clone(ThirdPartySaleLine original, ThirdPartySales copy) {
        ThirdPartySaleLine clone = (ThirdPartySaleLine) original.clone();
        clone.setId(assuranceItemIdGeneratorService.nextId());
        clone.setSaleDate(LocalDate.now());
        clone.setStatut(ThirdPartySaleStatut.DELETE);
        clone.setMontant(clone.getMontant() * (-1));
        copy.setLastUserEdit(storageService.getUser());
        clone.setSale(copy);
        thirdPartySaleLineRepository.save(clone);
        original.setStatut(ThirdPartySaleStatut.DELETE);
        thirdPartySaleLineRepository.save(original);
        return clone;
    }

    public ThirdPartySaleLine save(ThirdPartySaleLine thirdPartySaleLine) {
        return thirdPartySaleLineRepository.save(thirdPartySaleLine);
    }

    public ThirdPartySaleLine createThirdPartySaleLine(String numNon, ClientTiersPayant clientTiersPayant, int partTiersPayant) {
        ThirdPartySaleLine thirdPartySaleLine = new ThirdPartySaleLine();
        thirdPartySaleLine.setId(this.assuranceItemIdGeneratorService.nextId());
        thirdPartySaleLine.setCreated(LocalDateTime.now());
        thirdPartySaleLine.setUpdated(thirdPartySaleLine.getCreated());
        thirdPartySaleLine.setEffectiveUpdateDate(thirdPartySaleLine.getCreated());
        thirdPartySaleLine.setNumBon(numNon);
        thirdPartySaleLine.setClientTiersPayant(clientTiersPayant);
        thirdPartySaleLine.setMontant(partTiersPayant);
        return thirdPartySaleLine;
    }

    public void delete(ThirdPartySaleLine thirdPartySaleLine) {
        thirdPartySaleLineRepository.delete(thirdPartySaleLine);
    }

    public void deleteAll(List<ThirdPartySaleLine> thirdPartySaleLines) {
        thirdPartySaleLineRepository.deleteAll(thirdPartySaleLines);
    }

    public Optional<ThirdPartySaleLine> findFirstByClientTiersPayantIdAndSaleId(Long clientTiersPayantId, SaleId saleId) {
        return thirdPartySaleLineRepository.findFirstByClientTiersPayantIdAndSaleIdAndSaleSaleDate(clientTiersPayantId, saleId.getId(), saleId.getSaleDate());
    }

    public long countThirdPartySaleLineByNumBonAndClientTiersPayantIdAndSaleId(
        String numBon,
        Long saleId,
        Long clientTiersPayantId,
        SalesStatut salesStatut
    ) {
        return thirdPartySaleLineRepository.countThirdPartySaleLineByNumBonAndClientTiersPayantIdAndSaleId(
            numBon,
            saleId,
            clientTiersPayantId,
            salesStatut,
            LocalDate.now().minusMonths(3)
        );
    }

    public long countThirdPartySaleLineByNumBonAndClientTiersPayantId(String numBon, Long clientTiersPayantId, SalesStatut salesStatut) {
        return thirdPartySaleLineRepository.countThirdPartySaleLineByNumBonAndClientTiersPayantId(
            numBon,
            clientTiersPayantId,
            salesStatut,
            LocalDate.now().minusMonths(3)
        );
    }
}
