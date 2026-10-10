package com.kobe.warehouse.service.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.config.FileStorageProperties;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.dto.CommandeDTO;
import com.kobe.warehouse.service.dto.OrderLineDTO;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

/**
 * Le bon de commande est l'un des rares documents de l'officine qui sorte de ses murs : il part chez
 * le grossiste, qui livre ce qu'il y lit. Une ligne absente n'est pas une gêne d'affichage, c'est
 * une référence qui ne sera pas livrée.
 *
 * <p>Une commande de plus de cinquante-cinq lignes s'imprime sur plusieurs pages, et c'est là que
 * tout se joue. La pagination remplace, page après page, la variable de gabarit qui porte les
 * lignes — encore faut-il qu'elle remplace celle que le gabarit lit — et redemande la liste à
 * chaque découpe — encore faut-il qu'elle soit toujours dans le même ordre. Deux conditions que
 * rien ne vérifiait, et dont l'échec produit un bon parfaitement lisible mais faux.
 */
@DisplayName("CommandeReportReportService — bon de commande")
class CommandeReportReportServiceTest {

    /** Gabarit minimal : Flying Saucer rend réellement, il lui faut du XHTML valide. */
    private static final String HTML = "<html><head><title>Commande</title></head><body><p>commande</p></body></html>";

    // Doublures créées à l'initialisation des champs, et non dans un @BeforeEach : le délai de
    // quinze secondes qui borne les méthodes de cycle de vie coupait l'amorçage de Mockito.
    private final SpringTemplateEngine templateEngine = mock(SpringTemplateEngine.class);
    private final StorageService storageService = mock(StorageService.class);


    private final CommandeReportReportService service = new CommandeReportReportService(
        templateEngine,
        storageService
    );

    private Magasin magasin;

    /**
     * Tranches relevees au moment du rendu. Le contexte de gabarit est un objet unique, remplace
     * variable par variable a chaque page : le capturer ne rendrait que son dernier etat.
     */
    private final List<List<OrderLineDTO>> tranches = new ArrayList<>();

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
            List<OrderLineDTO> lignes = (List<OrderLineDTO>) context.getVariable(Constant.COMMANDE_ITEMS);
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
        @DisplayName("une commande courte tient sur une seule page")
        void commandeCourte() {
            byte[] pdf = service.export(commande("CMD-001", lignes(10)));

            assertThat(pdf).isNotEmpty();
            assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
            assertThat(service.getParameters())
                .containsEntry(Constant.IS_LAST_PAGE, true)
                .containsEntry(Constant.PAGE_COUNT, "1/1");
        }

        @Test
        @DisplayName("une commande longue s'imprime sur plusieurs pages")
        void commandeLongue() {
            byte[] pdf = service.export(commande("CMD-002", lignes(120)));

            assertThat(pdf).isNotEmpty();
            assertThat(service.getParameters()).containsEntry(Constant.PAGE_COUNT, "3/3");
        }

        @Test
        @DisplayName("le document porte le magasin, la commande et son nombre de lignes")
        void variablesDuGabarit() {
            CommandeDTO commande = commande("CMD-003", lignes(7));

            service.export(commande);

            assertThat(service.getParameters())
                .containsEntry(Constant.MAGASIN, magasin)
                .containsEntry(Constant.COMMANDE, commande)
                .containsEntry(Constant.ITEM_SIZE, 7);
        }

        @Test
        @DisplayName("le pied de page reprend les mentions légales de l'officine")
        void piedDePage() {
            service.export(commande("CMD-004", lignes(3)));

            assertThat((String) service.getParameters().get(Constant.FOOTER)).contains("RC-001").contains("0102030405");
        }

