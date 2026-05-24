package com.kobe.warehouse.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import java.time.LocalDateTime;

/**
 * A SuggestionLine.
 */
@Entity
@Table(name = "suggestion_line", uniqueConstraints = { @UniqueConstraint(columnNames = { "suggestion_id", "fournisseur_produit_id" }) })
public class SuggestionLine implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "quantity")
    private Integer quantity;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    /**
     * true = quantité modifiée manuellement par le pharmacien.
     * Le batch {@code creerSuggestionBatch()} ne touche pas ces lignes
     * tant que ce flag est actif. Réinitialisé via l'UI ("Réinitialiser qté").
     */
    @Column(name = "quantite_modifiee_manuel", nullable = false)
    private boolean quantiteModifieeManuel = false;

    @ManyToOne(optional = false)
    @JsonIgnoreProperties(value = "suggestionLines", allowSetters = true)
    private Suggestion suggestion;

    @ManyToOne(optional = false)
    @JoinColumn(name = "fournisseur_produit_id", referencedColumnName = "id")
    private FournisseurProduit fournisseurProduit;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isQuantiteModifieeManuel() {
        return quantiteModifieeManuel;
    }

    public void setQuantiteModifieeManuel(boolean quantiteModifieeManuel) {
        this.quantiteModifieeManuel = quantiteModifieeManuel;
    }

    public Suggestion getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(Suggestion suggestion) {
        this.suggestion = suggestion;
    }

    public FournisseurProduit getFournisseurProduit() {
        return fournisseurProduit;
    }

    public void setFournisseurProduit(FournisseurProduit fournisseurProduit) {
        this.fournisseurProduit = fournisseurProduit;
    }
}
