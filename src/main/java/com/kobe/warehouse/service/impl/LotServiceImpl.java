package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.DeliveryReceiptItem;
import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.repository.CommandeRepository;
import com.kobe.warehouse.repository.DeliveryReceiptItemRepository;
import com.kobe.warehouse.repository.LotRepository;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.LotDTO;
import com.kobe.warehouse.service.dto.LotJsonValue;
import com.kobe.warehouse.service.stock.LotService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
@Transactional
public class LotServiceImpl implements LotService {
  private final LotRepository lotRepository;
  private final StorageService storageService;
  private final CommandeRepository commandeRepository;
  private final DeliveryReceiptItemRepository deliveryReceiptItemRepository;

  public LotServiceImpl(
      LotRepository lotRepository,
      StorageService storageService,
      CommandeRepository commandeRepository,
      DeliveryReceiptItemRepository deliveryReceiptItemRepository) {
    this.lotRepository = lotRepository;
    this.storageService = storageService;
    this.commandeRepository = commandeRepository;
    this.deliveryReceiptItemRepository = deliveryReceiptItemRepository;
  }

  @Override
  public void addLot(
      LotJsonValue lotJsonValue, DeliveryReceiptItem receiptItem, String receiptRefernce) {
    Lot lot = new Lot();
    lot.setNumLot(lotJsonValue.getNumLot());
    lot.setCreatedDate(LocalDateTime.now());
    lot.setExpiryDate(lotJsonValue.getExpiryDate());
    lot.setManufacturingDate(lotJsonValue.getManufacturingDate());
    lot.setQuantity(lotJsonValue.getQuantityReceived() + lotJsonValue.getUgQuantityReceived());
    lot.setUgQuantityReceived(lotJsonValue.getUgQuantityReceived());
    lot.setQuantityReceived(lotJsonValue.getQuantityReceived());
    lot.setReceiptItem(receiptItem);
    lot.setReceiptRefernce(receiptRefernce);
    lotRepository.save(lot);
  }

  @Override
  public LotJsonValue addLot(LotJsonValue lot) {
    Commande commande = commandeRepository.getReferenceById(lot.getCommandeId());
    removeLotFromCommande(commande, lot);
    commande.getLots().add(lot);
    commande.setUpdatedAt(Instant.now());
    commande.setLastUserEdit(storageService.getUser());
    commandeRepository.saveAndFlush(commande);
    return lot;
  }

  @Override
  public LotDTO addLot(LotDTO lot) {
    DeliveryReceiptItem deliveryReceiptItem =
        this.deliveryReceiptItemRepository.getReferenceById(lot.getReceiptItemId());
    Lot entity = lot.toEntity();
    entity.setReceiptItem(deliveryReceiptItem);
    entity.setReceiptRefernce(deliveryReceiptItem.getDeliveryReceipt().getReceiptRefernce());
    return new LotDTO(this.lotRepository.saveAndFlush(entity));
  }

  @Override
  public LotDTO editLot(LotDTO lot) {
    Lot entity = this.lotRepository.getReferenceById(lot.getId());
    entity.setQuantityReceived(lot.getQuantityReceived());
    entity.setUgQuantityReceived(Optional.ofNullable(lot.getUgQuantityReceived()).orElse(0));
    entity.setExpiryDate(lot.getExpiryDate());
    entity.setManufacturingDate(lot.getManufacturingDate());
    entity.setNumLot(lot.getNumLot());
    entity.setQuantity(computeQuantity(lot));
    return new LotDTO(this.lotRepository.saveAndFlush(entity));
  }

  @Override
  public void remove(LotJsonValue lot) {
    Commande commande = commandeRepository.getReferenceById(lot.getCommandeId());
    removeLotFromCommande(commande, lot);
    commandeRepository.saveAndFlush(commande);
  }

  @Override
  public void remove(Long lotId) {
    this.lotRepository.deleteById(lotId);
  }



    private void removeLotFromCommande(Commande commande, LotJsonValue lot) {
    if (!CollectionUtils.isEmpty(commande.getLots())) {
      commande
          .getLots()
          .removeIf(
              e ->
                  e.getNumLot().equalsIgnoreCase(lot.getNumLot())
                      && e.getReceiptItem().equals(lot.getReceiptItem()));
    }
  }

  private int computeQuantity(LotDTO lot) {
    return lot.getQuantityReceived() + Optional.ofNullable(lot.getUgQuantityReceived()).orElse(0);
  }
}
