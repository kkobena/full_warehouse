package com.kobe.warehouse.service.pilotage.calcul;

import com.kobe.warehouse.service.dto.pilotage.DelaiObserveDTO;
import com.kobe.warehouse.service.dto.pilotage.FactureEncoursDTO;
import com.kobe.warehouse.service.dto.pilotage.FactureOrganismeDTO;
import com.kobe.warehouse.service.dto.pilotage.OrganismeTresorerieDTO;

/** Accumulateur d'un organisme ; son délai retenu est fixé à la première facture rencontrée. */
public final class OrganismeCreances {
    private static final double CENT = 100.0;
    /** Factures réglées à partir desquelles on se fie au délai observé d'un organisme (question encore ouverte : 3 par défaut). */
    public static final int SEUIL_HISTORIQUE = 3;

    private final String libelle;
    private final int delai;
    private final String origine;
    private long encours;
    private double ageMontant;
    private long enRetard;

    private OrganismeCreances(String libelle, int delai, String origine) {
        this.libelle = libelle;
        this.delai = delai;
        this.origine = origine;
    }

    public static OrganismeCreances retenir(FactureEncoursDTO facture, DelaiObserveDTO observe, int delaiDefaut) {
        if (observe != null && observe.factures() >= SEUIL_HISTORIQUE && observe.delaiMoyen() != null) {
            return new OrganismeCreances(facture.libelle(), (int) Math.round(observe.delaiMoyen()), "OBSERVE");
        }
        if (facture.delaiGroupe() != null) {
            return new OrganismeCreances(facture.libelle(), facture.delaiGroupe(), "GROUPE");
        }
        return new OrganismeCreances(facture.libelle(), delaiDefaut, "DEFAUT");
    }

    public static OrganismeTresorerieDTO sansEncours(FactureOrganismeDTO factures) {
        return new OrganismeTresorerieDTO(factures.cle(), factures.libelle(), factures.facture(), factures.regle(), taux(factures), 0, 0.0, null, 0, 0, null);
    }

    public int delai() {
        return delai;
    }

    public void ajouter(long reste, long age, boolean enRetardDePaiement) {
        encours += reste;
        ageMontant += (double) age * reste;
        if (enRetardDePaiement) {
            enRetard += reste;
        }
    }

    public OrganismeTresorerieDTO versLigne(String cle, FactureOrganismeDTO factures, long encoursTotal) {
        return new OrganismeTresorerieDTO(
            cle,
            libelle,
            factures == null ? 0 : factures.facture(),
            factures == null ? 0 : factures.regle(),
            factures == null ? null : taux(factures),
            encours,
            encoursTotal == 0 ? null : encours * CENT / encoursTotal,
            encours == 0 ? null : (int) Math.round(ageMontant / encours),
            enRetard,
            delai,
            origine
        );
    }

    private static Double taux(FactureOrganismeDTO factures) {
        return factures.facture() == 0 ? null : factures.regle() * CENT / factures.facture();
    }
}
