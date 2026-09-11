package com.kobe.warehouse.service.stock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.RetourDepotItem;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.enumeration.StorageType;
import com.kobe.warehouse.domain.enumeration.TypeMagasin;
import com.kobe.warehouse.service.dto.RetourDepotDTO;
import com.kobe.warehouse.service.dto.RetourDepotItemDTO;
import com.kobe.warehouse.service.errors.GenericError;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageRequest;

/**
 * Un retour de dépôt ramène en officine de la marchandise déposée ailleurs : une même opération
 * débite le stock du dépôt et crédite celui de l'officine. Les deux lignes de {@code stock_produit}
 * appartiennent à des magasins différents, et c'est le type d'emplacement — PRINCIPAL — qui décide
 * laquelle est touchée de chaque côté. Un service qui se tromperait de ligne ne se verrait pas sur
 * des doublures : il faut deux magasins réels, chacun avec ses emplacements, pour que la sélection
 * ait un sens.
 *
 * <p>La ligne de retour fige en plus les quatre quantités d'avant/après des deux côtés. Ce sont
 * elles qui rendent le mouvement relisible plus tard, et elles doivent s'accorder avec ce que les
 * deux stocks valent réellement après l'opération.
 */
@DisplayName("RetourDepotService — retours de dépôt sur PostgreSQL")
class RetourDepotServiceIntegrationTest extends AbstractStockIntegrationTest {

    @Test
    @DisplayName("Un retour débite le dépôt et crédite l'officine du même geste")
    void retourDeplaceLeStockDuDepotVersLOfficine() {
        Magasin depot = depot();
        Storage rayonDepot = rayonDe(depot);
        Produit produit = produitReference("DOLIPRANE");
        StockProduit stockOfficine = stock(produit, rayon, 5);
        StockProduit stockDepot = stock(produit, rayonDepot, 40);
        viderLeCache();

        RetourDepotDTO retour = services.retourDepotService.create(demandeDeRetour(depot, produit, 12));
        viderLeCache();

        assertEquals(depot.getId(), retour.getDepotId());
        assertEquals(28, compter("SELECT qty_stock FROM stock_produit WHERE id = " + stockDepot.getId()));
        assertEquals(17, compter("SELECT qty_stock FROM stock_produit WHERE id = " + stockOfficine.getId()));

        RetourDepotItem ligne = em
            .createQuery("SELECT i FROM RetourDepotItem i WHERE i.retourDepot.id = :id", RetourDepotItem.class)
            .setParameter("id", retour.getId())
            .getSingleResult();
        assertEquals(12, ligne.getQtyMvt());
        assertEquals(40, ligne.getInitStock(), "le stock du dépôt avant l'opération");
        assertEquals(28, ligne.getAfterStock());
        assertEquals(produit.getFournisseurProduitPrincipal().getPrixUni(), ligne.getRegularUnitPrice());

        // Les quantités côté officine ne sont pas des colonnes : elles sont marquées transient et ne
        // voyagent qu'en mémoire, jusqu'au journal de mouvement. C'est donc là qu'on les observe.
        RetourDepotItem journalisee = ligneJournalisee();
        assertEquals(5, journalisee.getOfficineInitStock());
        assertEquals(17, journalisee.getOfficineFinalStock());
        assertEquals(produit.getFournisseurProduitPrincipal().getPrixAchat(), journalisee.getPrixAchat());
    }

    @Test
    @DisplayName("Le stock de départ du dépôt est celui de son emplacement de vente, pas de sa réserve")
    void leDepotEstDebiteSurSonRayon() {
        Magasin depot = depot();
        Storage rayonDepot = rayonDe(depot);
        Storage reserveDepot = reserveDe(depot);
        Produit produit = produitReference("EN RESERVE AUSSI");
        stock(produit, rayon, 0);
        StockProduit stockRayonDepot = stock(produit, rayonDepot, 30);
        StockProduit stockReserveDepot = stock(produit, reserveDepot, 100);
        viderLeCache();

        services.retourDepotService.create(demandeDeRetour(depot, produit, 10));
        viderLeCache();

        assertEquals(20, compter("SELECT qty_stock FROM stock_produit WHERE id = " + stockRayonDepot.getId()));
        assertEquals(100, compter("SELECT qty_stock FROM stock_produit WHERE id = " + stockReserveDepot.getId()), "la réserve du dépôt est intacte");
    }

