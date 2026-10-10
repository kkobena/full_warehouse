package com.kobe.warehouse.service.report.pdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.CompteFournisseurAPDTO;
import com.kobe.warehouse.service.dto.FournisseurAPSummaryDTO;
import com.kobe.warehouse.service.dto.LigneFournisseurAPDTO;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * L'état des comptes fournisseurs dit ce que l'officine doit à ses grossistes. Il s'imprime sous
 * deux formes que le même service produit : la <b>vue d'ensemble</b>, un fournisseur par ligne, et
 * le <b>détail d'un compte</b>, une commande par ligne. C'est le document qu'on pose sur la table
 * lors d'une négociation de délai de paiement.
 *
 * <p>Deux choses s'y vérifient. Le <b>total dû</b>, qui n'est pas fourni par l'appelant mais
 * recalculé ici : un total qui ne correspond pas à la somme des lignes en dessous décrédibilise le
 * document entier. Et le <b>libellé de période</b>, dont les quatre formes — bornée, ouverte à
 * gauche, ouverte à droite, absente — doivent toutes dire quelque chose de juste ; « Du null au
 * 31/03/2026 » sur un document remis à un fournisseur ne passe pas.
 *
 * <p>Ce service est aussi l'un des rares à <b>vider son modèle</b> entre deux impressions. C'est ce
 * qui l'empêche de traîner les variables d'un document dans le suivant, et cela se vérifie.
 */
@DisplayName("AccountsPayableApPdfExportService — état des comptes fournisseurs")
class AccountsPayableApPdfExportServiceTest {

    private AccountsPayableApPdfExportService service;

    @BeforeEach
    void setUp() {
        SpringTemplateEngine moteurDeGabarits = mock(SpringTemplateEngine.class);
        when(moteurDeGabarits.process(anyString(), any(Context.class))).thenReturn("<html><body>etat</body></html>");

        Magasin magasin = new Magasin();
        magasin.setRegistre("RC-1");
        AppUser utilisateur = new AppUser();
        utilisateur.setMagasin(magasin);
        StorageService storageService = mock(StorageService.class);
        when(storageService.getUser()).thenReturn(utilisateur);

        service = new AccountsPayableApPdfExportService( storageService, moteurDeGabarits);
    }

    // ===== vue d'ensemble =====

    @Nested
    @DisplayName("Vue d'ensemble")
    class VueDEnsemble {

        /** Le total n'est pas fourni : il est recalculé, et doit correspondre aux lignes imprimées. */
        @Test
        @DisplayName("le total dû est la somme des soldes de chaque fournisseur")
        void totalDesSoldes() {
            service.exportGlobal(
                List.of(compte("LABOREX", 300_000), compte("DPCI", 150_000), compte("COPHARMED", 50_000)),
                resume(),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 3, 31)
            );

            assertThat(modele().get("totalSolde")).isEqualTo(500_000L);
        }

        @Test
        @DisplayName("un état sans fournisseur totalise zéro")
        void aucunFournisseur() {
            service.exportGlobal(List.of(), resume(), null, null);

            assertThat(modele().get("totalSolde")).isEqualTo(0L);
        }

