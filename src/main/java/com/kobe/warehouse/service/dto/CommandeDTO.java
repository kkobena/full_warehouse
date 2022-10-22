package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.enumeration.OrderStatut;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CommandeDTO {

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
  private Instant updatedAt;
  private OrderStatut orderStatus;
  private List<OrderLineDTO> orderLines;
  private MagasinDTO magasin;
  private UserDTO user;
  private UserDTO lastUserEdit;
  private FournisseurDTO fournisseur;
  private int totalProduits;

  public int getTotalProduits() {
    return totalProduits;
  }

  public CommandeDTO setTotalProduits(int totalProduits) {
    this.totalProduits = totalProduits;
    return this;
  }

  public String getReceiptRefernce() {
    return receiptRefernce;
  }

  public UserDTO getLastUserEdit() {
    return lastUserEdit;
  }

  public CommandeDTO setLastUserEdit(UserDTO lastUserEdit) {
    this.lastUserEdit = lastUserEdit;
    return this;
  }

  public CommandeDTO setReceiptRefernce(String receiptRefernce) {
    this.receiptRefernce = receiptRefernce;
    return this;
  }

  public Long getId() {
    return id;
  }

  public CommandeDTO setId(Long id) {
    this.id = id;
    return this;
  }

  public String getOrderRefernce() {
    return orderRefernce;
  }

  public CommandeDTO setOrderRefernce(String orderRefernce) {
    this.orderRefernce = orderRefernce;
    return this;
  }

  public LocalDateTime getReceiptDate() {
    return receiptDate;
  }

  public CommandeDTO setReceiptDate(LocalDateTime receiptDate) {
    this.receiptDate = receiptDate;
    return this;
  }

  public Integer getDiscountAmount() {
    return discountAmount;
  }

  public CommandeDTO setDiscountAmount(Integer discountAmount) {
    this.discountAmount = discountAmount;
    return this;
  }

  public Integer getOrderAmount() {
    return orderAmount;
  }

  public CommandeDTO setOrderAmount(Integer orderAmount) {
    this.orderAmount = orderAmount;
    return this;
  }

  public Integer getGrossAmount() {
    return grossAmount;
  }

  public CommandeDTO setGrossAmount(Integer grossAmount) {
    this.grossAmount = grossAmount;
    return this;
  }

  public Integer getNetAmount() {
    return netAmount;
  }

  public CommandeDTO setNetAmount(Integer netAmount) {
    this.netAmount = netAmount;
    return this;
  }

  public Integer getTaxAmount() {
    return taxAmount;
  }

  public CommandeDTO setTaxAmount(Integer taxAmount) {
    this.taxAmount = taxAmount;
    return this;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public CommandeDTO setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public CommandeDTO setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public OrderStatut getOrderStatus() {
    return orderStatus;
  }

  public CommandeDTO setOrderStatus(OrderStatut orderStatus) {
    this.orderStatus = orderStatus;
    return this;
  }

  public List<OrderLineDTO> getOrderLines() {
    return orderLines;
  }

  public CommandeDTO setOrderLines(List<OrderLineDTO> orderLines) {
    this.orderLines = orderLines;
    return this;
  }

  public MagasinDTO getMagasin() {
    return magasin;
  }

  public CommandeDTO setMagasin(MagasinDTO magasin) {
    this.magasin = magasin;
    return this;
  }

  public UserDTO getUser() {
    return user;
  }

  public FournisseurDTO getFournisseur() {
    return fournisseur;
  }

  public CommandeDTO setFournisseur(FournisseurDTO fournisseur) {
    this.fournisseur = fournisseur;
    return this;
  }

  public CommandeDTO setUser(UserDTO user) {
    this.user = user;
    return this;
  }

  public CommandeDTO() {}

  public CommandeDTO(Commande commande) {
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
    this.orderLines =
        commande.getOrderLines().stream()
            .map(OrderLineDTO::new)
            .sorted(Comparator.comparing(OrderLineDTO::getUpdatedAt, Comparator.reverseOrder()))
            .collect(Collectors.toList());
    this.magasin = Optional.ofNullable(commande.getMagasin()).map(MagasinDTO::new).orElse(null);
    this.user = Optional.ofNullable(commande.getUser()).map(UserDTO::new).orElse(null);
    this.lastUserEdit =
        Optional.ofNullable(commande.getLastUserEdit()).map(UserDTO::new).orElse(null);
    this.totalProduits = this.orderLines.size();
    this.fournisseur =
        Optional.ofNullable(commande.getFournisseur()).map(FournisseurDTO::new).orElse(null);
  }
}
