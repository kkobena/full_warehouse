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
import com.kobe.warehouse.service.dto.RetourBonDTO;
import com.kobe.warehouse.service.dto.RetourBonItemDTO;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * Le bon de retour fournisseur est un document contractuel : il accompagne physiquement la
 * marchandise renvoyée au grossiste et sert de preuve de ce qui a été expédié. Il s'imprime à
 * l'unité, ou groupé en un bordereau lorsqu'on renvoie plusieurs bons au même fournisseur.
 *
 * <p>Trois choses s'y vérifient. L'<b>ordre des lignes</b> : le magasinier coche le document en
 * préparant les cartons, et une liste dans un ordre arbitraire lui fait perdre son temps ou sauter
 * une référence. Le <b>montant du bordereau groupé</b>, qui est ce que le fournisseur devra créditer.
 * Et l'<b>isolement</b> entre deux impressions, ce service étant l'un des rares à vider son modèle.
 */
@DisplayName("RetourBonPdfReportService — bons de retour fournisseur")
class RetourBonPdfReportServiceTest {

    private SpringTemplateEngine moteurDeGabarits;
    private RetourBonPdfReportService service;

    @BeforeEach
    void setUp() {
        moteurDeGabarits = mock(SpringTemplateEngine.class);
        when(moteurDeGabarits.process(anyString(), any(Context.class))).thenReturn("<html><body>bon</body></html>");

        Magasin magasin = new Magasin();
        magasin.setRegistre("RC-1");
        AppUser utilisateur = new AppUser();
        utilisateur.setMagasin(magasin);
        StorageService storageService = mock(StorageService.class);
        when(storageService.getUser()).thenReturn(utilisateur);

        AppConfigurationService configuration = mock(AppConfigurationService.class);
        when(configuration.getDevise()).thenReturn("EUR");

        service = new RetourBonPdfReportService( storageService, moteurDeGabarits);
        service.setAppConfigurationService(configuration);
    }

    // ===== bon à l'unité =====

    @Nested
    @DisplayName("Bon à l'unité")
    class BonALUnite {

        /**
         * Le magasinier coche le document en préparant les cartons : les références doivent se
         * suivre dans l'ordre alphabétique, quel qu'ait été l'ordre de saisie.
         */
        @Test
        @DisplayName("les lignes sont classées par libellé de produit")
        void lignesClasseesParLibelle() {
            service.export(bon("BR-2026-001", "LABOREX", ligne("ZOVIRAX"), ligne("AMOXICILLINE"), ligne("MOTILIUM")));

            assertThat(lignesDuModele()).extracting(RetourBonItemDTO::getProduitLibelle).containsExactly("AMOXICILLINE", "MOTILIUM", "ZOVIRAX");
        }

        /** Un libellé manquant ne doit pas faire échouer le tri — il passe en fin de liste. */
        @Test
        @DisplayName("une ligne sans libellé se range en fin de liste")
        void ligneSansLibelle() {
            service.export(bon("BR-2026-001", "LABOREX", ligne("ZOVIRAX"), ligne(null), ligne("AMOXICILLINE")));

            assertThat(lignesDuModele()).extracting(RetourBonItemDTO::getProduitLibelle).containsExactly("AMOXICILLINE", "ZOVIRAX", null);
        }

        @Test
        @DisplayName("le titre reprend la référence du bon")
        void titreAvecReference() {
            service.export(bon("BR-2026-001", "LABOREX", ligne("ZOVIRAX")));

            assertThat(modele().get("reportTitle")).isEqualTo("BON DE RETOUR FOURNISSEUR BR-2026-001");
        }

        /** Un bon non encore référencé se désigne par son identifiant : il doit rester identifiable. */
        @Test
        @DisplayName("sans référence, le titre reprend l'identifiant")
        void titreSansReference() {
            RetourBonDTO bon = bon(null, "LABOREX", ligne("ZOVIRAX"));
            bon.setId(42);

            service.export(bon);

            assertThat(modele().get("reportTitle")).isEqualTo("BON DE RETOUR FOURNISSEUR N° 42");
        }

        @Test
        @DisplayName("le nombre de lignes et les mentions légales accompagnent le document")
        void modeleDuDocument() {
            service.export(bon("BR-2026-001", "LABOREX", ligne("A"), ligne("B")));

            Map<String, Object> modele = modele();

            assertThat(modele.get("item_size")).isEqualTo(2);
            assertThat(modele.get("isLastPage")).isEqualTo(true);
            assertThat(modele.get("footer").toString()).contains("RC N° RC-1");
        }

        @Test
        @DisplayName("un bon sans ligne s'imprime tout de même")
        void bonSansLigne() {
            service.export(bon("BR-2026-001", "LABOREX"));

            assertThat(modele().get("item_size")).isEqualTo(0);
            assertThat(lignesDuModele()).isEmpty();
        }
    }

    // ===== bordereau groupé =====

    @Nested
    @DisplayName("Bordereau groupé")
    class BordereauGroupe {

