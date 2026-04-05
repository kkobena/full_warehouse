package com.kobe.warehouse.service.errors;

import com.kobe.warehouse.service.dto.RetourBonLotResolutionDTO;
import java.io.Serial;
import java.util.List;

/**
 * Exception levée quand un lot est associé à plusieurs fournisseurs possibles et qu'aucun
 * n'est défini comme principal. Le payload contient la liste des fournisseurs disponibles
 * pour que le frontend affiche un sélecteur.
 * L'errorKey "multipleFournisseurs" est détectable côté frontend.
 */
public class MultipleFournisseursException extends BadRequestAlertException {

    @Serial
    private static final long serialVersionUID = 1L;

    public MultipleFournisseursException(Integer lotId, List<RetourBonLotResolutionDTO.FournisseurSimple> fournisseurs) {
        super(
            "Plusieurs fournisseurs associés au produit du lot #" + lotId
            + ". Veuillez sélectionner le fournisseur à utiliser pour ce retour.",
            "multipleFournisseurs",
            fournisseurs
        );
    }
}

