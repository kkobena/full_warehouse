package com.kobe.warehouse.service.stock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kobe.warehouse.domain.Ajust;
import com.kobe.warehouse.domain.Ajustement;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Rayon;
import com.kobe.warehouse.domain.RayonProduit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Substitut;
import com.kobe.warehouse.domain.enumeration.AjustType;
import com.kobe.warehouse.domain.enumeration.AjustementStatut;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.domain.enumeration.ProduitFlag;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.TypeProduit;
import com.kobe.warehouse.domain.enumeration.TypeSubstitut;
import com.kobe.warehouse.service.dto.FournisseurProduitDTO;
import com.kobe.warehouse.service.dto.ProduitDTO;
import com.kobe.warehouse.service.dto.StockProduitDTO;
import com.kobe.warehouse.service.dto.SubstitutDTO;
import com.kobe.warehouse.service.errors.BadRequestAlertException;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.stock.dto.ProduitSearch;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

/**
 * Ce que les doublures ne peuvent pas prouver du service produit, et que seule une vraie base
 * montre.
 *
 * <p><b>La suppression d'un produit</b> d'abord : elle efface une vingtaine de tables liées en
 * JPQL et en SQL natif, dans un ordre imposé par les clés étrangères. Une table oubliée ou une
 * ligne effacée trop tôt ne se voit pas sur un {@code EntityManager} simulé — elle se voit à la
 * violation de contrainte, et seulement contre Postgres. Le produit porte en plus une référence
 * vers son propre fournisseur principal, qu'il faut annuler avant d'effacer les fournisseurs, et
 * ses produits détail doivent partir avant lui.
 *
 * <p><b>La recherche</b> ensuite : elle n'est pas écrite en Java mais dans deux fonctions plpgsql
 * qui rendent du JSON. Les règles de correspondance (code en préfixe si la saisie commence par un
 * chiffre, libellé en préfixe sinon), le cloisonnement par magasin ou par emplacement et la forme
 * du JSON désérialisé en {@link ProduitSearch} vivent entièrement dans la base.
 *
 * <p><b>Les écritures</b> enfin : création, mise à jour et réapprovisionnement écrivent dans
 * {@code produit}, {@code stock_produit}, {@code rayon_produit} et {@code fournisseur_produit} à
 * travers des cascades JPA et sous des contraintes d'unicité — autant de choses qui n'existent
 * qu'au moment du {@code flush}.
 */
@DisplayName("ProduitService — catalogue produit sur PostgreSQL")
class ProduitServiceIntegrationTest extends AbstractStockIntegrationTest {

    private static final int RAYON_PRINCIPAL_ID = 2;
    private static final int TVA_ZERO_ID = 1;

    // ===== création =====

    @Test
    @DisplayName("Créer un produit écrit sa fiche, son stock rayon, son rayon et son fournisseur principal")
    void creationDUnProduit() {
        Fournisseur fournisseur = fournisseur();
        viderLeCache();

        Long id = services.produitService.save(fiche("DOLIPRANE 500", fournisseur));
        viderLeCache();

        Produit cree = services.produitRepository.findById(id.intValue()).orElseThrow();
        assertEquals("DOLIPRANE 500", cree.getLibelle());
        assertEquals(TypeProduit.PACKAGE, cree.getTypeProduit());
        assertEquals(Status.ENABLE, cree.getStatus());
        assertEquals(10_000, cree.getRegularUnitPrice());
        assertEquals(6_000, cree.getCostAmount());
        assertEquals(1, cree.getItemQty(), "un produit non déconditionnable vaut une unité");

        assertEquals(1, compter("SELECT COUNT(*) FROM stock_produit WHERE produit_id = " + id));
        assertEquals(
            STORAGE_RAYON_ID,
            compter("SELECT storage_id FROM stock_produit WHERE produit_id = " + id),
            "le stock est ouvert sur l'emplacement du rayon choisi"
        );
        assertEquals(0, compter("SELECT qty_stock FROM stock_produit WHERE produit_id = " + id));

        assertEquals(1, compter("SELECT COUNT(*) FROM rayon_produit WHERE produit_id = " + id));
        assertNotNull(cree.getFournisseurProduitPrincipal(), "la fiche pointe son fournisseur principal");
        assertEquals(fournisseur.getId(), cree.getFournisseurProduitPrincipal().getFournisseur().getId());
    }