        /** C'est le montant que le fournisseur devra créditer : il totalise les bons du bordereau. */
        @Test
        @DisplayName("le montant du bordereau totalise les bons regroupés")
        void montantDuBordereau() {
            service.exportGroupe(List.of(bonDe("LABOREX", 300_000), bonDe("LABOREX", 150_000), bonDe("LABOREX", 50_000)));

            assertThat(modele().get("montantTotalGroupe")).isEqualTo(500_000L);
        }

        @Test
        @DisplayName("le bordereau nomme le fournisseur concerné")
        void fournisseurDuBordereau() {
            service.exportGroupe(List.of(bonDe("LABOREX", 300_000)));

            assertThat(modele().get("fournisseurLibelle")).isEqualTo("LABOREX");
            assertThat(modele().get("reportTitle")).isEqualTo("BORDEREAU GROUPÉ RETOURS FOURNISSEUR — LABOREX");
        }

        /** Un bordereau vide ne doit pas faire échouer l'impression sur un appel à la première ligne. */
        @Test
        @DisplayName("un bordereau sans bon s'imprime avec un total nul")
        void bordereauVide() {
            service.exportGroupe(List.of());

            assertThat(modele().get("montantTotalGroupe")).isEqualTo(0L);
            assertThat(modele().get("fournisseurLibelle")).isEqualTo("");
        }
    }

    // ===== isolement et devise =====

    @Nested
    @DisplayName("Isolement et devise")
    class IsolementEtDevise {

        @Test
        @DisplayName("le bordereau groupé ne traîne pas les variables du bon unitaire")
        void bordereauNeTraînePasLeBonUnitaire() {
            service.export(bon("BR-2026-001", "LABOREX", ligne("ZOVIRAX")));

            service.exportGroupe(List.of(bonDe("DPCI", 100_000)));

            assertThat(modele()).doesNotContainKeys("retourBon", "retourBonItems", "item_size");
        }

        @Test
        @DisplayName("deux bons successifs ne cumulent pas leurs lignes")
        void deuxBonsSuccessifs() {
            service.export(bon("BR-1", "LABOREX", ligne("A"), ligne("B")));
            service.export(bon("BR-2", "LABOREX", ligne("C")));

            assertThat(modele().get("item_size")).isEqualTo(1);
            assertThat(lignesDuModele()).extracting(RetourBonItemDTO::getProduitLibelle).containsExactly("C");
        }

        /**
         * La devise vient de la configuration de l'officine — c'est précisément pour cela que
         * {@link com.kobe.warehouse.service.report.CommonReportService} la pose dans le contexte.
         * Un document qui la réécrit en dur impose le franc CFA à une officine qui compte en euros,
         * et le bon de retour est un document contractuel remis au fournisseur.
         */
        @Test
        @DisplayName("la devise du bon suit la configuration de l'officine")
        void deviseConfiguree() {
            service.export(bon("BR-2026-001", "LABOREX", ligne("ZOVIRAX")));

            assertThat(contexteRemisAuGabarit().getVariable("devise")).isEqualTo("EUR");
        }

        @Test
        @DisplayName("la devise du bordereau groupé suit elle aussi la configuration")
        void deviseDuBordereau() {
            service.exportGroupe(List.of(bonDe("LABOREX", 100_000)));

            assertThat(contexteRemisAuGabarit().getVariable("devise")).isEqualTo("EUR");
        }
    }

    // ===== fabriques =====

    private Map<String, Object> modele() {
        return service.getParameters();
    }

    /**
     * La devise n'est pas dans le modèle mais dans le contexte de rendu : c'est {@code
     * CommonReportService} qui l'y pose depuis la configuration, et le modèle n'a plus qu'à ne pas
     * l'écraser. On observe donc le contexte réellement remis au gabarit.
     */
    private Context contexteRemisAuGabarit() {
        ArgumentCaptor<Context> contexte = ArgumentCaptor.forClass(Context.class);
        org.mockito.Mockito.verify(moteurDeGabarits).process(anyString(), contexte.capture());
        return contexte.getValue();
    }

    @SuppressWarnings("unchecked")
    private List<RetourBonItemDTO> lignesDuModele() {
        return (List<RetourBonItemDTO>) modele().get("retourBonItems");
    }

    private static RetourBonDTO bon(String reference, String fournisseur, RetourBonItemDTO... lignes) {
        RetourBonDTO bon = new RetourBonDTO();
        bon.setReference(reference);
        bon.setFournisseurLibelle(fournisseur);
        bon.setRetourBonItems(new ArrayList<>(List.of(lignes)));
        return bon;
    }

    private static RetourBonDTO bonDe(String fournisseur, long montant) {
        RetourBonDTO bon = bon("BR-" + montant, fournisseur);
        bon.setMontantTotal(montant);
        return bon;
    }

    private static RetourBonItemDTO ligne(String produitLibelle) {
        RetourBonItemDTO ligne = new RetourBonItemDTO();
        ligne.setProduitLibelle(produitLibelle);
        return ligne;
    }
}
