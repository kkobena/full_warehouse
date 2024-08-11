package com.kobe.warehouse.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "authority_privilege", uniqueConstraints = { @UniqueConstraint(columnNames = { "privilege_name", "authority_name" }) })
public class AuthorityPrivilege implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @NotNull
    private Privilege privilege;

    @ManyToOne(optional = false)
    @NotNull
    private Authority authority;

    public Long getId() {
        return id;
    }

    public AuthorityPrivilege setId(Long id) {
        this.id = id;
        return this;
    }

    public @NotNull Privilege getPrivilege() {
        return privilege;
    }

    public AuthorityPrivilege setPrivilege(@NotNull Privilege privilege) {
        this.privilege = privilege;
        return this;
    }

    public @NotNull Authority getAuthority() {
        return authority;
    }

    public AuthorityPrivilege setAuthority(@NotNull Authority authority) {
        this.authority = authority;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AuthorityPrivilege that = (AuthorityPrivilege) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
