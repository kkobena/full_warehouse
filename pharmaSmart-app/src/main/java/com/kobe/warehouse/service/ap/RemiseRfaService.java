package com.kobe.warehouse.service.ap;

import com.kobe.warehouse.service.dto.AvoirFournisseurRfaDTO;
import com.kobe.warehouse.service.dto.RemiseRfaFournisseurDTO;
import java.util.List;

public interface RemiseRfaService {

    List<RemiseRfaFournisseurDTO> getRfaFournisseurs();

    /**
     * Avancement vers le palier du <b>groupe</b> auquel appartient ce fournisseur.
     *
     * <p>Destine au bandeau de saisie d'une commande : l'information n'a de valeur qu'a
     * l'instant ou elle peut encore changer une decision, et non dans un ecran financier qu'on
     * ouvre apres coup.
     *
     * @param fournisseurId une agence ou un grossiste principal, indifferemment
     * @return {@code null} si le fournisseur est inconnu, ou si son groupe n'a aucune commande
     *         close sur l'exercice
     */
    RemiseRfaFournisseurDTO lireRfaDuGroupe(Integer fournisseurId);

    List<AvoirFournisseurRfaDTO> getAvoirsFournisseurs();
}
