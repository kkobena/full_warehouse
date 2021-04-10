package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javax.persistence.*;
import javax.validation.constraints.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import com.kobe.warehouse.domain.enumeration.SalesStatut;

/**
 * A Sales.
 */
@Entity
@Table(name = "sales",uniqueConstraints = { @UniqueConstraint(columnNames = { "number_transaction", "date_dimension_date_key" }) })
public class Sales implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@NotNull
	@Column(name = "number_transaction", nullable = false)
	private String numberTransaction;
	@NotNull
	@Column(name = "discount_amount", nullable = false)
	private Integer discountAmount;

	@NotNull
	@Column(name = "sales_amount", nullable = false)
	private Integer salesAmount=0;

	@NotNull
	@Column(name = "gross_amount", nullable = false)
	private Integer grossAmount=0;

	@NotNull
	@Column(name = "net_amount", nullable = false)
	private Integer netAmount;

	@NotNull
	@Column(name = "tax_amount", nullable = false)
	private Integer taxAmount;

	@NotNull
	@Column(name = "cost_amount", nullable = false)
	private Integer costAmount=0;

	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(name = "statut", nullable = false)
	private SalesStatut statut;

	@NotNull
	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@NotNull
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@OneToMany(mappedBy = "sales")
	private Set<SalesLine> salesLines = new HashSet<>();

	@ManyToOne(optional = false)
	@JsonIgnoreProperties(value = "sales", allowSetters = true)
	private Customer customer;

	@ManyToOne(optional = false)
	@JsonIgnoreProperties(value = "sales", allowSetters = true)
	private DateDimension dateDimension;
	@NotNull
	@ManyToOne(optional = false)
	private User user;
	@OneToMany(mappedBy = "sales")
	private Set<Payment> payments = new HashSet<>();

	public String getNumberTransaction() {
		return numberTransaction;
	}

	public void setNumberTransaction(String numberTransaction) {
		this.numberTransaction = numberTransaction;
	}

	public Set<Payment> getPayments() {
		return payments;
	}

	public void setPayments(Set<Payment> payments) {
		this.payments = payments;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Integer getDiscountAmount() {
		return discountAmount;
	}

	public Sales discountAmount(Integer discountAmount) {
		this.discountAmount = discountAmount;
		return this;
	}

	public void setDiscountAmount(Integer discountAmount) {
		this.discountAmount = discountAmount;
	}

	public Integer getSalesAmount() {
		return salesAmount;
	}

	public Sales salesAmount(Integer salesAmount) {
		this.salesAmount = salesAmount;
		return this;
	}

	public void setSalesAmount(Integer salesAmount) {
		this.salesAmount = salesAmount;
	}

	public Integer getGrossAmount() {
		return grossAmount;
	}

	public Sales grossAmount(Integer grossAmount) {
		this.grossAmount = grossAmount;
		return this;
	}

	public void setGrossAmount(Integer grossAmount) {
		this.grossAmount = grossAmount;
	}

	public Integer getNetAmount() {
		return netAmount;
	}

	public Sales netAmount(Integer netAmount) {
		this.netAmount = netAmount;
		return this;
	}

	public void setNetAmount(Integer netAmount) {
		this.netAmount = netAmount;
	}

	public Integer getTaxAmount() {
		return taxAmount;
	}

	public Sales taxAmount(Integer taxAmount) {
		this.taxAmount = taxAmount;
		return this;
	}

	public void setTaxAmount(Integer taxAmount) {
		this.taxAmount = taxAmount;
	}

	public Integer getCostAmount() {
		return costAmount;
	}

	public Sales costAmount(Integer costAmount) {
		this.costAmount = costAmount;
		return this;
	}

	public void setCostAmount(Integer costAmount) {
		this.costAmount = costAmount;
	}

	public SalesStatut getStatut() {
		return statut;
	}

	public Sales statut(SalesStatut statut) {
		this.statut = statut;
		return this;
	}

	public void setStatut(SalesStatut statut) {
		this.statut = statut;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Sales createdAt(Instant createdAt) {
		this.createdAt = createdAt;
		return this;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public Sales updatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
		return this;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}

	public Set<SalesLine> getSalesLines() {
		return salesLines;
	}

	public Sales salesLines(Set<SalesLine> salesLines) {
		this.salesLines = salesLines;
		return this;
	}

	public Sales addSalesLine(SalesLine salesLine) {
		this.salesLines.add(salesLine);
		salesLine.setSales(this);
		return this;
	}

	public Sales removeSalesLine(SalesLine salesLine) {
		this.salesLines.remove(salesLine);
		salesLine.setSales(null);
		return this;
	}

	public void setSalesLines(Set<SalesLine> salesLines) {
		this.salesLines = salesLines;
	}

	public Customer getCustomer() {
		return customer;
	}

	public Sales customer(Customer customer) {
		this.customer = customer;
		return this;
	}

	public void setCustomer(Customer customer) {
		this.customer = customer;
	}

	public DateDimension getDateDimension() {
		return dateDimension;
	}

	public Sales dateDimension(DateDimension dateDimension) {
		this.dateDimension = dateDimension;
		return this;
	}

	public void setDateDimension(DateDimension dateDimension) {
		this.dateDimension = dateDimension;
	}
	// jhipster-needle-entity-add-getters-setters - JHipster will add getters and
	// setters here

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Sales)) {
			return false;
		}
		return id != null && id.equals(((Sales) o).id);
	}

	@Override
	public int hashCode() {
		return 31;
	}

	// prettier-ignore
	@Override
	public String toString() {
		return "Sales{" + "id=" + getId() + ", discountAmount=" + getDiscountAmount() + ", salesAmount="
				+ getSalesAmount() + ", grossAmount=" + getGrossAmount() + ", netAmount=" + getNetAmount()
				+ ", taxAmount=" + getTaxAmount() + ", costAmount=" + getCostAmount() + ", statut='" + getStatut() + "'"
				+ ", createdAt='" + getCreatedAt() + "'" + ", updatedAt='" + getUpdatedAt() + "'" + "}";
	}
}
