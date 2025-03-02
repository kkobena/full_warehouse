package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import java.time.LocalDate;
import java.time.LocalDateTime;

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

    private Long fournisseurId;
    private int totalProduits;
    private Integer receiptAmount;
    private String sequenceBon;

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

    public CommandeWrapperDTO() {}

    public Long getId() {
        return id;
    }

    public CommandeWrapperDTO setId(Long id) {
        this.id = id;
        return this;
    }

    public String getOrderRefernce() {
        return orderRefernce;
    }

    public CommandeWrapperDTO setOrderRefernce(String orderRefernce) {
        this.orderRefernce = orderRefernce;
        return this;
    }

    public String getReceiptRefernce() {
        return receiptRefernce;
    }

    public CommandeWrapperDTO setReceiptRefernce(String receiptRefernce) {
        this.receiptRefernce = receiptRefernce;
        return this;
    }

    public LocalDate getReceiptDate() {
        return receiptDate;
    }

    public CommandeWrapperDTO setReceiptDate(LocalDate receiptDate) {
        this.receiptDate = receiptDate;
        return this;
    }

    public Integer getDiscountAmount() {
        return discountAmount;
    }

    public CommandeWrapperDTO setDiscountAmount(Integer discountAmount) {
        this.discountAmount = discountAmount;
        return this;
    }

    public Integer getOrderAmount() {
        return orderAmount;
    }

    public CommandeWrapperDTO setOrderAmount(Integer orderAmount) {
        this.orderAmount = orderAmount;
        return this;
    }

    public Integer getGrossAmount() {
        return grossAmount;
    }

    public CommandeWrapperDTO setGrossAmount(Integer grossAmount) {
        this.grossAmount = grossAmount;
        return this;
    }

    public Integer getNetAmount() {
        return netAmount;
    }

    public CommandeWrapperDTO setNetAmount(Integer netAmount) {
        this.netAmount = netAmount;
        return this;
    }

    public Integer getTaxAmount() {
        return taxAmount;
    }

    public CommandeWrapperDTO setTaxAmount(Integer taxAmount) {
        this.taxAmount = taxAmount;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public CommandeWrapperDTO setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public CommandeWrapperDTO setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public OrderStatut getOrderStatus() {
        return orderStatus;
    }

    public CommandeWrapperDTO setOrderStatus(OrderStatut orderStatus) {
        this.orderStatus = orderStatus;
        return this;
    }

    public Long getFournisseurId() {
        return fournisseurId;
    }

    public CommandeWrapperDTO setFournisseurId(Long fournisseurId) {
        this.fournisseurId = fournisseurId;
        return this;
    }

    public int getTotalProduits() {
        return totalProduits;
    }

    public CommandeWrapperDTO setTotalProduits(int totalProduits) {
        this.totalProduits = totalProduits;
        return this;
    }

    public Integer getReceiptAmount() {
        return receiptAmount;
    }

    public CommandeWrapperDTO setReceiptAmount(Integer receiptAmount) {
        this.receiptAmount = receiptAmount;
        return this;
    }

    public String getSequenceBon() {
        return sequenceBon;
    }

    public CommandeWrapperDTO setSequenceBon(String sequenceBon) {
        this.sequenceBon = sequenceBon;
        return this;
    }

    public FournisseurDTO getFournisseur() {
        return fournisseur;
    }

    public CommandeWrapperDTO setFournisseur(FournisseurDTO fournisseur) {
        this.fournisseur = fournisseur;
        return this;
    }
}
