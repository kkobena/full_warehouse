package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public abstract class CommandeWrapperDTO {
    private Long id;
    private String orderRefernce;
    private String receiptRefernce;
    private LocalDate receiptDate;
    private Integer discountAmount;
    private Integer orderAmount;
    private Integer grossAmount;
    private Integer netAmount;
    private Integer taxAmount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private OrderStatut orderStatus;
    @Getter
    private Long fournisseurId;
    private int totalProduits;
    private Integer receiptAmount;
    private String sequenceBon;
    @Getter
    private FournisseurDTO fournisseur;

    public CommandeWrapperDTO(Commande commande) {
        id = commande.getId();
        orderRefernce = commande.getOrderRefernce();
        receiptRefernce = commande.getReceiptRefernce();
        receiptDate = commande.getReceiptDate();
        discountAmount = commande.getDiscountAmount();
        orderAmount = commande.getOrderAmount();
        grossAmount = commande.getGrossAmount();
        netAmount = commande.getNetAmount();
        taxAmount = commande.getTaxAmount();
        createdAt = commande.getCreatedAt();
        updatedAt = commande.getUpdatedAt();
        orderStatus = commande.getOrderStatus();
        receiptAmount = commande.getReceiptAmount();
        sequenceBon = commande.getSequenceBon();

    }

    public CommandeWrapperDTO() {
    }

    public CommandeWrapperDTO setId(Long id) {
        this.id = id;
        return this;
    }

    public CommandeWrapperDTO setOrderRefernce(String orderRefernce) {
        this.orderRefernce = orderRefernce;
        return this;
    }

    public CommandeWrapperDTO setReceiptRefernce(String receiptRefernce) {
        this.receiptRefernce = receiptRefernce;
        return this;
    }

    public CommandeWrapperDTO setReceiptDate(LocalDate receiptDate) {
        this.receiptDate = receiptDate;
        return this;
    }

    public CommandeWrapperDTO setDiscountAmount(Integer discountAmount) {
        this.discountAmount = discountAmount;
        return this;
    }

    public CommandeWrapperDTO setOrderAmount(Integer orderAmount) {
        this.orderAmount = orderAmount;
        return this;
    }

    public CommandeWrapperDTO setGrossAmount(Integer grossAmount) {
        this.grossAmount = grossAmount;
        return this;
    }

    public CommandeWrapperDTO setNetAmount(Integer netAmount) {
        this.netAmount = netAmount;
        return this;
    }

    public CommandeWrapperDTO setTaxAmount(Integer taxAmount) {
        this.taxAmount = taxAmount;
        return this;
    }

    public CommandeWrapperDTO setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public CommandeWrapperDTO setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public CommandeWrapperDTO setOrderStatus(OrderStatut orderStatus) {
        this.orderStatus = orderStatus;
        return this;
    }

    public CommandeWrapperDTO setTotalProduits(int totalProduits) {
        this.totalProduits = totalProduits;
        return this;
    }

    public CommandeWrapperDTO setReceiptAmount(Integer receiptAmount) {
        this.receiptAmount = receiptAmount;
        return this;
    }

    public CommandeWrapperDTO setSequenceBon(String sequenceBon) {
        this.sequenceBon = sequenceBon;
        return this;
    }

    public CommandeWrapperDTO setFournisseurId(Long fournisseurId) {
        this.fournisseurId = fournisseurId;
        return this;
    }

    public CommandeWrapperDTO setFournisseur(FournisseurDTO fournisseur) {
        this.fournisseur = fournisseur;
        return this;
    }
}
