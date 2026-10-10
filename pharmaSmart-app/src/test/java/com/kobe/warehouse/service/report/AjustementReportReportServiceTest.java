package com.kobe.warehouse.service.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.domain.Ajust;
import com.kobe.warehouse.domain.Ajustement;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.service.StorageService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * L'état d'ajustement de stock est une pièce justificative : il consigne ce qui a été ajouté ou
 * retranché du stock et pourquoi. Un contrôle s'appuie dessus, ce qui en fait un document où
 * l'exhaustivité compte autant que l'exactitude.
 *
 * <p>Il s'imprime sur plusieurs pages au-delà de cinquante-cinq lignes, et le découpage relit la
 * liste à chaque page : elle doit donc rester dans le même ordre d'un bout à l'autre, sous peine de
 * consigner deux fois le même ajustement et d'en omettre un autre.
 */
@DisplayName("AjustementReportReportService — état d'ajustement de stock")
class AjustementReportReportServiceTest {

    /** Gabarit minimal : Flying Saucer rend réellement, il lui faut du XHTML valide. */
    private static final String HTML = "<html><head><title>Ajustement</title></head><body><p>ajustement</p></body></html>";

    // Doublures créées à l'initialisation des champs, et non dans un @BeforeEach : le délai de
    // quinze secondes qui borne les méthodes de cycle de vie coupait l'amorçage de Mockito.
    private final SpringTemplateEngine templateEngine = mock(SpringTemplateEngine.class);
    private final StorageService storageService = mock(StorageService.class);


    private final AjustementReportReportService service = new AjustementReportReportService(
        templateEngine,
        storageService

    );

    private Magasin magasin;

    /** Tranches relevées au moment du rendu : le contexte de gabarit est réécrit à chaque page. */
    private final List<List<Ajustement>> tranches = new ArrayList<>();

    @BeforeEach
    void officine() {
        magasin = new Magasin();
        magasin.setId(1);
        magasin.setFullName("PHARMACIE DU PLATEAU");
        magasin.setRegistre("RC-001");
        magasin.setPhone("0102030405");
        AppUser utilisateur = new AppUser();
        utilisateur.setMagasin(magasin);
        when(storageService.getUser()).thenReturn(utilisateur);
        when(templateEngine.process(anyString(), any(Context.class))).thenAnswer(invocation -> {
            Context context = invocation.getArgument(1);
            @SuppressWarnings("unchecked")
            List<Ajustement> lignes = (List<Ajustement>) context.getVariable(Constant.ITEMS);
            if (lignes != null) {
                tranches.add(List.copyOf(lignes));
            }
            return HTML;
        });
    }

    // ===== document produit =====

    @Nested
    @DisplayName("Document produit")
    class DocumentProduit {

        @Test
        @DisplayName("un ajustement court tient sur une seule page")
        void ajustementCourt() {
            byte[] pdf = service.export(ajust(lignes(10)));

            assertThat(pdf).isNotEmpty();
            assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
            assertThat(service.getParameters())
                .containsEntry(Constant.IS_LAST_PAGE, true)
                .containsEntry(Constant.PAGE_COUNT, "1/1");
        }

        @Test
        @DisplayName("le document porte le magasin, l'ajustement et son nombre de lignes")
        void variablesDuGabarit() {
            Ajust ajust = ajust(lignes(7));

            service.export(ajust);

            assertThat(service.getParameters())
                .containsEntry(Constant.MAGASIN, magasin)
                .containsEntry(Constant.ENTITY, ajust)
                .containsEntry(Constant.ITEM_SIZE, 7);
        }

        @Test
        @DisplayName("le pied de page reprend les mentions légales de l'officine")
        void piedDePage() {
            service.export(ajust(lignes(3)));

            assertThat((String) service.getParameters().get(Constant.FOOTER)).contains("RC-001").contains("0102030405");
        }

