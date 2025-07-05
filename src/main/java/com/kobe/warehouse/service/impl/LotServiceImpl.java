package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.LotSold;
import com.kobe.warehouse.repository.LotRepository;
import com.kobe.warehouse.service.AppConfigurationService;
import com.kobe.warehouse.service.dto.LotDTO;
import com.kobe.warehouse.service.stock.LotService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
@Transactional
public class LotServiceImpl implements LotService {

    private final LotRepository lotRepository;
    private final AppConfigurationService appConfigurationService;

    public LotServiceImpl(LotRepository lotRepository, AppConfigurationService appConfigurationService) {
        this.lotRepository = lotRepository;

        this.appConfigurationService = appConfigurationService;
    }

    @Override
    public LotDTO addLot(LotDTO lot) {
        return new LotDTO(this.lotRepository.saveAndFlush(lot.toEntity()));
    }

    @Override
    public LotDTO editLot(LotDTO lot) {
        Lot entity = this.lotRepository.getReferenceById(lot.getId());
        entity.setFreeQty(Optional.ofNullable(lot.getFreeQty()).orElse(0));
        entity.setExpiryDate(lot.getExpiryDate());
        entity.setManufacturingDate(lot.getManufacturingDate());
        entity.setNumLot(lot.getNumLot());
        entity.setQuantity(computeQuantity(lot));
        return new LotDTO(this.lotRepository.saveAndFlush(entity));
    }

    @Override
    public void remove(LotDTO lot) {
        this.lotRepository.deleteById(lot.getId());
    }

    @Override
    public void remove(Long lotId) {
        this.lotRepository.deleteById(lotId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lot> findByProduitId(Long produitId) {
        return this.lotRepository.findByProduitId(
                produitId,
                LocalDate.now().minusDays(this.appConfigurationService.getNombreJourPeremption())
            );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lot> findProduitLots(Long produitId) {
        return this.lotRepository.findByProduitId(produitId);
    }

    @Override
    public void updateLots(List<LotSold> lots) {
        if (!CollectionUtils.isEmpty(lots)) {
            lots.forEach(lot -> {
                Lot entity = this.lotRepository.getReferenceById(lot.id());
                entity.setQuantity(entity.getQuantity() - lot.quantity());
                this.lotRepository.save(entity);
            });
        }
    }

    private int computeQuantity(LotDTO lot) {
        return lot.getQuantityReceived() + Optional.ofNullable(lot.getFreeQty()).orElse(0);
    }
}