    /**
     * À la création, {@code seuilMini} ne fait qu'un seul travail : décider si l'emplacement de
     * réserve est ouvert. Le seuil réellement inscrit, des deux côtés, est {@code qtySeuilMini} —
     * c'est la mise à jour de la fiche, et elle seule, qui ventile ensuite un seuil propre à la
     * réserve.
     */
    @Test
    @DisplayName("Un seuil de réserve ouvre un second emplacement de stock à la création")
    void creationAvecReserve() {
        Fournisseur fournisseur = fournisseur();
        viderLeCache();

        ProduitDTO fiche = fiche("AVEC RESERVE", fournisseur).setQtySeuilMini(15).setSeuilMini(30);
        Long id = services.produitService.save(fiche);
        viderLeCache();

        assertEquals(2, compter("SELECT COUNT(*) FROM stock_produit WHERE produit_id = " + id));
        assertEquals(
            15,
            compter("SELECT seuil_mini FROM stock_produit WHERE produit_id = %d AND storage_id = %d".formatted(id, STORAGE_RESERVE_ID)),
            "le seuil posé est celui du rayon : seuilMini n'est ici qu'un interrupteur"
        );
        assertEquals(0, compter("SELECT qty_stock FROM stock_produit WHERE produit_id = %d AND storage_id = %d".formatted(id, STORAGE_RESERVE_ID)));
    }

    @Test
    @DisplayName("Les fournisseurs supplémentaires sont référencés à côté du principal")
    void creationAvecFournisseursSupplementaires() {
        Fournisseur principal = fournisseur();
        Fournisseur secondaire = fournisseur();
        viderLeCache();

        FournisseurProduitDTO autre = new FournisseurProduitDTO();
        autre.setFournisseurId(secondaire.getId());
        autre.setCodeCip(unique("CIPBIS"));
        autre.setPrixAchat(5_500);
        autre.setPrixUni(9_000);
        autre.setQteColis(12);
        ProduitDTO fiche = fiche("MULTI FOURNISSEUR", principal);
        fiche.setFournisseurProduits(List.of(autre));

        Long id = services.produitService.save(fiche);
        viderLeCache();

        assertEquals(2, compter("SELECT COUNT(*) FROM fournisseur_produit WHERE produit_id = " + id));
        FournisseurProduit ajoute = services.fournisseurProduitRepository
            .findAll()
            .stream()
            .filter(fp -> fp.getProduit().getId().equals(id.intValue()))
            .filter(fp -> fp.getFournisseur().getId().equals(secondaire.getId()))
            .findFirst()
            .orElseThrow();
        assertEquals(12, ajoute.getQteColis());
        assertEquals(5_500, ajoute.getPrixAchat());
    }

    @Test
    @DisplayName("Un produit détail se rattache à sa boîte et en reprend le référencement")
    void creationDunProduitDetail() {
        Produit boite = boiteAvecRayon("BOITE DE 20", 20);
        int boiteId = boite.getId();
        int tvaId = boite.getTva().getId();
        viderLeCache();

        ProduitDTO detail = new ProduitDTO()
            .setLibelle("comprimé à l'unité")
            .setProduitId(boiteId)
            .setTypeProduit(TypeProduit.DETAIL)
            .setCostAmount(300)
            .setRegularUnitPrice(500)
            .setDeconditionnable(false)
            .setItemQty(30);

        Long id = services.produitService.saveDetail(detail);
        viderLeCache();

        Produit cree = services.produitRepository.findById(id.intValue()).orElseThrow();
        assertEquals("COMPRIMÉ À L'UNITÉ", cree.getLibelle(), "le libellé est normalisé en majuscules");
        assertEquals(TypeProduit.DETAIL, cree.getTypeProduit());
        assertEquals(boiteId, cree.getParent().getId());
        assertEquals(500, cree.getRegularUnitPrice());
        assertEquals(1, cree.getItemQty(), "le détail se vend à l'unité");
        assertEquals(tvaId, cree.getTva().getId(), "le détail hérite de la TVA de la boîte");
        assertEquals(
            1,
            compter("SELECT COUNT(*) FROM fournisseur_produit WHERE produit_id = " + id),
            "le détail est référencé chez le fournisseur de la boîte"
        );
        assertEquals(1, compter("SELECT COUNT(*) FROM stock_produit WHERE produit_id = " + id));
        assertEquals(30, compter("SELECT item_qty FROM produit WHERE id = " + boiteId), "le colisage saisi est reporté sur la boîte");
    }

