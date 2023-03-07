package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.DeliveryReceiptItem;
import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.repository.CommandeRepository;
import com.kobe.warehouse.repository.LotRepository;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.LotJsonValue;
import com.kobe.warehouse.service.stock.LotService;
import java.time.Instant;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
@Transactional
public class LotServiceImpl implements LotService {
  private final LotRepository lotRepository;
  private final StorageService storageService;
  private final CommandeRepository commandeRepository;

  public LotServiceImpl(
      LotRepository lotRepository,
      StorageService storageService,
      CommandeRepository commandeRepository) {
    this.lotRepository = lotRepository;
    this.storageService = storageService;
    this.commandeRepository = commandeRepository;
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
  public void remove(LotJsonValue lot) {
    Commande commande = commandeRepository.getReferenceById(lot.getCommandeId());
    removeLotFromCommande(commande, lot);
    commandeRepository.saveAndFlush(commande);
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
}
