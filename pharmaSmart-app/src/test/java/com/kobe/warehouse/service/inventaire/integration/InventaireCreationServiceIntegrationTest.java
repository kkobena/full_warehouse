package com.kobe.warehouse.service.inventaire.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.FamilleProduit;
import com.kobe.warehouse.domain.InventoryLot;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Rayon;
import com.kobe.warehouse.domain.enumeration.InventoryType;
import com.kobe.warehouse.service.dto.StoreInventoryDTO;
import com.kobe.warehouse.service.dto.records.StoreInventoryRecord;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * La constitution du périmètre d'un inventaire est faite d'{@code INSERT … SELECT} : c'est une
 * requête, pas du code Java, qui décide des produits à compter. Rien n'en rend compte hors d'une
 * vraie base — un produit désactivé qui se glisse dans le comptage, un lot périmé oublié, une
 * jointure de rayon qui duplique les lignes ne se voient qu'ici.
 */
@DisplayName("InventaireCreationService — constitution du périmètre sur PostgreSQL")
class InventaireCreationServiceIntegrationTest extends AbstractInventaireIntegrationTest {

    @Test
    @DisplayName("Un inventaire magasin prend tous les produits actifs, et eux seuls")
    void perimetreMagasin() {
        produitEnStock(unique("ACTIF A"), 10);
        produitEnStock(unique("ACTIF B"), 20);
        produitDesactive(unique("RETIRE"));
        viderLeCache();

        StoreInventoryDTO inventaire = creer(demande("MAGASIN"));

        assertEquals(2, lignesDe(inventaire.getId()), "le produit retiré du catalogue n'est pas à compter");
        assertEquals(0, lotsDe(inventaire.getId()), "sans gestion de lot, aucune ligne de lot");
    }

    @Test
    @DisplayName("Un inventaire de famille ne prend que les produits de cette famille")
    void perimetreFamille() {
        List<FamilleProduit> familles = em
            .createQuery("SELECT f FROM FamilleProduit f ORDER BY f.id", FamilleProduit.class)
            .setMaxResults(2)
            .getResultList();
        FamilleProduit visee = familles.get(0);
        FamilleProduit autre = familles.get(1);
        produit(unique("DANS LA FAMILLE"), 1_000, 600, visee);
        produit(unique("AILLEURS"), 1_000, 600, autre);
        viderLeCache();

        StoreInventoryDTO inventaire = creer(
            new StoreInventoryRecord(null, STORAGE_RAYON_ID, null, "FAMILLY", visee.getId(), "famille"));

        assertEquals(1, lignesDe(inventaire.getId()));
    }

    @Test
    @DisplayName("Un inventaire de rayon suit l'emplacement du rayon, pas celui demandé")
    void perimetreRayon() {
        Rayon antibiotiques = rayon(reserve, "ANTIBIOTIQUES");
        Produit range = produit(unique("RANGE"));
        ranger(range, antibiotiques);
        produit(unique("HORS RAYON"));
        viderLeCache();

        StoreInventoryDTO inventaire = creer(
            new StoreInventoryRecord(null, STORAGE_RAYON_ID, antibiotiques.getId(), "RAYON", null, "rayon"));

        assertEquals(1, lignesDe(inventaire.getId()));
        assertEquals(
            STORAGE_RESERVE_ID,
            ((Number) em
                    .createNativeQuery("SELECT storage_id FROM store_inventory WHERE id = " + inventaire.getId())
                    .getSingleResult()).intValue(),
            "l'emplacement de l'inventaire est déduit du rayon"
        );
    }

    @Test
    @DisplayName("Un produit rangé dans deux rayons du même emplacement ne donne qu'une ligne")
    void produitDansDeuxRayons() {
        Rayon premier = rayon(rayon, "TETE DE GONDOLE");
        Rayon second = rayon(rayon, "PROMOTIONS");
        Produit produit = produit(unique("PARTOUT"));
        ranger(produit, premier);
        ranger(produit, second);
        viderLeCache();

        StoreInventoryDTO inventaire = creer(demande("STORAGE"));

        assertEquals(1, lignesDe(inventaire.getId()),
            "un même produit ne se compte qu'une fois, sous peine de violer l'unicité de la ligne");
    }

    @Test
    @DisplayName("Un inventaire d'emplacement ne prend que les produits rangés dans ses rayons")
    void perimetreStorage() {
        Produit range = produit(unique("RANGE EN RAYON"));
        ranger(range, rayon(rayon, "TETE DE GONDOLE"));
        Produit ailleurs = produit(unique("RANGE EN RESERVE"));
        ranger(ailleurs, rayon(reserve, "ETAGERE HAUTE"));
        produit(unique("NON RANGE"));
        viderLeCache();

        StoreInventoryDTO inventaire = creer(demande("STORAGE"));

        assertEquals(1, lignesDe(inventaire.getId()),
            "ni le produit de la réserve, ni celui qu'aucun rayon ne réclame");
    }