    // ===== mise à jour =====

    @Test
    @DisplayName("Modifier une fiche déplace son rayon de vente et répercute les seuils par emplacement")
    void miseAJourDesSeuils() {
        Produit produit = boiteAvecRayon("A MODIFIER", 1);
        StockProduit stockRayon = stock(produit, rayon, 40);
        StockProduit stockReserve = stock(produit, reserve, 10);
        Rayon nouveauRayon = rayonSupplementaire();
        String libelle = produit.getLibelle();

        ProduitDTO modification = ficheExistante(produit)
            .setRayonId(nouveauRayon.getId())
            .setStockReassort(25)
            .setQtySeuilMini(8)
            .setSeuilMini(30);
        modification.setStockMaxi(60);
        viderLeCache();

        services.produitService.update(modification);
        viderLeCache();

        assertEquals(25, compter("SELECT stock_reassort FROM stock_produit WHERE id = " + stockRayon.getId()));
        assertEquals(8, compter("SELECT seuil_mini FROM stock_produit WHERE id = " + stockRayon.getId()));
        assertEquals(60, compter("SELECT stock_maxi FROM stock_produit WHERE id = " + stockRayon.getId()));
        assertEquals(30, compter("SELECT seuil_mini FROM stock_produit WHERE id = " + stockReserve.getId()), "la réserve reçoit son propre seuil");
        assertEquals(60, compter("SELECT stock_maxi FROM stock_produit WHERE id = " + stockReserve.getId()));

        assertEquals(
            nouveauRayon.getId().intValue(),
            compter("SELECT rayon_id FROM rayon_produit WHERE produit_id = " + produit.getId()),
            "le rayon de vente a bien été remplacé"
        );
        assertEquals(libelle, em.find(Produit.class, produit.getId()).getLibelle());
    }

    @Test
    @DisplayName("Un seuil de réserve demandé sur un produit qui n'en a pas lui ouvre l'emplacement")
    void miseAJourQuiOuvreLaReserve() {
        Produit produit = boiteAvecRayon("SANS RESERVE", 1);
        stock(produit, rayon, 40);
        ProduitDTO modification = ficheExistante(produit).setQtySeuilMini(5).setSeuilMini(30);
        viderLeCache();

        services.produitService.update(modification);
        viderLeCache();

        assertEquals(2, compter("SELECT COUNT(*) FROM stock_produit WHERE produit_id = " + produit.getId()));
        assertEquals(
            0,
            compter(
                "SELECT qty_stock FROM stock_produit WHERE produit_id = %d AND storage_id = %d".formatted(
                        produit.getId(),
                        STORAGE_RESERVE_ID
                    )
            ),
            "la réserve s'ouvre à vide, sans prélever le rayon"
        );
        assertEquals(40, compter("SELECT qty_stock FROM stock_produit WHERE produit_id = %d AND storage_id = %d".formatted(produit.getId(), STORAGE_RAYON_ID)));
    }

    @Test
    @DisplayName("Sans aucun seuil transmis, la mise à jour ne touche pas aux stocks")
    void miseAJourSansSeuil() {
        Produit produit = boiteAvecRayon("INTACT", 1);
        StockProduit stockRayon = stock(produit, rayon, 40);
        stockRayon.setSeuilMini(12);
        stockRayon.setStockReassort(7);
        ProduitDTO modification = ficheExistante(produit);
        viderLeCache();

        services.produitService.update(modification);
        viderLeCache();

        assertEquals(1, compter("SELECT COUNT(*) FROM stock_produit WHERE produit_id = " + produit.getId()));
        assertEquals(12, compter("SELECT seuil_mini FROM stock_produit WHERE id = " + stockRayon.getId()));
        assertEquals(7, compter("SELECT stock_reassort FROM stock_produit WHERE id = " + stockRayon.getId()));
    }

