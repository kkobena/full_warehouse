package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.enumeration.MotifBed;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class BedSummaryDTO {

    private Integer id;
    private LocalDate orderDate;
    private String receiptReference;
    private MotifBed motifBed;
    private String motifLabel;
    private String fournisseurLibelle;
    private OrderStatut orderStatus;
    private int lignesCount;
    private int grossAmount;
    private LocalDateTime createdAt;

    public BedSummaryDTO() {}

    public BedSummaryDTO(Commande c) {
        this.id = c.getId().getId();
        this.orderDate = c.getOrderDate();
        this.receiptReference = c.getReceiptReference();
        this.motifBed = c.getMotifBed();
        this.motifLabel = c.getMotifBed() != null ? c.getMotifBed().getLabel() : null;
        this.orderStatus = c.getOrderStatus();
        this.grossAmount = c.getGrossAmount() != null ? c.getGrossAmount() : 0;
        this.createdAt = c.getCreatedAt();
        this.lignesCount = c.getOrderLines() != null ? c.getOrderLines().size() : 0;
        if (c.getFournisseur() != null) {
            this.fournisseurLibelle = c.getFournisseur().getLibelle();
        }
    }

    public Integer getId() { return id; }
    public BedSummaryDTO setId(Integer id) { this.id = id; return this; }

    public LocalDate getOrderDate() { return orderDate; }
    public BedSummaryDTO setOrderDate(LocalDate orderDate) { this.orderDate = orderDate; return this; }

    public String getReceiptReference() { return receiptReference; }
    public BedSummaryDTO setReceiptReference(String receiptReference) { this.receiptReference = receiptReference; return this; }

    public MotifBed getMotifBed() { return motifBed; }
    public BedSummaryDTO setMotifBed(MotifBed motifBed) { this.motifBed = motifBed; return this; }

    public String getMotifLabel() { return motifLabel; }
    public BedSummaryDTO setMotifLabel(String motifLabel) { this.motifLabel = motifLabel; return this; }

    public String getFournisseurLibelle() { return fournisseurLibelle; }
    public BedSummaryDTO setFournisseurLibelle(String fournisseurLibelle) { this.fournisseurLibelle = fournisseurLibelle; return this; }

    public OrderStatut getOrderStatus() { return orderStatus; }
    public BedSummaryDTO setOrderStatus(OrderStatut orderStatus) { this.orderStatus = orderStatus; return this; }

    public int getLignesCount() { return lignesCount; }
    public BedSummaryDTO setLignesCount(int lignesCount) { this.lignesCount = lignesCount; return this; }

    public int getGrossAmount() { return grossAmount; }
    public BedSummaryDTO setGrossAmount(int grossAmount) { this.grossAmount = grossAmount; return this; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public BedSummaryDTO setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
}
