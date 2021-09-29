package com.kobe.warehouse.domain;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * A Tva.
 */
@Entity
@Table(name = "tva",uniqueConstraints = { @UniqueConstraint(columnNames = { "taux" }) })
public class Tva implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;

    @NotNull
    @Column(name = "taux", nullable = false,unique = true)
    private Integer taux=0;
    @OneToMany(mappedBy = "tva")
    private Set<Produit> produits = new HashSet<>();
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getTaux() {
        return taux;
    }

    public Tva taux(Integer taux) {
        this.taux = taux;
        return this;
    }

    public void setTaux(Integer taux) {
        this.taux = taux;
    }

    public Set<Produit> getProduits() {
        return produits;
    }

    public Tva produits(Set<Produit> produits) {
        this.produits = produits;
        return this;
    }



    public void setProduits(Set<Produit> produits) {
        this.produits = produits;
    }
    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Tva)) {
            return false;
        }
        return id != null && id.equals(((Tva) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Tva{" +
            "id=" + getId() +
            ", taux=" + getTaux() +
            "}";
    }
}