    @Test
    @DisplayName("Le réapprovisionnement crédite l'emplacement de vente du magasin, unités gratuites comprises")
    void reapprovisionnementDuRayon() {
        Produit produit = produit(unique("A REAPPROVISIONNER"));
        StockProduit stockRayon = stock(produit, rayon, 40);
        stock(produit, reserve, 10);
        viderLeCache();

        StockProduit credite = services.produitService.updateTotalStock(
            services.produitRepository.findById(produit.getId()).orElseThrow(),
            12,
            3
        );
        viderLeCache();

        assertEquals(stockRayon.getId(), credite.getId(), "c'est le rayon du magasin connecté qui est crédité");
        assertEquals(52, compter("SELECT qty_stock FROM stock_produit WHERE id = " + stockRayon.getId()));
        assertEquals(3, compter("SELECT qty_ug FROM stock_produit WHERE id = " + stockRayon.getId()));
        assertEquals(52, compter("SELECT qty_virtual FROM stock_produit WHERE id = " + stockRayon.getId()));
        assertEquals(
            10,
            compter("SELECT qty_stock FROM stock_produit WHERE produit_id = %d AND storage_id = %d".formatted(produit.getId(), STORAGE_RESERVE_ID)),
            "la réserve n'est pas touchée"
        );
    }

    @Test
    @DisplayName("Rattacher un stock à un produit inconnu est refusé")
    void stockSurProduitInconnu() {
        StockProduitDTO dto = new StockProduitDTO();
        dto.setProduitId(999_999);
        dto.setQtyStock(10);
        dto.setQtyVirtual(10);
        dto.setQtyUG(0);
        dto.setStorageId(STORAGE_RAYON_ID);

        assertThrows(GenericError.class, () -> services.produitService.save(dto));
    }

    // ===== lectures =====

    @Test
    @DisplayName("Le stock total d'un produit additionne tous ses emplacements, unités gratuites comprises")
    void stockTotalTousEmplacements() {
        Produit produit = produit(unique("MULTI EMPLACEMENT"));
        StockProduit stockRayon = stock(produit, rayon, 40);
        stockRayon.setQtyUG(5);
        stock(produit, reserve, 10);
        viderLeCache();

        assertEquals(55, services.produitService.getProductTotalStock(produit.getId()));
        assertEquals(50, services.produitService.produitTotalStock(services.produitRepository.findById(produit.getId()).orElseThrow()),
            "produitTotalStock ignore les unités gratuites");
    }

    @Test
    @DisplayName("Les génériques d'un produit sont lus depuis la table des substituts")
    void generiquesDunProduit() {
        Produit princeps = produit(unique("PRINCEPS"));
        fournisseurProduit(princeps);
        Produit generique = produit(unique("GENERIQUE"));
        fournisseurProduit(generique);
        stock(generique, rayon, 12);
        substitut(princeps, generique);
        viderLeCache();

        List<SubstitutDTO> generiques = services.produitService.findGeneriques(princeps.getId());

        assertEquals(1, generiques.size());
        assertEquals(generique.getLibelle(), generiques.getFirst().getProduit().getLibelle());
        assertEquals(TypeSubstitut.GENERIQUE.name(), generiques.getFirst().getTypeSubstitut());
        assertTrue(services.produitService.findGeneriques(generique.getId()).isEmpty(), "la relation n'est pas symétrique");
    }

    @Test
    @DisplayName("Lire plusieurs produits par identifiant ne rend que ceux qui existent")
    void lectureParIdentifiants() {
        Produit premier = produit(unique("PREMIER"));
        Produit second = produit(unique("SECOND"));
        viderLeCache();

        List<Produit> trouves = services.produitService.findByIds(Set.of(premier.getId(), second.getId(), 999_999));

        assertEquals(2, trouves.size());
    }

    // ===== recherche (fonctions plpgsql) =====

