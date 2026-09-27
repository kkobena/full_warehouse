package com.kobe.warehouse.service.mobile.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import com.kobe.warehouse.service.dto.mobile.MobileTodoDTO;
import com.kobe.warehouse.service.dto.mobile.MobileTodoDTO.TodoItemDTO;
import com.kobe.warehouse.service.dto.mobile.MobileTodoDTO.TodoPriority;
import com.kobe.warehouse.service.mobile.MobileTodoService;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Le pense-bête mobile répond à « que dois-je faire aujourd'hui ? » : commander ce qui manque,
 * relancer ce qui n'est pas payé, écouler ce qui va périmer.
 *
 * <p>C'est délibérément une liste courte — chaque nature d'action y est plafonnée — et c'est là
 * que se logeait un défaut : l'écran la parcourt page par page en se fiant à un <b>total</b>
 * calculé, lui, sans plafond. Une officine avec cent ruptures annonçait cent tâches et n'en servait
 * que vingt, les pages suivantes revenant vides sans explication.
 *
 * <p>Le second défaut est le même que celui des alertes : les impayés se listaient à travers une
 * jointure fermée sur le groupe de tiers payant, ce qui effaçait les tiers payants isolés.
 */

@DisplayName("MobileTodoService — pense-bête des actions prioritaires")
class MobileTodoServiceIntegrationTest extends AbstractMobileIntegrationTest {

    private MobileTodoService service;

    @BeforeEach
    void cablerLeService() {
        service = new MobileTodoService();
        ReflectionTestUtils.setField(service, "entityManager", em);
    }

    // ===== natures d'action =====

    private TodoItemDTO tacheDuProduit(Produit produit) {
        return service
            .getAllTodoItems()
            .stream()
            .filter(t -> "PRODUCT".equals(t.relatedEntityType())
                && produit.getId().longValue() == t.relatedEntityId())
            .findFirst()
            .orElseThrow();
    }

    // ===== regroupement par priorité =====

    private TodoItemDTO tacheDemarque(Produit produit) {
        return service
            .getAllTodoItems()
            .stream()
            .filter(t -> "CREATE_DISCOUNT".equals(t.type())
                && produit.getId().longValue() == t.relatedEntityId())
            .findFirst()
            .orElseThrow();
    }

    // ===== pagination =====

    private TodoItemDTO tacheDeLaFacture(FactureTiersPayant facture) {
        return service
            .getAllTodoItems()
            .stream()
            // Les deux identifiants sont des Long : les comparer avec == compare des references,
            // ce qui ne tient que tant que la valeur reste dans le cache des petits entiers.
            .filter(t -> "INVOICE".equals(t.relatedEntityType()) && Objects.requireNonNull(
                facture.getId()).getId().equals(t.relatedEntityId()))
            .findFirst()
            .orElseThrow();
    }

    // ===== compteurs par priorité =====

    @Nested
    @DisplayName("Natures d'action")
    class NaturesDAction {

        @Test
        @DisplayName("un produit en rupture appelle une commande")
        void rupture() {
            Produit produit = produitEnStock("DOLIPRANE RUPTURE", 0);
            em.flush();

            TodoItemDTO tache = tacheDuProduit(produit);

            assertThat(tache.type()).isEqualTo("REORDER");
            assertThat(tache.title()).isEqualTo("Commander DOLIPRANE RUPTURE");
            assertThat(tache.description()).isEqualTo("Rupture de stock");
            assertThat(tache.priority()).isEqualTo(TodoPriority.URGENT);
            assertThat(tache.actionData()).containsEntry("productId", produit.getId().longValue());
        }

        @Test
        @DisplayName("un produit sous son seuil appelle un réapprovisionnement")
        void stockFaible() {
            Produit produit = produitEnStock("EFFERALGAN BAS", 3);
            em.flush();

            TodoItemDTO tache = tacheDuProduit(produit);

            assertThat(tache.priority()).isEqualTo(TodoPriority.NORMAL);
            assertThat(tache.description()).isEqualTo("Stock: 3/5 (seuil mini)");
        }

        /**
         * Réapprovisionner jusqu'au double du seuil, jamais moins que le seuil lui-même.
         */
        @Test
        @DisplayName("la quantité suggérée vise le double du seuil")
        void quantiteSuggeree() {
            Produit produit = produitEnStock("EFFERALGAN BAS", 3);
            em.flush();

            assertThat(tacheDuProduit(produit).actionData()).containsEntry("suggestedQuantity", 7);
        }

