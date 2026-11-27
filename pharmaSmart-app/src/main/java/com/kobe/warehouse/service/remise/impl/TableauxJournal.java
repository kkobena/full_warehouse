package com.kobe.warehouse.service.remise.impl;

import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.domain.enumeration.TypePromotion;
import com.kobe.warehouse.service.remise.dto.LigneJournalDTO;
import com.kobe.warehouse.service.remise.dto.NatureJournal;
import com.kobe.warehouse.service.remise.dto.SyntheseJournalDTO;
import com.kobe.warehouse.service.remise.dto.SyntheseJournalDTO.Totaux;
import com.kobe.warehouse.service.remise.dto.SyntheseJournalDTO.TotauxUg;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToLongFunction;

/**
 * Le journal mis en tableaux, une seule fois pour les trois formats : le PDF, le classeur et le CSV
 * écrivent les mêmes colonnes, les mêmes lignes et les mêmes totaux.
 */
final class TableauxJournal {

    enum TypeCellule {
        TEXTE,
        DATE,
        HEURE,
        ENTIER,
        MONTANT,
        TAUX;

        boolean estNumerique() {
            return this == ENTIER || this == MONTANT || this == TAUX;
        }
    }

    record Colonne(String libelle, TypeCellule type) {}

    /**
     * @param feuille la feuille du classeur ; plusieurs tableaux peuvent la partager
     * @param total une valeur par colonne, nulle pour une colonne sans total ; nul sans ligne de total
     */
    record Tableau(String feuille, String titre, List<Colonne> colonnes, List<Object[]> lignes, Object[] total) {}

    private record Champ(Colonne colonne, Function<LigneJournalDTO, Object> valeur, boolean totalise) {}

    private TableauxJournal() {}

    static List<Tableau> construireTableaux(
            SyntheseJournalDTO synthese, Map<NatureJournal, List<LigneJournalDTO>> journaux) {
        List<Tableau> tableaux = new ArrayList<>();
        if (synthese != null) {
            tableaux.addAll(construireTableauxSynthese(synthese));
        }
        journaux.forEach((nature, lignes) -> tableaux.add(construireJournal(nature, lignes)));
        return tableaux;
    }

    // ===== synthèse =====

    private static List<Tableau> construireTableauxSynthese(SyntheseJournalDTO s) {
        List<Tableau> tableaux = new ArrayList<>();
        List<Colonne> colonnes = List.of(
                creerColonneTexte("Nature"),
                creerColonneEntier("Lignes"),
                creerColonneEntier("Tickets"),
                creerColonneEntier("Unités"),
                creerColonneMontant("Montant brut"),
                creerColonneMontant("Remise accordée"),
                creerColonneMontant("Net"));
        List<Object[]> lignes = List.of(
                construireTotaux("Promotions", s.promotions()),
                construireTotaux("Remises produit", s.remisesProduit()));
        tableaux.add(new Tableau("Synthèse", "Remises accordées", colonnes, lignes, new Object[] {
            "Total", null, null, null, null, s.remiseTotale(), null
        }));

        TotauxUg ug = s.ug();
        tableaux.add(new Tableau(
                "Synthèse",
                "UG vendues (unités reçues gratuitement du fournisseur) — hors remises",
                List.of(
                        creerColonneEntier("Lignes"),
                        creerColonneEntier("Tickets"),
                        creerColonneEntier("Unités UG"),
                        creerColonneMontant("Valeur des UG"),
                        creerColonneMontant("Remise sur les UG"),
                        creerColonneMontant("HT net des UG"),
                        creerColonneMontant("Coût d'achat évité"),
                        creerColonneMontant("Marge des UG"),
                        creerColonneMontant("Marge comptabilisée des lignes"),
                        creerColonneMontant("Marge réelle des lignes")),
                List.<Object[]>of(new Object[] {
                    ug.lignes(),
                    ug.tickets(),
                    ug.unites(),
                    ug.valeur(),
                    ug.remise(),
                    ug.htNet(),
                    ug.coutEvite(),
                    ug.marge(),
                    ug.margeComptabilisee(),
                    ug.margeReelle()
                }),
                null));

        tableaux.add(new Tableau(
                "Synthèse",
                "Par promotion",
                List.of(
                        creerColonneTexte("Promotion"),
                        creerColonneTexte("Mécanique"),
                        creerColonneEntier("Lignes"),
                        creerColonneEntier("Tickets"),
                        creerColonneEntier("Unités remisées"),
                        creerColonneEntier("Unités offertes"),
                        creerColonneMontant("Remise"),
                        creerColonneMontant("Net")),
                s.parPromotion().stream()
                        .map(p -> new Object[] {
                            p.libelle(),
                            libellerMecanique(p.type()),
                            p.lignes(),
                            p.tickets(),
                            p.unitesRemisees(),
                            p.unitesOffertes(),
                            p.remise(),
                            p.montantNet()
                        })
                        .toList(),
                new Object[] {
                    "Total",
                    null,
                    additionner(s.parPromotion(), SyntheseJournalDTO.ParPromotion::lignes),
                    null,
                    additionner(s.parPromotion(), SyntheseJournalDTO.ParPromotion::unitesRemisees),
                    null,
                    additionner(s.parPromotion(), SyntheseJournalDTO.ParPromotion::remise),
                    additionner(s.parPromotion(), SyntheseJournalDTO.ParPromotion::montantNet)
                }));

        tableaux.add(new Tableau(
                "Synthèse",
                "Par grille de remise",
                List.of(
                        creerColonneTexte("Grille"),
                        creerColonneEntier("Lignes"),
                        creerColonneEntier("Tickets"),
                        creerColonneMontant("Remise"),
                        creerColonneMontant("Net")),
                s.parGrille().stream()
                        .map(g -> new Object[] {g.grille(), g.lignes(), g.tickets(), g.remise(), g.montantNet()})
                        .toList(),
                new Object[] {
                    "Total",
                    additionner(s.parGrille(), SyntheseJournalDTO.ParGrille::lignes),
                    null,
                    additionner(s.parGrille(), SyntheseJournalDTO.ParGrille::remise),
                    additionner(s.parGrille(), SyntheseJournalDTO.ParGrille::montantNet)
                }));
        return tableaux;
    }

