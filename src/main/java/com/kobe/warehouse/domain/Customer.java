package com.kobe.warehouse.domain;


import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.TypeAssure;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
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
    @NotEmpty
    @Column(name = "first_name", nullable = false)
    private String firstName;
    @NotNull
    @NotEmpty
    @Column(name = "last_name", nullable = false)
    private String lastName;
    @Column(name = "phone")
    private String phone;
    @Email
    @Column(name = "email")
    private String email;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
    @NotNull
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false)
    private Status status = Status.ENABLE;
    @OneToMany(mappedBy = "customer")
    private Set<Payment> payments = new HashSet<>();
    @NotNull
    @Column(name = "code", nullable = false, unique = true)
    private String code;
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type_assure", nullable = false,length = 15)
    private TypeAssure typeAssure ;

    public TypeAssure getTypeAssure() {
        return typeAssure;
    }

    public Customer setTypeAssure(TypeAssure typeAssure) {
        this.typeAssure = typeAssure;
        return this;
    }

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

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
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

    public String getCode() {
        return code;
    }

    public Customer setCode(String code) {
        this.code = code;
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
