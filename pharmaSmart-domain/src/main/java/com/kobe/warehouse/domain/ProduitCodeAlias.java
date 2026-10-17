package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.kobe.warehouse.domain.enumeration.CodeAliasSource;
import com.kobe.warehouse.domain.enumeration.CodeAliasType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Code alternatif permettant de retrouver un produit à partir d'un scan : GTIN du fabricant,
 * EAN d'une autre boîte, code interne d'un grossiste.
 *
 * <p>Complète {@code fournisseur_produit.code_cip} / {@code code_ean} et
 * {@code produit.code_ean_labo} sans les recopier. L'unicité est posée par produit et non sur le
 * code seul : un même code peut viser plusieurs produits, auquel cas l'utilisateur tranche.
 */
@Entity
@Table(name = "produit_code_alias", uniqueConstraints = { @UniqueConstraint(columnNames = { "produit_id", "code" }) })
public class ProduitCodeAlias implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @NotNull
    @JsonIgnoreProperties(value = "codesAlias", allowSetters = true)
    private Produit produit;

    @NotNull
    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "code_type", nullable = false, length = 20)
    private CodeAliasType codeType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private CodeAliasSource source = CodeAliasSource.MANUAL;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "created_by", length = 50)
    private String createdBy;

    public ProduitCodeAlias() {}

    public Integer getId() {
        return id;
    }

    public ProduitCodeAlias setId(Integer id) {
        this.id = id;
        return this;
    }

    public Produit getProduit() {
        return produit;
    }

    public ProduitCodeAlias setProduit(Produit produit) {
        this.produit = produit;
        return this;
    }

    public String getCode() {
        return code;
    }

    public ProduitCodeAlias setCode(String code) {
        this.code = code;
        return this;
    }

    public CodeAliasType getCodeType() {
        return codeType;
    }

    public ProduitCodeAlias setCodeType(CodeAliasType codeType) {
        this.codeType = codeType;
        return this;
    }

    public CodeAliasSource getSource() {
        return source;
    }

    public ProduitCodeAlias setSource(CodeAliasSource source) {
        this.source = source;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public ProduitCodeAlias setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public ProduitCodeAlias setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
        return this;
    }
}
