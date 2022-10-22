package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.Produit;

import java.time.Instant;
import java.time.LocalDateTime;

public class OrderLineDTO {
  private int totalQuantity;
  private int regularUnitPrice;
  private int orderUnitPrice;
  private Long id;
  private LocalDateTime receiptDate;
  private Integer quantityReceived;
  private Integer quantityRequested;
  private Integer quantityReturned;
  private Integer discountAmount;
  private Integer orderAmount;
  private Integer grossAmount;
  private Integer netAmount;
  private Integer taxAmount;
  private Instant createdAt;
  private Instant updatedAt;
  private Integer costAmount;
  private CommandeDTO commande;
  private Long produitId;
  private Long fournisseurProduitId;
  private String produitLibelle;
  private String produitCip;
  private String produitCodeEan;
  private int orderCostAmount;
  private int initStock;
  private long commandeId;
  private String commandeOrderRefernce;
  private String commandeReceiptRefernce;
  private Boolean provisionalCode;
  private Integer quantityUg;
  private Integer quantityReceivedTmp;

  public Boolean getProvisionalCode() {
    return provisionalCode;
  }

  public OrderLineDTO setProvisionalCode(Boolean provisionalCode) {
    this.provisionalCode = provisionalCode;
    return this;
  }

  public Integer getQuantityUg() {
    return quantityUg;
  }

  public OrderLineDTO setQuantityUg(Integer quantityUg) {
    this.quantityUg = quantityUg;
    return this;
  }

  public long getCommandeId() {
    return commandeId;
  }

  public OrderLineDTO setCommandeId(long commandeId) {
    this.commandeId = commandeId;
    return this;
  }

  public String getCommandeOrderRefernce() {
    return commandeOrderRefernce;
  }

  public OrderLineDTO setCommandeOrderRefernce(String commandeOrderRefernce) {
    this.commandeOrderRefernce = commandeOrderRefernce;
    return this;
  }

  public String getCommandeReceiptRefernce() {
    return commandeReceiptRefernce;
  }

  public OrderLineDTO setCommandeReceiptRefernce(String commandeReceiptRefernce) {
    this.commandeReceiptRefernce = commandeReceiptRefernce;
    return this;
  }

  public int getInitStock() {
    return initStock;
  }

  public OrderLineDTO setInitStock(int initStock) {
    this.initStock = initStock;
    return this;
  }

  public int getTotalQuantity() {
    return totalQuantity;
  }

  public OrderLineDTO setTotalQuantity(int totalQuantity) {
    this.totalQuantity = totalQuantity;
    return this;
  }

  public int getRegularUnitPrice() {
    return regularUnitPrice;
  }

  public OrderLineDTO setRegularUnitPrice(int regularUnitPrice) {
    this.regularUnitPrice = regularUnitPrice;
    return this;
  }

  public int getOrderUnitPrice() {
    return orderUnitPrice;
  }

  public OrderLineDTO setOrderUnitPrice(int orderUnitPrice) {
    this.orderUnitPrice = orderUnitPrice;
    return this;
  }

  public Long getId() {
    return id;
  }

  public OrderLineDTO setId(Long id) {
    this.id = id;
    return this;
  }

  public LocalDateTime getReceiptDate() {
    return receiptDate;
  }

  public OrderLineDTO setReceiptDate(LocalDateTime receiptDate) {
    this.receiptDate = receiptDate;
    return this;
  }

  public Integer getQuantityReceived() {
    return quantityReceived;
  }

  public OrderLineDTO setQuantityReceived(Integer quantityReceived) {
    this.quantityReceived = quantityReceived;
    return this;
  }

  public Integer getQuantityRequested() {
    return quantityRequested;
  }

  public OrderLineDTO setQuantityRequested(Integer quantityRequested) {
    this.quantityRequested = quantityRequested;
    return this;
  }

  public Integer getQuantityReturned() {
    return quantityReturned;
  }

  public OrderLineDTO setQuantityReturned(Integer quantityReturned) {
    this.quantityReturned = quantityReturned;
    return this;
  }

  public Integer getDiscountAmount() {
    return discountAmount;
  }

  public OrderLineDTO setDiscountAmount(Integer discountAmount) {
    this.discountAmount = discountAmount;
    return this;
  }

  public Integer getOrderAmount() {
    return orderAmount;
  }

  public OrderLineDTO setOrderAmount(Integer orderAmount) {
    this.orderAmount = orderAmount;
    return this;
  }

  public Integer getGrossAmount() {
    return grossAmount;
  }