        @Test
        @DisplayName("un produit approvisionné n'appelle aucune action")
        void produitApprovisionne() {
            Produit produit = produitEnStock("STOCK CONFORTABLE", 50);
            em.flush();

            assertThat(service.getAllTodoItems()).noneMatch(
                t -> produit.getId().longValue() == t.relatedEntityId());
        }

        @Test
        @DisplayName("un lot proche de péremption appelle une démarque")
        void peremption() {
            Produit produit = produitEnStock("AMOXICILLINE", 40);
            lot(produit, LocalDate.now().plusDays(20), 12);
            em.flush();

            TodoItemDTO tache = tacheDemarque(produit);

            assertThat(tache.type()).isEqualTo("CREATE_DISCOUNT");
            assertThat(tache.description()).isEqualTo("Expire dans 20 jours (12 unites)");
            assertThat(tache.actionData()).containsEntry("quantity", 12);
        }

        /**
         * Un mois : en deçà, la démarque presse ; au-delà, elle peut attendre.
         */
        @Test
        @DisplayName("l'urgence de la démarque dépend du délai restant")
        void urgenceDeLaDemarque() {
            Produit imminent = produitEnStock("IMMINENT", 40);
            lot(imminent, LocalDate.now().plusDays(30), 10);
            Produit lointain = produitEnStock("LOINTAIN", 40);
            lot(lointain, LocalDate.now().plusDays(31), 10);
            em.flush();

            assertThat(tacheDemarque(imminent).priority()).isEqualTo(TodoPriority.URGENT);
            assertThat(tacheDemarque(lointain).priority()).isEqualTo(TodoPriority.IMPORTANT);
        }

        @Test
        @DisplayName("une facture impayée appelle une relance")
        void impaye() {
            FactureTiersPayant facture = factureAgee(
                tiersPayant("Caisse", groupeTiersPayant("CNAM")),
                120,
                500_000,
                200_000,
                InvoiceStatut.PARTIALLY_PAID
            );
            em.flush();

            TodoItemDTO tache = tacheDeLaFacture(facture);

            assertThat(tache.type()).isEqualTo("CALL_CLIENT");
            assertThat(tache.actionLabel()).isEqualTo("Appeler");
            assertThat(tache.description()).containsPattern(
                "Facture impayee depuis 120 jours \\(300.000 F\\)");
            assertThat(tache.actionData()).containsEntry("phone", "0102030405");
        }

        /**
         * Le défaut que ce test fixe : la liste des relances joignait le groupe de tiers payant en
         * jointure fermée. Une officine dont les tiers payants ne sont pas regroupés n'avait jamais
         * de relance à faire, alors que le compteur en annonçait.
         */
        @Test
        @DisplayName("la facture d'un tiers payant isolé appelle aussi une relance")
        void impayeSansGroupe() {
            FactureTiersPayant facture = factureAgee(tiersPayant("MUGEF isolé", null), 150, 300_000,
                0, InvoiceStatut.NOT_PAID);
            em.flush();

            TodoItemDTO tache = tacheDeLaFacture(facture);

            assertThat(tache.relatedEntityName()).isEqualTo("MUGEF isolé");
            assertThat(tache.actionData()).containsEntry("phone", "0607080910");
        }

        @Test
        @DisplayName("un titre trop long est abrégé pour tenir sur la ligne")
        void titreAbrege() {
            Produit produit = produitEnStock("PARACETAMOL BIOGARAN 1000MG COMPRIMES", 0);
            em.flush();

            // Vingt-cinq caractères au plus, points de suspension compris.
            assertThat(tacheDuProduit(produit).title()).isEqualTo(
                "Commander PARACETAMOL BIOGARAN 1...");
        }
    }

    // ===== utilitaires =====

    @Nested
    @DisplayName("Regroupement par priorité")
    class RegroupementParPriorite {

