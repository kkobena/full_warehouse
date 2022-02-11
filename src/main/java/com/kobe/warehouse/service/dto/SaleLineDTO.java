package com.kobe.warehouse.service.dto;

import java.time.Instant;

import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.SalesLine;

public class SaleLineDTO {
	private Long id;
	private Integer quantitySold;
    private Integer quantityRequested;
	private Integer regularUnitPrice;
	private Integer discountUnitPrice;
	private Integer netUnitPrice;
	private Integer discountAmount;
	private Integer salesAmount;
	private Integer htAmount;
	private Integer netAmount;
	private Integer taxAmount;
	private Integer costAmount;
	private Instant createdAt;
	private Instant updatedAt;
	private String produitLibelle,code;
	private Long produitId;
	private Long saleId;
	private Integer quantityStock;
    private Integer quantiyAvoir;
    private Integer montantTvaUg = 0;
    private Integer quantityUg;
    private Integer amountToBeTakenIntoAccount;
    private boolean toIgnore ;
    private Instant effectiveUpdateDate;
    private Integer taxValue;
    private boolean forceStock;//mis pour forcer le stock a la vente

    public boolean isForceStock() {
        return forceStock;
    }

    public SaleLineDTO setForceStock(boolean forceStock) {
        this.forceStock = forceStock;
        return this;
    }

    public Integer getHtAmount() {
        return htAmount;
    }

    public SaleLineDTO setHtAmount(Integer htAmount) {
        this.htAmount = htAmount;
        return this;
    }

    public Integer getTaxValue() {
        return taxValue;
    }

    public SaleLineDTO setTaxValue(Integer taxValue) {
        this.taxValue = taxValue;
        return this;
    }

    public Integer getAmountToBeTakenIntoAccount() {
        return amountToBeTakenIntoAccount;
    }

    public SaleLineDTO setAmountToBeTakenIntoAccount(Integer amountToBeTakenIntoAccount) {
        this.amountToBeTakenIntoAccount = amountToBeTakenIntoAccount;
        return this;
    }

    public boolean isToIgnore() {
        return toIgnore;
    }

    public SaleLineDTO setToIgnore(boolean toIgnore) {
        this.toIgnore = toIgnore;
        return this;
    }

    public Instant getEffectiveUpdateDate() {
        return effectiveUpdateDate;
    }

    public SaleLineDTO setEffectiveUpdateDate(Instant effectiveUpdateDate) {
        this.effectiveUpdateDate = effectiveUpdateDate;
        return this;
    }

    public Integer getQuantityUg() {
        return quantityUg;
    }

    public SaleLineDTO setQuantityUg(Integer quantityUg) {
        this.quantityUg = quantityUg;
        return this;
    }

    public Integer getQuantityRequested() {
        return quantityRequested;
    }

    public SaleLineDTO setQuantityRequested(Integer quantityRequested) {
        this.quantityRequested = quantityRequested;
        return this;
    }

    public Integer getQuantiyAvoir() {
        return quantiyAvoir;
    }

    public SaleLineDTO setQuantiyAvoir(Integer quantiyAvoir) {
        this.quantiyAvoir = quantiyAvoir;
        return this;
    }

    public Integer getMontantTvaUg() {
        return montantTvaUg;
    }

    public SaleLineDTO setMontantTvaUg(Integer montantTvaUg) {
        this.montantTvaUg = montantTvaUg;
        return this;
    }

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

    public String getCode() {
        return code;
    }

    public SaleLineDTO setCode(String code) {
        this.code = code;
        return this;
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
        this.quantityRequested=salesLine.getQuantityRequested();
		this.regularUnitPrice = salesLine.getRegularUnitPrice();
		this.discountUnitPrice = salesLine.getDiscountUnitPrice();
		this.netUnitPrice = salesLine.getNetUnitPrice();
		this.discountAmount = salesLine.getDiscountAmount();
		this.salesAmount = salesLine.getSalesAmount();
		this.netAmount = salesLine.getNetAmount();
		this.costAmount = salesLine.getCostAmount();
		this.createdAt = salesLine.getCreatedAt();
		this.updatedAt = salesLine.getUpdatedAt();
		Produit produit = salesLine.getProduit();
      FournisseurProduit fournisseurProduit= produit.getFournisseurProduitPrincipal();
      if(fournisseurProduit!=null){
          this.code=fournisseurProduit.getCodeCip();
      }
		this.produitLibelle = produit.getLibelle();
		this.produitId = produit.getId();
		this.saleId = salesLine.getSales().getId();


	}

}
