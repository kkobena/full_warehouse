package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.domain.*;
import com.kobe.warehouse.repository.LotRepository;
import com.kobe.warehouse.service.OrderLineService;
import com.kobe.warehouse.service.dto.LotDTO;
import com.kobe.warehouse.service.dto.LotJsonValue;
import com.kobe.warehouse.service.stock.LotService;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
@Transactional
public class LotServiceImpl implements LotService {

    private final LotRepository lotRepository;
    private final OrderLineService orderLineService;

    public LotServiceImpl(
        LotRepository lotRepository,
        OrderLineService orderLineService
    ) {
        this.lotRepository = lotRepository;
        this.orderLineService = orderLineService;
    }

    @Override
    public void addLot(LotJsonValue lotJsonValue, OrderLine orderLine, String receiptRefernce) {
        Lot lot = new Lot();
        lot.setNumLot(lotJsonValue.getNumLot());
        lot.setCreatedDate(LocalDateTime.now());
        lot.setExpiryDate(lotJsonValue.getExpiryDate());
        lot.setManufacturingDate(lotJsonValue.getManufacturingDate());
        lot.setQuantity(lotJsonValue.getQuantity() + lotJsonValue.getFreeQuantity());
        lot.setFreeQty(lotJsonValue.getFreeQuantity());
        lotRepository.save(lot);
    }

    @Override
    public LotJsonValue addLot(LotJsonValue lot) {
        OrderLine orderLine = orderLineService.findOneById(lot.getLinkedId()).orElseThrow();
       // orderLine.getLots().add(lot);
        orderLine.setUpdatedAt(LocalDateTime.now());
        orderLineService.save(orderLine);
        return lot;
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
    public void addLot(Set<LotJsonValue> lots, OrderLine orderLine) {
        if (!CollectionUtils.isEmpty(lots)) {
            lots.forEach(l -> {
                Lot lot = new Lot();
                lot.setNumLot(l.getNumLot());
                lot.setCreatedDate(LocalDateTime.now());
                lot.setExpiryDate(l.getExpiryDate());
                lot.setManufacturingDate(l.getManufacturingDate());
                lot.setQuantity(l.getQuantity() + l.getFreeQuantity());
                lot.setFreeQty(l.getFreeQuantity());
                lotRepository.save(lot);
            });
        }
    }

    private int computeQuantity(LotDTO lot) {
        return lot.getQuantityReceived() + Optional.ofNullable(lot.getFreeQty()).orElse(0);
    }
}
