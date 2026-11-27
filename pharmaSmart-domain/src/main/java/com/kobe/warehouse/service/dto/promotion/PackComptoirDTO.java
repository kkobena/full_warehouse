package com.kobe.warehouse.service.dto.promotion;

import java.util.List;

/**
 * Un pack lu au scan de son code-barres, au comptoir : de quoi ajouter ses articles au panier, chacun
 * sur sa ligne et à sa quantité. Le prix du pack n'est qu'informatif ici — c'est le moteur qui pose la
 * remise, une fois le pack complet dans le panier.
 */
public record PackComptoirDTO(Long promotionId, String libelle, int prix, List<Composant> composants) {

    /**
     * Un article du pack, avec les champs que le comptoir attend d'un produit recherché ({@code
     * itemQty}, {@code parentId} : le déconditionnement).
     */
    public record Composant(
            Integer produitId, String libelle, int regularUnitPrice, int quantite, int itemQty, Integer parentId) {}
}
