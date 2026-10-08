package com.kobe.warehouse.service.ordonnance.controle;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.pharmacovigilance.ContreIndication;
import com.kobe.warehouse.domain.pharmacovigilance.CritereContreIndication;
import com.kobe.warehouse.domain.pharmacovigilance.Interaction;
import com.kobe.warehouse.domain.pharmacovigilance.NiveauInteraction;
import com.kobe.warehouse.service.dto.controle.TraitementEnCoursDTO;
import com.kobe.warehouse.service.dto.controle.EntreeControleDTO;
import com.kobe.warehouse.service.dto.controle.MoleculeDTO;
import com.kobe.warehouse.service.dto.controle.ProfilPatientDTO;
import com.kobe.warehouse.service.dto.controle.AlerteControleDTO;
import com.kobe.warehouse.service.dto.controle.ProduitNonControleDTO;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class MoteurControleTest {

    private static final MoleculeDTO WARFARINE = new MoleculeDTO(1, "WARFARINE");
    private static final MoleculeDTO IBUPROFENE = new MoleculeDTO(2, "IBUPROFENE");
    private static final MoleculeDTO KETOPROFENE = new MoleculeDTO(3, "KETOPROFENE");
    private static final Map<Integer, String> SOURCES = Map.of(7, "ANSM 2026");
    private static final ProfilPatientDTO PROFIL = new ProfilPatientDTO(40, "F", false, false);

    private static EntreeControleDTO entree(
        Map<Integer, String> panier,
        Map<Integer, List<MoleculeDTO>> molecules,
        List<TraitementEnCoursDTO> enCours,
        Map<Integer, Set<Integer>> classesParDci,
        List<Interaction> interactions,
        List<ContreIndication> cis,
        Map<Integer, String> sources,
        ProfilPatientDTO profil
    ) {
        return new EntreeControleDTO(panier, molecules, enCours, classesParDci, Map.of(50, "AINS"), interactions, cis, sources, profil);
    }

    @Test
    void interactionDirecteEntreDeuxProduitsDuPanier() {
        Interaction ci = new Interaction(7, 1, null, 2, null, NiveauInteraction.CI, null, "Contre-indiquer");
        var resultat = MoteurControle.evaluer(
            entree(
                Map.of(10, "COUMADINE", 11, "ADVIL"),
                Map.of(10, List.of(WARFARINE), 11, List.of(IBUPROFENE)),
                List.of(),
                Map.of(),
                List.of(ci),
                List.of(),
                SOURCES,
                PROFIL
            )
        );
        assertThat(resultat.alertes()).hasSize(1);
        AlerteControleDTO alerte = resultat.alertes().get(0);
        assertThat(alerte.niveau()).isEqualTo(NiveauInteraction.CI);
        // Le moteur ne décide pas du blocage : c'est le paramètre du niveau, appliqué par le service.
        assertThat(alerte.exigeMotif()).isFalse();
        assertThat(alerte.bloquantSi(true).exigeMotif()).isTrue();
        assertThat(alerte.source()).isEqualTo("ANSM 2026");
        assertThat(alerte.produitIds()).containsExactlyInAnyOrder(10, 11);
        assertThat(resultat.referentielPublie()).isTrue();
    }

    @Test
    void interactionParClasseAvecUnTraitementEnCours() {
        // WARFARINE (molécule) x classe AINS ; le client prend déjà du kétoprofène, on lui vend de la warfarine.
        Interaction i = new Interaction(7, 1, null, null, 50, NiveauInteraction.AD, null, null);
        var resultat = MoteurControle.evaluer(
            entree(
                Map.of(10, "COUMADINE"),
                Map.of(10, List.of(WARFARINE)),
                List.of(new TraitementEnCoursDTO(99, "PROFENID", KETOPROFENE)),
                Map.of(3, Set.of(50)),
                List.of(i),
                List.of(),
                SOURCES,
                PROFIL
            )
        );
        assertThat(resultat.alertes()).extracting(AlerteControleDTO::niveau).containsExactly(NiveauInteraction.AD);
        assertThat(resultat.alertes().get(0).message()).contains("en cours : PROFENID");
    }

    @Test
    void deuxTraitementsDejaEnCoursNeLevantPasDAlerte() {
        Interaction i = new Interaction(7, 1, null, 2, null, NiveauInteraction.CI, null, null);
        var resultat = MoteurControle.evaluer(
            entree(
                Map.of(),
                Map.of(),
                List.of(new TraitementEnCoursDTO(98, "COUMADINE", WARFARINE), new TraitementEnCoursDTO(99, "ADVIL", IBUPROFENE)),
                Map.of(),
                List.of(i),
                List.of(),
                SOURCES,
                PROFIL
            )
        );
        assertThat(resultat.alertes()).isEmpty();
    }

    @Test
    void renouvelerLeMemeProduitNEstPasUneRedondanceMaisUnAutreProduitDeLaMemeMoleculeSi() {
        var renouvellement = MoteurControle.evaluer(
            entree(
                Map.of(10, "DOLIPRANE"),
                Map.of(10, List.of(IBUPROFENE)),
                List.of(new TraitementEnCoursDTO(10, "DOLIPRANE", IBUPROFENE)),
                Map.of(),
                List.of(),
                List.of(),
                SOURCES,
                PROFIL
            )
        );
        assertThat(renouvellement.alertes()).isEmpty();

        var autreProduit = MoteurControle.evaluer(
            entree(
                Map.of(10, "GENERIQUE"),
                Map.of(10, List.of(IBUPROFENE)),
                List.of(new TraitementEnCoursDTO(11, "ADVIL", IBUPROFENE)),
                Map.of(),
                List.of(),
                List.of(),
                SOURCES,
                PROFIL
            )
        );
        assertThat(autreProduit.alertes()).extracting(AlerteControleDTO::type).containsExactly("REDONDANCE");
    }

    @Test
    void contreIndicationLieeAuProfil() {
        ContreIndication grossesse = new ContreIndication(7, 2, CritereContreIndication.GROSSESSE, null, null, NiveauInteraction.CI, "Toxicité fœtale", null);
        ContreIndication enfant = new ContreIndication(7, 2, CritereContreIndication.AGE_MIN, new BigDecimal("12"), null, NiveauInteraction.AD, null, null);
        Map<Integer, String> panier = Map.of(11, "ADVIL");
        Map<Integer, List<MoleculeDTO>> molecules = Map.of(11, List.of(IBUPROFENE));

        var enceinte = MoteurControle.evaluer(
            entree(panier, molecules, List.of(), Map.of(), List.of(), List.of(grossesse, enfant), SOURCES, new ProfilPatientDTO(30, "F", true, false))
        );
        assertThat(enceinte.alertes()).extracting(AlerteControleDTO::niveau).containsExactly(NiveauInteraction.CI);

        var petit = MoteurControle.evaluer(
            entree(panier, molecules, List.of(), Map.of(), List.of(), List.of(grossesse, enfant), SOURCES, new ProfilPatientDTO(8, "M", false, false))
        );
        assertThat(petit.alertes()).extracting(AlerteControleDTO::niveau).containsExactly(NiveauInteraction.AD);

        // Âge inconnu : la règle d'âge ne peut pas être évaluée, elle ne se déclenche pas.
        var inconnu = MoteurControle.evaluer(
            entree(panier, molecules, List.of(), Map.of(), List.of(), List.of(enfant), SOURCES, new ProfilPatientDTO(null, null, false, false))
        );
        assertThat(inconnu.alertes()).isEmpty();
    }

    @Test
    void produitSansMoleculeEstNonControleEtReferentielAbsentEstSignale() {
        var resultat = MoteurControle.evaluer(
            entree(Map.of(12, "PRODUIT LOCAL"), Map.of(), List.of(), Map.of(), List.of(), List.of(), Map.of(), PROFIL)
        );
        assertThat(resultat.nonControles()).extracting(ProduitNonControleDTO::produitId).containsExactly(12);
        assertThat(resultat.referentielPublie()).isFalse();
        assertThat(resultat.alertes()).isEmpty();
    }
}
