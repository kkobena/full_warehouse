package com.kobe.warehouse.domain.nav;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Ordre personnalisé d'un NavItem pour un utilisateur spécifique.
 */
@Entity
@Table(
    name = "nav_item_user_order",
    uniqueConstraints = { @UniqueConstraint(columnNames = { "user_login", "nav_item_id" }) }
)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class NavItemUserOrder implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Column(name = "user_login", nullable = false, length = 50)
    private String userLogin;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "nav_item_id", nullable = false)
    private NavItem navItem;

    @Column(name = "ordre", nullable = false)
    private int ordre;

    public Integer getId() {
        return id;
    }

    public NavItemUserOrder setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getUserLogin() {
        return userLogin;
    }

    public NavItemUserOrder setUserLogin(String userLogin) {
        this.userLogin = userLogin;
        return this;
    }

    public NavItem getNavItem() {
        return navItem;
    }

    public NavItemUserOrder setNavItem(NavItem navItem) {
        this.navItem = navItem;
        return this;
    }

    public int getOrdre() {
        return ordre;
    }

    public NavItemUserOrder setOrdre(int ordre) {
        this.ordre = ordre;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NavItemUserOrder)) return false;
        return id != null && id.equals(((NavItemUserOrder) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }
}

