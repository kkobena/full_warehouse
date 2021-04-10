package com.kobe.warehouse.service.dto;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.kobe.warehouse.domain.Customer;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.domain.enumeration.SalesStatut;

public class SaleDTO {
	private Long id;

	private Integer discountAmount;
	private Customer customer;
	private String numberTransaction;
	private Long customerId;
	private Integer salesAmount;
	private String userFullName;
	private Integer grossAmount;

	private Integer netAmount;

	private Integer taxAmount;

	private Integer costAmount;

	private SalesStatut statut;

	private Instant createdAt;

	private Instant updatedAt;
	private List<SaleLineDTO> salesLines = new ArrayList<>();
	private List<PaymentDTO> payments = new ArrayList<>();

	public Long getId() {
		return id;
	}

	public String getUserFullName() {
		return userFullName;
	}

	public void setUserFullName(String userFullName) {
		this.userFullName = userFullName;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Integer getDiscountAmount() {
		return discountAmount;
	}

	public void setDiscountAmount(Integer discountAmount) {
		this.discountAmount = discountAmount;
	}

	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}

	public Long getCustomerId() {
		return customerId;
	}

	public void setCustomerId(Long customerId) {
		this.customerId = customerId;
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

	public SalesStatut getStatut() {
		return statut;
	}

	public void setStatut(SalesStatut statut) {
		this.statut = statut;
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

	public String getNumberTransaction() {
		return numberTransaction;
	}

	public void setNumberTransaction(String numberTransaction) {
		this.numberTransaction = numberTransaction;
	}
	public SaleDTO() {
		
	}
	public SaleDTO(Sales sale) {
		super();
		this.id = sale.getId();
		this.discountAmount = sale.getDiscountAmount();
		this.customer = sale.getCustomer();
		this.salesAmount = sale.getSalesAmount();
		this.grossAmount = sale.getGrossAmount();
		this.netAmount = sale.getNetAmount();
		this.taxAmount = sale.getTaxAmount();
		this.costAmount = sale.getCostAmount();
		this.statut = sale.getStatut();
		this.createdAt = sale.getCreatedAt();
		this.updatedAt = sale.getUpdatedAt();
		this.salesLines = sale.getSalesLines().stream().map(SaleLineDTO::new).collect(Collectors.toList());
		this.payments = sale.getPayments().stream().map(PaymentDTO::new).collect(Collectors.toList());
		User user = sale.getUser();
		this.userFullName = user.getFirstName() + " " + user.getLastName();
		this.numberTransaction = sale.getNumberTransaction();
	}

	public List<SaleLineDTO> getSalesLines() {
		return salesLines;
	}

	public void setSalesLines(List<SaleLineDTO> salesLines) {
		this.salesLines = salesLines;
	}

	public List<PaymentDTO> getPayments() {
		return payments;
	}

	public void setPayments(List<PaymentDTO> payments) {
		this.payments = payments;
	}

}
