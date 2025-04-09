package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.domain.*;
import com.kobe.warehouse.repository.DeliveryReceiptItemRepository;
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
    private final DeliveryReceiptItemRepository deliveryReceiptItemRepository;
    private final OrderLineService orderLineService;

    public LotServiceImpl(
        LotRepository lotRepository,
        DeliveryReceiptItemRepository deliveryReceiptItemRepository,
        OrderLineService orderLineService
    ) {
        this.lotRepository = lotRepository;
        this.deliveryReceiptItemRepository = deliveryReceiptItemRepository;
        this.orderLineService = orderLineService;
    }

    @Override
    public void addLot(LotJsonValue lotJsonValue, DeliveryReceiptItem receiptItem, String receiptRefernce) {
        Lot lot = new Lot();
        lot.setNumLot(lotJsonValue.getNumLot());
        lot.setCreatedDate(LocalDateTime.now());
        lot.setExpiryDate(lotJsonValue.getExpiryDate());
        lot.setManufacturingDate(lotJsonValue.getManufacturingDate());
        lot.setQuantity(lotJsonValue.getQuantity() + lotJsonValue.getFreeQuantity());
        lot.setUgQuantityReceived(lotJsonValue.getFreeQuantity());
        lot.setReceiptItem(receiptItem);
        lot.setReceiptRefernce(receiptRefernce);
        lotRepository.save(lot);
    }

    @Override
    public LotJsonValue addLot(LotJsonValue lot) {
        OrderLine orderLine = orderLineService.findOneById(lot.getLinkedId()).orElseThrow();
        orderLine.getLots().add(lot);
        orderLine.setUpdatedAt(LocalDateTime.now());
        orderLineService.save(orderLine);
        return lot;
    }

    @Override
    public LotDTO addLot(LotDTO lot) {
        DeliveryReceiptItem deliveryReceiptItem = this.deliveryReceiptItemRepository.getReferenceById(lot.getReceiptItemId());
        Lot entity = lot.toEntity();
        entity.setReceiptItem(deliveryReceiptItem);
        entity.setReceiptRefernce(deliveryReceiptItem.getDeliveryReceipt().getReceiptRefernce());
        return new LotDTO(this.lotRepository.saveAndFlush(entity));
    }

    @Override
    public LotDTO editLot(LotDTO lot) {
        Lot entity = this.lotRepository.getReferenceById(lot.getId());
        entity.setUgQuantityReceived(Optional.ofNullable(lot.getUgQuantityReceived()).orElse(0));
        entity.setExpiryDate(lot.getExpiryDate());
        entity.setManufacturingDate(lot.getManufacturingDate());
        entity.setNumLot(lot.getNumLot());
        entity.setQuantity(computeQuantity(lot));
        return new LotDTO(this.lotRepository.saveAndFlush(entity));
    }

    @Override
    public void remove(LotJsonValue lot) {
        orderLineService
            .findOneById(lot.getLinkedId())
            .ifPresent(orderLine -> {
                orderLine.getLots().removeIf(e -> e.getNumLot().equalsIgnoreCase(lot.getNumLot()));
                orderLine.setUpdatedAt(LocalDateTime.now());
                orderLineService.save(orderLine);
            });
    }

    @Override
    public void remove(Long lotId) {
        this.lotRepository.deleteById(lotId);
    }

    @Override
    public void addLot(Set<LotJsonValue> lots, DeliveryReceiptItem receiptItem) {
        if (!CollectionUtils.isEmpty(lots)) {
            lots.forEach(l -> {
                Lot lot = new Lot();
                lot.setNumLot(l.getNumLot());
                lot.setCreatedDate(LocalDateTime.now());
                lot.setExpiryDate(l.getExpiryDate());
                lot.setManufacturingDate(l.getManufacturingDate());
                lot.setQuantity(l.getQuantity() + l.getFreeQuantity());
                lot.setUgQuantityReceived(l.getFreeQuantity());
                lot.setReceiptItem(receiptItem);
                lotRepository.save(lot);
            });
        }
    }

    private int computeQuantity(LotDTO lot) {
        return lot.getQuantityReceived() + Optional.ofNullable(lot.getUgQuantityReceived()).orElse(0);
    }
}