    @Test
    @DisplayName("La recherche par libellé ne retient que les débuts de libellé du magasin")
    void rechercheParLibelle() {
        Produit trouve = produit(unique("ZYRTECOMPRIME"));
        fournisseurProduit(trouve);
        stock(trouve, rayon, 25);
        rayonProduit(trouve, RAYON_PRINCIPAL_ID);

        Produit ailleursDansLeLibelle = produit("COMPRIME " + unique("ZYRTEC"));
        fournisseurProduit(ailleursDansLeLibelle);
        stock(ailleursDansLeLibelle, rayon, 10);
        viderLeCache();

        List<ProduitSearch> resultats = services.produitService.searchProducts("ZYRTECO", MAGASIN_ID, PageRequest.of(0, 20));

        assertEquals(1, resultats.size(), "la correspondance porte sur le début du libellé, pas n'importe où");
        ProduitSearch resultat = resultats.getFirst();
        assertEquals(trouve.getId(), resultat.id());
        assertEquals(trouve.getLibelle(), resultat.libelle());
        assertEquals(25, resultat.totalQuantity(), "le stock rayon remonte avec le produit");
        assertEquals(0, resultat.reserveQuantity());
        assertEquals(trouve.getFournisseurProduitPrincipal().getCodeCip(), resultat.codeProduit());
        assertFalse(resultat.rayons().isEmpty(), "le rayon de rangement accompagne le produit");
    }

    @Test
    @DisplayName("Une saisie qui commence par un chiffre cherche sur les codes, pas sur le libellé")
    void rechercheParCode() {
        Produit produit = produit(unique("AMOXICILLINE"));
        FournisseurProduit reference = fournisseurProduit(produit);
        reference.setCodeCip("3400931234567");
        stock(produit, rayon, 8);
        viderLeCache();

        assertEquals(1, services.produitService.searchProducts("34009312", MAGASIN_ID, PageRequest.of(0, 20)).size());
        assertTrue(
            services.produitService.searchProducts("0093123", MAGASIN_ID, PageRequest.of(0, 20)).isEmpty(),
            "le code doit correspondre depuis son début"
        );
    }

    @Test
    @DisplayName("Une recherche sans résultat rend une liste vide plutôt qu'une erreur")
    void rechercheSansResultat() {
        assertTrue(services.produitService.searchProducts(unique("INTROUVABLE"), MAGASIN_ID, PageRequest.of(0, 20)).isEmpty());
    }

    @Test
    @DisplayName("Un code DataMatrix est réduit au code produit qu'il contient avant la recherche")
    void rechercheDepuisUnDataMatrix() {
        Produit produit = produit(unique("THERMOSENSIBLE"));
        FournisseurProduit reference = fournisseurProduit(produit);
        // Le GTIN « 03400931234567 » du DataMatrix redonne l EAN-13 « 3400931234567 » : c est ce
        // code-la, et non les quatorze chiffres bruts, que la recherche recoit.
        reference.setCodeEan("3400931234567");
        stock(produit, rayon, 8);
        viderLeCache();

        List<ProduitSearch> resultats = services.produitService.searchProducts(
            "0103400931234567" + "17260101" + "10LOT42",
            MAGASIN_ID,
            PageRequest.of(0, 20)
        );

        assertEquals(1, resultats.size(), "le code extrait du DataMatrix sert de terme de recherche");
        assertEquals(produit.getId(), resultats.getFirst().id());
    }

    @Test
    @DisplayName("La recherche par emplacement ne rend que les produits stockés à cet emplacement")
    void rechercheParEmplacement() {
        Produit enRayon = produit(unique("XENRAYON"));
        fournisseurProduit(enRayon);
        stock(enRayon, rayon, 12);

        Produit enReserve = produit(unique("XENRESERVE"));
        fournisseurProduit(enReserve);
        stock(enReserve, reserve, 30);
        viderLeCache();

        List<ProduitSearch> coteRayon = services.produitService.searchProductsByStorage(STORAGE_RAYON_ID, "XEN", PageRequest.of(0, 20));
        List<ProduitSearch> coteReserve = services.produitService.searchProductsByStorage(STORAGE_RESERVE_ID, "XEN", PageRequest.of(0, 20));

        assertEquals(List.of(enRayon.getId()), coteRayon.stream().map(ProduitSearch::id).toList());
        assertEquals(List.of(enReserve.getId()), coteReserve.stream().map(ProduitSearch::id).toList());
    }

    @Test
    @DisplayName("Sans emplacement, la recherche par emplacement ne consulte pas la base")
    void rechercheParEmplacementSansEmplacement() {
        assertTrue(services.produitService.searchProductsByStorage(null, "XEN", PageRequest.of(0, 20)).isEmpty());
    }

    // ===== drapeaux et statut =====

