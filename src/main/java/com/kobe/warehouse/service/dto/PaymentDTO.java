package com.kobe.warehouse.service.dto;

import java.time.Instant;

import com.kobe.warehouse.domain.Customer;
import com.kobe.warehouse.domain.Payment;
import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.User;

public class PaymentDTO {
	private Long id;
	private Integer netAmount;
	private Integer paidAmount;
	private Integer restToPay;
	private Instant createdAt;
	private Instant updatedAt;
	private PaymentModeDTO paymentMode;
	private Long userId;
	private Customer customer;
	private String saleNumberTransaction;
	private Long customerId;
	private Long salesId;
	private Integer salesAmount;
	private Integer salesNetAmount;
	private String userFullName;
    private Integer reelPaidAmount;
    private String paymentCode;
    private Integer montantVerse=0;


	public Integer getSalesAmount() {
		return salesAmount;
	}

	public void setSalesAmount(Integer salesAmount) {
		this.salesAmount = salesAmount;
	}

	public Integer getSalesNetAmount() {
		return salesNetAmount;
	}

	public void setSalesNetAmount(Integer salesNetAmount) {
		this.salesNetAmount = salesNetAmount;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Integer getNetAmount() {
		return netAmount;
	}

	public void setNetAmount(Integer netAmount) {
		this.netAmount = netAmount;
	}

	public Integer getPaidAmount() {
		return paidAmount;
	}

	public void setPaidAmount(Integer paidAmount) {
		this.paidAmount = paidAmount;
	}

	public Integer getRestToPay() {
		return restToPay;
	}

	public void setRestToPay(Integer restToPay) {
		this.restToPay = restToPay;
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

    public PaymentModeDTO getPaymentMode() {
        return paymentMode;
    }

    public PaymentDTO setPaymentMode(PaymentModeDTO paymentMode) {
        this.paymentMode = paymentMode;
        return this;
    }

    public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public Customer getCustomer() {
		return customer;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}

	public String getUserFullName() {
		return userFullName;
	}

	public void setUserFullName(String userFullName) {
		this.userFullName = userFullName;
	}

	public String getSaleNumberTransaction() {
		return saleNumberTransaction;
	}

	public void setSaleNumberTransaction(String saleNumberTransaction) {
		this.saleNumberTransaction = saleNumberTransaction;
	}

	public Long getCustomerId() {
		return customerId;
	}

	public void setCustomerId(Long customerId) {
		this.customerId = customerId;
	}

	public Long getSalesId() {
		return salesId;
	}

	public void setSalesId(Long salesId) {
		this.salesId = salesId;
	}

    public Integer getReelPaidAmount() {
        return reelPaidAmount;
    }

    public PaymentDTO setReelPaidAmount(Integer reelPaidAmount) {
        this.reelPaidAmount = reelPaidAmount;
        return this;
    }

    public String getPaymentCode() {
        return paymentCode;
    }

    public PaymentDTO setPaymentCode(String paymentCode) {
        this.paymentCode = paymentCode;
        return this;
    }

    public Integer getMontantVerse() {
        return montantVerse;
    }

    public PaymentDTO setMontantVerse(Integer montantVerse) {
        this.montantVerse = montantVerse;
        return this;
    }

    public PaymentDTO(Payment payment) {
		super();
		this.id = payment.getId();
		this.netAmount = payment.getNetAmount();
		this.paidAmount = payment.getPaidAmount();
		this.createdAt = payment.getCreatedAt();
		this.updatedAt = payment.getUpdatedAt();
		this.paymentMode = new PaymentModeDTO(payment.getPaymentMode());
		User user = payment.getUser();
		this.userId = user.getId();
		this.userFullName = user.getFirstName()+" " + user.getLastName();
		Customer customer=payment.getCustomer();
		this.customer = customer;
		Sales sales=payment.getSales();
		this.saleNumberTransaction = sales.getNumberTransaction();
		this.salesId = sales.getId();
		this.salesAmount=sales.getSalesAmount();
		this.salesNetAmount=sales.getNetAmount();
	}

    public PaymentDTO() {
    }
}
