package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.DeliveryReceiptItem;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Tableau;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.util.CollectionUtils;

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
    private final Integer afterStock;
    private final Integer tva;
    private final LocalDate datePeremption;
    private final String datePeremptionTmp;

    public DeliveryReceiptItemDTO(DeliveryReceiptItem receiptItem) {
        tva = receiptItem.getTva();
        datePeremption = receiptItem.getDatePeremption();
        datePeremptionTmp = Optional.ofNullable(receiptItem.getDatePeremption()).map(LocalDate::toString).orElse(null);
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
        orderCostAmount = receiptItem.getOrderCostAmount();
        effectifGrossIncome = receiptItem.getEffectifGrossIncome();
        effectifOrderAmount = receiptItem.getEffectifOrderAmount();
        FournisseurProduit fournisseurProduit = receiptItem.getFournisseurProduit();
        regularUnitPrice = fournisseurProduit.getPrixUni();
        costAmount = fournisseurProduit.getPrixAchat();
        Produit produit = fournisseurProduit.getProduit();
        orderUnitPrice = Optional.ofNullable(produit.getTableau()).map(Tableau::getValue).orElse(0) + receiptItem.getOrderUnitPrice();
        fournisseurProduitId = fournisseurProduit.getId();
        produitId = produit.getId();
        fournisseurProduitCip = fournisseurProduit.getCodeCip();
        fournisseurProduitEan = produit.getCodeEan();
        fournisseurProduitLibelle = produit.getLibelle();
        updated = receiptItem.getUpdated();
        afterStock = receiptItem.getAfterStock();
        quantityReceivedTmp = BooleanUtils.isFalse(updated) ? quantityRequested : quantityReceived;
        List<Lot> lots1 = receiptItem.getLots();
        lots = !CollectionUtils.isEmpty(lots1) ? lots1.stream().map(LotDTO::new).toList() : new ArrayList<>();
    }

    public Long getId() {
        return id;
    }

    public Integer getUgQuantity() {
        return ugQuantity;
    }

    public Integer getQuantityReceived() {
        return quantityReceived;
    }

    public Integer getInitStock() {
        return initStock;
    }

    public Integer getQuantityRequested() {
        return quantityRequested;
    }

    public Integer getQuantityReturned() {
        return quantityReturned;
    }

    public Integer getDiscountAmount() {
        return discountAmount;
    }

    public Integer getNetAmount() {
        return netAmount;
    }

    public Integer getTaxAmount() {
        return taxAmount;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public Integer getOrderUnitPrice() {
        return orderUnitPrice;
    }

    public Integer getRegularUnitPrice() {
        return regularUnitPrice;
    }

    public Integer getOrderCostAmount() {
        return orderCostAmount;
    }

    public Integer getEffectifGrossIncome() {
        return effectifGrossIncome;
    }

    public Integer getEffectifOrderAmount() {
        return effectifOrderAmount;
    }

    public long getFournisseurProduitId() {
        return fournisseurProduitId;
    }

    public long getProduitId() {
        return produitId;
    }

    public String getFournisseurProduitLibelle() {
        return fournisseurProduitLibelle;
    }

    public String getFournisseurProduitCip() {
        return fournisseurProduitCip;
    }

    public String getFournisseurProduitEan() {
        return fournisseurProduitEan;
    }

    public List<LotDTO> getLots() {
        return lots;
    }

    public Boolean getUpdated() {
        return updated;
    }

    public Integer getQuantityReceivedTmp() {
        return quantityReceivedTmp;
    }

    public Integer getCostAmount() {
        return costAmount;
    }

    public Integer getAfterStock() {
        return afterStock;
    }

    public LocalDate getDatePeremption() {
        return datePeremption;
    }

    public Integer getTva() {
        return tva;
    }

    public String getDatePeremptionTmp() {
        return datePeremptionTmp;
    }
}
