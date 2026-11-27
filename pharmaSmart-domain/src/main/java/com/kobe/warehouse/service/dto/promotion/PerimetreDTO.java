package com.kobe.warehouse.service.dto.promotion;

import java.util.List;

/**
 * Les articles actifs d'un rayon ou d'une famille, proposés en bloc au formulaire d'une promotion.
 * Le rayon ou la famille n'est qu'un moyen de remplir la liste : c'est elle qui est enregistrée, et
 * chaque article y passe les contrôles ordinaires.
 *
 * @param articles tous les articles actifs, prix de référence et prix d'achat connus
 * @param administres les libellés de ceux qui sont à prix administré (TVA 0) : ajoutés, mais signalés —
 *     c'est au pharmacien de décider s'il les garde
 */
public record PerimetreDTO(List<Article> articles, List<String> administres) {

    /**
     * @param prixAchat chez le fournisseur principal, à défaut le prix de revient : le formulaire
     *     signale d'avance une vente à perte
     * @param prixAdministre médicament à prix administré (TVA 0), signalé dans la liste des articles
     */
    public record Article(
            Integer produitId, String libelle, String codeCip, int prix, int prixAchat, boolean prixAdministre) {

        public Article(Integer produitId, String libelle, String codeCip, int prix, int prixAchat) {
            this(produitId, libelle, codeCip, prix, prixAchat, false);
        }
    }
}