    @Test
    @DisplayName("Désactiver un produit, suivre ses lots et lever ses drapeaux se retrouvent en base")
    void drapeauxEtStatut() {
        Produit produit = produit(unique("A BASCULER"));
        viderLeCache();

        services.produitService.changeStatus(produit.getId(), Status.DISABLE);
        services.produitService.toggleGestionLot(produit.getId(), true);
        services.produitService.toggleFlag(produit.getId(), ProduitFlag.THERMOSENSIBLE, true);
        services.produitService.toggleFlag(produit.getId(), ProduitFlag.MEDICAMENT_ESSENTIEL, true);
        services.produitService.toggleFlag(produit.getId(), ProduitFlag.PRODUIT_GARDE, true);
        services.produitService.toggleFlag(produit.getId(), ProduitFlag.CLASSIFICATION_OVERRIDDEN, true);
        viderLeCache();

        Produit relu = services.produitRepository.findById(produit.getId()).orElseThrow();
        assertEquals(Status.DISABLE, relu.getStatus());
        assertTrue(relu.getGestionLot());
        assertTrue(relu.getThermosensible());
        assertTrue(relu.getEstMedicamentEssentiel());
        assertTrue(relu.getEstProduitGarde());
        assertTrue(relu.getIsClassificationOverridden());
    }

    @Test
    @DisplayName("Basculer un produit inconnu ne lève rien et n'écrit rien")
    void bascuedDunProduitInconnu() {
        services.produitService.changeStatus(999_999, Status.DISABLE);
        services.produitService.toggleGestionLot(999_999, true);
        services.produitService.toggleFlag(999_999, ProduitFlag.THERMOSENSIBLE, true);
        viderLeCache();

        assertEquals(0, compter("SELECT COUNT(*) FROM produit WHERE id = 999999"));
    }

    // ===== suppression =====

    @Test
    @DisplayName("Supprimer un produit efface toutes ses données liées sans violer une seule clé étrangère")
    void suppressionEnCascade() {
        Produit produit = boiteAvecRayon("A SUPPRIMER", 1);
        FournisseurProduit principal = produit.getFournisseurProduitPrincipal();
        fournisseurSupplementaire(produit);
        StockProduit stockRayon = stock(produit, rayon, 40);
        stock(produit, reserve, 10);
        Lot lot = lotSurEmplacement(produit, unique("LOT"), LocalDate.now().plusMonths(6), rayon, 40);
        ajustement(stockRayon, lot);
        instantaneDeStock(produit);

        Produit autre = produit(unique("AUTRE"));
        fournisseurProduit(autre);
        substitut(produit, autre);
        substitut(autre, produit);

        int id = produit.getId();
        viderLeCache();

        services.produitService.deleteProduit(id);
        viderLeCache();

        assertEquals(0, compter("SELECT COUNT(*) FROM produit WHERE id = " + id));
        assertEquals(0, compter("SELECT COUNT(*) FROM stock_produit WHERE produit_id = " + id));
        assertEquals(0, compter("SELECT COUNT(*) FROM lot WHERE produit_id = " + id));
        assertEquals(0, compter("SELECT COUNT(*) FROM lot_stock_location WHERE lot_id = " + lot.getId()));
        assertEquals(0, compter("SELECT COUNT(*) FROM ajustement WHERE stock_produit_id = " + stockRayon.getId()));
        assertEquals(0, compter("SELECT COUNT(*) FROM fournisseur_produit WHERE produit_id = " + id));
        assertEquals(0, compter("SELECT COUNT(*) FROM rayon_produit WHERE produit_id = " + id));
        assertEquals(0, compter("SELECT COUNT(*) FROM stock_produit_snapshot WHERE produit_id = " + id));
        assertEquals(
            0,
            compter("SELECT COUNT(*) FROM substitut WHERE produit_id = %d OR substitut_id = %d".formatted(id, id)),
            "les deux sens de la relation de substitution partent"
        );

        assertEquals(1, compter("SELECT COUNT(*) FROM produit WHERE id = " + autre.getId()), "le produit voisin survit");
        assertEquals(
            0,
            compter("SELECT COUNT(*) FROM fournisseur_produit WHERE id = " + principal.getId()),
            "le fournisseur principal a été détaché avant d'être effacé"
        );
    }

