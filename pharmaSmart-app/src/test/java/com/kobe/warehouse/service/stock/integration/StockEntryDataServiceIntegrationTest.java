package com.kobe.warehouse.service.stock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.service.dto.DeliveryReceiptDTO;
import com.kobe.warehouse.service.dto.DeliveryTotalsDTO;
import com.kobe.warehouse.service.dto.filter.DeliveryReceiptFilterDTO;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * L'écran des bons de livraison ne fait que lire, mais il lit de six façons différentes : une
 * requête Criteria partant des lignes de commande pour compter les bons distincts, une autre pour
 * les paginer, une {@code Specification} partant des commandes, une agrégation en {@code Tuple}
 * pour les totaux, une projection de recherche et une projection de détail.
 *
 * <p>Aucune de ces six ne s'éprouve sur des doublures : ce sont des requêtes construites
 * dynamiquement, sur des tables <b>partitionnées par date</b> et à clé composite, dont le filtrage
 * par défaut se limite aux cinq derniers mois. Le seul moyen de vérifier qu'elles cadrent le même
 * ensemble de bons — et que le compte, la page et les totaux restent d'accord entre eux — est de
 * les poser sur de vraies commandes.
 */
@DisplayName("StockEntryDataService — consultation des bons de livraison sur PostgreSQL")
class StockEntryDataServiceIntegrationTest extends AbstractStockIntegrationTest {

    @Test
    @DisplayName("Le compte et la page des bons portent sur le même ensemble")
    void compteEtPageSontDAccord() {
        Produit produit = produitReference("DOLIPRANE");
        Commande premiere = bonRecu(produit, 10, 6_000);
        Commande seconde = bonRecu(produit, 4, 6_000);
        viderLeCache();

        Page<DeliveryReceiptDTO> page = services.stockEntryDataService.fetchAllReceipts(filtreParDefaut(), PageRequest.of(0, 20));

        assertEquals(2, page.getTotalElements());
        assertEquals(
            Set.of(premiere.getId().getId(), seconde.getId().getId()),
            page.getContent().stream().map(DeliveryReceiptDTO::getId).collect(java.util.stream.Collectors.toSet())
        );
    }

    @Test
    @DisplayName("Un bon sans ligne n'apparaît pas dans une liste construite depuis les lignes")
    void bonSansLigneEstInvisible() {
        Produit produit = produitReference("AVEC LIGNE");
        bonRecu(produit, 10, 6_000);
        commande(produit.getFournisseurProduitPrincipal().getFournisseur(), OrderStatut.CLOSED);
        viderLeCache();

        assertEquals(
            1,
            services.stockEntryDataService.fetchAllReceipts(filtreParDefaut(), PageRequest.of(0, 20)).getTotalElements(),
            "la requête part des lignes de commande : un bon vide n'y figure pas"
        );
        assertEquals(
            2,
            services.stockEntryDataService.fetchAllWithoutDetail(filtreParDefaut(), PageRequest.of(0, 20)).getTotalElements(),
            "la liste sans détail part des commandes et voit donc le bon vide"
        );
    }

    @Test
    @DisplayName("La liste sans détail rend le nombre de lignes de chaque bon")
    void listeSansDetailCompteLesLignes() {
        Produit premier = produitReference("PREMIER");
        Produit second = produitReference("SECOND");
        Fournisseur fournisseur = premier.getFournisseurProduitPrincipal().getFournisseur();
        Commande commande = commande(fournisseur, OrderStatut.CLOSED);
        ligneDeCommande(commande, premier.getFournisseurProduitPrincipal(), 10, 10);
        ligneDeCommande(commande, second.getFournisseurProduitPrincipal(), 4, 4);
        viderLeCache();

        DeliveryReceiptDTO bon = services.stockEntryDataService
            .fetchAllWithoutDetail(filtreParDefaut(), PageRequest.of(0, 20))
            .getContent()
            .getFirst();

        assertEquals(commande.getId().getId(), bon.getId());
        assertEquals(2, bon.getItemSize());
    }