        /** Les lignes se lisent dans l'ordre du code CIP, celui des rayons et des inventaires. */
        @Test
        @DisplayName("les lignes sont rangées par code CIP")
        void ordreParCodeCip() {
            service.export(ajust(lignesDesordonnees(20)));

            assertThat(tranches.getFirst()).extracting(a -> codeCip(a)).isSorted();
        }
    }

    // ===== pagination =====

    @Nested
    @DisplayName("Pagination")
    class Pagination {

        @Test
        @DisplayName("un ajustement long s'imprime sur plusieurs pages")
        void ajustementLong() {
            service.export(ajust(lignes(120)));

            assertThat(service.getParameters()).containsEntry(Constant.PAGE_COUNT, "3/3");
        }

        @Test
        @DisplayName("chaque page ne porte que sa tranche de lignes")
        void tranchesParPage() {
            service.export(ajust(lignes(120)));

            assertThat(tranches).hasSize(3);
            assertThat(tranches.get(0)).hasSize(55);
            assertThat(tranches.get(1)).hasSize(55);
            assertThat(tranches.get(2)).hasSize(10);
        }

        @Test
        @DisplayName("aucune ligne n'est consignée deux fois, aucune n'est oubliée")
        void toutesLesLignesUneSeuleFois() {
            service.export(ajust(lignesDesordonnees(120)));

            List<String> consignees = tranches.stream().flatMap(List::stream).map(a -> codeCip(a)).toList();

            assertThat(consignees).hasSize(120).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("les lignes restent ordonnées d'une page à l'autre")
        void ordreConserveEntreLesPages() {
            service.export(ajust(lignesDesordonnees(120)));

            List<String> consignees = tranches.stream().flatMap(List::stream).map(a -> codeCip(a)).toList();

            assertThat(consignees).isSorted();
        }

        @Test
        @DisplayName("seule la dernière page est signalée comme telle")
        void dernierePageSignalee() {
            service.export(ajust(lignes(120)));

            assertThat(service.getParameters()).containsEntry(Constant.IS_LAST_PAGE, true);
        }

        @Test
        @DisplayName("un nombre de lignes multiple de la page ne crée pas de page vide")
        void multipleExact() {
            service.export(ajust(lignes(110)));

            assertThat(tranches).hasSize(2);
            assertThat(service.getParameters()).containsEntry(Constant.PAGE_COUNT, "2/2");
        }
    }

    // ===== fabriques =====

    private static String codeCip(Ajustement ajustement) {
        return ajustement.getStockProduit().getProduit().getFournisseurProduitPrincipal().getCodeCip();
    }

    private static Ajust ajust(List<Ajustement> ajustements) {
        Ajust ajust = new Ajust();
        ajust.setAjustements(ajustements);
        return ajust;
    }

    private static Ajustement ajustement(String codeCip) {
        Produit produit = new Produit();
        produit.setLibelle("PRODUIT " + codeCip);
        FournisseurProduit reference = new FournisseurProduit();
        reference.setCodeCip(codeCip);
        produit.setFournisseurProduitPrincipal(reference);

        StockProduit stock = new StockProduit();
        stock.setProduit(produit);

        Ajustement ajustement = new Ajustement();
        ajustement.setStockProduit(stock);
        ajustement.setQtyMvt(1);
        return ajustement;
    }

    private static List<Ajustement> lignes(int nombre) {
        List<Ajustement> lignes = new ArrayList<>();
        for (int i = 0; i < nombre; i++) {
            lignes.add(ajustement(String.format("CIP%04d", i)));
        }
        return lignes;
    }

    /** Les lignes arrivent de la base dans un ordre quelconque : c'est le rapport qui les range. */
    private static List<Ajustement> lignesDesordonnees(int nombre) {
        List<Ajustement> lignes = lignes(nombre);
        java.util.Collections.shuffle(lignes, new java.util.Random(42));
        return lignes;
    }
}
