package com.kobe.warehouse.service.impl;

import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.enumeration.ContrainteColisage;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.service.ColisageService;
import com.kobe.warehouse.service.dto.AjustementColisage;

import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ColisageServiceImpl implements ColisageService {

    @Override
    public Optional<AjustementColisage> ajusterLigne(OrderLine orderLine) {
        FournisseurProduit fournisseurProduit = orderLine.getFournisseurProduit();
        if (fournisseurProduit == null || orderLine.getQuantityRequested() == null) {
            return Optional.empty();
        }
        int demandee = orderLine.getQuantityRequested();
        int retenue = fournisseurProduit.appliquerColisage(demandee);
        if (retenue == demandee) {
            return Optional.empty();
        }
        orderLine.setQuantityRequested(retenue);
        return Optional.of(reporterAjustement(fournisseurProduit, demandee, retenue));
    }

    @Override
    public int ajusterQuantite(FournisseurProduit fournisseurProduit, int quantiteDemandee) {
        if (fournisseurProduit == null) {
            return quantiteDemandee;
        }
        return fournisseurProduit.appliquerColisage(quantiteDemandee);
    }

    private AjustementColisage reporterAjustement(FournisseurProduit fournisseurProduit, int demandee, int retenue) {
        int colis = lireColis(fournisseurProduit);
        int minimum = lireMinimum(fournisseurProduit);
        return new AjustementColisage(
            fournisseurProduit.getProduit() != null ? fournisseurProduit.getProduit().getLibelle() : null,
            demandee,
            retenue,
            colis,
            minimum,
            determinerContrainte(colis, minimum, retenue)
        );
    }

    /**
     * Dit laquelle des deux contraintes a emporté la décision, sans refaire le calcul de
     * {@link FournisseurProduit#appliquerColisage(int)} : l'arrondi au colis rend toujours un
     * multiple du colis, donc une quantité retenue égale au minimum sans en être un multiple ne
     * peut venir que du minimum. Quand les deux aboutissent au même nombre, le colisage est
     * désigné, parce qu'il renseigne davantage : il dit aussi combien de colis partent.
     */
    private ContrainteColisage determinerContrainte(int colis, int minimum, int retenue) {
        if (minimum > 0 && retenue == minimum && (colis <= 1 || minimum % colis != 0)) {
            return ContrainteColisage.QUANTITE_MINIMALE;
        }
        return colis > 1 ? ContrainteColisage.COLISAGE : ContrainteColisage.QUANTITE_MINIMALE;
    }

    private int lireColis(FournisseurProduit fournisseurProduit) {
        Integer colis = fournisseurProduit.getQteColis();
        return colis != null && colis > 1 ? colis : 1;
    }

    private int lireMinimum(FournisseurProduit fournisseurProduit) {
        Integer minimum = fournisseurProduit.getQteMinimaleCommande();
        return minimum != null && minimum > 0 ? minimum : 0;
    }
}
