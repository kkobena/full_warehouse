package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.TypeSubstitut;
import jakarta.persistence.*;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "substitut", uniqueConstraints = { @UniqueConstraint(columnNames = { "produit_id", "substitut_id" }) })
public class Substitut implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false,fetch = FetchType.LAZY)
    private Produit produit;

    @ManyToOne(optional = false)
    private Produit substitut;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_substitut", nullable = false)
    private TypeSubstitut type;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Produit getProduit() {
        return produit;
    }

    public void setProduit(Produit produit) {
        this.produit = produit;
    }

    public Produit getSubstitut() {
        return substitut;
    }

    public void setSubstitut(Produit substitut) {
        this.substitut = substitut;
    }

    public TypeSubstitut getType() {
        return type;
    }

    public void setType(TypeSubstitut type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Substitut substitut = (Substitut) o;
        return Objects.equals(id, substitut.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