    @Test
    @DisplayName("Supprimer une boîte emporte d'abord ses produits détail")
    void suppressionDesProduitsDetail() {
        Produit boite = boiteAvecRayon("BOITE DE 20", 20);
        stock(boite, rayon, 5);
        viderLeCache();

        ProduitDTO detailDto = new ProduitDTO()
            .setLibelle("comprimé")
            .setProduitId(boite.getId())
            .setTypeProduit(TypeProduit.DETAIL)
            .setCostAmount(300)
            .setRegularUnitPrice(500)
            .setDeconditionnable(false);
        int detailId = services.produitService.saveDetail(detailDto).intValue();
        viderLeCache();

        services.produitService.deleteProduit(boite.getId());
        viderLeCache();

        assertEquals(0, compter("SELECT COUNT(*) FROM produit WHERE id = " + boite.getId()));
        assertEquals(0, compter("SELECT COUNT(*) FROM produit WHERE id = " + detailId), "le détail part avec sa boîte");
        assertEquals(0, compter("SELECT COUNT(*) FROM fournisseur_produit WHERE produit_id = " + detailId));
    }

    @Test
    @DisplayName("Un produit sur lequel des commandes existent ne peut pas être supprimé")
    void suppressionRefuseeSurCommande() {
        Produit produit = boiteAvecRayon("DEJA COMMANDE", 1);
        FournisseurProduit reference = produit.getFournisseurProduitPrincipal();
        stock(produit, rayon, 10);
        ligneDeCommande(commande(reference.getFournisseur(), OrderStatut.REQUESTED), reference, 10, 0);
        int id = produit.getId();
        viderLeCache();

        BadRequestAlertException erreur = assertThrows(
            BadRequestAlertException.class,
            () -> services.produitService.deleteProduit(id)
        );
        assertTrue(erreur.getMessage().contains("des commandes sont enregistrées"));

        viderLeCache();
        assertEquals(1, compter("SELECT COUNT(*) FROM produit WHERE id = " + id), "rien n'a été effacé");
        assertEquals(1, compter("SELECT COUNT(*) FROM stock_produit WHERE produit_id = " + id));
    }

    @Test
    @DisplayName("Supprimer un produit introuvable est refusé")
    void suppressionDunProduitInconnu() {
        BadRequestAlertException erreur = assertThrows(
            BadRequestAlertException.class,
            () -> services.produitService.deleteProduit(999_999)
        );
        assertTrue(erreur.getMessage().contains("Produit introuvable"));
    }

    // ===== fabriques du jeu d'essai propres au catalogue =====

    private Fournisseur fournisseur() {
        Fournisseur fournisseur = new Fournisseur();
        String code = unique("FRS");
        fournisseur.setLibelle("FOURNISSEUR " + code);
        fournisseur.setCode(code);
        em.persist(fournisseur);
        em.flush();
        return fournisseur;
    }

    /** La fiche telle que l'écran de création la transmet. */
    private ProduitDTO fiche(String libelle, Fournisseur fournisseur) {
        ProduitDTO dto = new ProduitDTO()
            .setLibelle(libelle)
            .setCostAmount(6_000)
            .setRegularUnitPrice(10_000)
            .setDeconditionnable(false)
            .setCodeCip(unique("CIP"))
            .setFournisseurId(fournisseur.getId())
            .setRayonId(RAYON_PRINCIPAL_ID)
            .setQtyAppro(1)
            .setQtySeuilMini(1);
        dto.setTvaId(TVA_ZERO_ID);
        dto.setFamilleId(premiereFamilleId());
        return dto;
    }

    /** La même fiche, rejouée sur un produit déjà en base : c'est ce que l'écran de modification poste. */
    private ProduitDTO ficheExistante(Produit produit) {
        ProduitDTO dto = new ProduitDTO()
            .setId(produit.getId())
            .setLibelle(produit.getLibelle())
            .setCostAmount(produit.getCostAmount())
            .setRegularUnitPrice(produit.getRegularUnitPrice())
            .setDeconditionnable(false)
            .setCodeCip(produit.getFournisseurProduitPrincipal().getCodeCip())
            .setFournisseurId(produit.getFournisseurProduitPrincipal().getFournisseur().getId())
            .setRayonId(RAYON_PRINCIPAL_ID)
            .setQtyAppro(produit.getQtyAppro())
            .setDateperemption(false);
        dto.setTvaId(produit.getTva().getId());
        dto.setFamilleId(produit.getFamille().getId());
        return dto;
    }

