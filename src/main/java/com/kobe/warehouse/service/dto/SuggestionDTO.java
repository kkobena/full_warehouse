package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.*;
import com.kobe.warehouse.domain.enumeration.TypeSuggession;
import com.kobe.warehouse.service.dto.projection.SuggestionAggregator;
import java.time.LocalDateTime;

public class SuggestionDTO {

    private Integer id;

    private String suggessionReference;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String magasin;

    private String lastUserEdit;

    private TypeSuggession typeSuggession;
    private String fournisseurLibelle;
    private SuggestionAggregator suggestionAggregator;

    public SuggestionAggregator getSuggestionAggregator() {
        return suggestionAggregator;
    }

    public SuggestionDTO setSuggestionAggregator(SuggestionAggregator suggestionAggregator) {
        this.suggestionAggregator = suggestionAggregator;
        return this;
    }

    public Integer getId() {
        return id;
    }

    public SuggestionDTO setId(Integer id) {
        this.id = id;
        return this;
    }

    public String getSuggessionReference() {
        return suggessionReference;
    }

    public SuggestionDTO setSuggessionReference(String suggessionReference) {
        this.suggessionReference = suggessionReference;
        return this;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public SuggestionDTO setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public SuggestionDTO setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public String getMagasin() {
        return magasin;
    }

    public SuggestionDTO setMagasin(String magasin) {
        this.magasin = magasin;
        return this;
    }

    public String getLastUserEdit() {
        return lastUserEdit;
    }

    public SuggestionDTO setLastUserEdit(String lastUserEdit) {
        this.lastUserEdit = lastUserEdit;
        return this;
    }

    public TypeSuggession getTypeSuggession() {
        return typeSuggession;
    }

    public SuggestionDTO setTypeSuggession(TypeSuggession typeSuggession) {
        this.typeSuggession = typeSuggession;
        return this;
    }

    public String getFournisseurLibelle() {
        return fournisseurLibelle;
    }

    public SuggestionDTO setFournisseurLibelle(String fournisseurLibelle) {
        this.fournisseurLibelle = fournisseurLibelle;
        return this;
    }

    public SuggestionDTO(Suggestion suggestion) {
        this.id = suggestion.getId();
        this.suggessionReference = suggestion.getSuggessionReference();
        this.createdAt = suggestion.getCreatedAt();
        this.updatedAt = suggestion.getUpdatedAt();
        /* this.suggestionLines = suggestion.getSuggestionLines()
            .stream().map(e -> {
                FournisseurProduit fournisseurProduit = e.getFournisseurProduit();
                Produit produit = fournisseurProduit.getProduit();
                return new SuggestionLineDTO(e.getId(), e.getQuantity(), e.getCreatedAt(), e.getUpdatedAt(), produit.getLibelle(), fournisseurProduit.getCodeCip(), produit.getCodeEan());
            }).toList();*/
        this.typeSuggession = suggestion.getTypeSuggession();
        Fournisseur fournisseur = suggestion.getFournisseur();
        this.fournisseurLibelle = fournisseur.getLibelle();
    }
}
