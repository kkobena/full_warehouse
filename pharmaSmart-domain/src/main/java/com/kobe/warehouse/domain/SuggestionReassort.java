package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.StatutReassort;
import com.kobe.warehouse.domain.enumeration.TypeReassort;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A SuggestionReassort.
 */
@Entity
@Table(name = "suggestion_reassort", indexes = {@Index(columnList = "type_reassort", name = "type_reassort_index"),
    @Index(columnList = "statut", name = "suggestion_reassort_statut_index")
})
public class SuggestionReassort implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    @NotNull
    @Column(name = "reference", length = 20, nullable = false)
    private String reference;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @NotNull
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "reassort", cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<LigneReassort> ligneReassorts = new HashSet<>();

    @ManyToOne(optional = false)
    @NotNull
    private Magasin magasin;

    @ManyToOne
    @JoinColumn(name = "last_user_edit_id", referencedColumnName = "id")
    private AppUser lastUserEdit;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_reassort", length = 10)
    private TypeReassort typeReassort;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", length = 15)
    private StatutReassort statut = StatutReassort.OPEN;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getReference() {
        return reference;
    }

    public SuggestionReassort setReference(String suggessionReference) {
        this.reference = suggessionReference;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public SuggestionReassort createdAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Set<LigneReassort> getLigneReassorts() {
        return ligneReassorts;
    }

    public void setLigneReassorts(Set<LigneReassort> ligneReassorts) {
        this.ligneReassorts = ligneReassorts;
    }

    public TypeReassort getTypeReassort() {
        return typeReassort;
    }

    public void setTypeReassort(TypeReassort typeReassort) {
        this.typeReassort = typeReassort;
    }

    public StatutReassort getStatut() {
        return statut;
    }

    public void setStatut(StatutReassort statut) {
        this.statut = statut;
    }

    public Magasin getMagasin() {
        return magasin;
    }

    public void setMagasin(Magasin magasin) {
        this.magasin = magasin;
    }

    public AppUser getLastUserEdit() {
        return lastUserEdit;
    }

    public void setLastUserEdit(AppUser lastUserEdit) {
        this.lastUserEdit = lastUserEdit;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        SuggestionReassort that = (SuggestionReassort) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
