package com.kobe.warehouse.service.stock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.MotifRetourProduit;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.RetourBon;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.domain.enumeration.RetourStatut;
import com.kobe.warehouse.service.dto.RetourBonDTO;
import com.kobe.warehouse.service.dto.RetourBonItemDTO;
import com.kobe.warehouse.service.errors.GenericError;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

/**
 * Retourner de la marchandise à un fournisseur sort du stock et, quand le retour porte sur un lot,
 * en décrémente la quantité. Ces deux écritures doivent rester d'accord, et surtout rester
 * réversibles : modifier ou supprimer un retour encore en attente doit remettre exactement ce qui
 * avait été retiré, des deux côtés.
 *
 * <p>Le garde-fou le plus délicat est la <b>quantité retournable d'un lot</b> : elle se calcule en
 * retranchant ce qui a déjà été retourné ailleurs, via une somme SQL qui exclut le retour courant
 * et les retours clos. Une agrégation de ce genre ne se vérifie pas sur des doublures — il faut
 * plusieurs retours réellement enregistrés pour que le calcul ait un sens.
 */
@DisplayName("RetourBonService — retours fournisseur sur PostgreSQL")
class RetourBonServiceIntegrationTest extends AbstractStockIntegrationTest {

    // ===== création =====

    @Test
    @DisplayName("Un retour sort la marchandise du stock et se numérote sur son identifiant")
    void retourSortLaMarchandiseDuStock() {
        Produit produit = produitReference("DOLIPRANE");
        StockProduit stockRayon = stock(produit, rayon, 40);
        OrderLine ligneCommande = ligneDeCommandeRecue(produit, 40);
        viderLeCache();

        RetourBonDTO retour = services.retourBonService.create(demandeDeRetour(ligneCommande, produit, 12));
        viderLeCache();

        assertEquals("RET-" + java.time.Year.now().getValue() + "-" + String.format("%04d", retour.getId()), retour.getReference());
        assertEquals(RetourStatut.VALIDATED, retour.getStatut());
        assertEquals(28, compter("SELECT qty_stock FROM stock_produit WHERE id = " + stockRayon.getId()));

        assertEquals(1, compter("SELECT COUNT(*) FROM retour_bon_item WHERE retour_bon_id = " + retour.getId()));
        assertEquals(12, compter("SELECT qty_mvt FROM retour_bon_item WHERE retour_bon_id = " + retour.getId()));
        assertEquals(40, compter("SELECT init_stock FROM retour_bon_item WHERE retour_bon_id = " + retour.getId()));
        assertEquals(28, compter("SELECT after_stock FROM retour_bon_item WHERE retour_bon_id = " + retour.getId()));
    }

    @Test
    @DisplayName("Le stock de départ additionne tous les emplacements, mais seul le rayon est débité")
    void stockInitialSurTousLesEmplacements() {
        Produit produit = produitReference("MULTI EMPLACEMENT");
        StockProduit stockRayon = stock(produit, rayon, 40);
        StockProduit stockReserve = stock(produit, reserve, 10);
        OrderLine ligneCommande = ligneDeCommandeRecue(produit, 50);
        viderLeCache();

        RetourBonDTO retour = services.retourBonService.create(demandeDeRetour(ligneCommande, produit, 12));
        viderLeCache();

        assertEquals(50, compter("SELECT init_stock FROM retour_bon_item WHERE retour_bon_id = " + retour.getId()));
        assertEquals(38, compter("SELECT after_stock FROM retour_bon_item WHERE retour_bon_id = " + retour.getId()));
        assertEquals(38, compter("SELECT qty_stock FROM stock_produit WHERE id = " + stockRayon.getId()));
        assertEquals(10, compter("SELECT qty_stock FROM stock_produit WHERE id = " + stockReserve.getId()), "la réserve n'est pas touchée");
    }

    @Test
    @DisplayName("Retourner plus que le stock disponible est refusé")
    void retourSuperieurAuStock() {
        Produit produit = produitReference("PEU DE STOCK");
        stock(produit, rayon, 5);
        OrderLine ligneCommande = ligneDeCommandeRecue(produit, 40);
        viderLeCache();

        RetourBonDTO demande = demandeDeRetour(ligneCommande, produit, 12);

        assertTrue(
            assertThrows(GenericError.class, () -> services.retourBonService.create(demande)).getMessage().contains("Stock insuffisant")
        );
    }

