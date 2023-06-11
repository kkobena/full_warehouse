package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.DeliveryReceiptItem;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Tableau;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.util.CollectionUtils;

@Getter
public class DeliveryReceiptItemDTO {

  private final Long id;
  private final Integer ugQuantity;
  private final Integer quantityReceived;
  private final Integer initStock;
  private final Integer quantityRequested;
  private final Integer quantityReturned;
  private final Integer discountAmount;
  private final Integer netAmount;
  private final Integer taxAmount;
  private final LocalDateTime createdDate;
  private final Integer orderUnitPrice;
  private final Integer regularUnitPrice;
  private final Integer orderCostAmount;
  private final Integer effectifGrossIncome;
  private final Integer effectifOrderAmount;
  private final long fournisseurProduitId;
  private final long produitId;
  private final String fournisseurProduitLibelle;
  private final String fournisseurProduitCip;
  private final String fournisseurProduitEan;
  private final List<LotDTO> lots;
  private final Boolean updated;
  private final Integer quantityReceivedTmp;
  private final Integer costAmount;
  private final Integer  afterStock;

    public DeliveryReceiptItemDTO(DeliveryReceiptItem receiptItem) {
    id = receiptItem.getId();
    ugQuantity = receiptItem.getUgQuantity();
    quantityReceived = receiptItem.getQuantityReceived();
    initStock = receiptItem.getInitStock();
    quantityRequested = receiptItem.getQuantityRequested();
    quantityReturned = receiptItem.getQuantityReturned();
    discountAmount = receiptItem.getDiscountAmount();
    netAmount = receiptItem.getNetAmount();
    taxAmount = receiptItem.getTaxAmount();
    createdDate = receiptItem.getCreatedDate();
      orderCostAmount=receiptItem.getOrderCostAmount();
    effectifGrossIncome = receiptItem.getEffectifGrossIncome();
    effectifOrderAmount = receiptItem.getEffectifOrderAmount();
    FournisseurProduit fournisseurProduit = receiptItem.getFournisseurProduit();
    regularUnitPrice = fournisseurProduit.getPrixUni();
    costAmount = fournisseurProduit.getPrixAchat();
    Produit produit = fournisseurProduit.getProduit();
    orderUnitPrice =
        Optional.ofNullable(produit.getTableau()).map(Tableau::getValue).orElse(0)
            + receiptItem.getOrderUnitPrice();
    fournisseurProduitId = fournisseurProduit.getId();
    produitId = produit.getId();
    fournisseurProduitCip = fournisseurProduit.getCodeCip();
    fournisseurProduitEan = produit.getCodeEan();
    fournisseurProduitLibelle = produit.getLibelle();
    updated = receiptItem.getUpdated();
    afterStock=receiptItem.getAfterStock();
    quantityReceivedTmp = BooleanUtils.isFalse(updated) ? quantityRequested : quantityReceived;
    List<Lot> lots1 = receiptItem.getLots();
    lots =
        !CollectionUtils.isEmpty(lots1)
            ? lots1.stream().map(LotDTO::new).toList()
            : new ArrayList<>();
  }
}