        @Test
        @DisplayName("ruptures et impayés sont urgents, les péremptions importantes, le reste normal")
        void troisNiveaux() {
            Produit enRupture = produitEnStock(unique("RUPTURE"), 0);
            Produit sousSeuil = produitEnStock(unique("BAS"), 2);
            Produit aPerimer = produitEnStock(unique("PEREMPTION"), 40);
            lot(aPerimer, LocalDate.now().plusDays(60), 10);
            factureAgee(tiersPayant("Caisse", groupeTiersPayant("CNAM")), 120, 500_000, 0,
                InvoiceStatut.NOT_PAID);
            em.flush();

            MobileTodoDTO penseBete = service.getTodoList();

            assertThat(penseBete.urgent()).anyMatch(
                t -> enRupture.getId().longValue() == t.relatedEntityId());
            assertThat(penseBete.important()).anyMatch(
                t -> aPerimer.getId().longValue() == t.relatedEntityId());
            assertThat(penseBete.normal()).anyMatch(
                t -> sousSeuil.getId().longValue() == t.relatedEntityId());
        }

        /**
         * L'incohérence que ce test fixe : un lot périmant dans la semaine porte la priorité
         * {@code URGENT}, mais la liste le rangeait dans le groupe « important » au seul motif
         * qu'il relève de la nature « péremptions ». L'écran affichait donc une ligne rouge dans la
         * section orange, et le compteur d'urgences l'ignorait.
         */
        @Test
        @DisplayName("un lot périmant dans le mois est rangé parmi les urgences")
        void peremptionImminenteEstUrgente() {
            Produit imminent = produitEnStock(unique("IMMINENT"), 40);
            lot(imminent, LocalDate.now().plusDays(5), 10);
            em.flush();

            MobileTodoDTO penseBete = service.getTodoList();

            assertThat(penseBete.urgent()).anyMatch(
                t -> imminent.getId().longValue() == t.relatedEntityId());
            assertThat(penseBete.important()).noneMatch(
                t -> imminent.getId().longValue() == t.relatedEntityId());
        }

        /**
         * Chaque tâche est rangée dans le groupe que sa propre priorité désigne, sans exception.
         */
        @Test
        @DisplayName("aucune tâche n'est rangée dans un groupe qui contredit sa priorité")
        void groupesConformesAuxPriorites() {
            Produit imminent = produitEnStock(unique("IMMINENT"), 40);
            lot(imminent, LocalDate.now().plusDays(5), 10);
            Produit lointain = produitEnStock(unique("LOINTAIN"), 40);
            lot(lointain, LocalDate.now().plusDays(70), 10);
            produitEnStock(unique("RUPTURE"), 0);
            produitEnStock(unique("BAS"), 2);
            em.flush();

            MobileTodoDTO penseBete = service.getTodoList();

            assertThat(penseBete.urgent()).allMatch(t -> t.priority() == TodoPriority.URGENT);
            assertThat(penseBete.important()).allMatch(t -> t.priority() == TodoPriority.IMPORTANT);
            assertThat(penseBete.normal()).allMatch(t -> t.priority() == TodoPriority.NORMAL);
        }

        @Test
        @DisplayName("la liste à plat suit l'ordre de priorité")
        void ordreDeLaListePlate() {
            produitEnStock(unique("RUPTURE"), 0);
            Produit aPerimer = produitEnStock(unique("PEREMPTION"), 40);
            lot(aPerimer, LocalDate.now().plusDays(60), 10);
            produitEnStock(unique("BAS"), 2);
            em.flush();

            List<TodoItemDTO> taches = service.getAllTodoItems();
            int premiereImportante = indexPremiere(taches, TodoPriority.IMPORTANT);
            int premiereNormale = indexPremiere(taches, TodoPriority.NORMAL);

            assertThat(premiereImportante).isLessThan(premiereNormale);
        }

        private int indexPremiere(List<TodoItemDTO> taches, TodoPriority priorite) {
            return IntStream.range(0, taches.size())
                .filter(i -> taches.get(i).priority() == priorite)
                .findFirst()
                .orElse(Integer.MAX_VALUE);
        }
    }

    @Nested
    @DisplayName("Pagination")
    class Pagination {

        /**
         * Le défaut que ce test fixe : le total servait à paginer une liste plafonnée qu'il ne
         * connaissait pas. Vingt et une ruptures annonçaient vingt et une tâches et n'en servaient
         * que vingt, la page suivante revenant vide sans explication.
         */
        @Test
        @DisplayName("le total annoncé est exactement ce que la liste contient")
        void totalEgaleContenu() {
            IntStream.rangeClosed(1, 25).forEach(i -> produitEnStock(unique("RUPTURE"), 0));
            em.flush();

            assertThat(service.getTodoItemsCount()).isEqualTo(service.getAllTodoItems().size());
        }