    @Test
    @DisplayName("Un retour sur lot en décrémente la quantité")
    void retourSurLotDecrementeLeLot() {
        Produit produit = produitReference("AVEC LOT");
        stock(produit, rayon, 40);
        Lot lot = lotSurEmplacement(produit, unique("LOT"), LocalDate.now().plusMonths(2), rayon, 40);
        OrderLine ligneCommande = ligneDeCommandeRecue(produit, 40);
        viderLeCache();

        RetourBonDTO demande = demandeDeRetour(ligneCommande, produit, 12);
        demande.getRetourBonItems().getFirst().setLotId(lot.getId());

        services.retourBonService.create(demande);
        viderLeCache();

        assertEquals(28, compter("SELECT quantity FROM lot WHERE id = " + lot.getId()));
    }

    @Test
    @DisplayName("Deux retours sur le même lot ne peuvent pas dépasser sa quantité")
    void quantiteRetournableDunLot() {
        Produit produit = produitReference("LOT PARTAGE");
        stock(produit, rayon, 40);
        Lot lot = lotSurEmplacement(produit, unique("LOT"), LocalDate.now().plusMonths(2), rayon, 20);
        OrderLine ligneCommande = ligneDeCommandeRecue(produit, 40);
        viderLeCache();

        RetourBonDTO premier = demandeDeRetour(ligneCommande, produit, 8);
        premier.getRetourBonItems().getFirst().setLotId(lot.getId());
        services.retourBonService.create(premier);
        viderLeCache();

        assertEquals(12, compter("SELECT quantity FROM lot WHERE id = " + lot.getId()));

        RetourBonDTO second = demandeDeRetour(ligneCommande, produit, 15);
        second.getRetourBonItems().getFirst().setLotId(lot.getId());

        assertTrue(
            assertThrows(GenericError.class, () -> services.retourBonService.create(second))
                .getMessage()
                .contains("Quantité insuffisante dans le lot")
        );
    }

    @Test
    @DisplayName("Retourner plus que ce que la commande a reçu est refusé")
    void retourSuperieurAuRecu() {
        Produit produit = produitReference("PEU RECU");
        stock(produit, rayon, 40);
        OrderLine ligneCommande = ligneDeCommandeRecue(produit, 5);
        viderLeCache();

        RetourBonDTO demande = demandeDeRetour(ligneCommande, produit, 12);

        assertThrows(GenericError.class, () -> services.retourBonService.create(demande));
    }

    // ===== modification et suppression =====

    @Test
    @DisplayName("Modifier un retour remet le stock d'origine avant d'appliquer la nouvelle quantité")
    void modificationRejoueLeMouvement() {
        Produit produit = produitReference("A MODIFIER");
        StockProduit stockRayon = stock(produit, rayon, 40);
        OrderLine ligneCommande = ligneDeCommandeRecue(produit, 40);
        viderLeCache();

        RetourBonDTO retour = services.retourBonService.create(demandeDeRetour(ligneCommande, produit, 12));
        viderLeCache();
        assertEquals(28, compter("SELECT qty_stock FROM stock_produit WHERE id = " + stockRayon.getId()));

        RetourBonDTO correction = demandeDeRetour(ligneCommande, produit, 5).setId(retour.getId()).setCommentaire("corrigé");
        services.retourBonService.update(correction);
        viderLeCache();

        assertEquals(35, compter("SELECT qty_stock FROM stock_produit WHERE id = " + stockRayon.getId()), "40 rendu puis 5 repris");
        assertEquals(1, compter("SELECT COUNT(*) FROM retour_bon_item WHERE retour_bon_id = " + retour.getId()));
        assertEquals(5, compter("SELECT qty_mvt FROM retour_bon_item WHERE retour_bon_id = " + retour.getId()));
        assertEquals("corrigé", em.find(RetourBon.class, retour.getId()).getCommentaire());
    }

