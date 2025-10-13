package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.CodeGrilleRemise;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * A Remise.
 */
@Entity
@Table(
    name = "grille_remise",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "code" }, name = "grille_remise_code_un_index"),
        @UniqueConstraint(columnNames = { "code", "remise_produit_id" }, name = "remise_produit_id_code_un_index"),
    }
)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class GrilleRemise implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "enable", nullable = false, columnDefinition = "boolean default true")
    private boolean enable = true;

    @NotNull
    @Column(name = "remise_value", nullable = false)
    private Float remiseValue;

    @ManyToOne(optional = false)
    @NotNull
    @JoinColumn(name = "remise_produit_id", referencedColumnName = "id")
    private RemiseProduit remiseProduit;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "code", length = 10, nullable = false, unique = true)
    private CodeGrilleRemise code;

    @Transient
    private float tauxRemise;

    public RemiseProduit getRemiseProduit() {
        return remiseProduit;
    }

    public GrilleRemise setRemiseProduit(RemiseProduit remiseProduit) {
        this.remiseProduit = remiseProduit;
        return this;
    }

    public Float getRemiseValue() {
        return remiseValue;
    }

    public GrilleRemise setRemiseValue(Float remiseValue) {
        this.remiseValue = remiseValue;
        return this;
    }

    public Long getId() {
        return id;
    }

    public GrilleRemise setId(Long id) {
        this.id = id;
        return this;
    }

    public boolean isEnable() {
        return enable;
    }

    public GrilleRemise setEnable(boolean enable) {
        this.enable = enable;
        return this;
    }

    public CodeGrilleRemise getCode() {
        return code;
    }

    public GrilleRemise setCode(CodeGrilleRemise code) {
        this.code = code;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GrilleRemise)) {
            return false;
        }
        return id != null && id.equals(((GrilleRemise) o).id);
    }

    @Override
    public int hashCode() {
        return 31;
    }

    public float getTauxRemise() {
        if (Objects.nonNull(remiseValue)) {
            tauxRemise = remiseValue / 100;
        }
        return tauxRemise;
    }

    public void setTauxRemise(float tauxRemise) {
        this.tauxRemise = tauxRemise;
    }
}
