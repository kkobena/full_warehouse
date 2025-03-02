package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.Commande;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CommandeEntryDTO extends CommandeWrapperDTO {

    private List<OrderLineDTO> orderLines;
    private FournisseurDTO fournisseur;

    public CommandeEntryDTO(Commande commande) {
        super(commande);
        orderLines = commande
            .getOrderLines()
            .stream()
            .map(OrderLineDTO::new)
            .sorted(Comparator.comparing(OrderLineDTO::getProduitLibelle))
            .collect(Collectors.toList());
        fournisseur = Optional.ofNullable(commande.getFournisseur()).map(FournisseurDTO::new).orElse(null);
    }

    public FournisseurDTO getFournisseur() {
        return fournisseur;
    }

    public CommandeEntryDTO setFournisseur(FournisseurDTO fournisseur) {
        this.fournisseur = fournisseur;
        return this;
    }

    public List<OrderLineDTO> getOrderLines() {
        return orderLines;
    }

    public CommandeEntryDTO setOrderLines(List<OrderLineDTO> orderLines) {
        this.orderLines = orderLines;
        return this;
    }
}