        @Test
        @DisplayName("les comptes, la synthèse et le titre accompagnent le document")
        void modeleDuDocument() {
            service.exportGlobal(List.of(compte("LABOREX", 300_000)), resume(), null, null);

            Map<String, Object> modele = modele();

            assertThat((List<?>) modele.get("comptes")).hasSize(1);
            assertThat(modele).containsKey("summary");
            assertThat(modele.get("reportTitle")).isEqualTo("État des Comptes Fournisseurs");
            assertThat(modele.get("footer").toString()).contains("RC N° RC-1");
        }
    }

    // ===== libellé de période =====

    @Nested
    @DisplayName("Libellé de période")
    class LibelleDePeriode {

        @Test
        @DisplayName("une période bornée se lit « Du … au … »")
        void periodeBornee() {
            service.exportGlobal(List.of(), resume(), LocalDate.of(2026, 1, 5), LocalDate.of(2026, 3, 31));

            assertThat(modele().get("periode")).isEqualTo("Du 05/01/2026 au 31/03/2026");
        }

        @Test
        @DisplayName("une période ouverte à droite se lit « À partir du … »")
        void periodeOuverteADroite() {
            service.exportGlobal(List.of(), resume(), LocalDate.of(2026, 1, 5), null);

            assertThat(modele().get("periode")).isEqualTo("À partir du 05/01/2026");
        }

        @Test
        @DisplayName("une période ouverte à gauche se lit « Jusqu'au … »")
        void periodeOuverteAGauche() {
            service.exportGlobal(List.of(), resume(), null, LocalDate.of(2026, 3, 31));

            assertThat(modele().get("periode")).isEqualTo("Jusqu'au 31/03/2026");
        }

        /** Sans borne, le document le dit plutôt que d'afficher un libellé vide ou un « null ». */
        @Test
        @DisplayName("sans borne, le document annonce toutes périodes")
        void aucunePeriode() {
            service.exportGlobal(List.of(), resume(), null, null);

            assertThat(modele().get("periode")).isEqualTo("Toutes périodes");
        }
    }

    // ===== détail d'un compte =====

    @Nested
    @DisplayName("Détail d'un compte")
    class DetailDUnCompte {

        @Test
        @DisplayName("le restant dû totalise les lignes du compte")
        void totalRestantDu() {
            service.exportFournisseur(
                compte("LABOREX", 300_000),
                List.of(ligne("BON-1", 200_000, 50_000), ligne("BON-2", 150_000, 0))
            );

            assertThat(modele().get("totalRestant")).isEqualTo(300_000L); // 150 000 + 150 000
        }

        @Test
        @DisplayName("le titre nomme le fournisseur concerné")
        void titreNommeLeFournisseur() {
            service.exportFournisseur(compte("LABOREX", 300_000), List.of());

            assertThat(modele().get("reportTitle")).isEqualTo("Détail Compte Fournisseur — LABOREX");
        }

        @Test
        @DisplayName("un compte sans commande totalise zéro")
        void compteSansCommande() {
            service.exportFournisseur(compte("LABOREX", 0), List.of());

            assertThat(modele().get("totalRestant")).isEqualTo(0L);
        }
    }

    // ===== isolement entre documents =====

    @Nested
    @DisplayName("Isolement entre documents")
    class IsolementEntreDocuments {

        /**
         * Le modèle est un champ de l'instance, et l'instance est un singleton Spring. Sans remise à
         * zéro, le détail d'un compte traînerait la liste complète des fournisseurs imprimée juste
         * avant — et le gabarit, qui ne connaît pas la provenance de ses variables, l'afficherait.
         */
        @Test
        @DisplayName("le détail d'un compte ne traîne pas les variables de la vue d'ensemble")
        void detailNeTraînePasLaVueDEnsemble() {
            service.exportGlobal(List.of(compte("LABOREX", 300_000)), resume(), LocalDate.of(2026, 1, 5), null);

            service.exportFournisseur(compte("DPCI", 100_000), List.of(ligne("BON-1", 100_000, 0)));

            Map<String, Object> modele = modele();

            assertThat(modele).doesNotContainKeys("comptes", "summary", "totalSolde", "periode");
            assertThat(modele).containsKeys("compte", "lignes", "totalRestant");
        }

        @Test
        @DisplayName("la vue d'ensemble ne traîne pas les variables d'un détail")
        void vueDEnsembleNeTraînePasLeDetail() {
            service.exportFournisseur(compte("DPCI", 100_000), List.of(ligne("BON-1", 100_000, 0)));

            service.exportGlobal(List.of(compte("LABOREX", 300_000)), resume(), null, null);

            assertThat(modele()).doesNotContainKeys("compte", "lignes", "totalRestant");
        }

        @Test
        @DisplayName("deux états successifs ne cumulent pas leurs totaux")
        void deuxEtatsSuccessifs() {
            service.exportGlobal(List.of(compte("LABOREX", 300_000)), resume(), null, null);
            service.exportGlobal(List.of(compte("DPCI", 100_000)), resume(), null, null);

            assertThat(modele().get("totalSolde")).isEqualTo(100_000L);
        }
    }

    // ===== fabriques =====

    private Map<String, Object> modele() {
        return service.getParameters();
    }

    private static CompteFournisseurAPDTO compte(String nom, long solde) {
        return new CompteFournisseurAPDTO(1, nom, "F1", "0100000000", "0700000000", solde, 0L, solde, 0L, null, "ACTIF");
    }

    private static LigneFournisseurAPDTO ligne(String numBon, long montant, long montantRegle) {
        return new LigneFournisseurAPDTO(1, numBon, "01/01/2026", "31/01/2026", montant, montantRegle, montant - montantRegle, "NOT_PAID");
    }

    private static FournisseurAPSummaryDTO resume() {
        return new FournisseurAPSummaryDTO(500_000L, 0L, 0L, 3L);
    }
}