    @Test
    @DisplayName("Les totaux agrègent les montants des bons retenus par le filtre")
    void totauxAgreges() {
        Produit produit = produitReference("A TOTALISER");
        Commande premiere = bonRecu(produit, 10, 6_000);
        premiere.setGrossAmount(60_000);
        premiere.setHtAmount(50_000);
        premiere.setTaxAmount(10_000);
        Commande seconde = bonRecu(produit, 4, 6_000);
        seconde.setGrossAmount(24_000);
        seconde.setHtAmount(20_000);
        seconde.setTaxAmount(4_000);
        viderLeCache();

        DeliveryTotalsDTO totaux = services.stockEntryDataService.computeTotals(filtreParDefaut());

        assertEquals(2, totaux.count());
        assertEquals(84_000, totaux.totalGrossAmount());
        assertEquals(70_000, totaux.totalNetAmount());
        assertEquals(14_000, totaux.totalTaxAmount());
    }

    @Test
    @DisplayName("Le filtre par statut, fournisseur et période cadre le même ensemble partout")
    void filtrageParStatutFournisseurEtPeriode() {
        Produit produit = produitReference("A FILTRER");
        Fournisseur fournisseur = produit.getFournisseurProduitPrincipal().getFournisseur();
        Commande close = bonRecu(produit, 10, 6_000);
        close.setGrossAmount(60_000);

        Produit autre = produitReference("AUTRE FOURNISSEUR");
        Commande enAttente = commande(autre.getFournisseurProduitPrincipal().getFournisseur(), OrderStatut.REQUESTED);
        ligneDeCommande(enAttente, autre.getFournisseurProduitPrincipal(), 5, 0);
        viderLeCache();

        DeliveryReceiptFilterDTO parStatut = filtreParDefaut();
        parStatut.setStatuts(Set.of(OrderStatut.CLOSED));
        assertEquals(1, services.stockEntryDataService.fetchAllReceipts(parStatut, PageRequest.of(0, 20)).getTotalElements());
        assertEquals(1, services.stockEntryDataService.computeTotals(parStatut).count());

        DeliveryReceiptFilterDTO parFournisseur = filtreParDefaut().setFournisseurId(fournisseur.getId().longValue());
        parFournisseur.setStatuts(Set.of(OrderStatut.CLOSED, OrderStatut.REQUESTED));
        assertEquals(
            List.of(close.getId().getId()),
            services.stockEntryDataService
                .fetchAllWithoutDetail(parFournisseur, PageRequest.of(0, 20))
                .map(DeliveryReceiptDTO::getId)
                .getContent()
        );

        DeliveryReceiptFilterDTO horsPeriode = new DeliveryReceiptFilterDTO()
            .setFromDate(LocalDate.now().minusYears(3))
            .setToDate(LocalDate.now().minusYears(2));
        // meme fenetre pour les deux chemins de lecture
        horsPeriode.setStatuts(Set.of(OrderStatut.CLOSED));
        assertEquals(0, services.stockEntryDataService.fetchAllReceipts(horsPeriode, PageRequest.of(0, 20)).getTotalElements());
        assertEquals(0, services.stockEntryDataService.computeTotals(horsPeriode).count());
    }

    @Test
    @DisplayName("La recherche par référence retrouve le bon, le détail rend ses lignes")
    void rechercheParReferenceEtDetail() {
        Produit produit = produitReference("A CHERCHER");
        Commande commande = bonRecu(produit, 10, 6_000);
        viderLeCache();

        DeliveryReceiptFilterDTO parReference = filtreParDefaut().setSearchByRef(commande.getReceiptReference());
        assertEquals(1, services.stockEntryDataService.fetchAllWithoutDetail(parReference, PageRequest.of(0, 20)).getTotalElements());

        assertEquals(
            1,
            services.stockEntryDataService.findAllByCommandeIdAndCommandeOrderDate(commande.getId()).size(),
            "le détail du bon rend sa ligne"
        );
        assertTrue(services.stockEntryDataService.findOneById(commande.getId()).isPresent());
        assertTrue(
            services.stockEntryDataService
                .findOneById(new com.kobe.warehouse.domain.CommandeId(999_999, LocalDate.now()))
                .isEmpty()
        );
    }