    @Test
    @DisplayName("Supprimer un retour rend au stock et au lot ce qui en avait été retiré")
    void suppressionRendLeStock() {
        Produit produit = produitReference("A SUPPRIMER");
        StockProduit stockRayon = stock(produit, rayon, 40);
        Lot lot = lotSurEmplacement(produit, unique("LOT"), LocalDate.now().plusMonths(2), rayon, 40);
        OrderLine ligneCommande = ligneDeCommandeRecue(produit, 40);
        viderLeCache();

        RetourBonDTO demande = demandeDeRetour(ligneCommande, produit, 12);
        demande.getRetourBonItems().getFirst().setLotId(lot.getId());
        RetourBonDTO retour = services.retourBonService.create(demande);
        viderLeCache();

        services.retourBonService.delete(retour.getId());
        viderLeCache();

        assertEquals(40, compter("SELECT qty_stock FROM stock_produit WHERE id = " + stockRayon.getId()));
        assertEquals(40, compter("SELECT quantity FROM lot WHERE id = " + lot.getId()));
        assertEquals(0, compter("SELECT COUNT(*) FROM retour_bon WHERE id = " + retour.getId()));
        assertEquals(0, compter("SELECT COUNT(*) FROM retour_bon_item WHERE retour_bon_id = " + retour.getId()));
    }

    @Test
    @DisplayName("Un retour pris en charge ne se modifie plus, ne se supprime plus, ne se reprend plus")
    void retourEnCoursEstFige() {
        Produit produit = produitReference("EN COURS");
        stock(produit, rayon, 40);
        OrderLine ligneCommande = ligneDeCommandeRecue(produit, 40);
        viderLeCache();

        RetourBonDTO retour = services.retourBonService.create(demandeDeRetour(ligneCommande, produit, 12));
        viderLeCache();

        assertEquals(RetourStatut.PROCESSING, services.retourBonService.markAsProcessing(retour.getId()).getStatut());
        viderLeCache();

        Integer id = retour.getId();
        RetourBonDTO correction = demandeDeRetour(ligneCommande, produit, 5).setId(id);

        assertTrue(
            assertThrows(GenericError.class, () -> services.retourBonService.update(correction))
                .getMessage()
                .contains("en attente peuvent être modifiés")
        );
        assertTrue(
            assertThrows(GenericError.class, () -> services.retourBonService.delete(id))
                .getMessage()
                .contains("en attente peuvent être supprimés")
        );
        assertTrue(
            assertThrows(GenericError.class, () -> services.retourBonService.markAsProcessing(id))
                .getMessage()
                .contains("en attente peuvent être marqués")
        );
    }

    @Test
    @DisplayName("Un retour introuvable ne se lit, ne se modifie ni ne se supprime")
    void retourIntrouvable() {
        assertTrue(services.retourBonService.findOne(999_999).isEmpty());
        assertThrows(GenericError.class, () -> services.retourBonService.delete(999_999));
        assertThrows(GenericError.class, () -> services.retourBonService.markAsProcessing(999_999));
        assertThrows(GenericError.class, () -> services.retourBonService.update(new RetourBonDTO().setId(999_999)));
    }

    // ===== lectures =====

    @Test
    @DisplayName("Un retour se relit avec sa ligne, son lot et son fournisseur")
    void relectureDunRetour() {
        Produit produit = produitReference("A RELIRE");
        stock(produit, rayon, 40);
        OrderLine ligneCommande = ligneDeCommandeRecue(produit, 40);
        viderLeCache();

        RetourBonDTO cree = services.retourBonService.create(demandeDeRetour(ligneCommande, produit, 12));
        viderLeCache();

        RetourBonDTO relu = services.retourBonService.findOne(cree.getId()).orElseThrow();
        assertEquals(cree.getReference(), relu.getReference());
        assertEquals(RetourStatut.VALIDATED, relu.getStatut());
        assertEquals(1, relu.getRetourBonItems().size());
        RetourBonItemDTO ligne = relu.getRetourBonItems().getFirst();
        assertEquals(12, ligne.getQtyMvt());
        assertEquals(produit.getLibelle(), ligne.getProduitLibelle());
    }

