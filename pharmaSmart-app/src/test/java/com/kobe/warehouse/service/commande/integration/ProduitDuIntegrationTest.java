package com.kobe.warehouse.service.commande.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.kobe.warehouse.domain.AvoirClient;
import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.UninsuredCustomer;
import com.kobe.warehouse.domain.enumeration.AvoirClientStatut;
import com.kobe.warehouse.domain.enumeration.CanalEnvoi;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.repository.AvoirClientRepository;
import com.kobe.warehouse.service.dashboard.integration.AbstractDashboardIntegrationTest;
import com.kobe.warehouse.service.dto.ClientsEnAttenteDTO;
import com.kobe.warehouse.service.dto.ProduitDuNonCommandeDTO;
import com.kobe.warehouse.service.impl.ProduitDuServiceImpl;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Ce que l'officine doit à ses clients, vu depuis la commande.
 *
 * <p>Le cœur de la règle est dans le JPQL, pas dans le service : ces cas vérifient donc surtout ce
 * que « déjà commandé » veut dire, et notamment qu'un brouillon ne couvre rien.
 */
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Produits dus aux clients")
class ProduitDuIntegrationTest extends AbstractDashboardIntegrationTest {

    private ProduitDuServiceImpl service;
    private Fournisseur grossiste;

    @BeforeEach
    void preparer() {
        service = new ProduitDuServiceImpl(IntegrationPostgresDatabase.bean(AvoirClientRepository.class));
        grossiste = fournisseur("GROSSISTE DU");
    }

    @Nested
    @DisplayName("Liste des produits dus non commandés")
    class ProduitsDusNonCommandes {

        @Test
        @DisplayName("un produit promis et jamais commandé figure dans la liste")
        void promisEtJamaisCommande() {
            Produit produit = produit("DOLIPRANE 1000", 1);
            avoir(produit, client("KOUAME", "Ama", "0101010101"), 3, 0);

            List<ProduitDuNonCommandeDTO> dus = service.lireProduitsDusNonCommandes();

            ProduitDuNonCommandeDTO ligne = ligneDe(dus, produit);
            assertThat(ligne.quantiteDue()).isEqualTo(3);
            assertThat(ligne.nbClients()).isEqualTo(1);
            assertThat(ligne.nbDemandes()).isEqualTo(1);
            assertThat(ligne.attenteDepuis()).isNotNull();
        }

        @Test
        @DisplayName("une commande ENVOYÉE couvre le produit : il sort de la liste")
        void commandeEnvoyeeCouvre() {
            Produit produit = produit("AMOXICILLINE 500", 1);
            avoir(produit, client("DIALLO", "Sita", "0202020202"), 2, 0);
            commandeSur(produit, LocalDateTime.now());

            assertThat(service.lireProduitsDusNonCommandes())
                    .extracting(ProduitDuNonCommandeDTO::produitId)
                    .doesNotContain(produit.getId());
        }

        @Test
        @DisplayName("un BROUILLON ne couvre rien : le produit reste dû")
        void brouillonNeCouvrePas() {
            // Le cœur de la règle. Avant que l'envoi ne soit daté, toute commande en attente
            // passait pour transmise : un bon oublié dans un tiroir faisait disparaître le
            // produit de cette liste, c'est-à-dire exactement le cas qu'elle doit signaler.
            Produit produit = produit("IBUPROFENE 400", 1);
            avoir(produit, client("TRAORE", "Moussa", "0303030303"), 5, 0);
            commandeSur(produit, null);

            assertThat(service.lireProduitsDusNonCommandes())
                    .extracting(ProduitDuNonCommandeDTO::produitId)
                    .contains(produit.getId());
        }

        @Test
        @DisplayName("un avoir déjà rattaché à une commande ne compte pas")
        void avoirDejaRattache() {
            Produit produit = produit("PARACETAMOL 500", 1);
            AvoirClient avoir = avoir(produit, client("BAMBA", "Awa", "0404040404"), 4, 0);
            avoir.setCommande(commandeSur(produit, LocalDateTime.now()));
            em.flush();

            assertThat(service.lireProduitsDusNonCommandes())
                    .extracting(ProduitDuNonCommandeDTO::produitId)
                    .doesNotContain(produit.getId());
        }

        @Test
        @DisplayName("un avoir intégralement remis est soldé")
        void avoirIntegralementRemis() {
            Produit produit = produit("VITAMINE C", 1);
            avoir(produit, client("KONE", "Yao", "0505050505"), 3, 3);

            assertThat(service.lireProduitsDusNonCommandes())
                    .extracting(ProduitDuNonCommandeDTO::produitId)
                    .doesNotContain(produit.getId());
        }

        @Test
        @DisplayName("un avoir partiellement remis reste dû pour son solde")
        void avoirPartiellementRemis() {
            Produit produit = produit("ASPIRINE 500", 1);
            avoir(produit, client("YAO", "Afi", "0606060606"), 5, 2);

            assertThat(ligneDe(service.lireProduitsDusNonCommandes(), produit).quantiteDue())
                    .isEqualTo(3);
        }

        @Test
        @DisplayName("un avoir au porteur compte comme demande, sans client identifié")
        void avoirAuPorteur() {
            // createAvoirsFromSale crée un avoir même sans client. Le compter comme « 0 client »
            // le ferait disparaître de l'écran alors que la promesse a bien été faite : c'est
            // pourquoi le service rend les deux chiffres.
            Produit produit = produit("SERUM PHYSIOLOGIQUE", 1);
            avoir(produit, null, 2, 0);

            ProduitDuNonCommandeDTO ligne = ligneDe(service.lireProduitsDusNonCommandes(), produit);
            assertThat(ligne.nbClients()).isZero();
            assertThat(ligne.nbDemandes()).isEqualTo(1);
            assertThat(ligne.quantiteDue()).isEqualTo(2);
        }

