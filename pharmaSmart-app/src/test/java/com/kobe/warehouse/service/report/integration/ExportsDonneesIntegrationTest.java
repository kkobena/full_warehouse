package com.kobe.warehouse.service.report.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.ExportFichier;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.ExportDonnees;
import com.kobe.warehouse.domain.enumeration.FiltreExport;
import com.kobe.warehouse.domain.enumeration.FormatExport;
import com.kobe.warehouse.domain.enumeration.StatutExport;
import com.kobe.warehouse.repository.exports.ExportDonneesRepository;
import com.kobe.warehouse.repository.PilotageAgregatRepository;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Chaque export du catalogue se lit sur la base, avec ou sans filtre, et chaque ligne a les colonnes que l'export annonce. */
@DisplayName("Exports — lecture des données sur la base")
class ExportsDonneesIntegrationTest extends AbstractReportIntegrationTest {

    // Les partitions des tables source du conteneur de test ne couvrent que l'année en cours.
    private static final LocalDate JOUR = LocalDate.now().withMonth(1).withDayOfMonth(15);

    private ExportDonneesRepository exportDonneesRepository;

    @BeforeEach
    void vendreUnProduit() {
        exportDonneesRepository = IntegrationPostgresDatabase.bean(ExportDonneesRepository.class);
        Produit produit = produitAnalysable(unique("EXPORT"), 1_500);
        referencement(produit, fournisseur(unique("FRS EXPORT")));
        vendu(produit, 2, JOUR);
        em.flush();
        IntegrationPostgresDatabase.bean(PilotageAgregatRepository.class).recalculer(JOUR, JOUR);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(ExportDonnees.class)
    void chaqueExportSeLit(ExportDonnees export) {
        try (Stream<Object[]> lignes = exportDonneesRepository.lireLignes(export, JOUR, JOUR, Map.of())) {
            List<Object[]> lues = lignes.toList();
            assertThat(lues).allSatisfy(ligne -> assertThat(ligne).hasSize(export.getColonnes().size()));
            if (export == ExportDonnees.VENTES || export == ExportDonnees.LIGNES_VENTE || export == ExportDonnees.VENTES_JOUR_PRODUIT) {
                assertThat(lues).isNotEmpty();
            }
        }
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(ExportDonnees.class)
    @DisplayName("Chaque filtre annoncé s'applique : la requête tient debout et ne rend que des lignes complètes")
    void chaqueFiltreSApplique(ExportDonnees export) {
        for (FiltreExport filtre : export.getFiltres()) {
            try (Stream<Object[]> lignes = exportDonneesRepository.lireLignes(export, JOUR, JOUR, Map.of(filtre, valeurs(filtre)))) {
                assertThat(lignes.toList()).allSatisfy(ligne -> assertThat(ligne).hasSize(export.getColonnes().size()));
            }
        }
    }

    @Test
    @DisplayName("Un filtre que l'export n'annonce pas est ignoré, il ne casse pas la requête")
    void unFiltreEtrangerEstIgnore() {
        try (Stream<Object[]> lignes = exportDonneesRepository.lireLignes(ExportDonnees.CLIENTS, null, null, Map.of(FiltreExport.MAGASIN, List.of("1")))) {
            assertThat(lignes.toList()).allSatisfy(ligne -> assertThat(ligne).hasSize(ExportDonnees.CLIENTS.getColonnes().size()));
        }
    }

    @Test
    @DisplayName("Les filtres d'un export survivent à l'aller-retour en base, dans leur colonne texte")
    void lesFiltresSeRangentEnBase() {
        ExportFichier fichier = new ExportFichier()
            .setExport(ExportDonnees.VENTES)
            .setFormat(FormatExport.PDF)
            .setDu(JOUR)
            .setAu(JOUR)
            .setFiltres(Map.of(FiltreExport.NATURE_VENTE, List.of("COMPTANT", "CARNET"), FiltreExport.VENDEUR, List.of("1")))
            .setStatut(StatutExport.EN_ATTENTE)
            .setDemandePar(utilisateur)
            .setDemandeLe(LocalDateTime.now());
        em.persist(fichier);
        em.flush();
        em.clear();

        ExportFichier relu = em.find(ExportFichier.class, fichier.getId());

        assertThat(relu.getFiltres())
            .containsEntry(FiltreExport.NATURE_VENTE, List.of("COMPTANT", "CARNET"))
            .containsEntry(FiltreExport.VENDEUR, List.of("1"));
    }

    /** Une valeur plausible du filtre : son type compte, pas son existence — on vérifie que la requête s'écrit et s'exécute. */
    private static List<String> valeurs(FiltreExport filtre) {
        return filtre.getSource().estReferentiel() ? List.of("1") : List.of(filtre.getSource().getOptions().getFirst().valeur());
    }
}
