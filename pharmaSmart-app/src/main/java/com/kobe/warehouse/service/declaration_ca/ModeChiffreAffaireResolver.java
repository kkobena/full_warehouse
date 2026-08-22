package com.kobe.warehouse.service.declaration_ca;

import com.kobe.warehouse.license.Feature;
import com.kobe.warehouse.service.financiel_transaction.dto.ModeChiffreAffaire;
import com.kobe.warehouse.service.license.LicenseService;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Décide du mode de lecture des états destinés à la comptabilité.
 *
 * <p>Pendant serveur de {@code ModeCaService} côté navigateur : les deux répondent à la même
 * question et doivent répondre pareil.
 */
@Service
public class ModeChiffreAffaireResolver {

    /**
     * Les modules qui séparent le chiffre réel du chiffre déclaré.
     *
     * <p>Énumérés, et non déduits de {@code Feature.isOptional()} : toute option vendue plus tard —
     * un module de fidélité, un connecteur — deviendrait sinon un motif de basculer la comptabilité
     * en mode déclaré, alors qu'elle n'a rien à y retrancher.
     */
    private static final Set<Feature> MODULES_RETRAITEMENT = EnumSet.of(
        Feature.EXCLUSION_RAYON,
        Feature.EXCLUSION_TP,
        Feature.EXCLUSION_UG,
        Feature.CALLEBASSE
    );

    private final LicenseService licenseService;

    public ModeChiffreAffaireResolver(LicenseService licenseService) {
        this.licenseService = licenseService;
    }

    public ModeChiffreAffaire resoudre(ModeChiffreAffaire demande) {
        if (demande != ModeChiffreAffaire.DECLARE) {
            return ModeChiffreAffaire.REEL;
        }
        return modeComptabilite();
    }

    /**
     * Sans aucun module souscrit, les deux chiffres sont identiques par construction : autant lire
     * le réel, qui est l'état neutre.
     */
    public ModeChiffreAffaire modeComptabilite() {
        return MODULES_RETRAITEMENT.stream().anyMatch(licenseService::hasFeature)
            ? ModeChiffreAffaire.DECLARE
            : ModeChiffreAffaire.REEL;
    }
}
