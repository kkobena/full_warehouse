package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.enumeration.MotifBed;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class BedDTO {

    private Integer id;
    private LocalDate orderDate;
    private String receiptReference;
    private MotifBed motifBed;
    private String commentaireBed;
    private Integer fournisseurId;
    private String fournisseurLibelle;
    private OrderStatut orderStatus;
    private List<BedLigneDTO> lignes;
    private int grossAmount;
    private LocalDateTime createdAt;

    public BedDTO() {}

    public BedDTO(Commande c) {
        this.id = c.getId().getId();
        this.orderDate = c.getOrderDate();
        this.receiptReference = c.getReceiptReference();
        this.motifBed = c.getMotifBed();
        this.commentaireBed = c.getCommentaireBed();
        this.orderStatus = c.getOrderStatus();
        this.grossAmount = c.getGrossAmount() != null ? c.getGrossAmount() : 0;
        this.createdAt = c.getCreatedAt();
        if (c.getFournisseur() != null) {
            this.fournisseurId = c.getFournisseur().getId();
            this.fournisseurLibelle = c.getFournisseur().getLibelle();
        }
    }

    public Integer getId() { return id; }
    public BedDTO setId(Integer id) { this.id = id; return this; }

    public LocalDate getOrderDate() { return orderDate; }
    public BedDTO setOrderDate(LocalDate orderDate) { this.orderDate = orderDate; return this; }

    public String getReceiptReference() { return receiptReference; }
    public BedDTO setReceiptReference(String receiptReference) { this.receiptReference = receiptReference; return this; }

    public MotifBed getMotifBed() { return motifBed; }
    public BedDTO setMotifBed(MotifBed motifBed) { this.motifBed = motifBed; return this; }

    public String getCommentaireBed() { return commentaireBed; }
    public BedDTO setCommentaireBed(String commentaireBed) { this.commentaireBed = commentaireBed; return this; }

    public Integer getFournisseurId() { return fournisseurId; }
    public BedDTO setFournisseurId(Integer fournisseurId) { this.fournisseurId = fournisseurId; return this; }

    public String getFournisseurLibelle() { return fournisseurLibelle; }
    public BedDTO setFournisseurLibelle(String fournisseurLibelle) { this.fournisseurLibelle = fournisseurLibelle; return this; }

    public OrderStatut getOrderStatus() { return orderStatus; }
    public BedDTO setOrderStatus(OrderStatut orderStatus) { this.orderStatus = orderStatus; return this; }

    public List<BedLigneDTO> getLignes() { return lignes; }
    public BedDTO setLignes(List<BedLigneDTO> lignes) { this.lignes = lignes; return this; }

    public int getGrossAmount() { return grossAmount; }
    public BedDTO setGrossAmount(int grossAmount) { this.grossAmount = grossAmount; return this; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public BedDTO setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
}
