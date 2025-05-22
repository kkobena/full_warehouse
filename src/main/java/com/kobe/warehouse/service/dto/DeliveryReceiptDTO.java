package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public class DeliveryReceiptDTO {

    private final Long id;

    private final String receiptRefernce;

    private final LocalDate receiptDate;

    private final int discountAmount ;

    private final Integer receiptAmount;

    private final LocalDateTime createdDate;

    private final LocalDateTime modifiedDate;

    private final String createdUser;
    private final Long fournisseurId;
    private final String fournisseurLibelle;
    private final Integer netAmount;

    private final Integer taxAmount;

    private final List<DeliveryReceiptItemDTO> receiptItems;
    private final int itemSize;


    public DeliveryReceiptDTO(Commande commande) {
        id = commande.getId();
        discountAmount= commande.getDiscountAmount();
        receiptRefernce = commande.getReceiptReference();
        receiptDate = commande.getReceiptDate();
        receiptAmount = commande.getGrossAmount();
        createdDate = commande.getCreatedAt();
        modifiedDate = commande.getUpdatedAt();
        User user1 = commande.getUser();
        createdUser = String.format("%s. %s", user1.getFirstName().charAt(0), user1.getLastName());

        Fournisseur fournisseur = commande.getFournisseur();
        fournisseurId = fournisseur.getId();
        fournisseurLibelle = fournisseur.getLibelle();
        netAmount = commande.getHtAmount();
        taxAmount = commande.getTaxAmount();

        receiptItems = commande
            .getOrderLines()
            .stream()
            .map(DeliveryReceiptItemDTO::new)
            .sorted(Comparator.comparing(DeliveryReceiptItemDTO::getFournisseurProduitLibelle))
            .toList();
        itemSize = receiptItems.size();

    }

    public Long getId() {
        return id;
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


}