    @Test
    @DisplayName("La recherche rapide ne remonte que les bons clos des douze derniers mois")
    void rechercheRapide() {
        Produit produit = produitReference("RECHERCHE RAPIDE");
        Commande close = bonRecu(produit, 10, 6_000);
        Commande enAttente = commande(produit.getFournisseurProduitPrincipal().getFournisseur(), OrderStatut.REQUESTED);
        ligneDeCommande(enAttente, produit.getFournisseurProduitPrincipal(), 5, 0);
        viderLeCache();

        assertTrue(
            services.stockEntryDataService
                .fetchAllReceipts(close.getReceiptReference())
                .getContent()
                .stream()
                .anyMatch(p -> p.getReceiptReference().equals(close.getReceiptReference()))
        );
        assertTrue(
            services.stockEntryDataService
                .fetchAllReceipts(enAttente.getReceiptReference())
                .getContent()
                .isEmpty(),
            "un bon encore en attente n'est pas proposé"
        );
    }

    /**
     * Les deux chemins de lecture ne traitent pas l'absence de période de la même façon : celui qui
     * part des lignes de commande applique un {@code BETWEEN} sur les bornes du filtre telles
     * quelles — nulles, il ne rend rien — là où celui qui part des commandes retombe sur les cinq
     * derniers mois. L'écran pose donc toujours une période ; ce test fige l'écart.
     */
    @Test
    @DisplayName("Sans période, la liste détaillée ne rend rien là où la liste simple retombe sur cinq mois")
    void periodeObligatoireCoteLignes() {
        Produit produit = produitReference("SANS PERIODE");
        bonRecu(produit, 10, 6_000);
        viderLeCache();

        DeliveryReceiptFilterDTO sansPeriode = new DeliveryReceiptFilterDTO();

        assertEquals(0, services.stockEntryDataService.fetchAllReceipts(sansPeriode, PageRequest.of(0, 20)).getTotalElements());
        assertEquals(1, services.stockEntryDataService.fetchAllWithoutDetail(sansPeriode, PageRequest.of(0, 20)).getTotalElements());
    }

    @Test
    @DisplayName("Le compteur par statut ne retient que les commandes fournisseur")
    void compteurParStatut() {
        Produit produit = produitReference("A COMPTER");
        long depart = services.stockEntryDataService.countByOrderStatusAndType(OrderStatut.REQUESTED);

        Commande enAttente = commande(produit.getFournisseurProduitPrincipal().getFournisseur(), OrderStatut.REQUESTED);
        ligneDeCommande(enAttente, produit.getFournisseurProduitPrincipal(), 5, 0);
        viderLeCache();

        assertEquals(depart + 1, services.stockEntryDataService.countByOrderStatusAndType(OrderStatut.REQUESTED));
    }

    // ===== fabriques du jeu d'essai propres aux bons de livraison =====

    /**
     * Une période est toujours posée : la requête partant des lignes de commande applique un
     * {@code BETWEEN} inconditionnel sur les bornes du filtre (cf. {@link #periodeObligatoireCoteLignes()}).
     */
    private DeliveryReceiptFilterDTO filtreParDefaut() {
        return new DeliveryReceiptFilterDTO().setFromDate(LocalDate.now().minusDays(1)).setToDate(LocalDate.now().plusDays(1));
    }

    private Produit produitReference(String libelle) {
        Produit produit = produit(unique(libelle));
        fournisseurProduit(produit);
        em.flush();
        return produit;
    }

    /** Un bon de livraison clos, avec sa ligne : le cas nominal de l'écran. */
    private Commande bonRecu(Produit produit, int quantite, int prixAchat) {
        FournisseurProduit reference = produit.getFournisseurProduitPrincipal();
        Commande commande = commande(reference.getFournisseur(), OrderStatut.CLOSED);
        ligneDeCommande(commande, reference, quantite, quantite);
        return commande;
    }
}
