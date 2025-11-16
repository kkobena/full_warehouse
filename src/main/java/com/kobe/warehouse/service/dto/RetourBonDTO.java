package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.Fournisseur;
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
    private LocalDate receiptDate;
    private String receiptReference;
    private String fournisseurLibelle;
    private List<RetourBonItemDTO> retourBonItems = new ArrayList<>();

    public RetourBonDTO() {}

    public RetourBonDTO(RetourBon retourBon) {
        this.id = retourBon.getId();
        this.dateMtv = retourBon.getDateMtv();
        this.user = new UserDTO(retourBon.getUser());
        this.statut = retourBon.getStatut();
        this.commentaire = retourBon.getCommentaire();
        Commande commande = retourBon.getCommande();
        Fournisseur fournisseur = commande.getFournisseur();
        this.commandeId = commande.getId().getId();
        this.commandeOrderDate = commande.getOrderDate();
        this.receiptDate = commande.getReceiptDate();
        this.receiptReference = commande.getReceiptReference();
        this.fournisseurLibelle = fournisseur.getLibelle();
        this.retourBonItems = retourBon.getRetourBonItems().stream().map(RetourBonItemDTO::new).toList();
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

    public LocalDate getReceiptDate() {
        return receiptDate;
    }

    public void setReceiptDate(LocalDate receiptDate) {
        this.receiptDate = receiptDate;
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

    public String getReceiptReference() {
        return receiptReference;
    }

    public RetourBonDTO setReceiptReference(String receiptReference) {
        this.receiptReference = receiptReference;
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