    @Test
    @DisplayName("Le stock initial de l'officine additionne tous ses emplacements")
    void leStockInitialDeLOfficineAdditionneSesEmplacements() {
        Magasin depot = depot();
        Storage rayonDepot = rayonDe(depot);
        Produit produit = produitReference("MULTI EMPLACEMENT");
        StockProduit stockRayonOfficine = stock(produit, rayon, 5);
        stock(produit, reserve, 8);
        stock(produit, rayonDepot, 40);
        viderLeCache();

        services.retourDepotService.create(demandeDeRetour(depot, produit, 10));
        viderLeCache();

        RetourDepotItem journalisee = ligneJournalisee();
        assertEquals(13, journalisee.getOfficineInitStock(), "5 en rayon et 8 en réserve");
        assertEquals(23, journalisee.getOfficineFinalStock());
        assertEquals(15, compter("SELECT qty_stock FROM stock_produit WHERE id = " + stockRayonOfficine.getId()), "seul le rayon est crédité");
        assertEquals(8, compter("SELECT qty_stock FROM stock_produit WHERE produit_id = %d AND storage_id = %d".formatted(produit.getId(), STORAGE_RESERVE_ID)));
    }

    @Test
    @DisplayName("Plusieurs produits dans le même retour sont traités ensemble")
    void retourDePlusieursProduits() {
        Magasin depot = depot();
        Storage rayonDepot = rayonDe(depot);
        Produit premier = produitReference("PREMIER");
        Produit second = produitReference("SECOND");
        stock(premier, rayon, 0);
        stock(second, rayon, 3);
        StockProduit stockDepotPremier = stock(premier, rayonDepot, 20);
        StockProduit stockDepotSecond = stock(second, rayonDepot, 50);
        viderLeCache();

        RetourDepotDTO demande = new RetourDepotDTO().setDepotId(depot.getId());
        demande.setRetourDepotItems(List.of(ligneDeRetour(premier, 5), ligneDeRetour(second, 7)));

        RetourDepotDTO retour = services.retourDepotService.create(demande);
        viderLeCache();

        assertEquals(2, compter("SELECT COUNT(*) FROM retour_depot_item WHERE retour_depot_id = " + retour.getId()));
        assertEquals(15, compter("SELECT qty_stock FROM stock_produit WHERE id = " + stockDepotPremier.getId()));
        assertEquals(43, compter("SELECT qty_stock FROM stock_produit WHERE id = " + stockDepotSecond.getId()));
        assertEquals(5, compter("SELECT qty_stock FROM stock_produit WHERE produit_id = %d AND storage_id = %d".formatted(premier.getId(), STORAGE_RAYON_ID)));
        assertEquals(10, compter("SELECT qty_stock FROM stock_produit WHERE produit_id = %d AND storage_id = %d".formatted(second.getId(), STORAGE_RAYON_ID)));
    }

    @Test
    @DisplayName("Un retour sans ligne n'écrit qu'un en-tête")
    void retourSansLigne() {
        Magasin depot = depot();
        viderLeCache();

        RetourDepotDTO retour = services.retourDepotService.create(new RetourDepotDTO().setDepotId(depot.getId()));
        viderLeCache();

        assertEquals(1, compter("SELECT COUNT(*) FROM retour_depot WHERE id = " + retour.getId()));
        assertEquals(0, compter("SELECT COUNT(*) FROM retour_depot_item WHERE retour_depot_id = " + retour.getId()));
    }

    // ===== refus =====

    @Test
    @DisplayName("Un dépôt ou un produit inconnu est refusé")
    void depotOuProduitInconnu() {
        Magasin depot = depot();
        Produit produit = produitReference("EXISTE");
        stock(produit, rayon, 5);
        stock(produit, rayonDe(depot), 10);
        viderLeCache();

        RetourDepotDTO surDepotInconnu = new RetourDepotDTO().setDepotId(999_999);
        assertTrue(
            assertThrows(GenericError.class, () -> services.retourDepotService.create(surDepotInconnu)).getMessage().contains("Dépôt non trouvé")
        );

        RetourDepotDTO surProduitInconnu = new RetourDepotDTO().setDepotId(depot.getId());
        surProduitInconnu.setRetourDepotItems(List.of(new RetourDepotItemDTO().setProduitId(999_999).setQtyMvt(1)));
        assertTrue(
            assertThrows(GenericError.class, () -> services.retourDepotService.create(surProduitInconnu))
                .getMessage()
                .contains("Produit non trouvé")
        );
    }

    @Test
    @DisplayName("Un produit sans stock dans le dépôt est refusé")
    void produitAbsentDuDepot() {
        Magasin depot = depot();
        rayonDe(depot);
        Produit produit = produitReference("JAMAIS DEPOSE");
        stock(produit, rayon, 5);
        viderLeCache();

        RetourDepotDTO demande = demandeDeRetour(depot, produit, 3);

        assertTrue(
            assertThrows(GenericError.class, () -> services.retourDepotService.create(demande)).getMessage().contains("Stock non trouvé")
        );
    }

    // ===== lectures =====

