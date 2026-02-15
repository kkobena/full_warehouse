package com.kobe.warehouse.service.sale.impl;

import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.SaleId;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.repository.ThirdPartySaleLineRepository;
import com.kobe.warehouse.service.id_generator.AssuranceItemIdGeneratorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ThirdPartySaleLineService {

    private final AssuranceItemIdGeneratorService assuranceItemIdGeneratorService;
    private final ThirdPartySaleLineRepository thirdPartySaleLineRepository;

    public ThirdPartySaleLineService(
        AssuranceItemIdGeneratorService assuranceItemIdGeneratorService,
        ThirdPartySaleLineRepository thirdPartySaleLineRepository
    ) {
        this.assuranceItemIdGeneratorService = assuranceItemIdGeneratorService;
        this.thirdPartySaleLineRepository = thirdPartySaleLineRepository;
    }

    public List<ThirdPartySaleLine> findAllBySaleId(SaleId saleId) {
        return thirdPartySaleLineRepository.findAllBySaleIdAndSaleSaleDate(saleId.getId(), saleId.getSaleDate());
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

    public void saveAll(List<ThirdPartySaleLine> thirdPartySaleLines) {
        thirdPartySaleLineRepository.saveAll(thirdPartySaleLines);
    }

    public Optional<ThirdPartySaleLine> findFirstByClientTiersPayantIdAndSaleId(Integer clientTiersPayantId, SaleId saleId) {
        return thirdPartySaleLineRepository.findFirstByClientTiersPayantIdAndSaleIdAndSaleSaleDate(
            clientTiersPayantId,
            saleId.getId(),
            saleId.getSaleDate()
        );
    }

    @Transactional(readOnly = true)
    public long countThirdPartySaleLineByNumBonAndClientTiersPayantIdAndSaleId(
        String numBon,
        Long saleId,
        Integer clientTiersPayantId,
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

    @Transactional(readOnly = true)
    public long countThirdPartySaleLineByNumBonAndClientTiersPayantId(String numBon, Integer clientTiersPayantId, SalesStatut salesStatut) {
        return thirdPartySaleLineRepository.countThirdPartySaleLineByNumBonAndClientTiersPayantId(
            numBon,
            clientTiersPayantId,
            salesStatut,
            LocalDate.now().minusMonths(3)
        );
    }
}
