package com.kobe.warehouse.service.customer.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.UninsuredCustomer;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.repository.AssuredCustomerRepository;
import com.kobe.warehouse.repository.ClientTiersPayantRepository;
import com.kobe.warehouse.repository.CustomerRepository;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.repository.UninsuredCustomerRepository;
import com.kobe.warehouse.service.CustomerDataService;
import com.kobe.warehouse.service.UninsuredCustomerService;
import com.kobe.warehouse.service.customer.CustomerFicheService;
import com.kobe.warehouse.service.customer.CustomerSyntheseDTO;
import com.kobe.warehouse.service.customer.ProduitDelivreDTO;
import com.kobe.warehouse.service.reglement.differe.service.ReglementDiffereService;
import com.kobe.warehouse.service.dashboard.integration.AbstractDashboardIntegrationTest;
import com.kobe.warehouse.service.dto.CustomerDTO;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Fiche client (docs/PLAN-FICHE-CLIENT.md). Lot 0 : l'encours affiché, le filtre de statut, la
 * désactivation et la suppression d'un client qui a des ventes. Lot 1 : la synthèse et les
 * produits délivrés.
 */
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Fiche client — lots 0 et 1")
class FicheClientIntegrationTest extends AbstractDashboardIntegrationTest {

    private CustomerDataService customers;
    private CashRegister caisse;
    private String nom;

    @BeforeEach
    void preparer() {
        customers = new CustomerDataService(
            IntegrationPostgresDatabase.bean(CustomerRepository.class),
            IntegrationPostgresDatabase.bean(AssuredCustomerRepository.class),
            em,
            IntegrationPostgresDatabase.bean(ClientTiersPayantRepository.class),
            IntegrationPostgresDatabase.bean(UninsuredCustomerRepository.class),
            IntegrationPostgresDatabase.bean(SalesRepository.class)
        );
        caisse = caisseOuverte(0);
        nom = unique("FICHE").toUpperCase();
    }

    @Test
    @DisplayName("l'encours est le reste dû sur les ventes différées, hors annulées et hors comptant")
    void encoursReel() {
        UninsuredCustomer debiteur = client(nom, "Awa", "0700000001");
        UninsuredCustomer aJour = client(nom, "Yao", "0700000002");
        venteDifferee(caisse, 10_000, 3_000, debiteur, LocalDate.now());
        venteDifferee(caisse, 8_000, 2_000, debiteur, LocalDate.now());
        CashSale annulee = venteDifferee(caisse, 5_000, 5_000, debiteur, LocalDate.now());
        annulee.setCanceled(true);
        venteComptant(caisse, 4_000).setCustomer(debiteur);
        em.flush();

        List<CustomerDTO> liste = rechercher("STANDARD", Status.ENABLE);

        assertThat(liste).filteredOn(c -> c.getId().equals(debiteur.getId())).singleElement().extracting(CustomerDTO::getEncours).isEqualTo(5_000);
        assertThat(liste).filteredOn(c -> c.getId().equals(aJour.getId())).singleElement().extracting(CustomerDTO::getEncours).isEqualTo(0);
    }

    @Test
    @DisplayName("l'encours est aussi calculé dans la vue « Tous »")
    void encoursVueTous() {
        UninsuredCustomer debiteur = client(nom, "Awa", "0700000003");
        venteDifferee(caisse, 10_000, 4_500, debiteur, LocalDate.now());

        assertThat(rechercher("TOUT", Status.ENABLE)).singleElement().extracting(CustomerDTO::getEncours).isEqualTo(4_500);
    }

    @Test
    @DisplayName("une recherche ne fait pas remonter les clients désactivés")
    void rechercheGardeLeStatut() {
        client(nom, "Actif", "0700000004");
        UninsuredCustomer inactif = client(nom, "Inactif", "0700000005");
        inactif.setStatus(Status.DISABLE);
        em.flush();

        assertThat(rechercher("STANDARD", Status.ENABLE)).extracting(CustomerDTO::getFirstName).containsExactly("Actif");
        assertThat(rechercher("STANDARD", Status.DISABLE)).extracting(CustomerDTO::getFirstName).containsExactly("Inactif");
    }

    @Test
    @DisplayName("un client désactivé passe dans la liste des désactivés, et revient une fois réactivé")
    void desactiverPuisReactiver() {
        UninsuredCustomer client = client(nom, "Kofi", "0700000006");

        customers.changeStatus(client.getId(), Status.DISABLE);
        em.flush();
        assertThat(rechercher("STANDARD", Status.ENABLE)).isEmpty();
        assertThat(rechercher("STANDARD", Status.DISABLE)).hasSize(1);

        customers.changeStatus(client.getId(), Status.ENABLE);
        em.flush();
        assertThat(rechercher("STANDARD", Status.ENABLE)).hasSize(1);
    }