        @Test
        @DisplayName("deux avoirs du même client comptent pour un client, deux demandes")
        void memeClientDeuxFois() {
            Produit produit = produit("SIROP TOUX", 1);
            UninsuredCustomer client = client("SANOGO", "Ali", "0707070707");
            avoir(produit, client, 1, 0);
            avoir(produit, client, 2, 0);

            ProduitDuNonCommandeDTO ligne = ligneDe(service.lireProduitsDusNonCommandes(), produit);
            assertThat(ligne.nbClients()).isEqualTo(1);
            assertThat(ligne.nbDemandes()).isEqualTo(2);
            assertThat(ligne.quantiteDue()).isEqualTo(3);
        }

        @Test
        @DisplayName("un avoir clôturé ne figure plus")
        void avoirCloture() {
            Produit produit = produit("PANSEMENT", 1);
            AvoirClient avoir = avoir(produit, client("OUATTARA", "Ina", "0808080808"), 2, 0);
            avoir.setStatut(AvoirClientStatut.CLOTURE);
            em.flush();

            assertThat(service.lireProduitsDusNonCommandes())
                    .extracting(ProduitDuNonCommandeDTO::produitId)
                    .doesNotContain(produit.getId());
        }

        @Test
        @DisplayName("la liste commence par la promesse la plus ancienne")
        void laPlusAncienneDAbord() {
            Produit recent = produit("RECENT", 1);
            Produit ancien = produit("ANCIEN", 1);
            avoir(recent, client("A", "A", "0001"), 1, 0)
                    .setCreatedAt(LocalDateTime.now().minusDays(1));
            avoir(ancien, client("B", "B", "0002"), 1, 0)
                    .setCreatedAt(LocalDateTime.now().minusDays(40));
            em.flush();

            List<Integer> ordre = service.lireProduitsDusNonCommandes().stream()
                    .map(ProduitDuNonCommandeDTO::produitId)
                    .filter(id -> id.equals(recent.getId()) || id.equals(ancien.getId()))
                    .toList();

            assertThat(ordre).containsExactly(ancien.getId(), recent.getId());
        }
    }

    @Nested
    @DisplayName("Clients en attente, par produit")
    class ClientsEnAttente {

        @Test
        @DisplayName("une commande envoyée ne fait pas disparaître le dû")
        void commandeEnvoyeeNEffacePasLeDu() {
            // Contrairement à la liste précédente : « 3 clients attendent ce produit » reste vrai
            // pendant que le bon est en route, et c'est ce qui justifie de ne pas baisser la
            // quantité commandée.
            Produit produit = produit("OMEPRAZOLE 20", 1);
            avoir(produit, client("COULIBALY", "Ben", "0909090909"), 6, 0);
            commandeSur(produit, LocalDateTime.now());

            Map<Integer, ClientsEnAttenteDTO> parProduit = service.lireClientsEnAttente(Set.of(produit.getId()));

            assertThat(parProduit).containsKey(produit.getId());
            assertThat(parProduit.get(produit.getId()).quantiteDue()).isEqualTo(6);
        }

        @Test
        @DisplayName("une liste de produits vide ne déclenche aucune requête")
        void listeVide() {
            assertThat(service.lireClientsEnAttente(Set.of())).isEmpty();
            assertThat(service.lireClientsEnAttente(null)).isEmpty();
        }

        @Test
        @DisplayName("un produit sans dû est absent du résultat")
        void produitSansDu() {
            Produit produit = produit("COMPRESSE", 1);

            assertThat(service.lireClientsEnAttente(Set.of(produit.getId()))).isEmpty();
        }
    }

    // --- fabriques ---

    private ProduitDuNonCommandeDTO ligneDe(List<ProduitDuNonCommandeDTO> dus, Produit produit) {
        return dus.stream()
                .filter(d -> d.produitId().equals(produit.getId()))
                .findFirst()
                .orElseThrow();
    }

    private AvoirClient avoir(Produit produit, UninsuredCustomer client, int quantite, int quantiteRemise) {
        AvoirClient avoir = new AvoirClient();
        avoir.setReference(unique("AV"));
        avoir.setProduit(produit);
        avoir.setCustomer(client);
        avoir.setQuantite(quantite);
        avoir.setQuantiteRemise(quantiteRemise);
        avoir.setMontant(quantite * 1_000);
        avoir.setStatut(AvoirClientStatut.OUVERT);
        avoir.setCreatedAt(LocalDateTime.now().minusDays(3));
        avoir.setCreatedBy(utilisateur);
        em.persist(avoir);
        em.flush();
        return avoir;
    }

    /** Commande REQUESTED portant le produit, datée d'un envoi ou laissée à l'état de brouillon. */
    private Commande commandeSur(Produit produit, LocalDateTime dateEnvoi) {
        referencement(produit, grossiste);
        Commande commande = commande(grossiste, OrderStatut.REQUESTED, 0);
        if (dateEnvoi != null) {
            commande.setDateEnvoi(dateEnvoi);
            commande.setCanalEnvoi(CanalEnvoi.MAIL);
        }
        ligneDeCommande(commande, produit);
        em.flush();
        return commande;
    }
}