    @Test
    @DisplayName("Un inventaire de périmés ne retient que les lots expirés encore en stock")
    void perimetrePerime() {
        Produit perime = produitEnStock(unique("PERIME"), 5);
        lot(perime, unique("LOT-VIEUX"), LocalDate.now().minusDays(10), 3);
        Produit epuise = produitEnStock(unique("PERIME EPUISE"), 0);
        lot(epuise, unique("LOT-VIDE"), LocalDate.now().minusDays(10), 0);
        Produit valide = produitEnStock(unique("VALIDE"), 5);
        lot(valide, unique("LOT-BON"), LocalDate.now().plusYears(1), 3);
        viderLeCache();

        StoreInventoryDTO inventaire = creer(demande("PERIME"));

        assertEquals(1, lignesDe(inventaire.getId()), "un lot périmé à zéro n'est plus à retirer");
        assertEquals(1, lotsDe(inventaire.getId()), "le lot périmé est détaillé, même sans gestion de lot globale");
        assertEquals(
            3,
            em
                .createQuery("SELECT il FROM InventoryLot il WHERE il.storeInventoryLine.storeInventory.id = :id", InventoryLot.class)
                .setParameter("id", inventaire.getId())
                .getSingleResult()
                .getQuantityInit(),
            "la quantité initiale du lot est figée à la création"
        );
    }

    @Test
    @DisplayName("L'alerte de péremption balaie la fenêtre demandée, bornes comprises")
    void perimetreAlertePeremption() {
        Produit dansLaFenetre = produitEnStock(unique("BIENTOT"), 5);
        lot(dansLaFenetre, unique("LOT-30J"), LocalDate.now().plusDays(30), 4);
        Produit horsFenetre = produitEnStock(unique("PLUS TARD"), 5);
        lot(horsFenetre, unique("LOT-120J"), LocalDate.now().plusDays(120), 4);
        Produit dejaPerime = produitEnStock(unique("DEJA PERIME"), 5);
        lot(dejaPerime, unique("LOT-HIER"), LocalDate.now().minusDays(1), 4);
        viderLeCache();

        StoreInventoryDTO inventaire = creer(
            new StoreInventoryRecord(null, STORAGE_RAYON_ID, null, "ALERTE_PEREMPTION", null, "alerte",
                null, null, 60, null));

        assertEquals(1, lignesDe(inventaire.getId()), "ni le lot déjà périmé ni celui à 120 jours");
        assertEquals(1, lotsDe(inventaire.getId()));
    }

    @Test
    @DisplayName("Sans nombre de jours, l'alerte de péremption retient les quatre-vingt-dix jours par défaut")
    void alertePeremptionParDefaut() {
        Produit produit = produitEnStock(unique("A 80 JOURS"), 5);
        lot(produit, unique("LOT-80J"), LocalDate.now().plusDays(80), 4);
        viderLeCache();

        StoreInventoryDTO inventaire = creer(demande("ALERTE_PEREMPTION"));

        assertEquals(1, lignesDe(inventaire.getId()));
    }

    @Test
    @DisplayName("Un inventaire sous seuil compare chaque emplacement à son propre seuil")
    void perimetreSousSeuil() {
        Produit sousSeuil = produit(unique("SOUS SEUIL"));
        stock(sousSeuil, rayon, 3, 5);
        stock(sousSeuil, reserve, 40, 5);
        Produit auDessus = produit(unique("AU DESSUS"));
        stock(auDessus, rayon, 12, 5);
        Produit sansSeuil = produit(unique("SANS SEUIL"));
        stock(sansSeuil, rayon, 0, null);
        viderLeCache();

        StoreInventoryDTO inventaire = creer(demande("SOUS_SEUIL"));

        assertEquals(1, lignesDe(inventaire.getId()),
            "le stock de réserve ne compense pas un rayon sous son seuil, et un produit sans seuil n'est pas concerné");
    }

    @Test
    @DisplayName("Un inventaire de ruptures n'est déclenché que si tout le magasin est à zéro")
    void perimetreEnRupture() {
        Produit enRupture = produit(unique("RUPTURE"));
        stock(enRupture, rayon, 0);
        stock(enRupture, reserve, 0);
        Produit enReserve = produit(unique("RESTE EN RESERVE"));
        stock(enReserve, rayon, 0);
        stock(enReserve, reserve, 15);
        viderLeCache();

        StoreInventoryDTO inventaire = creer(demande("EN_RUPTURE"));

        assertEquals(1, lignesDe(inventaire.getId()), "le produit encore en réserve n'est pas en rupture");
    }

    @Test
    @DisplayName("Un inventaire d'invendus retient les produits sans vente sur la période")
    void perimetreInvendu() {
        produitEnStock(unique("JAMAIS VENDU A"), 10);
        produitEnStock(unique("JAMAIS VENDU B"), 10);
        viderLeCache();

        StoreInventoryDTO inventaire = creer(
            new StoreInventoryRecord(null, STORAGE_RAYON_ID, null, "INVENDU", null, "invendus",
                LocalDate.now().minusMonths(3), LocalDate.now(), null, null));

        assertEquals(2, lignesDe(inventaire.getId()));
    }