        /** La devise vient de la configuration : le rapport ne doit pas en imposer une. */
        @Test
        @DisplayName("la devise n'est pas fixée par le rapport")
        void deviseNonImposee() {
            service.export(commande("CMD-005", lignes(3)));

            assertThat(service.getParameters()).doesNotContainKey(Constant.DEVISE);
        }
    }

    // ===== pagination =====

    @Nested
    @DisplayName("Pagination")
    class Pagination {

        /**
         * Le premier des deux défauts : la pagination écrivait la tranche de lignes sous la variable
         * commune {@code items}, alors que le gabarit de la commande lit {@code commande_items}. Les
         * pages suivantes réimprimaient donc les cinquante-cinq premières lignes, indéfiniment.
         */
        @Test
        @DisplayName("chaque page porte sa tranche dans la variable que lit le gabarit")
        void tranchesDansLaBonneVariable() {
            service.export(commande("CMD-010", lignes(120)));

            List<List<OrderLineDTO>> pages = tranchesRendues();

            assertThat(pages).hasSize(3);
            assertThat(pages.get(0)).hasSize(55);
            assertThat(pages.get(1)).hasSize(55);
            assertThat(pages.get(2)).hasSize(10);
        }

        /**
         * Le second défaut : {@code export} triait une copie locale, mais la pagination rappelle
         * {@code getItems()} pour découper les pages suivantes. La première page sortait dans l'ordre
         * alphabétique et les suivantes dans celui de la base — certaines lignes paraissaient deux
         * fois, d'autres jamais.
         */
        @Test
        @DisplayName("aucune ligne n'est imprimée deux fois, aucune n'est oubliée")
        void toutesLesLignesUneSeuleFois() {
            CommandeDTO commande = commande("CMD-011", lignes(120));

            service.export(commande);

            List<String> imprimees = tranchesRendues()
                .stream()
                .flatMap(List::stream)
                .map(OrderLineDTO::getProduitLibelle)
                .toList();

            assertThat(imprimees).hasSize(120).doesNotHaveDuplicates();
            assertThat(imprimees)
                .containsExactlyInAnyOrderElementsOf(
                    commande.getOrderLines().stream().map(OrderLineDTO::getProduitLibelle).toList()
                );
        }

        @Test
        @DisplayName("les lignes sortent dans l'ordre alphabétique, d'un bout à l'autre du bon")
        void ordreAlphabetique() {
            service.export(commande("CMD-012", lignesDesordonnees(120)));

            List<String> imprimees = tranchesRendues().stream().flatMap(List::stream).map(OrderLineDTO::getProduitLibelle).toList();

            assertThat(imprimees).isSorted();
        }

        @Test
        @DisplayName("seule la dernière page est signalée comme telle")
        void dernierePageSignalee() {
            service.export(commande("CMD-013", lignes(120)));

            assertThat(service.getParameters()).containsEntry(Constant.IS_LAST_PAGE, true);
        }

        @Test
        @DisplayName("un nombre de lignes multiple de la page ne crée pas de page vide")
        void multipleExact() {
            service.export(commande("CMD-014", lignes(110)));

            assertThat(tranchesRendues()).hasSize(2);
            assertThat(service.getParameters()).containsEntry(Constant.PAGE_COUNT, "2/2");
        }

        private List<List<OrderLineDTO>> tranchesRendues() {
            return tranches;
        }
    }

    // ===== fabriques =====

    private static CommandeDTO commande(String reference, List<OrderLineDTO> lignes) {
        CommandeDTO commande = new CommandeDTO();
        commande.setOrderReference(reference);
        commande.setOrderLines(lignes);
        return commande;
    }

    private static List<OrderLineDTO> lignes(int nombre) {
        List<OrderLineDTO> lignes = new ArrayList<>();
        for (int i = 0; i < nombre; i++) {
            lignes.add(new OrderLineDTO().setProduitLibelle(String.format("PRODUIT-%03d", i)));
        }
        return lignes;
    }

    /** Les lignes arrivent de la base dans un ordre quelconque : c'est le rapport qui les range. */
    private static List<OrderLineDTO> lignesDesordonnees(int nombre) {
        List<OrderLineDTO> lignes = lignes(nombre);
        java.util.Collections.shuffle(lignes, new java.util.Random(42));
        return lignes;
    }
}