    @Test
    @DisplayName("La recherche filtre par statut, période et libellé de fournisseur")
    void rechercheMultiCritere() {
        Produit produit = produitReference("A CHERCHER");
        stock(produit, rayon, 60);
        OrderLine ligneCommande = ligneDeCommandeRecue(produit, 60);
        viderLeCache();

        RetourBonDTO enAttente = services.retourBonService.create(demandeDeRetour(ligneCommande, produit, 5));
        RetourBonDTO enCours = services.retourBonService.create(demandeDeRetour(ligneCommande, produit, 4));
        services.retourBonService.markAsProcessing(enCours.getId());
        viderLeCache();

        LocalDate veille = LocalDate.now().minusDays(1);
        LocalDate lendemain = LocalDate.now().plusDays(1);

        assertEquals(
            1,
            services.retourBonService.findAll(RetourStatut.PROCESSING, null, veille, lendemain, null, PageRequest.of(0, 20)).getTotalElements()
        );
        assertEquals(
            1,
            services.retourBonService.findAll(null, RetourStatut.PROCESSING, veille, lendemain, null, PageRequest.of(0, 20)).getTotalElements(),
            "le filtre d'exclusion écarte les retours pris en charge"
        );
        assertEquals(
            1,
            services.retourBonService.findAll(null, null, null, null, enAttente.getReference(), PageRequest.of(0, 20)).getTotalElements()
        );
        assertTrue(
            services.retourBonService
                .findAll(null, null, veille.minusDays(30), veille.minusDays(10), null, PageRequest.of(0, 20))
                .isEmpty(),
            "hors période, rien ne remonte"
        );
        assertEquals(2, services.retourBonService.findAllByCommande(ligneCommande.getCommande().getId().getId(), LocalDate.now()).size());
    }

    /**
     * « En attente » se lit ici comme « pas encore clos » : un retour pris en charge reste à
     * traiter et continue de compter. Seule la clôture le sort du compteur.
     */
    @Test
    @DisplayName("Le compteur de retours en attente compte tout ce qui n'est pas clos")
    void compteurDesRetoursEnAttente() {
        Produit produit = produitReference("A COMPTER");
        stock(produit, rayon, 60);
        OrderLine ligneCommande = ligneDeCommandeRecue(produit, 60);
        viderLeCache();

        long depart = services.retourBonService.countEnAttente();
        RetourBonDTO retour = services.retourBonService.create(demandeDeRetour(ligneCommande, produit, 5));
        viderLeCache();
        assertEquals(depart + 1, services.retourBonService.countEnAttente());

        services.retourBonService.markAsProcessing(retour.getId());
        viderLeCache();
        assertEquals(depart + 1, services.retourBonService.countEnAttente(), "un retour pris en charge reste à traiter");

        em.find(RetourBon.class, retour.getId()).setStatut(RetourStatut.CLOSED);
        viderLeCache();
        assertEquals(depart, services.retourBonService.countEnAttente(), "seule la clôture le sort du compteur");
    }

    // ===== fabriques du jeu d'essai propres aux retours =====

    private Produit produitReference(String libelle) {
        Produit produit = produit(unique(libelle));
        fournisseurProduit(produit);
        em.flush();
        return produit;
    }

    /** Une ligne de commande réceptionnée : c'est elle qui borne ce qui peut être retourné. */
    private OrderLine ligneDeCommandeRecue(Produit produit, int quantiteRecue) {
        FournisseurProduit reference = produit.getFournisseurProduitPrincipal();
        Commande commande = commande(reference.getFournisseur(), OrderStatut.CLOSED);
        return ligneDeCommande(commande, reference, quantiteRecue, quantiteRecue);
    }

    private RetourBonDTO demandeDeRetour(OrderLine ligneCommande, Produit produit, int quantite) {
        RetourBonItemDTO ligne = new RetourBonItemDTO()
            .setProduitId(produit.getId())
            .setProduitCip(produit.getFournisseurProduitPrincipal().getCodeCip())
            .setQtyMvt(quantite)
            .setMotifRetourId(premierMotif().getId())
            .setOrderLineId(ligneCommande.getId().getId())
            .setOrderLineOrderDate(ligneCommande.getId().getOrderDate());
        return new RetourBonDTO()
            .setCommandeId(ligneCommande.getCommande().getId().getId())
            .setCommandeOrderDate(ligneCommande.getCommande().getId().getOrderDate())
            .setRetourBonItems(new java.util.ArrayList<>(List.of(ligne)));
    }

    private MotifRetourProduit premierMotif() {
        return em
            .createQuery("SELECT m FROM MotifRetourProduit m ORDER BY m.id", MotifRetourProduit.class)
            .setMaxResults(1)
            .getSingleResult();
    }
}
