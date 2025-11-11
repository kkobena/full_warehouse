package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.RetourBon;
import com.kobe.warehouse.domain.enumeration.RetourStatut;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RetourBonDTO {
    private Integer id;
    private LocalDateTime dateMtv;
    private UserDTO user;
    private RetourStatut statut;
    private String commentaire;
    private Integer commandeId;
    private LocalDate commandeOrderDate;
    private String commandeOrderReference;
    private String fournisseurLibelle;
    private List<RetourBonItemDTO> retourBonItems = new ArrayList<>();

    public RetourBonDTO() {
    }

    public RetourBonDTO(RetourBon retourBon) {
        this.id = retourBon.getId();
        this.dateMtv = retourBon.getDateMtv();
        this.user = new UserDTO(retourBon.getUser());
        this.statut = retourBon.getStatut();
        this.commentaire = retourBon.getCommentaire();
        if (retourBon.getCommande() != null) {
            this.commandeId = retourBon.getCommande().getId().getId();
            this.commandeOrderDate = retourBon.getCommande().getId().getOrderDate();
            this.commandeOrderReference = retourBon.getCommande().getOrderReference();
            if (retourBon.getCommande().getFournisseur() != null) {
                this.fournisseurLibelle = retourBon.getCommande().getFournisseur().getLibelle();
            }
        }
        this.retourBonItems = retourBon.getRetourBonItems()
            .stream()
            .map(RetourBonItemDTO::new)
            .toList();
    }

    public Integer getId() {
        return id;
    }

    public RetourBonDTO setId(Integer id) {
        this.id = id;
        return this;
    }

    public LocalDateTime getDateMtv() {
        return dateMtv;
    }

    public RetourBonDTO setDateMtv(LocalDateTime dateMtv) {
        this.dateMtv = dateMtv;
        return this;
    }

    public UserDTO getUser() {
        return user;
    }

    public RetourBonDTO setUser(UserDTO user) {
        this.user = user;
        return this;
    }

    public RetourStatut getStatut() {
        return statut;
    }

    public RetourBonDTO setStatut(RetourStatut statut) {
        this.statut = statut;
        return this;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public RetourBonDTO setCommentaire(String commentaire) {
        this.commentaire = commentaire;
        return this;
    }

    public Integer getCommandeId() {
        return commandeId;
    }

    public RetourBonDTO setCommandeId(Integer commandeId) {
        this.commandeId = commandeId;
        return this;
    }

    public LocalDate getCommandeOrderDate() {
        return commandeOrderDate;
    }

    public RetourBonDTO setCommandeOrderDate(LocalDate commandeOrderDate) {
        this.commandeOrderDate = commandeOrderDate;
        return this;
    }

    public String getCommandeOrderReference() {
        return commandeOrderReference;
    }

    public RetourBonDTO setCommandeOrderReference(String commandeOrderReference) {
        this.commandeOrderReference = commandeOrderReference;
        return this;
    }

    public String getFournisseurLibelle() {
        return fournisseurLibelle;
    }

    public RetourBonDTO setFournisseurLibelle(String fournisseurLibelle) {
        this.fournisseurLibelle = fournisseurLibelle;
        return this;
    }

    public List<RetourBonItemDTO> getRetourBonItems() {
        return retourBonItems;
    }

    public RetourBonDTO setRetourBonItems(List<RetourBonItemDTO> retourBonItems) {
        this.retourBonItems = retourBonItems;
        return this;
    }
}
