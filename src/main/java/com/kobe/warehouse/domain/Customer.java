package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kobe.warehouse.domain.enumeration.SalesStatut;

import javax.persistence.*;
import javax.validation.constraints.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * A Customer.
 */
@Entity
@Table(name = "customer")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class Customer implements Serializable {
	private static final long serialVersionUID = 1L;
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@NotNull
	@Column(name = "first_name", nullable = false)
	private String firstName;
	@NotNull
	@Column(name = "last_name", nullable = false)
	private String lastName;
	@NotNull
	@Column(name = "phone", nullable = false)
	private String phone;
	@Column(name = "email")
	private String email;
	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt=Instant.now();
    @NotNull
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false)
    private SalesStatut status=SalesStatut.ACTIVE;

	@OneToMany(mappedBy = "customer")
	private Set<Payment> payments = new HashSet<>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getFirstName() {
		return firstName;
	}

	public Customer firstName(String firstName) {
		this.firstName = firstName;
		return this;
	}

    public SalesStatut getStatus() {
        return status;
    }

    public void setStatus(SalesStatut status) {
        this.status = status;
    }

    public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public Customer lastName(String lastName) {
		this.lastName = lastName;
		return this;
	}



    public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getPhone() {
		return phone;
	}

	public Customer phone(String phone) {
		this.phone = phone;
		return this;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getEmail() {
		return email;
	}

	public Customer email(String email) {
		this.email = email;
		return this;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Instant getCreatedAt() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
		return createdAt;
	}

	public Customer createdAt(Instant createdAt) {
		this.createdAt = createdAt;
		return this;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getUpdatedAt() {
		if (updatedAt == null) {
			this.updatedAt = Instant.now();
		}
		return updatedAt;
	}

	public Customer updatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
		return this;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}



	public Set<Payment> getPayments() {
		return payments;
	}

	public Customer payments(Set<Payment> payments) {
		this.payments = payments;
		return this;
	}

	public void setPayments(Set<Payment> payments) {
		this.payments = payments;
	}



	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof Customer)) {
			return false;
		}
		return id != null && id.equals(((Customer) o).id);
	}

	@Override
	public int hashCode() {
		return 31;
	}

	// prettier-ignore
	@Override
	public String toString() {
		return "Customer{" + "id=" + getId() + ", firstName='" + getFirstName() + "'" + ", lastName='" + getLastName()
				+ "'" + ", phone='" + getPhone() + "'" + ", email='" + getEmail() + "'" + ", createdAt='"
				+ getCreatedAt() + "'" + ", updatedAt='" + getUpdatedAt() + "'" + "}";
	}
}
