package com.kobe.warehouse.domain;

import com.kobe.warehouse.service.dto.FamilleProduitDTO;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;


/**
 * A FamilleProduit.
 */
@Entity
@Table(name = "famille_produit")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class FamilleProduit implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    private Long id;
    @Column(name = "code")
    private String code;
    @NotNull
    @Column(name = "libelle", nullable = false, unique = true)
    private String libelle;
    @OneToMany(mappedBy = "famille")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<Produit> produits = new HashSet<>();
    @ManyToOne(optional = false)
    private Categorie categorie;

    public FamilleProduit() {
    }

    public FamilleProduit(FamilleProduitDTO familleProduitDTO) {
        id = familleProduitDTO.getId();
        code = familleProduitDTO.getCode();
        libelle = familleProduitDTO.getLibelle();
        categorie = new Categorie().id(familleProduitDTO.getId());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public FamilleProduit code(String code) {
        this.code = code;
        return this;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public FamilleProduit libelle(String libelle) {
        this.libelle = libelle;
        return this;
    }

    public Set<Produit> getProduits() {
        return produits;
    }

    public void setProduits(Set<Produit> produits) {
        this.produits = produits;
    }

    public FamilleProduit produits(Set<Produit> produits) {
        this.produits = produits;
        return this;
    }

    public Categorie getCategorie() {
        return categorie;
    }

    public void setCategorie(Categorie categorie) {
        this.categorie = categorie;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FamilleProduit)) {
            return false;
        }
        return id != null && id.equals(((FamilleProduit) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "FamilleProduit{" +
            "id=" + getId() +
            ", code='" + getCode() + "'" +
            ", libelle='" + getLibelle() + "'" +

            "}";
    }
}
