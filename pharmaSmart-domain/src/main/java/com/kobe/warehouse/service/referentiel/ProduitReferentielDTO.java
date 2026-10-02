package com.kobe.warehouse.service.referentiel;

import com.kobe.warehouse.domain.enumeration.DecisionRapprochement;
import com.kobe.warehouse.domain.enumeration.StatutRapprochement;
import com.kobe.warehouse.domain.enumeration.TypeGenerique;
import java.util.List;

/**
 * Ce que le référentiel médicament dit d'un produit, pour le comptoir et le conseil.
 *
 * <p>{@code specialite}, {@code molecules}, {@code rcp} et {@code substituts} ne sont renseignés
 * que pour un rapprochement de confiance (décision AUTO ou VALIDE) : une proposition encore à
 * relire ne doit pas orienter un conseil.
 */
public record ProduitReferentielDTO(
    Integer produitId,
    StatutRapprochement statut,
    DecisionRapprochement decision,
    int score,
    String motif,
    boolean dciPoseeAutomatiquement,
    Specialite specialite,
    List<Molecule> molecules,
    Rcp rcp,
    List<Substitut> substituts
) {
    public record Specialite(
        String cis,
        String libelle,
        String forme,
        String voies,
        String titulaire,
        boolean commercialisee,
        TypeGenerique typeGenerique,
        String groupeGenerique,
        String princepsDuGroupe
    ) {}

    public record Molecule(String substance, String dci, String nature, String dosage, String referenceDosage) {}

    /** Texte libre du RCP, repris tel quel : à afficher, pas à interpréter. */
    public record Rcp(String indications, String posologie, String contreIndications) {}

    /** Produit du catalogue du même groupe générique : substituable à celui-ci. */
    public record Substitut(Integer produitId, String libelle, TypeGenerique typeGenerique, int prixUnitaire) {}

    public static ProduitReferentielDTO creerSansReferentiel(Integer produitId) {
        return new ProduitReferentielDTO(
            produitId,
            StatutRapprochement.NON_TROUVE,
            DecisionRapprochement.EN_ATTENTE,
            0,
            null,
            false,
            null,
            List.of(),
            null,
            List.of()
        );
    }
}
