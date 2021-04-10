package com.kobe.warehouse.service.dto;

import java.time.Instant;

import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.SalesLine;

public class SaleLineDTO {
	private Long id;

	private Integer quantitySold;

	private Integer regularUnitPrice;

	private Integer discountUnitPrice;

	private Integer netUnitPrice;

	private Integer discountAmount;

	private Integer salesAmount;

	private Integer grossAmount;

	private Integer netAmount;

	private Integer taxAmount;

	private Integer costAmount;

	private Instant createdAt;

	private Instant updatedAt;
	private String produitLibelle;
	private Long produitId;
	private Long saleId;
	private Integer quantityStock;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Integer getQuantitySold() {
		return quantitySold;
	}

	public void setQuantitySold(Integer quantitySold) {
		this.quantitySold = quantitySold;
	}

	public Integer getRegularUnitPrice() {
		return regularUnitPrice;
	}

	public void setRegularUnitPrice(Integer regularUnitPrice) {
		this.regularUnitPrice = regularUnitPrice;
	}

	public Integer getDiscountUnitPrice() {
		return discountUnitPrice;
	}

	public void setDiscountUnitPrice(Integer discountUnitPrice) {
		this.discountUnitPrice = discountUnitPrice;
	}

	public Integer getNetUnitPrice() {
		return netUnitPrice;
	}

	public void setNetUnitPrice(Integer netUnitPrice) {
		this.netUnitPrice = netUnitPrice;
	}

	public Integer getDiscountAmount() {
		return discountAmount;
	}

	public void setDiscountAmount(Integer discountAmount) {
		this.discountAmount = discountAmount;
	}

	public Integer getSalesAmount() {
		return salesAmount;
	}

	public void setSalesAmount(Integer salesAmount) {
		this.salesAmount = salesAmount;
	}

	public Integer getGrossAmount() {
		return grossAmount;
	}

	public void setGrossAmount(Integer grossAmount) {
		this.grossAmount = grossAmount;
	}

	public Integer getNetAmount() {
		return netAmount;
	}

	public void setNetAmount(Integer netAmount) {
		this.netAmount = netAmount;
	}

	public Integer getTaxAmount() {
		return taxAmount;
	}

	public void setTaxAmount(Integer taxAmount) {
		this.taxAmount = taxAmount;
	}

	public Integer getCostAmount() {
		return costAmount;
	}

	public void setCostAmount(Integer costAmount) {
		this.costAmount = costAmount;
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

	public String getProduitLibelle() {
		return produitLibelle;
	}

	public void setProduitLibelle(String produitLibelle) {
		this.produitLibelle = produitLibelle;
	}

	public Long getProduitId() {
		return produitId;
	}

	public void setProduitId(Long produitId) {
		this.produitId = produitId;
	}

	public SaleLineDTO() {

	}

	public Integer getQuantityStock() {
		return quantityStock;
	}

	public void setQuantityStock(Integer quantityStock) {
		this.quantityStock = quantityStock;
	}

	public Long getSaleId() {
		return saleId;
	}

	public void setSaleId(Long saleId) {
		this.saleId = saleId;
	}

	public SaleLineDTO(SalesLine salesLine) {
		super();
		this.id = salesLine.getId();
		this.quantitySold = salesLine.getQuantitySold();
		this.regularUnitPrice = salesLine.getRegularUnitPrice();
		this.discountUnitPrice = salesLine.getDiscountUnitPrice();
		this.netUnitPrice = salesLine.getNetUnitPrice();
		this.discountAmount = salesLine.getDiscountAmount();
		this.salesAmount = salesLine.getSalesAmount();
		this.grossAmount = salesLine.getGrossAmount();
		this.netAmount = salesLine.getNetAmount();
		this.taxAmount = salesLine.getTaxAmount();
		this.costAmount = salesLine.getCostAmount();
		this.createdAt = salesLine.getCreatedAt();
		this.updatedAt = salesLine.getUpdatedAt();
		Produit produit = salesLine.getProduit();
		this.produitLibelle = produit.getLibelle();
		this.produitId = produit.getId();
		this.saleId = salesLine.getSales().getId();
		this.quantityStock = produit.getQuantity();
	}

}
