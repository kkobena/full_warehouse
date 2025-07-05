package com.kobe.warehouse.domain;

import com.kobe.warehouse.domain.enumeration.StatutSuggession;
import com.kobe.warehouse.domain.enumeration.TypeSuggession;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * A Commande.
 */
@Entity
@Table(name = "suggestion", indexes = { @Index(columnList = "type_suggession", name = "type_suggession_index") })
public class Suggestion implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "suggession_reference")
    private String suggessionReference;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "suggestion", cascade = { CascadeType.PERSIST, CascadeType.REMOVE })
    private Set<SuggestionLine> suggestionLines = new HashSet<>();

    @ManyToOne(optional = false)
    @NotNull
    private Magasin magasin;

    @ManyToOne
    private User lastUserEdit;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_suggession", length = 10)
    private TypeSuggession typeSuggession;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", length = 15)
    private StatutSuggession statut = StatutSuggession.OPEN;

    @ManyToOne(optional = false)
    @NotNull
    private Fournisseur fournisseur;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSuggessionReference() {
        return suggessionReference;
    }

    public Suggestion setSuggessionReference(String suggessionReference) {
        this.suggessionReference = suggessionReference;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Suggestion createdAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Set<SuggestionLine> getSuggestionLines() {
        return suggestionLines;
    }

    public void setSuggestionLines(Set<SuggestionLine> suggestionLines) {
        this.suggestionLines = suggestionLines;
    }

    public Magasin getMagasin() {
        return magasin;
    }

    public void setMagasin(Magasin magasin) {
        this.magasin = magasin;
    }

    public User getLastUserEdit() {
        return lastUserEdit;
    }

    public void setLastUserEdit(User lastUserEdit) {
        this.lastUserEdit = lastUserEdit;
    }

    public TypeSuggession getTypeSuggession() {
        return typeSuggession;
    }

    public void setTypeSuggession(TypeSuggession typeSuggession) {
        this.typeSuggession = typeSuggession;
    }

    public StatutSuggession getStatut() {
        return statut;
    }

    public Suggestion setStatut(StatutSuggession statut) {
        this.statut = statut;
        return this;
    }

    public Fournisseur getFournisseur() {
        return fournisseur;
    }

    public void setFournisseur(Fournisseur fournisseur) {
        this.fournisseur = fournisseur;
    }
}
