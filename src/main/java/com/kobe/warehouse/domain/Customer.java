package com.kobe.warehouse.domain;


import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.TypeAssure;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Getter;

/**
 * A Customer.
 */
@Entity
@Table(name = "customer")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class Customer implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Getter
    @NotNull
    @NotEmpty
    @Column(name = "first_name", nullable = false)
    private String firstName;
    @Getter
    @NotNull
    @NotEmpty
    @Column(name = "last_name", nullable = false)
    private String lastName;
    @Getter
    @Column(name = "phone")
    private String phone;
    @Getter
    @Email
    @Column(name = "email")
    private String email;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();
    @Getter
    @NotNull
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false)
    private Status status = Status.ENABLE;
    @Getter
    @OneToMany(mappedBy = "customer")
    private Set<Payment> payments = new HashSet<>();
    @Getter
    @NotNull
    @Column(name = "code", nullable = false, unique = true)
    private String code;
    @Getter
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type_assure", nullable = false, length = 15)
    private TypeAssure typeAssure;

    public Customer setTypeAssure(TypeAssure typeAssure) {
        this.typeAssure = typeAssure;
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public Customer firstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Customer lastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public Customer setCode(String code) {
        this.code = code;
        return this;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Customer phone(String phone) {
        this.phone = phone;
        return this;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Customer email(String email) {
        this.email = email;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Customer createdAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public LocalDateTime getUpdatedAt() {
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Customer updatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public void setPayments(Set<Payment> payments) {
        this.payments = payments;
    }

    public Customer payments(Set<Payment> payments) {
        this.payments = payments;
        return this;
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