    @Test
    @DisplayName("Un retour se relit avec ses lignes et son dépôt")
    void relectureDunRetour() {
        Magasin depot = depot();
        Storage rayonDepot = rayonDe(depot);
        Produit produit = produitReference("A RELIRE");
        stock(produit, rayon, 5);
        stock(produit, rayonDepot, 40);
        viderLeCache();

        RetourDepotDTO cree = services.retourDepotService.create(demandeDeRetour(depot, produit, 12));
        viderLeCache();

        RetourDepotDTO relu = services.retourDepotService.findOne(cree.getId()).orElseThrow();
        assertEquals(depot.getId(), relu.getDepotId());
        assertEquals(depot.getFullName(), relu.getDepotName());
        assertEquals(1, relu.getRetourDepotItems().size());
        RetourDepotItemDTO ligne = relu.getRetourDepotItems().getFirst();
        assertEquals(12, ligne.getQtyMvt());
        assertEquals(produit.getId(), ligne.getProduitId());
        assertEquals(produit.getFournisseurProduitPrincipal().getCodeCip(), ligne.getProduitCip());

        assertTrue(services.retourDepotService.findOne(999_999).isEmpty());
    }

    @Test
    @DisplayName("La recherche par période ne rend que les retours du dépôt visé")
    void rechercheParPeriodeEtDepot() {
        Magasin depot = depot();
        Magasin autreDepot = depot();
        Produit produit = produitReference("A CHERCHER");
        stock(produit, rayon, 5);
        stock(produit, rayonDe(depot), 40);
        stock(produit, rayonDe(autreDepot), 40);
        viderLeCache();

        services.retourDepotService.create(demandeDeRetour(depot, produit, 4));
        services.retourDepotService.create(demandeDeRetour(autreDepot, produit, 6));
        viderLeCache();

        LocalDate veille = LocalDate.now().minusDays(1);
        LocalDate lendemain = LocalDate.now().plusDays(1);

        assertEquals(
            1,
            services.retourDepotService.findAllByDateRange(depot.getId(), veille, lendemain, PageRequest.of(0, 20)).getTotalElements()
        );
        assertEquals(
            2,
            services.retourDepotService.findAllByDateRange(null, veille, lendemain, PageRequest.of(0, 20)).getTotalElements(),
            "sans dépôt, les retours des deux dépôts remontent"
        );
        assertTrue(
            services.retourDepotService
                .findAllByDateRange(depot.getId(), veille.minusDays(30), veille.minusDays(10), PageRequest.of(0, 20))
                .isEmpty(),
            "hors période, rien ne remonte"
        );
    }

    /** La ligne telle qu'elle part au journal : seule à porter les quantités côté officine. */
    private RetourDepotItem ligneJournalisee() {
        ArgumentCaptor<RetourDepotItem> capteur = ArgumentCaptor.forClass(RetourDepotItem.class);
        verify(services.inventoryTransactionService).save(capteur.capture());
        return capteur.getValue();
    }

    // ===== fabriques du jeu d'essai propres aux dépôts =====

    /** Un dépôt : un magasin distinct de l'officine, avec ses propres emplacements. */
    private Magasin depot() {
        Magasin depot = new Magasin();
        String nom = unique("DEPOT");
        depot.setName(nom);
        depot.setFullName("DEPOT " + nom);
        depot.setTypeMagasin(TypeMagasin.DEPOT);
        em.persist(depot);
        em.flush();
        return depot;
    }

    private Storage rayonDe(Magasin magasinCible) {
        return storage(magasinCible, StorageType.PRINCIPAL, "Stock rayon");
    }

    private Storage reserveDe(Magasin magasinCible) {
        return storage(magasinCible, StorageType.SAFETY_STOCK, "Stock réserve");
    }

    private Storage storage(Magasin magasinCible, StorageType type, String nom) {
        Storage emplacement = new Storage();
        emplacement.setMagasin(magasinCible);
        emplacement.setStorageType(type);
        emplacement.setName(nom);
        em.persist(emplacement);
        em.flush();
        return emplacement;
    }

    private Produit produitReference(String libelle) {
        Produit produit = produit(unique(libelle));
        fournisseurProduit(produit);
        em.flush();
        return produit;
    }

    private RetourDepotDTO demandeDeRetour(Magasin depot, Produit produit, int quantite) {
        RetourDepotDTO demande = new RetourDepotDTO().setDepotId(depot.getId());
        demande.setRetourDepotItems(List.of(ligneDeRetour(produit, quantite)));
        return demande;
    }

    private RetourDepotItemDTO ligneDeRetour(Produit produit, int quantite) {
        return new RetourDepotItemDTO()
            .setProduitId(produit.getId())
            .setQtyMvt(quantite)
            .setProduitCip(produit.getFournisseurProduitPrincipal().getCodeCip());
    }
}
