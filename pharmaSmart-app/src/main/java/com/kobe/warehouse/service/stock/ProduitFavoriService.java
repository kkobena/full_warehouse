package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.service.stock.dto.FavoriSuggere;
import com.kobe.warehouse.service.stock.dto.ProduitSearch;
import java.util.List;

/** Grille des produits favoris du comptoir, propre au magasin de l'utilisateur connecté. */
public interface ProduitFavoriService {
    /** Les favoris au format de la recherche (stock compris), dans l'ordre de la grille. */
    List<ProduitSearch> lister();

    /**
     * Les produits les plus vendus au comptoir sur 30 jours, sans ordonnance et pas encore épinglés, pour aider à remplir la
     * grille. Une suggestion ne devient favori que par {@link #ajouter}.
     */
    List<FavoriSuggere> suggerer(int limite);

    /** Épingle un produit en dernière position ; sans effet s'il l'est déjà. */
    void ajouter(Integer produitId);

    /** Retire l'épingle ; sans effet si le produit n'en a pas. */
    void retirer(Integer produitId);

    /** Range la grille dans l'ordre reçu ; un identifiant qui n'est pas favori est ignoré, un favori oublié passe après. */
    void reordonner(List<Integer> produitIds);
}