    private static Object[] construireTotaux(String nature, Totaux t) {
        return new Object[] {nature, t.lignes(), t.tickets(), t.unites(), t.montantBrut(), t.remise(), t.montantNet()};
    }

    // ===== journaux =====

    private static Tableau construireJournal(NatureJournal nature, List<LigneJournalDTO> lignes) {
        List<Champ> champs = listerChamps(nature);
        List<Object[]> valeurs = lignes.stream()
                .map(l -> champs.stream().map(c -> c.valeur().apply(l)).toArray())
                .toList();
        Object[] total = new Object[champs.size()];
        total[0] = "Total (" + lignes.size() + " lignes)";
        for (int i = 1; i < champs.size(); i++) {
            if (champs.get(i).totalise()) {
                int colonne = i;
                total[i] = valeurs.stream()
                        .mapToLong(v -> v[colonne] == null ? 0 : ((Number) v[colonne]).longValue())
                        .sum();
            }
        }
        return new Tableau(
                nature.getLibelle(),
                "Journal — " + nature.getLibelle(),
                champs.stream().map(Champ::colonne).toList(),
                valeurs,
                total);
    }

    private static List<Champ> listerChamps(NatureJournal nature) {
        List<Champ> champs = new ArrayList<>(List.of(
                new Champ(new Colonne("Date", TypeCellule.DATE), LigneJournalDTO::date, false),
                new Champ(new Colonne("Heure", TypeCellule.HEURE), LigneJournalDTO::heure, false),
                new Champ(creerColonneTexte("Ticket"), LigneJournalDTO::ticket, false),
                new Champ(creerColonneTexte("Vente"), l -> libellerNatureVente(l.natureVente()), false),
                new Champ(creerColonneTexte("Caissier"), LigneJournalDTO::caissier, false)));
        switch (nature) {
            case PROMOTIONS ->
                champs.addAll(List.of(
                        new Champ(creerColonneTexte("Produit"), LigneJournalDTO::produit, false),
                        new Champ(creerColonneTexte("CIP"), LigneJournalDTO::cip, false),
                        new Champ(creerColonneTexte("Promotion"), LigneJournalDTO::promotionLibelle, false),
                        new Champ(creerColonneEntier("Quantité"), LigneJournalDTO::quantite, true),
                        new Champ(creerColonneEntier("Unités en promotion"), LigneJournalDTO::unitesPromo, true),
                        new Champ(creerColonneMontant("Prix unitaire"), LigneJournalDTO::prixUnitaire, false),
                        new Champ(creerColonneMontant("Montant brut"), LigneJournalDTO::montantBrut, true),
                        new Champ(creerColonneMontant("Remise promotion"), LigneJournalDTO::partPromotion, true),
                        new Champ(
                                creerColonneMontant("Remise produit cumulée"),
                                LigneJournalDTO::partRemiseProduit,
                                true),
                        new Champ(creerColonneMontant("Net"), LigneJournalDTO::montantNet, true)));
            case REMISES ->
                champs.addAll(List.of(
                        new Champ(creerColonneTexte("Client"), LigneJournalDTO::client, false),
                        new Champ(creerColonneTexte("Produit"), LigneJournalDTO::produit, false),
                        new Champ(creerColonneTexte("CIP"), LigneJournalDTO::cip, false),
                        new Champ(creerColonneTexte("Grille"), LigneJournalDTO::grille, false),
                        new Champ(
                                creerColonneTexte("Origine"),
                                l -> l.origine() == null ? null : l.origine().getLibelle(),
                                false),
                        new Champ(creerColonneEntier("Quantité"), LigneJournalDTO::quantite, true),
                        new Champ(creerColonneMontant("Prix unitaire"), LigneJournalDTO::prixUnitaire, false),
                        new Champ(creerColonneMontant("Montant brut"), LigneJournalDTO::montantBrut, true),
                        new Champ(new Colonne("Taux %", TypeCellule.TAUX), LigneJournalDTO::tauxRemise, false),
                        new Champ(creerColonneMontant("Remise produit"), LigneJournalDTO::partRemiseProduit, true),
                        new Champ(
                                creerColonneMontant("Remise promotion cumulée"), LigneJournalDTO::partPromotion, true),
                        new Champ(creerColonneMontant("Net"), LigneJournalDTO::montantNet, true)));
            case UG ->
                champs.addAll(List.of(
                        new Champ(creerColonneTexte("Produit"), LigneJournalDTO::produit, false),
                        new Champ(creerColonneTexte("CIP"), LigneJournalDTO::cip, false),
                        new Champ(creerColonneTexte("Famille"), LigneJournalDTO::famille, false),
                        new Champ(creerColonneEntier("Quantité"), LigneJournalDTO::quantite, true),
                        new Champ(creerColonneEntier("Unités UG"), LigneJournalDTO::quantiteUg, true),
                        new Champ(creerColonneMontant("Prix unitaire"), LigneJournalDTO::prixUnitaire, false),
                        new Champ(creerColonneMontant("Valeur des UG"), LigneJournalDTO::valeurUg, true),
                        new Champ(creerColonneMontant("Remise sur les UG"), LigneJournalDTO::remiseUg, true),
                        new Champ(creerColonneMontant("HT net des UG"), LigneJournalDTO::htNetUg, true),
                        new Champ(creerColonneMontant("Prix d'achat"), LigneJournalDTO::prixAchat, false),
                        new Champ(creerColonneMontant("Coût évité"), LigneJournalDTO::coutEviteUg, true),
                        new Champ(creerColonneMontant("Marge des UG"), LigneJournalDTO::margeUg, true),
                        new Champ(
                                creerColonneMontant("Marge comptabilisée"), LigneJournalDTO::margeComptabilisee, true),
                        new Champ(creerColonneMontant("Marge réelle"), LigneJournalDTO::margeReelle, true)));
        }
        return champs;
    }

    static String libellerNatureVente(NatureVente nature) {
        if (nature == null) {
            return null;
        }
        return switch (nature) {
            case COMPTANT -> "Comptant";
            case ASSURANCE -> "Assurance";
            case CARNET -> "Carnet";
        };
    }

    static String libellerMecanique(TypePromotion type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case PROMO_POURCENTAGE -> "Pourcentage";
            case PROMO_MONTANT -> "Montant";
            case PROMO_PRIX_FIXE -> "Prix fixe";
            case PROMO_OFFRE_NM -> "Offre N+M";
            case PROMO_PACK -> "Pack";
            case PROMO_SEUIL_MONTANT -> "Seuil en montant";
        };
    }

    private static <T> long additionner(List<T> lignes, ToLongFunction<T> valeur) {
        return lignes.stream().mapToLong(valeur).sum();
    }

    private static Colonne creerColonneTexte(String libelle) {
        return new Colonne(libelle, TypeCellule.TEXTE);
    }

    private static Colonne creerColonneEntier(String libelle) {
        return new Colonne(libelle, TypeCellule.ENTIER);
    }

    private static Colonne creerColonneMontant(String libelle) {
        return new Colonne(libelle, TypeCellule.MONTANT);
    }
}