  public OrderLineDTO setGrossAmount(Integer grossAmount) {
    this.grossAmount = grossAmount;
    return this;
  }

  public Integer getNetAmount() {
    return netAmount;
  }

  public OrderLineDTO setNetAmount(Integer netAmount) {
    this.netAmount = netAmount;
    return this;
  }

  public Integer getTaxAmount() {
    return taxAmount;
  }

  public OrderLineDTO setTaxAmount(Integer taxAmount) {
    this.taxAmount = taxAmount;
    return this;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public OrderLineDTO setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public OrderLineDTO setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public Integer getCostAmount() {
    return costAmount;
  }

  public OrderLineDTO setCostAmount(Integer costAmount) {
    this.costAmount = costAmount;
    return this;
  }

  public CommandeDTO getCommande() {
    return commande;
  }

  public OrderLineDTO setCommande(CommandeDTO commande) {
    this.commande = commande;
    return this;
  }

  public Long getProduitId() {
    return produitId;
  }

  public OrderLineDTO setProduitId(Long produitId) {
    this.produitId = produitId;
    return this;
  }

  public Long getFournisseurProduitId() {
    return fournisseurProduitId;
  }

  public OrderLineDTO setFournisseurProduitId(Long fournisseurProduitId) {
    this.fournisseurProduitId = fournisseurProduitId;
    return this;
  }

  public String getProduitLibelle() {
    return produitLibelle;
  }

  public OrderLineDTO setProduitLibelle(String produitLibelle) {
    this.produitLibelle = produitLibelle;
    return this;
  }

  public String getProduitCip() {
    return produitCip;
  }

  public OrderLineDTO setProduitCip(String produitCip) {
    this.produitCip = produitCip;
    return this;
  }

  public String getProduitCodeEan() {
    return produitCodeEan;
  }

  public OrderLineDTO setProduitCodeEan(String produitCodeEan) {
    this.produitCodeEan = produitCodeEan;
    return this;
  }

  public int getOrderCostAmount() {
    return orderCostAmount;
  }

  public OrderLineDTO setOrderCostAmount(int orderCostAmount) {
    this.orderCostAmount = orderCostAmount;
    return this;
  }

  public Integer getQuantityReceivedTmp() {
    return quantityReceivedTmp;
  }

  public OrderLineDTO setQuantityReceivedTmp(Integer quantityReceivedTmp) {
    this.quantityReceivedTmp = quantityReceivedTmp;
    return this;
  }

  public OrderLineDTO() {}

  public OrderLineDTO(OrderLine orderLine) {
    this.initStock = orderLine.getInitStock();
    this.regularUnitPrice = orderLine.getRegularUnitPrice();
    this.orderUnitPrice = orderLine.getOrderUnitPrice();
    this.id = orderLine.getId();
    this.receiptDate = orderLine.getReceiptDate();
    this.quantityReceived = orderLine.getQuantityReceived();
    this.quantityRequested = orderLine.getQuantityRequested();
    this.quantityReturned = orderLine.getQuantityReturned();
    this.discountAmount = orderLine.getDiscountAmount();
    this.orderAmount = orderLine.getOrderAmount();
    this.grossAmount = orderLine.getGrossAmount();
    this.netAmount = orderLine.getNetAmount();
    this.taxAmount = orderLine.getTaxAmount();
    this.createdAt = orderLine.getCreatedAt();
    this.updatedAt = orderLine.getUpdatedAt();
    this.costAmount = orderLine.getCostAmount();
    Commande commande = orderLine.getCommande();
    this.commandeId = commande.getId();
    this.commandeOrderRefernce = commande.getOrderRefernce();
    this.commandeReceiptRefernce = commande.getReceiptRefernce();
    FournisseurProduit fournisseurProduit = orderLine.getFournisseurProduit();
    Produit produit = fournisseurProduit.getProduit();
    this.produitId = produit.getId();
    this.fournisseurProduitId = fournisseurProduit.getId();
    this.produitLibelle = produit.getLibelle();
    this.produitCip = fournisseurProduit.getCodeCip();
    this.produitCodeEan = produit.getCodeEan();
    this.orderCostAmount = orderLine.getOrderCostAmount();
    this.provisionalCode = orderLine.getProvisionalCode();
    this.quantityUg = orderLine.getQuantityUg() != null ? orderLine.getQuantityUg() : 0;
    this.quantityReceivedTmp =
        orderLine.getQuantityReceived() != null
            ? orderLine.getQuantityReceived()
            : orderLine.getQuantityRequested();
  }
}
