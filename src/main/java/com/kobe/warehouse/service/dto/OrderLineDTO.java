package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Tva;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class OrderLineDTO {

    private  TvaDTO tva;
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
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
    private Integer ugQuantity;
    private Set<LotDTO> lots = new HashSet<>();

    public OrderLineDTO() {}

    public OrderLineDTO(OrderLine orderLine) {
        initStock = orderLine.getInitStock();
        orderUnitPrice = orderLine.getOrderUnitPrice();
        id = orderLine.getId();
        quantityReceived = orderLine.getQuantityReceived();
        quantityRequested = orderLine.getQuantityRequested();
        quantityReturned = orderLine.getQuantityReturned();
        discountAmount = orderLine.getDiscountAmount();
        orderAmount = orderLine.getOrderAmount();
        grossAmount = orderLine.getGrossAmount();
        netAmount = orderLine.getNetAmount();
        taxAmount = orderLine.getTaxAmount();
        createdAt = orderLine.getCreatedAt();
        updatedAt = orderLine.getUpdatedAt();
       // costAmount = orderLine.getCostAmount();
        FournisseurProduit fournisseurProduit = orderLine.getFournisseurProduit();
        regularUnitPrice = fournisseurProduit.getPrixUni();
        Produit produit = fournisseurProduit.getProduit();
        produitId = produit.getId();
        fournisseurProduitId = fournisseurProduit.getId();
        produitLibelle = produit.getLibelle();
        produitCip = fournisseurProduit.getCodeCip();
        produitCodeEan = produit.getCodeEan();
        orderCostAmount = orderLine.getOrderCostAmount();
        provisionalCode = orderLine.getProvisionalCode();
        quantityUg = orderLine.getFreeQty() ;
        ugQuantity = orderLine.getFreeQty() ;
        quantityReceivedTmp = orderLine.getQuantityReceived() != null ? orderLine.getQuantityReceived() : orderLine.getQuantityRequested();
        lots = orderLine.getLots().stream().map(LotDTO::new).collect(java.util.stream.Collectors.toSet()) ;
       Tva tvaEntity = orderLine.getTva();
        tva = Objects.nonNull(tvaEntity) ?new TvaDTO(tvaEntity): null;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public OrderLineDTO setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public OrderLineDTO setUpdatedAt(LocalDateTime updatedAt) {
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

    public int getInitStock() {
        return initStock;
    }

    public OrderLineDTO setInitStock(int initStock) {
        this.initStock = initStock;
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

    public Integer getQuantityReceivedTmp() {
        return quantityReceivedTmp;
    }

    public OrderLineDTO setQuantityReceivedTmp(Integer quantityReceivedTmp) {
        this.quantityReceivedTmp = quantityReceivedTmp;
        return this;
    }

    public Integer getUgQuantity() {
        return ugQuantity;
    }

    public OrderLineDTO setUgQuantity(Integer ugQuantity) {
        this.ugQuantity = ugQuantity;
        return this;
    }

    public TvaDTO getTva() {
        return tva;
    }

    public void setTva(TvaDTO tva) {
        this.tva = tva;
    }

    public Set<LotDTO> getLots() {
        return lots;
    }

    public OrderLineDTO setLots(Set<LotDTO> lots) {
        this.lots = lots;
        return this;
    }
}
