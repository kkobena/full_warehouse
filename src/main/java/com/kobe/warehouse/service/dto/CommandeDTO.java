package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.Commande;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class CommandeDTO extends CommandeWrapperDTO {

    private List<OrderLineDTO> orderLines;
    private UserDTO user;


    public CommandeDTO() {
    }

    public CommandeDTO(Commande commande) {
        super(commande);
        FournisseurDTO fournisseur = Optional.ofNullable(commande.getFournisseur()).map(FournisseurDTO::new).orElse(null);
        super.setFournisseur(fournisseur);
        super.setFournisseurId(fournisseur.getId());
        orderLines = commande
            .getOrderLines()
            .stream()
            .map(OrderLineDTO::new)
            .sorted(Comparator.comparing(OrderLineDTO::getUpdatedAt, Comparator.reverseOrder()))
            .toList();

        user = Optional.ofNullable(commande.getUser()).map(UserDTO::new).orElse(null);

        setTotalProduits(orderLines.size());
    }

    public List<OrderLineDTO> getOrderLines() {
        return orderLines;
    }

    public CommandeDTO setOrderLines(List<OrderLineDTO> orderLines) {
        this.orderLines = orderLines;
        return this;
    }


    public UserDTO getUser() {
        return user;
    }

    public CommandeDTO setUser(UserDTO user) {
        this.user = user;
        return this;
    }


}