    @Test
    @DisplayName("supprimer un client qui a des ventes est refusé par un message, et non par une erreur 500")
    void suppressionRefusee() {
        UninsuredCustomer client = client(nom, "Mariam", "0700000007");
        venteDifferee(caisse, 3_000, 0, client, LocalDate.now());
        UninsuredCustomerService service = new UninsuredCustomerService(IntegrationPostgresDatabase.bean(UninsuredCustomerRepository.class));

        assertThatThrownBy(() -> service.deleteCustomerById(client.getId())).isInstanceOf(GenericError.class).hasMessageContaining("ventes");
    }

    // ── Lot 1 : fiche 360° ──

    @Test
    @DisplayName("la synthèse donne l'encours, la dernière visite et les achats des douze derniers mois")
    void synthese() {
        UninsuredCustomer client = client(nom, "Awa", "0700000010");
        venteDifferee(caisse, 10_000, 2_500, client, LocalDate.now());
        venteComptant(caisse, 4_000).setCustomer(client);
        venteDifferee(caisse, 7_000, 0, client, LocalDate.now().minusMonths(13));
        CashSale annulee = venteComptant(caisse, 9_000);
        annulee.setCustomer(client);
        annulee.setCanceled(true);
        em.flush();

        CustomerSyntheseDTO synthese = fiche().synthese(client.getId());

        assertThat(synthese.encours()).isEqualTo(2_500);
        assertThat(synthese.nombreAchats()).isEqualTo(2);
        assertThat(synthese.montantAchats()).isEqualTo(14_000);
        assertThat(synthese.derniereVisite()).isNotNull();
    }

    @Test
    @DisplayName("un client sans achat a une synthèse vide, pas une erreur")
    void syntheseVide() {
        UninsuredCustomer client = client(nom, "Yao", "0700000011");
        CustomerSyntheseDTO synthese = fiche().synthese(client.getId());
        assertThat(synthese.encours()).isZero();
        assertThat(synthese.nombreAchats()).isZero();
        assertThat(synthese.derniereVisite()).isNull();
    }

    @Test
    @DisplayName("les produits délivrés sont agrégés par produit, sur la période, hors ventes annulées")
    void produitsDelivres() {
        UninsuredCustomer client = client(nom, "Kofi", "0700000012");
        Produit doliprane = produit(unique("DOLIPRANE"), 0);
        Produit amoxicilline = produit(unique("AMOXICILLINE"), 0);
        CashSale premiere = venteComptant(caisse, 0);
        premiere.setCustomer(client);
        ligneDeVente(premiere, doliprane, 2);
        CashSale seconde = venteComptant(caisse, 0);
        seconde.setCustomer(client);
        ligneDeVente(seconde, doliprane, 1);
        ligneDeVente(seconde, amoxicilline, 1);
        CashSale annulee = venteComptant(caisse, 0);
        annulee.setCustomer(client);
        annulee.setCanceled(true);
        ligneDeVente(annulee, amoxicilline, 5);
        em.flush();

        List<ProduitDelivreDTO> produits = fiche()
            .produitsDelivres(client.getId(), LocalDate.now().minusDays(1), LocalDate.now(), null, PageRequest.of(0, 20))
            .getContent();

        assertThat(produits).hasSize(2);
        ProduitDelivreDTO paracetamol = produits.stream().filter(p -> p.produitId().equals(doliprane.getId())).findFirst().orElseThrow();
        assertThat(paracetamol.nombreDelivrances()).isEqualTo(2);
        assertThat(paracetamol.quantite()).isEqualTo(3);
        ProduitDelivreDTO antibiotique = produits.stream().filter(p -> p.produitId().equals(amoxicilline.getId())).findFirst().orElseThrow();
        assertThat(antibiotique.quantite()).isEqualTo(1);

        assertThat(
            fiche().produitsDelivres(client.getId(), LocalDate.now().minusDays(1), LocalDate.now(), "amoxi", PageRequest.of(0, 20)).getContent()
        ).extracting(ProduitDelivreDTO::produitId).containsExactly(amoxicilline.getId());
    }

    @Test
    @DisplayName("un client sans différé n'interroge pas le service des différés (qui échouerait)")
    void sansDiffere() {
        UninsuredCustomer client = client(nom, "Mariam", "0700000013");
        venteComptant(caisse, 4_000).setCustomer(client);
        em.flush();
        ReglementDiffereService differes = mock(ReglementDiffereService.class);

        assertThat(new CustomerFicheService(em, IntegrationPostgresDatabase.bean(SalesRepository.class), differes).differes(client.getId())).isEmpty();
        verifyNoInteractions(differes);
    }

    private CustomerFicheService fiche() {
        return new CustomerFicheService(em, IntegrationPostgresDatabase.bean(SalesRepository.class), mock(ReglementDiffereService.class));
    }

    private List<CustomerDTO> rechercher(String categorie, Status statut) {
        return customers.fetchAllCustomers(categorie, nom, statut, PageRequest.of(0, 20)).getContent();
    }
}
