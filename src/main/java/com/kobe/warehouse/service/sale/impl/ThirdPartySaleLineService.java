package com.kobe.warehouse.service.sale.impl;

import com.kobe.warehouse.config.IdGeneratorService;
import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.ThirdPartySaleStatut;
import com.kobe.warehouse.repository.ThirdPartySaleLineRepository;
import com.kobe.warehouse.service.StorageService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ThirdPartySaleLineService {

    private final IdGeneratorService idGeneratorService;
    private final ThirdPartySaleLineRepository thirdPartySaleLineRepository;
    private final StorageService storageService;

    public ThirdPartySaleLineService(
        IdGeneratorService idGeneratorService,
        ThirdPartySaleLineRepository thirdPartySaleLineRepository,
        StorageService storageService
    ) {
        this.idGeneratorService = idGeneratorService;
        this.thirdPartySaleLineRepository = thirdPartySaleLineRepository;
        this.storageService = storageService;
        this.idGeneratorService.setSequenceName("id_sale_assurance_item_seq");
    }

    public List<ThirdPartySaleLine> findAllBySaleId(Long saleId) {
        return thirdPartySaleLineRepository.findAllBySaleId(saleId);
    }

    public ThirdPartySaleLine clone(ThirdPartySaleLine original, ThirdPartySales copy) {
        ThirdPartySaleLine clone = (ThirdPartySaleLine) original.clone();
        clone.setId(idGeneratorService.nextId());
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
        thirdPartySaleLine.setId(this.idGeneratorService.nextId());
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

    public Optional<ThirdPartySaleLine> findFirstByClientTiersPayantIdAndSaleId(Long clientTiersPayantId, Long saleId) {
        return thirdPartySaleLineRepository.findFirstByClientTiersPayantIdAndSaleId(clientTiersPayantId, saleId);
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
            salesStatut
        );
    }

    public long countThirdPartySaleLineByNumBonAndClientTiersPayantId(String numBon, Long clientTiersPayantId, SalesStatut salesStatut) {
        return thirdPartySaleLineRepository.countThirdPartySaleLineByNumBonAndClientTiersPayantId(numBon, clientTiersPayantId, salesStatut);
    }
}
