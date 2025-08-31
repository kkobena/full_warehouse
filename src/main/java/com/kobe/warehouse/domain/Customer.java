package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.TypeAssure;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * A Customer.
 */
@Entity
@Table(name = "customer",
    indexes = {
        @Index(columnList = "first_name", name = "client_first_name_index"),
        @Index(columnList = "status", name = "client_status_index"),
        @Index(columnList = "last_name", name = "client_last_name_index")

    }
)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class Customer implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @ManyToOne
    @JoinColumn(name = "remise_client_id", referencedColumnName = "id")
    private RemiseClient remiseClient;


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
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.ENABLE;

    @NotNull
    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type_assure", nullable = false, length = 15)
    private TypeAssure typeAssure;

    public RemiseClient getRemiseClient() {
        return remiseClient;
    }

    public void setRemiseClient(RemiseClient remiseClient) {
        this.remiseClient = remiseClient;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public @NotNull @NotEmpty String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public @NotNull @NotEmpty String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public @Email String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public @NotNull Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public @NotNull String getCode() {
        return code;
    }

    public Customer setCode(String code) {
        this.code = code;
        return this;
    }

    public @NotNull TypeAssure getTypeAssure() {
        return typeAssure;
    }

    public Customer setTypeAssure(TypeAssure typeAssure) {
        this.typeAssure = typeAssure;
        return this;
    }

    public Customer firstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public Customer lastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public Customer phone(String phone) {
        this.phone = phone;
        return this;
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
        return "Customer{"
            + "id="
            + getId()
            + ", firstName='"
            + getFirstName()
            + "'"
            + ", lastName='"
            + getLastName()
            + "'"
            + ", phone='"
            + getPhone()
            + "'"
            + ", email='"
            + getEmail()
            + "'"
            + ", createdAt='"
            + getCreatedAt()
            + "'"
            + ", updatedAt='"
            + getUpdatedAt()
            + "'"
            + "}";
    }
}
