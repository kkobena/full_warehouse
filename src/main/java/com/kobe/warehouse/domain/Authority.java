package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * An authority (a security role) used by Spring Security.
 */
@Entity
@Table(name = "authority")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Authority implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull
    @Size(max = 50)
    @Id
    @Column(length = 50)
    private String name;

    @Column(length = 100)
    private String libelle;

    @JsonIgnore
    @ManyToMany
    @JoinTable(
        name = "authority_menu",
        joinColumns = { @JoinColumn(name = "authority_name", referencedColumnName = "name") },
        inverseJoinColumns = { @JoinColumn(name = "menu_id", referencedColumnName = "id") }
    )
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<Menu> menus = new HashSet<>();

    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @OneToMany(mappedBy = "authority", cascade = { CascadeType.REMOVE })
    private Set<AuthorityPrivilege> privileges = new HashSet<>();

    public Set<AuthorityPrivilege> getPrivileges() {
        return privileges;
    }

    public Authority setPrivileges(Set<AuthorityPrivilege> privilege) {
        this.privileges = privilege;
        return this;
    }

    public Set<Menu> getMenus() {
        return menus;
    }

    public Authority setMenus(Set<Menu> menus) {
        this.menus = menus;
        return this;
    }

    public String getLibelle() {
        return libelle;
    }

    public Authority setLibelle(String libelle) {
        this.libelle = libelle;
        return this;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Authority)) {
            return false;
        }
        return Objects.equals(name, ((Authority) o).name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Authority{" + "name='" + name + '\'' + "}";
    }
}
