package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.DeliveryReceipt;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.domain.enumeration.ReceiptStatut;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public class DeliveryReceiptDTO {

    private final Long id;

    private final String numberTransaction;

    private final String sequenceBon;

    private final String receiptRefernce;

    private final LocalDate receiptDate;

    private final Integer discountAmount = 0;

    private final Integer receiptAmount;

    private final LocalDateTime createdDate;

    private final LocalDateTime modifiedDate;

    private final String createdUser;

    private final String modifiedUser;

    private final Long fournisseurId;
    private final String fournisseurLibelle;
    private final Integer netAmount;

    private final Integer taxAmount;

    private final List<DeliveryReceiptItemDTO> receiptItems;
    private final int itemSize;
    private final String orderReference;
    private final ReceiptStatut statut;

    public DeliveryReceiptDTO(DeliveryReceipt deliveryReceipt) {
        id = deliveryReceipt.getId();
        numberTransaction = deliveryReceipt.getNumberTransaction();
        sequenceBon = deliveryReceipt.getSequenceBon();
        receiptRefernce = deliveryReceipt.getReceiptReference();
        receiptDate = deliveryReceipt.getReceiptDate();
        receiptAmount = deliveryReceipt.getReceiptAmount();
        createdDate = deliveryReceipt.getCreatedDate();
        modifiedDate = deliveryReceipt.getModifiedDate();
        User user1 = deliveryReceipt.getCreatedUser();
        createdUser = String.format("%s. %s", user1.getFirstName().charAt(0), user1.getLastName());
        User user2 = deliveryReceipt.getModifiedUser();
        modifiedUser = String.format("%s. %s", user2.getFirstName().charAt(0), user2.getLastName());
        Fournisseur fournisseur = deliveryReceipt.getFournisseur();
        fournisseurId = fournisseur.getId();
        fournisseurLibelle = fournisseur.getLibelle();
        netAmount = deliveryReceipt.getNetAmount();
        taxAmount = deliveryReceipt.getTaxAmount();
        orderReference = deliveryReceipt.getOrderReference();
        receiptItems = deliveryReceipt
            .getReceiptItems()
            .stream()
            .map(DeliveryReceiptItemDTO::new)
            .sorted(Comparator.comparing(DeliveryReceiptItemDTO::getFournisseurProduitLibelle))
            .toList();
        itemSize = receiptItems.size();
        statut = deliveryReceipt.getReceiptStatut();
    }

    public Long getId() {
        return id;
    }

    public String getNumberTransaction() {
        return numberTransaction;
    }

    public String getSequenceBon() {
        return sequenceBon;
    }

    public String getReceiptRefernce() {
        return receiptRefernce;
    }

    public LocalDate getReceiptDate() {
        return receiptDate;
    }

    public Integer getDiscountAmount() {
        return discountAmount;
    }

    public Integer getReceiptAmount() {
        return receiptAmount;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public LocalDateTime getModifiedDate() {
        return modifiedDate;
    }

    public String getCreatedUser() {
        return createdUser;
    }

    public String getModifiedUser() {
        return modifiedUser;
    }

    public Long getFournisseurId() {
        return fournisseurId;
    }

    public String getFournisseurLibelle() {
        return fournisseurLibelle;
    }

    public Integer getNetAmount() {
        return netAmount;
    }

    public Integer getTaxAmount() {
        return taxAmount;
    }

    public List<DeliveryReceiptItemDTO> getReceiptItems() {
        return receiptItems;
    }

    public int getItemSize() {
        return itemSize;
    }

    public String getOrderReference() {
        return orderReference;
    }

    public ReceiptStatut getStatut() {
        return statut;
    }
}
