package com.kobe.warehouse.service.dto;

import com.kobe.warehouse.domain.Commande;
import java.util.Optional;

public class CommandeLiteDTO extends CommandeWrapperDTO {

    private FournisseurDTO fournisseur;
    private UserDTO lastUserEdit;
    private int itemSize;

    public CommandeLiteDTO() {}

    public CommandeLiteDTO(Commande commande) {
        super(commande);
        fournisseur = Optional.ofNullable(commande.getFournisseur()).map(FournisseurDTO::new).orElse(null);
        lastUserEdit = Optional.ofNullable(commande.getUser()).map(UserDTO::user).orElse(null);
    }

    public CommandeLiteDTO(Commande commande, long count) {
        super(commande);
        fournisseur = Optional.ofNullable(commande.getFournisseur()).map(FournisseurDTO::new).orElse(null);
        lastUserEdit = Optional.ofNullable(commande.getUser()).map(UserDTO::user).orElse(null);
        itemSize = (int) count;
    }

    public int getItemSize() {
        return itemSize;
    }

    public CommandeLiteDTO setItemSize(int itemSize) {
        this.itemSize = itemSize;
        return this;
    }

    public FournisseurDTO getFournisseur() {
        return fournisseur;
    }

    public CommandeLiteDTO setFournisseur(FournisseurDTO fournisseur) {
        this.fournisseur = fournisseur;
        return this;
    }

    public UserDTO getLastUserEdit() {
        return lastUserEdit;
    }

    public CommandeLiteDTO setLastUserEdit(UserDTO lastUserEdit) {
        this.lastUserEdit = lastUserEdit;
        return this;
    }
}
