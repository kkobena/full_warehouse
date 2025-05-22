package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Tableau;
import com.kobe.warehouse.domain.Tva;
import org.apache.commons.lang3.BooleanUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.nonNull;

public class DeliveryReceiptItemDTO {

    private final Long id;
    private final Integer freeQty;
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
    private Long tvaId;
    private TvaDTO tva;


    public DeliveryReceiptItemDTO(OrderLine orderLine) {
        lots = new ArrayList<>();
        Tva tvaEntity = orderLine.getTva();
        if (nonNull(tvaEntity)) {
            tvaId = tvaEntity.getId();
            tva = new TvaDTO(tvaEntity);
        }

        id = orderLine.getId();
        freeQty = orderLine.getFreeQty();
        quantityReceived = orderLine.getQuantityReceived();
        initStock = orderLine.getInitStock();
        quantityRequested = orderLine.getQuantityRequested();
        quantityReturned = orderLine.getQuantityReturned();
        discountAmount = orderLine.getDiscountAmount();
        netAmount = orderLine.getNetAmount();
        taxAmount = orderLine.getTaxAmount();
        createdDate = orderLine.getCreatedAt();
        orderCostAmount = orderLine.getOrderCostAmount();
        effectifGrossIncome = orderLine.getGrossAmount();
        effectifOrderAmount = orderLine.getOrderAmount();
        FournisseurProduit fournisseurProduit = orderLine.getFournisseurProduit();
        regularUnitPrice = fournisseurProduit.getPrixUni();
        costAmount = fournisseurProduit.getPrixAchat();
        Produit produit = fournisseurProduit.getProduit();
        orderUnitPrice = Optional.ofNullable(produit.getTableau()).map(Tableau::getValue).orElse(0) + orderLine.getOrderUnitPrice();
        fournisseurProduitId = fournisseurProduit.getId();
        produitId = produit.getId();
        fournisseurProduitCip = fournisseurProduit.getCodeCip();
        fournisseurProduitEan = produit.getCodeEan();
        fournisseurProduitLibelle = produit.getLibelle();
        updated = orderLine.getUpdated();
        afterStock = orderLine.getFinalStock();
        quantityReceivedTmp = BooleanUtils.isFalse(updated) ? quantityRequested : quantityReceived;


    }

    public Long getId() {
        return id;
    }

    public Integer getFreeQty() {
        return freeQty;
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

    public Long getTvaId() {
        return tvaId;
    }

    public void setTvaId(Long tvaId) {
        this.tvaId = tvaId;
    }

    public TvaDTO getTva() {
        return tva;
    }

    public void setTva(TvaDTO tva) {
        this.tva = tva;
    }
}
