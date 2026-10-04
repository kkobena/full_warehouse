package com.kobe.warehouse.service.stock;

import com.kobe.warehouse.service.stock.dto.SubstitutPropose;
import java.util.List;

/** Équivalents à proposer au comptoir quand un produit manque ou que le client en demande un moins cher. */
public interface SubstitutionComptoirService {
    /**
     * Les substituts du produit qui ont du stock au rayon du magasin de l'utilisateur connecté : les génériques avant les
     * substituts thérapeutiques, et du moins cher au plus cher dans chaque groupe.
     */
    List<SubstitutPropose> lireSubstitutsDisponibles(Integer produitId);
}
