package com.kobe.warehouse.service.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.TypeProduit;
import org.apache.commons.lang3.RandomStringUtils;

public class ProduitDTO {
    private Long id;

    private String libelle;
    private int itemQuantity;
    private String imageUrl;

    private TypeProduit typeProduit;

    private Integer quantity;

    private Integer costAmount;

    private Integer regularUnitPrice;

    private Integer netUnitPrice;

    private Instant createdAt;

    private Instant updatedAt;

    private Integer itemQty;

    private Integer itemCostAmount;

    private Integer itemRegularUnitPrice;
    private Long produitId;
    private String produitLibelle;
    private String data;
    private String imageType;
    private int quantityReceived;
    private List<ProduitDTO> produits = new ArrayList<>();

    public int getItemQuantity() {
        return itemQuantity;
    }

    public void setItemQuantity(int itemQuantity) {
        this.itemQuantity = itemQuantity;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public TypeProduit getTypeProduit() {
        return typeProduit;
    }

    public void setTypeProduit(TypeProduit typeProduit) {
        this.typeProduit = typeProduit;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getCostAmount() {
        return costAmount;
    }

    public void setCostAmount(Integer costAmount) {
        this.costAmount = costAmount;
    }

    public Integer getRegularUnitPrice() {
        return regularUnitPrice;
    }

    public void setRegularUnitPrice(Integer regularUnitPrice) {
        this.regularUnitPrice = regularUnitPrice;
    }

    public Integer getNetUnitPrice() {
        return netUnitPrice;
    }

    public void setNetUnitPrice(Integer netUnitPrice) {
        this.netUnitPrice = netUnitPrice;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getItemQty() {
        return itemQty;
    }

    public void setItemQty(Integer itemQty) {
        this.itemQty = itemQty;
    }

    public Integer getItemCostAmount() {
        return itemCostAmount;
    }

    public void setItemCostAmount(Integer itemCostAmount) {
        this.itemCostAmount = itemCostAmount;
    }

    public Integer getItemRegularUnitPrice() {
        return itemRegularUnitPrice;
    }

    public void setItemRegularUnitPrice(Integer itemRegularUnitPrice) {
        this.itemRegularUnitPrice = itemRegularUnitPrice;
    }


    public int getQuantityReceived() {
        return quantityReceived;
    }

    public void setQuantityReceived(int quantityReceived) {
        this.quantityReceived = quantityReceived;
    }

    public ProduitDTO() {
        super();
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getImageType() {
        return imageType;
    }

    public void setImageType(String imageType) {
        this.imageType = imageType;
    }

    public ProduitDTO(Produit produit) {
        super();
        this.id = produit.getId();
        this.libelle = produit.getLibelle();
        this.itemQuantity = produit.getProduits().stream().collect(Collectors.summingInt(Produit::getQuantity));
        this.imageUrl = produit.getImageUrl();
        this.typeProduit = produit.getTypeProduit();
        this.quantity = produit.getQuantity();
        this.costAmount = produit.getCostAmount();
        this.regularUnitPrice = produit.getRegularUnitPrice();
        this.netUnitPrice = produit.getNetUnitPrice();
        this.createdAt = produit.getCreatedAt();
        this.updatedAt = produit.getUpdatedAt();
        this.itemQty = produit.getItemQty();
        this.itemCostAmount = produit.getItemCostAmount();
        this.itemRegularUnitPrice = produit.getItemRegularUnitPrice();
        this.imageType = produit.getImageType();
        this.data = produit.getData();
        Produit parent = produit.getParent();
        if (parent != null) {
            this.produitId = parent.getId();
            this.produitLibelle = parent.getLibelle();
        }

        this.produits = produit.getProduits().stream().map(ProduitDTO::new).collect(Collectors.toList());
    }

    public static Produit fromDTO(ProduitDTO produitDTO) {
        Produit produit = new Produit();
        produit.setLibelle(produitDTO.getLibelle().trim().toUpperCase());
        produit.setCode(RandomStringUtils.randomAlphabetic(5));
        produit.setNetUnitPrice(0);
        produit.setQuantity(0);
        if (produitDTO.getTypeProduit() == TypeProduit.DETAIL) {
            produit.setParent(fromId(produitDTO.getProduitId()));
        }
        produit.setTypeProduit(produitDTO.getTypeProduit());
        produit.setCreatedAt(Instant.now());
        produit.setUpdatedAt(produit.getCreatedAt());
        produit.setCostAmount(produitDTO.getCostAmount());
        produit.setItemCostAmount(produitDTO.getItemCostAmount());
        produit.setItemQty(produitDTO.getItemQty());
        produit.setItemRegularUnitPrice(produitDTO.getItemRegularUnitPrice());
        produit.setRegularUnitPrice(produitDTO.getRegularUnitPrice());
        return produit;
    }

    public Long getProduitId() {
        return produitId;
    }

    public void setProduitId(Long produitId) {
        this.produitId = produitId;
    }

    public String getProduitLibelle() {
        return produitLibelle;
    }

    public void setProduitLibelle(String produitLibelle) {
        this.produitLibelle = produitLibelle;
    }

    private static Produit fromId(Long produitId) {
        if (produitId == null) {
            return null;
        }
        Produit produit = new Produit();
        produit.setId(produitId);
        return produit;
    }

    public List<ProduitDTO> getProduits() {
        return produits;
    }

    public void setProduits(List<ProduitDTO> produits) {
        this.produits = produits;
    }
}
