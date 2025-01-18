package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.CodeGrilleRemise;
import com.kobe.warehouse.domain.enumeration.Status;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * A Remise.
 */
@Entity
@Table(name = "grille_remise", uniqueConstraints = { @UniqueConstraint(columnNames = { "remise_produit_id", "code" }) })
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

    @NotNull
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false)
    private Status status = Status.ENABLE;

    @ManyToOne(optional = false)
    @NotNull
    private RemiseProduit remiseProduit;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "code", length = 10, nullable = false)
    private CodeGrilleRemise code;

    @Transient
    private float tauxRemise;

    public RemiseProduit getRemiseProduit() {
        return remiseProduit;
    }

    public void setRemiseProduit(RemiseProduit remiseProduit) {
        this.remiseProduit = remiseProduit;
    }

    public Float getRemiseValue() {
        return remiseValue;
    }

    public void setRemiseValue(Float remiseValue) {
        this.remiseValue = remiseValue;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
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