    @Test
    @DisplayName("Un inventaire de vendus sans vente sur la période ne retient rien")
    void perimetreVenduSansVente() {
        produitEnStock(unique("EN STOCK"), 10);
        viderLeCache();

        StoreInventoryDTO inventaire = creer(
            new StoreInventoryRecord(null, STORAGE_RAYON_ID, null, "VENDU", null, "vendus",
                LocalDate.now().minusMonths(3), LocalDate.now(), null, null));

        assertEquals(0, lignesDe(inventaire.getId()));
    }

    @Test
    @DisplayName("Une période de vente absente ou inversée est refusée avant toute écriture")
    void periodeDeVenteInvalide() {
        assertThrows(
            IllegalArgumentException.class,
            () -> creer(new StoreInventoryRecord(null, STORAGE_RAYON_ID, null, "VENDU", null, "sans dates"))
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> creer(new StoreInventoryRecord(null, STORAGE_RAYON_ID, null, "INVENDU", null, "dates inversées",
                LocalDate.now(), LocalDate.now().minusDays(1), null, null))
        );
    }

    @Test
    @DisplayName("Une sélection explicite de produits ne compte que les produits actifs listés")
    void perimetreSelectionProduit() {
        Produit premier = produitEnStock(unique("CHOISI A"), 10);
        Produit second = produitEnStock(unique("CHOISI B"), 10);
        Produit retire = produitDesactive(unique("CHOISI MAIS RETIRE"));
        produitEnStock(unique("NON CHOISI"), 10);
        viderLeCache();

        StoreInventoryDTO inventaire = creer(
            new StoreInventoryRecord(null, STORAGE_RAYON_ID, null, "SELECTION_PRODUIT", null, "sélection",
                null, null, null, null, null, List.of(premier.getId(), second.getId(), retire.getId())));

        assertEquals(2, lignesDe(inventaire.getId()));
    }

    @Test
    @DisplayName("Une sélection vide est refusée")
    void selectionVide() {
        assertThrows(
            IllegalArgumentException.class,
            () -> creer(new StoreInventoryRecord(null, STORAGE_RAYON_ID, null, "SELECTION_PRODUIT", null, "vide"))
        );
    }

    @Test
    @DisplayName("Le type grossiste n'est pas encore pris en charge")
    void typeGrossiste() {
        assertThrows(IllegalArgumentException.class, () -> creer(demande("GROSSISTE")));
    }

    @Test
    @DisplayName("Avec la gestion de lot, chaque lot approvisionné devient une ligne à compter")
    void gestionDeLotActivee() {
        when(services.appConfigurationService.useGestionLotInventaire()).thenReturn(true);
        Produit produit = produitEnStock(unique("A LOTS"), 30);
        lot(produit, unique("LOT-A"), LocalDate.now().plusYears(1), 20);
        lot(produit, unique("LOT-B"), LocalDate.now().plusYears(2), 10);
        lot(produit, unique("LOT-VIDE"), LocalDate.now().plusYears(2), 0);
        viderLeCache();

        StoreInventoryDTO inventaire = creer(demande("MAGASIN"));

        assertEquals(1, lignesDe(inventaire.getId()));
        assertEquals(2, lotsDe(inventaire.getId()), "un lot épuisé n'a rien à faire compter");
    }

    @Test
    @DisplayName("Un inventaire créé par le planificateur est marqué programmé et porte son destinataire")
    void inventaireProgramme() {
        produitEnStock(unique("PROGRAMME"), 5);
        viderLeCache();

        StoreInventoryDTO inventaire = creer(
            new StoreInventoryRecord(null, STORAGE_RAYON_ID, null, "MAGASIN", null, "tournant",
                null, null, null, null, utilisateur.getId(), List.of()));

        viderLeCache();
        var relu = services.storeInventoryRepository.findById(inventaire.getId()).orElseThrow();
        assertEquals(InventoryType.PROGRAMME, relu.getInventoryType());
        assertEquals(utilisateur.getId(), relu.getUser().getId());
    }

    @Test
    @DisplayName("Sans emplacement demandé, l'inventaire retombe sur l'emplacement principal de l'utilisateur")
    void emplacementParDefaut() {
        produitEnStock(unique("PAR DEFAUT"), 5);
        viderLeCache();

        StoreInventoryDTO inventaire = creer(
            new StoreInventoryRecord(null, null, null, "MAGASIN", null, "sans emplacement"));

        assertTrue(lignesDe(inventaire.getId()) > 0);
        assertEquals(
            STORAGE_RAYON_ID,
            ((Number) em
                    .createNativeQuery("SELECT storage_id FROM store_inventory WHERE id = " + inventaire.getId())
                    .getSingleResult()).intValue()
        );
    }

    // ===== outils =====

    private StoreInventoryDTO creer(StoreInventoryRecord demande) {
        StoreInventoryDTO inventaire = services.inventaireCreationService.create(demande);
        viderLeCache();
        return inventaire;
    }

    private StoreInventoryRecord demande(String categorie) {
        return new StoreInventoryRecord(null, STORAGE_RAYON_ID, null, categorie, null, categorie.toLowerCase());
    }
}
