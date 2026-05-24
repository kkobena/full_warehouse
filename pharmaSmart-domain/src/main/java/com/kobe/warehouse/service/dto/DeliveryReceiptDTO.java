package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.Fournisseur;

import java.util.Comparator;
import java.util.List;

public class DeliveryReceiptDTO extends CommandeWrapperDTO {

    private  Integer fournisseurId;
    private  String fournisseurLibelle;


    private final List<OrderLineDTO> orderLines;
    private  int itemSize;

    public DeliveryReceiptDTO(Commande commande) {
        this(commande, commande
            .getOrderLines()
            .stream()
            .map(OrderLineDTO::new)
            .sorted(Comparator.comparing(OrderLineDTO::getProduitLibelle))
            .toList());
    }


    public DeliveryReceiptDTO(Commande commande, List<OrderLineDTO> items) {
        super(commande);
        Fournisseur fournisseur = commande.getFournisseur();
        if (fournisseur != null) {
            fournisseurId = fournisseur.getId();
            fournisseurLibelle = fournisseur.getLibelle();
        }


        orderLines = items;
        itemSize = items.size();
    }

    @Override
    public DeliveryReceiptDTO setFournisseurId(Integer fournisseurId) {
        this.fournisseurId = fournisseurId;
        return this;
    }

    public DeliveryReceiptDTO setItemSize(int itemSize) {
        this.itemSize = itemSize;
        return this;
    }

    public DeliveryReceiptDTO setFournisseurLibelle(String fournisseurLibelle) {
        this.fournisseurLibelle = fournisseurLibelle;
        return this;
    }

    public Integer getFournisseurId() {
        return fournisseurId;
    }

    public String getFournisseurLibelle() {
        return fournisseurLibelle;
    }


    public List<OrderLineDTO> getOrderLines() {
        return orderLines;
    }

    public int getItemSize() {
        return itemSize;
    }
}