        @Test
        @DisplayName("le total reste juste quand aucune nature n'atteint son plafond")
        void totalSousLesPlafonds() {
            produitEnStock(unique("RUPTURE"), 0);
            produitEnStock(unique("BAS"), 2);
            em.flush();

            assertThat(service.getTodoItemsCount()).isEqualTo(service.getAllTodoItems().size());
        }

        @Test
        @DisplayName("les pages se suivent sans recouvrement")
        void pagesDistinctes() {
            IntStream.rangeClosed(1, 6).forEach(i -> produitEnStock(unique("RUPTURE"), 0));
            em.flush();

            List<TodoItemDTO> premiere = service.getAllTodoItems(0, 3);
            List<TodoItemDTO> seconde = service.getAllTodoItems(1, 3);

            assertThat(premiere).hasSize(3);
            assertThat(seconde).hasSize(3);
            assertThat(premiere).extracting(TodoItemDTO::id)
                .doesNotContainAnyElementsOf(seconde.stream().map(TodoItemDTO::id).toList());
        }

        @Test
        @DisplayName("une page au-delà de la fin est vide")
        void pageAuDela() {
            assertThat(service.getAllTodoItems(999, 20)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Compteurs par priorité")
    class CompteursParPriorite {

        @Test
        @DisplayName("le compteur urgent additionne ruptures et impayés")
        void compteurUrgent() {
            MobileTodoService.TodoCountsDTO avant = service.getTodoCounts();
            produitEnStock(unique("RUPTURE"), 0);
            produitEnStock(unique("RUPTURE"), 0);
            factureAgee(tiersPayant("Caisse", groupeTiersPayant("CNAM")), 120, 500_000, 0,
                InvoiceStatut.NOT_PAID);
            em.flush();

            assertThat(service.getTodoCounts().urgent() - avant.urgent()).isEqualTo(3);
        }

        @Test
        @DisplayName("le compteur normal suit les produits sous leur seuil")
        void compteurNormal() {
            MobileTodoService.TodoCountsDTO avant = service.getTodoCounts();
            produitEnStock(unique("BAS"), 1);
            produitEnStock(unique("BAS"), 5);
            em.flush();

            assertThat(service.getTodoCounts().normal() - avant.normal()).isEqualTo(2);
        }

        /**
         * Le compteur ne peut plus diverger du groupe : les deux se déduisent des mêmes tâches.
         */
        @Test
        @DisplayName("chaque compteur égale la taille du groupe qu'il annonce")
        void compteursEgalentLesGroupes() {
            Produit imminent = produitEnStock(unique("IMMINENT"), 40);
            lot(imminent, LocalDate.now().plusDays(5), 10);
            Produit lointain = produitEnStock(unique("LOINTAIN"), 40);
            lot(lointain, LocalDate.now().plusDays(70), 10);
            produitEnStock(unique("RUPTURE"), 0);
            produitEnStock(unique("BAS"), 2);
            factureAgee(tiersPayant("Caisse", groupeTiersPayant("CNAM")), 120, 500_000, 0,
                InvoiceStatut.NOT_PAID);
            em.flush();

            MobileTodoDTO penseBete = service.getTodoList();
            MobileTodoService.TodoCountsDTO compteurs = service.getTodoCounts();

            assertThat(compteurs.urgent()).isEqualTo(penseBete.urgent().size());
            assertThat(compteurs.important()).isEqualTo(penseBete.important().size());
            assertThat(compteurs.normal()).isEqualTo(penseBete.normal().size());
            assertThat(compteurs.total()).isEqualTo((int) service.getTodoItemsCount());
        }

        @Test
        @DisplayName("une péremption imminente est comptée comme urgente, non comme importante")
        void peremptionImminenteCompteeUrgente() {
            MobileTodoService.TodoCountsDTO avant = service.getTodoCounts();
            Produit imminent = produitEnStock(unique("IMMINENT"), 40);
            lot(imminent, LocalDate.now().plusDays(5), 10);
            em.flush();

            MobileTodoService.TodoCountsDTO apres = service.getTodoCounts();

            assertThat(apres.urgent() - avant.urgent()).isEqualTo(1);
            assertThat(apres.important() - avant.important()).isZero();
        }

        @Test
        @DisplayName("le total des compteurs additionne les trois niveaux")
        void totalDesCompteurs() {
            MobileTodoService.TodoCountsDTO compteurs = service.getTodoCounts();

            assertThat(compteurs.total()).isEqualTo(
                compteurs.urgent() + compteurs.important() + compteurs.normal());
        }
    }
}