    /**
     * Une boîte complète : fiche PACKAGE, fournisseur principal et rangement en rayon. Le
     * rangement n'est pas décoratif — la création d'un produit détail y lit l'emplacement de son
     * stock, et la mise à jour y cherche le rayon de vente à remplacer.
     */
    private Produit boiteAvecRayon(String libelle, int colisage) {
        Produit produit = produit(unique(libelle));
        produit.setTypeProduit(TypeProduit.PACKAGE);
        produit.setItemQty(colisage);
        fournisseurProduit(produit);
        rayonProduit(produit, RAYON_PRINCIPAL_ID);
        em.flush();
        return produit;
    }

    /** Un second rayon sur l'emplacement de vente : sans lui, changer de rayon n'a nulle part où aller. */
    private Rayon rayonSupplementaire() {
        Rayon nouveauRayon = new Rayon();
        nouveauRayon.setCode(unique("RAY"));
        nouveauRayon.setLibelle("RAYON " + unique("L"));
        nouveauRayon.setStorage(rayon);
        nouveauRayon.setExclude(false);
        em.persist(nouveauRayon);
        em.flush();
        return nouveauRayon;
    }

    private RayonProduit rayonProduit(Produit produit, int rayonId) {
        RayonProduit rayonProduit = new RayonProduit().setProduit(produit).setRayon(em.find(Rayon.class, rayonId));
        em.persist(rayonProduit);
        produit.getRayonProduits().add(rayonProduit);
        em.flush();
        return rayonProduit;
    }

    private FournisseurProduit fournisseurSupplementaire(Produit produit) {
        FournisseurProduit extra = new FournisseurProduit();
        extra.setProduit(produit);
        extra.setFournisseur(fournisseur());
        extra.setCodeCip(unique("CIPX"));
        extra.setPrixAchat(produit.getCostAmount());
        extra.setPrixUni(produit.getRegularUnitPrice());
        em.persist(extra);
        em.flush();
        return extra;
    }

    private Substitut substitut(Produit produit, Produit generique) {
        Substitut substitut = new Substitut();
        substitut.setProduit(produit);
        substitut.setSubstitut(generique);
        substitut.setType(TypeSubstitut.GENERIQUE);
        em.persist(substitut);
        em.flush();
        return substitut;
    }

    /** Un ajustement validé : il référence à la fois le stock et le lot, les deux FK les plus contraignantes. */
    private Ajustement ajustement(StockProduit stockProduit, Lot lot) {
        Ajust ajust = new Ajust();
        ajust.setCommentaire("casse");
        ajust.setUser(utilisateur);
        ajust.setDateMtv(LocalDateTime.now());
        ajust.setStatut(AjustementStatut.PENDING);
        em.persist(ajust);

        Ajustement ajustement = new Ajustement();
        ajustement.setAjust(ajust);
        ajustement.setStockProduit(stockProduit);
        ajustement.setLot(lot);
        ajustement.setQtyMvt(-2);
        ajustement.setStockBefore(40);
        ajustement.setStockAfter(38);
        ajustement.setType(AjustType.AJUSTEMENT_OUT);
        ajustement.setDateMtv(LocalDateTime.now());
        em.persist(ajustement);
        em.flush();
        return ajustement;
    }

    /**
     * {@code stock_produit_snapshot} n'est pas une entité JPA : la table est alimentée en SQL natif
     * et effacée de même à la suppression du produit. Elle n'existe donc que dans ce test.
     */
    private void instantaneDeStock(Produit produit) {
        em
            .createNativeQuery(
                "INSERT INTO stock_produit_snapshot (produit_id, storage_id, qty_stock, source_type) VALUES (:produit, :storage, 40, 'INITIAL')"
            )
            .setParameter("produit", produit.getId())
            .setParameter("storage", STORAGE_RAYON_ID)
            .executeUpdate();
    }

    private Integer premiereFamilleId() {
        return ((Number) em.createNativeQuery("SELECT id FROM famille_produit ORDER BY id LIMIT 1").getSingleResult()).intValue();
    }
}
