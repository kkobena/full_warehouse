package com.kobe.warehouse.service.pilotage.alertes.regles;

import com.kobe.warehouse.domain.enumeration.DroitPilotage;
import com.kobe.warehouse.domain.enumeration.GraviteAlerte;
import com.kobe.warehouse.domain.enumeration.IndicateurPilotage;
import com.kobe.warehouse.service.dto.pilotage.AlertePilotageDTO;
import com.kobe.warehouse.service.dto.pilotage.LigneRemiseDTO;
import com.kobe.warehouse.service.dto.pilotage.RemisesRentabiliteDTO;
import com.kobe.warehouse.service.pilotage.RemisesPilotageService;
import com.kobe.warehouse.service.pilotage.alertes.RegleAlerte;
import com.kobe.warehouse.service.pilotage.calcul.RequetesAlertes;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Vendeurs dont le taux de remise du mois dépasse le multiple fixé du taux de l'équipe (le signalement de l'onglet Remises). */
@Component
@Order(5)
public class RegleRemisesAnormales implements RegleAlerte {

    private final RemisesPilotageService remisesPilotageService;

    public RegleRemisesAnormales(RemisesPilotageService remisesPilotageService) {
        this.remisesPilotageService = remisesPilotageService;
    }

    @Override
    public DroitPilotage lireDroit() {
        return DroitPilotage.RENTABILITE_REMISES;
    }

    @Override
    public List<AlertePilotageDTO> evaluer(LocalDate aujourdhui) {
        RemisesRentabiliteDTO remises = remisesPilotageService.analyserRemises(RequetesAlertes.lireMoisADate(aujourdhui, List.of(IndicateurPilotage.TAUX_REMISE)));
        if (remises.vendeurs() == null) {
            return List.of();
        }
        List<String> vendeurs = new ArrayList<>();
        for (LigneRemiseDTO vendeur : remises.vendeurs()) {
            if (vendeur.alerte()) {
                vendeurs.add(vendeur.libelle() + " (" + RequetesAlertes.formaterDecimal(vendeur.tauxRemise().valeur()) + " %)");
            }
        }
        if (vendeurs.isEmpty()) {
            return List.of();
        }
        return List.of(
            new AlertePilotageDTO(
                "REMISES_ANORMALES",
                GraviteAlerte.MOYENNE,
                "Remises anormales",
                "Taux de remise du mois au-delà de " + RequetesAlertes.formaterDecimal(remises.multipleAlerte()) + " fois celui de l'équipe (" +
                RequetesAlertes.formaterDecimal(remises.tauxRemise().valeur()) + " %) : " + String.join(", ", vendeurs) + ".",
                "rentabilite-remises"
            )
        );
    }
}
