package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.enumeration.OrderStatut;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CommandeLiteDTO {

  private Long id;
  private String orderRefernce;
  private String receiptRefernce;
  private LocalDateTime receiptDate;
  private Integer discountAmount;
  private Integer orderAmount;
  private Integer grossAmount;
  private Integer netAmount;
  private Integer taxAmount;
  private Instant createdAt;
  private int itemSize;
  private Instant updatedAt;
  private OrderStatut orderStatus;
  private FournisseurDTO fournisseur;
  private UserDTO lastUserEdit;

    public int getItemSize() {
        return itemSize;
    }

    public CommandeLiteDTO setItemSize(int itemSize) {
        this.itemSize = itemSize;
        return this;
    }

    public FournisseurDTO getFournisseur() {
        return fournisseur;
    }

    public CommandeLiteDTO setFournisseur(FournisseurDTO fournisseur) {
        this.fournisseur = fournisseur;
        return this;
    }

    public UserDTO getLastUserEdit() {
        return lastUserEdit;
    }

    public CommandeLiteDTO setLastUserEdit(UserDTO lastUserEdit) {
        this.lastUserEdit = lastUserEdit;
        return this;
    }

    public String getReceiptRefernce() {
    return receiptRefernce;
  }


  public CommandeLiteDTO setReceiptRefernce(String receiptRefernce) {
    this.receiptRefernce = receiptRefernce;
    return this;
  }

  public Long getId() {
    return id;
  }

  public CommandeLiteDTO setId(Long id) {
    this.id = id;
    return this;
  }

  public String getOrderRefernce() {
    return orderRefernce;
  }

  public CommandeLiteDTO setOrderRefernce(String orderRefernce) {
    this.orderRefernce = orderRefernce;
    return this;
  }

  public LocalDateTime getReceiptDate() {
    return receiptDate;
  }

  public CommandeLiteDTO setReceiptDate(LocalDateTime receiptDate) {
    this.receiptDate = receiptDate;
    return this;
  }

  public Integer getDiscountAmount() {
    return discountAmount;
  }

  public CommandeLiteDTO setDiscountAmount(Integer discountAmount) {
    this.discountAmount = discountAmount;
    return this;
  }

  public Integer getOrderAmount() {
    return orderAmount;
  }

  public CommandeLiteDTO setOrderAmount(Integer orderAmount) {
    this.orderAmount = orderAmount;
    return this;
  }

  public Integer getGrossAmount() {
    return grossAmount;
  }

  public CommandeLiteDTO setGrossAmount(Integer grossAmount) {
    this.grossAmount = grossAmount;
    return this;
  }

  public Integer getNetAmount() {
    return netAmount;
  }

  public CommandeLiteDTO setNetAmount(Integer netAmount) {
    this.netAmount = netAmount;
    return this;
  }

  public Integer getTaxAmount() {
    return taxAmount;
  }

  public CommandeLiteDTO setTaxAmount(Integer taxAmount) {
    this.taxAmount = taxAmount;
    return this;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public CommandeLiteDTO setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public CommandeLiteDTO setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public OrderStatut getOrderStatus() {
    return orderStatus;
  }

  public CommandeLiteDTO setOrderStatus(OrderStatut orderStatus) {
    this.orderStatus = orderStatus;
    return this;
  }





    public CommandeLiteDTO() {
    }

    public CommandeLiteDTO(Commande commande) {
    this.id = commande.getId();
    this.orderRefernce = commande.getOrderRefernce();
    this.receiptRefernce = commande.getReceiptRefernce();
    this.receiptDate = commande.getReceiptDate();
    this.discountAmount = commande.getDiscountAmount();
    this.orderAmount = commande.getOrderAmount();
    this.grossAmount = commande.getGrossAmount();
    this.netAmount = commande.getNetAmount();
    this.taxAmount = commande.getTaxAmount();
    this.createdAt = commande.getCreatedAt();
    this.updatedAt = commande.getUpdatedAt();
    this.orderStatus = commande.getOrderStatus();
    this.fournisseur=Optional.ofNullable(commande.getFournisseur()).map(FournisseurDTO::new).orElse(null);
    this.lastUserEdit=Optional.ofNullable(commande.getUser()).map(UserDTO::user).orElse(null);


  }

    public CommandeLiteDTO(Commande commande,long count) {
        this.id = commande.getId();
        this.orderRefernce = commande.getOrderRefernce();
        this.receiptRefernce = commande.getReceiptRefernce();
        this.receiptDate = commande.getReceiptDate();
        this.discountAmount = commande.getDiscountAmount();
        this.orderAmount = commande.getOrderAmount();
        this.grossAmount = commande.getGrossAmount();
        this.netAmount = commande.getNetAmount();
        this.taxAmount = commande.getTaxAmount();
        this.createdAt = commande.getCreatedAt();
        this.updatedAt = commande.getUpdatedAt();
        this.orderStatus = commande.getOrderStatus();
        this.fournisseur=Optional.ofNullable(commande.getFournisseur()).map(FournisseurDTO::new).orElse(null);
        this.lastUserEdit=Optional.ofNullable(commande.getUser()).map(UserDTO::user).orElse(null);
         this.itemSize=(int)count;

    }
}
