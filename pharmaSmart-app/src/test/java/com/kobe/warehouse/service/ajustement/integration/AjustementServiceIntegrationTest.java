package com.kobe.warehouse.service.ajustement.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kobe.warehouse.domain.Ajust;
import com.kobe.warehouse.domain.Ajustement;
import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.AjustType;
import com.kobe.warehouse.domain.enumeration.AjustementStatut;
import com.kobe.warehouse.domain.enumeration.StatutLot;
import com.kobe.warehouse.service.dto.AjustDTO;
import com.kobe.warehouse.service.dto.AjustementDTO;
import com.kobe.warehouse.service.errors.GenericError;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Valider un ajustement écrit dans quatre endroits du même geste : la quantité de
 * {@code stock_produit}, la quantité courante des {@code lot}, leur répartition par emplacement
 * dans {@code lot_stock_location}, et le journal {@code inventory_transaction}. Ces écritures
 * doivent rester d'accord entre elles — c'est le seul garde-fou contre un stock qui se met à
 * diverger de ses lots, et il ne tient que dans les requêtes FEFO et les contraintes de la base.
 */
@DisplayName("AjustementService — ajustement de stock sur PostgreSQL")
class AjustementServiceIntegrationTest extends AbstractAjustementIntegrationTest {

    // ===== saisie =====

    @Test
    @DisplayName("Créer un ajustement l'enregistre en brouillon sans toucher au stock")
    void creationEnBrouillon() {
        Produit produit = produitEnStock(unique("DOLIPRANE"), 20);
        viderLeCache();

        AjustDTO cree = services.ajustementService.createAjsut(
            creation("casse en rayon", ligneSansAjust(produit, -4)));
        viderLeCache();

        Ajust relu = services.ajustRepository.findById(cree.getId()).orElseThrow();
        assertEquals(AjustementStatut.PENDING, relu.getStatut(), "rien n'est appliqué avant validation");
        assertEquals("casse en rayon", relu.getCommentaire());
        assertEquals(utilisateur.getId(), relu.getUser().getId());

        List<Ajustement> lignes = services.ajustementRepository.findAllByAjustId(cree.getId());
        assertEquals(1, lignes.size());
        assertEquals(-4, lignes.getFirst().getQtyMvt());
        assertEquals(20, lignes.getFirst().getStockBefore());
        assertEquals(16, lignes.getFirst().getStockAfter());
        assertEquals(AjustType.AJUSTEMENT_OUT, lignes.getFirst().getType());
        assertEquals(20, stockEnBase(produit, rayon), "le stock physique n'a pas bougé");
    }

    @Test
    @DisplayName("Une seconde saisie du même produit cumule sur la ligne existante")
    void cumulSurLaMemeLigne() {
        Ajust ajust = ajustEnCours("inventaire tournant");
        Produit produit = produitEnStock(unique("DOLIPRANE"), 20);
        viderLeCache();

        services.ajustementService.createOrUpdate(ligne(ajust, produit, -4));
        viderLeCache();
        services.ajustementService.createOrUpdate(ligne(ajust, produit, -2));
        viderLeCache();

        List<Ajustement> lignes = services.ajustementRepository.findAllByAjustId(ajust.getId());
        assertEquals(1, lignes.size(), "la contrainte d'unicité (ajustement, stock) tient");
        assertEquals(-6, lignes.getFirst().getQtyMvt());
        assertEquals(14, lignes.getFirst().getStockAfter());
    }

    @Test
    @DisplayName("Un produit sans stock dans l'emplacement demandé est refusé")
    void produitSansStockDansLEmplacement() {
        Ajust ajust = ajustEnCours("saisie");
        Produit produit = produit(unique("JAMAIS APPROVISIONNE"));
        stock(produit, reserve, 5);
        viderLeCache();

        AjustementDTO saisie = ligne(ajust, produit, -2).setStorageId(STORAGE_RAYON_ID);

        GenericError erreur = assertThrows(GenericError.class,
            () -> services.ajustementService.createOrUpdate(saisie));
        assertTrue(erreur.getMessage().contains("n'a pas de stock dans le magasin selectionné"));
    }

    @Test
    @DisplayName("La saisie porte sur l'emplacement demandé, pas sur celui de vente")
    void saisieSurLaReserve() {
        Ajust ajust = ajustEnCours("réserve");
        Produit produit = produit(unique("EN RESERVE"));
        stock(produit, rayon, 20);
        stock(produit, reserve, 8);
        viderLeCache();

        services.ajustementService.createOrUpdate(ligne(ajust, produit, -3).setStorageId(STORAGE_RESERVE_ID));
        viderLeCache();

        Ajustement ligne = services.ajustementRepository.findAllByAjustId(ajust.getId()).getFirst();
        assertEquals(STORAGE_RESERVE_ID, ligne.getStockProduit().getStorage().getId());
        assertEquals(8, ligne.getStockBefore(), "le stock de départ est celui de la réserve");
    }

    @Test
    @DisplayName("Le motif saisi est rattaché à la ligne")
    void motifRattache() {
        Ajust ajust = ajustEnCours("péremption");
        Produit produit = produitEnStock(unique("PERIME"), 20);
        var motif = motif(unique("PEREMPTION"));
        viderLeCache();

        services.ajustementService.createOrUpdate(ligne(ajust, produit, -3).setMotifAjustementId(motif.getId()));
        viderLeCache();

        assertEquals(
            motif.getId(),
            services.ajustementRepository.findAllByAjustId(ajust.getId()).getFirst()
                .getMotifAjustement().getId()
        );
    }

    // ===== validation : stock et lots =====

    @Test
    @DisplayName("Un ajustement négatif écrit le stock et débite les lots au plus proche de la péremption")
    void validationNegative() {
        Ajust ajust = ajustEnCours("casse");
        Produit produit = produit(unique("DOLIPRANE"), 10_000, 6_000);
        stock(produit, rayon, 30);
        Lot proche = lotSurEmplacement(produit, unique("LOT-PROCHE"), LocalDate.now().plusMonths(2), rayon, 12);
        Lot lointain = lotSurEmplacement(produit, unique("LOT-LOINTAIN"), LocalDate.now().plusYears(2), rayon, 18);
        services.ajustementService.createOrUpdate(ligne(ajust, produit, -15));
        viderLeCache();

        services.ajustementService.saveAjust(validation(ajust, "validation"));
        viderLeCache();

        assertEquals(15, stockEnBase(produit, rayon));
        assertEquals(0, quantiteCourante(proche), "le lot le plus proche de la péremption part en premier");
        assertEquals(15, quantiteCourante(lointain), "le reliquat est pris sur le lot suivant");
        assertEquals(15, quantiteSurEmplacement(lointain, rayon));
        assertEquals(AjustementStatut.CLOSED,
            services.ajustRepository.findById(ajust.getId()).orElseThrow().getStatut());
    }

    @Test
    @DisplayName("Un lot vidé passe en épuisé et disparaît de l'emplacement")
    void lotEpuise() {
        Ajust ajust = ajustEnCours("casse");
        Produit produit = produitEnStock(unique("DOLIPRANE"), 12);
        Lot lot = lotSurEmplacement(produit, unique("LOT-A"), LocalDate.now().plusMonths(2), rayon, 12);
        services.ajustementService.createOrUpdate(ligne(ajust, produit, -12));
        viderLeCache();

        services.ajustementService.saveAjust(validation(ajust, "validation"));
        viderLeCache();

        assertEquals(0, stockEnBase(produit, rayon));
        assertEquals(0, quantiteCourante(lot));
        assertEquals(
            StatutLot.SOLD.name(),
            em.createNativeQuery("SELECT statut FROM lot WHERE id = " + lot.getId()).getSingleResult()
        );
        assertEquals(-1, quantiteSurEmplacement(lot, rayon), "la ligne d'emplacement à zéro est supprimée");
    }

    @Test
    @DisplayName("Le débit des lots ne dépasse jamais le stock physique de départ")
    void debitPlafonneAuStockInitial() {
        Ajust ajust = ajustEnCours("erreur de saisie");
        Produit produit = produitEnStock(unique("DOLIPRANE"), 5);
        Lot lot = lotSurEmplacement(produit, unique("LOT-A"), LocalDate.now().plusMonths(6), rayon, 5);
        services.ajustementService.createOrUpdate(ligne(ajust, produit, -20));
        viderLeCache();

        services.ajustementService.saveAjust(validation(ajust, "validation"));
        viderLeCache();

        assertEquals(-15, stockEnBase(produit, rayon), "le stock suit la saisie, fût-elle absurde");
        assertEquals(0, quantiteCourante(lot), "mais on ne débite pas plus de lot qu'il n'y en avait");
    }

    @Test
    @DisplayName("Un stock de départ nul ne débite aucun lot")
    void stockInitialNul() {
        Ajust ajust = ajustEnCours("casse");
        Produit produit = produitEnStock(unique("DOLIPRANE"), 0);
        Lot lot = lotSurEmplacement(produit, unique("LOT-A"), LocalDate.now().plusMonths(6), rayon, 7);
        services.ajustementService.createOrUpdate(ligne(ajust, produit, -3));
        viderLeCache();

        services.ajustementService.saveAjust(validation(ajust, "validation"));
        viderLeCache();

        assertEquals(7, quantiteCourante(lot), "le lot est laissé intact");
        assertEquals(7, quantiteSurEmplacement(lot, rayon));
    }

    @Test
    @DisplayName("Un ajustement positif sans lot choisi crédite le dernier lot reçu")
    void validationPositiveSansLotChoisi() {
        Ajust ajust = ajustEnCours("retrouvé en réserve");
        Produit produit = produitEnStock(unique("DOLIPRANE"), 10);
        Lot ancien = lotSurEmplacement(produit, unique("LOT-ANCIEN"), LocalDate.now().plusMonths(2), rayon, 4);
        Lot dernier = lotSurEmplacement(produit, unique("LOT-RECENT"), LocalDate.now().plusYears(2), rayon, 6);
        services.ajustementService.createOrUpdate(ligne(ajust, produit, 5));
        viderLeCache();

        services.ajustementService.saveAjust(validation(ajust, "validation"));
        viderLeCache();

        assertEquals(15, stockEnBase(produit, rayon));
        assertEquals(4, quantiteCourante(ancien), "le lot le plus ancien n'est pas touché");
        assertEquals(11, quantiteCourante(dernier));
        assertEquals(11, quantiteSurEmplacement(dernier, rayon));
    }

    @Test
    @DisplayName("Un ajustement positif sur un lot choisi crédite ce lot-là")
    void validationPositiveSurLotChoisi() {
        Ajust ajust = ajustEnCours("retour de tournée");
        Produit produit = produitEnStock(unique("DOLIPRANE"), 10);
        Lot vise = lotSurEmplacement(produit, unique("LOT-VISE"), LocalDate.now().plusMonths(2), rayon, 4);
        Lot dernier = lotSurEmplacement(produit, unique("LOT-RECENT"), LocalDate.now().plusYears(2), rayon, 6);
        services.ajustementService.createOrUpdate(ligne(ajust, produit, 5).setLotId(vise.getId()));
        viderLeCache();

        services.ajustementService.saveAjust(validation(ajust, "validation"));
        viderLeCache();

        assertEquals(15, stockEnBase(produit, rayon));
        assertEquals(9, quantiteCourante(vise), "c'est le lot désigné qui reçoit la quantité");
        assertEquals(9, quantiteSurEmplacement(vise, rayon));
        assertEquals(6, quantiteCourante(dernier), "l'heuristique du dernier reçu ne s'applique pas");
    }

    @Test
    @DisplayName("Créditer un lot absent de l'emplacement l'y fait apparaître")
    void lotAbsentDeLEmplacement() {
        Ajust ajust = ajustEnCours("remontée de réserve");
        Produit produit = produitEnStock(unique("DOLIPRANE"), 10);
        Lot enReserve = lotSurEmplacement(produit, unique("LOT-RESERVE"), LocalDate.now().plusYears(1), reserve, 8);
        services.ajustementService.createOrUpdate(ligne(ajust, produit, 3).setLotId(enReserve.getId()));
        viderLeCache();

        services.ajustementService.saveAjust(validation(ajust, "validation"));
        viderLeCache();

        assertEquals(3, quantiteSurEmplacement(enReserve, rayon), "la présence en rayon est créée");
        assertEquals(8, quantiteSurEmplacement(enReserve, reserve), "celle de la réserve est intacte");
        assertEquals(11, quantiteCourante(enReserve));
    }

    @Test
    @DisplayName("Valider remet les unités gratuites à zéro, elles sont comptées dans le stock")
    void unitesGratuitesResorbees() {
        Ajust ajust = ajustEnCours("régularisation");
        Produit produit = produit(unique("AVEC UG"));
        stock(produit, rayon, 20, 3);
        services.ajustementService.createOrUpdate(ligne(ajust, produit, -5));
        viderLeCache();

        services.ajustementService.saveAjust(validation(ajust, "validation"));
        viderLeCache();

        assertEquals(15, stockEnBase(produit, rayon), "le stock physique perd les 5 unités");
        assertEquals(0, compter("SELECT qty_ug FROM stock_produit WHERE produit_id = %d AND storage_id = %d"
            .formatted(produit.getId(), STORAGE_RAYON_ID)));
        Ajustement ligne = services.ajustementRepository.findAllByAjustId(ajust.getId()).getFirst();
        assertEquals(23, ligne.getStockBefore(), "l'écart est calculé sur le stock total, UG comprises");
        assertEquals(15, ligne.getStockAfter(), "mais le stock écrit ne compte plus les UG");
    }

    @Test
    @DisplayName("Un ajustement de la réserve ne touche pas au rayon")
    void validationSurLaReserve() {
        Ajust ajust = ajustEnCours("réserve");
        Produit produit = produit(unique("DOUBLE EMPLACEMENT"));
        stock(produit, rayon, 20);
        stock(produit, reserve, 8);
        services.ajustementService.createOrUpdate(ligne(ajust, produit, -3).setStorageId(STORAGE_RESERVE_ID));
        viderLeCache();

        services.ajustementService.saveAjust(validation(ajust, "validation"));
        viderLeCache();

        assertEquals(5, stockEnBase(produit, reserve));
        assertEquals(20, stockEnBase(produit, rayon));
    }

    @Test
    @DisplayName("Chaque ligne de l'ajustement est appliquée")
    void toutesLesLignes() {
        Ajust ajust = ajustEnCours("inventaire tournant");
        Produit premier = produitEnStock(unique("PREMIER"), 20);
        Produit second = produitEnStock(unique("SECOND"), 10);
        services.ajustementService.createOrUpdate(ligne(ajust, premier, -5));
        services.ajustementService.createOrUpdate(ligne(ajust, second, 3));
        viderLeCache();

        services.ajustementService.saveAjust(validation(ajust, "validation"));
        viderLeCache();

        assertEquals(15, stockEnBase(premier, rayon));
        assertEquals(13, stockEnBase(second, rayon));
    }

    @Test
    @DisplayName("Valider deux fois n'applique le mouvement qu'une seule fois")
    void doubleValidation() {
        Ajust ajust = ajustEnCours("casse");
        Produit produit = produitEnStock(unique("DOLIPRANE"), 20);
        Lot lot = lotSurEmplacement(produit, unique("LOT-A"), LocalDate.now().plusMonths(6), rayon, 20);
        services.ajustementService.createOrUpdate(ligne(ajust, produit, -5));
        viderLeCache();
        services.ajustementService.saveAjust(validation(ajust, "validation"));
        viderLeCache();

        AjustDTO seconde = validation(ajust, "revalidation");
        GenericError erreur = assertThrows(GenericError.class,
            () -> services.ajustementService.saveAjust(seconde));
        viderLeCache();

        assertTrue(erreur.getMessage().contains("déjà été validé"));
        assertEquals(15, stockEnBase(produit, rayon), "le stock n'est débité qu'une fois");
        assertEquals(15, quantiteCourante(lot), "le lot non plus");
        assertEquals(15, quantiteSurEmplacement(lot, rayon));
        assertEquals(
            1,
            compter("SELECT count(*) FROM inventory_transaction WHERE produit_id = " + produit.getId()),
            "le journal ne porte qu'un seul mouvement"
        );
        assertEquals(
            "validation",
            em.createNativeQuery("SELECT commentaire FROM ajust WHERE id = " + ajust.getId()).getSingleResult(),
            "le commentaire de la première validation fait foi"
        );
    }

    // ===== journal =====

    @Test
    @DisplayName("La validation journalise un mouvement daté, signé et rattaché à l'emplacement")
    void journalDuMouvement() {
        Ajust ajust = ajustEnCours("casse");
        Produit produit = produit(unique("DOLIPRANE"), 10_000, 6_000);
        stock(produit, rayon, 20);
        services.ajustementService.createOrUpdate(ligne(ajust, produit, -5));
        viderLeCache();

        services.ajustementService.saveAjust(validation(ajust, "validation"));
        viderLeCache();

        String filtre = "WHERE produit_id = " + produit.getId();
        assertEquals(1, compter("SELECT count(*) FROM inventory_transaction " + filtre));
        assertEquals("AJUSTEMENT_OUT",
            em.createNativeQuery("SELECT mouvement_type FROM inventory_transaction " + filtre).getSingleResult());
        assertEquals(-5, compter("SELECT quantity FROM inventory_transaction " + filtre));
        assertEquals(20, compter("SELECT quantity_befor FROM inventory_transaction " + filtre));
        assertEquals(15, compter("SELECT quantity_after FROM inventory_transaction " + filtre));
        assertEquals(6_000, compter("SELECT cost_amount FROM inventory_transaction " + filtre),
            "le prix vient du fournisseur principal");
        assertEquals(STORAGE_RAYON_ID, compter("SELECT storage_id FROM inventory_transaction " + filtre));
        assertEquals(MAGASIN_ID, compter("SELECT magasin_id FROM inventory_transaction " + filtre));
        assertEquals(utilisateur.getId().intValue(),
            (int) compter("SELECT user_id FROM inventory_transaction " + filtre));
    }

    @Test
    @DisplayName("Un ajustement positif est journalisé comme une entrée")
    void journalDUneEntree() {
        Ajust ajust = ajustEnCours("retrouvé");
        Produit produit = produitEnStock(unique("DOLIPRANE"), 20);
        services.ajustementService.createOrUpdate(ligne(ajust, produit, 6));
        viderLeCache();

        services.ajustementService.saveAjust(validation(ajust, "validation"));
        viderLeCache();

        assertEquals("AJUSTEMENT_IN",
            em.createNativeQuery("SELECT mouvement_type FROM inventory_transaction WHERE produit_id = " + produit.getId())
                .getSingleResult());
        assertEquals(6, compter("SELECT quantity FROM inventory_transaction WHERE produit_id = " + produit.getId()));
    }

    // ===== consultation =====

    @Test
    @DisplayName("Les lignes d'un ajustement sont rendues du comptage le plus récent au plus ancien")
    void consultationTriee() {
        Ajust ajust = ajustEnCours("saisie");
        Produit ancien = produitEnStock("AAA ANCIEN " + unique(""), 20);
        Produit recent = produitEnStock("ZZZ RECENT " + unique(""), 20);
        services.ajustementService.createOrUpdate(ligne(ajust, ancien, -2));
        services.ajustementService.createOrUpdate(ligne(ajust, recent, -3));
        viderLeCache();
        // La date de mouvement est posée à la saisie : on la recule pour départager les deux lignes.
        em.createNativeQuery(
                "UPDATE ajustement SET date_mtv = date_mtv - INTERVAL '1 day' WHERE stock_produit_id IN (SELECT id FROM stock_produit WHERE produit_id = %d)"
                    .formatted(ancien.getId()))
            .executeUpdate();
        viderLeCache();

        List<AjustementDTO> lignes = services.ajustementService.findAll(ajust.getId(), null);

        assertEquals(2, lignes.size());
        assertTrue(lignes.getFirst().getProduitLibelle().startsWith("ZZZ RECENT"));
    }

    @Test
    @DisplayName("La recherche porte sur le libellé du produit comme sur son code CIP")
    void rechercheDansLesLignes() {
        Ajust ajust = ajustEnCours("saisie");
        Produit doliprane = produit("DOLIPRANE 1000MG");
        stock(doliprane, rayon, 20);
        Produit efferalgan = produit("EFFERALGAN 500MG");
        stock(efferalgan, rayon, 20);
        services.ajustementService.createOrUpdate(ligne(ajust, doliprane, -2));
        services.ajustementService.createOrUpdate(ligne(ajust, efferalgan, -3));
        viderLeCache();

        assertEquals(1, services.ajustementService.findAll(ajust.getId(), "dolipra").size(),
            "la casse n'entre pas en ligne de compte");
        assertEquals(1, services.ajustementService.findAll(ajust.getId(), codeCip(efferalgan)).size());
        assertEquals(0, services.ajustementService.findAll(ajust.getId(), "ASPIRINE").size());
        assertEquals(2, services.ajustementService.findAll(ajust.getId(), null).size());
    }

    @Test
    @DisplayName("Les lignes d'un ajustement voisin ne remontent pas")
    void cloisonnementEntreAjustements() {
        Ajust vise = ajustEnCours("le mien");
        Ajust voisin = ajustEnCours("celui du collègue");
        Produit produit = produitEnStock(unique("DOLIPRANE"), 20);
        services.ajustementService.createOrUpdate(ligne(vise, produit, -2));
        viderLeCache();

        assertEquals(1, services.ajustementService.findAll(vise.getId(), null).size());
        assertEquals(0, services.ajustementService.findAll(voisin.getId(), null).size());
    }

    // ===== modification et suppression =====

    @Test
    @DisplayName("Modifier une ligne remplace la quantité sans toucher au stock physique")
    void modificationDUneLigne() {
        Ajust ajust = ajustEnCours("saisie");
        Produit produit = produit(unique("DOLIPRANE"));
        stock(produit, rayon, 20, 3);
        services.ajustementService.createOrUpdate(ligne(ajust, produit, -4));
        viderLeCache();
        Ajustement saisie = services.ajustementRepository.findAllByAjustId(ajust.getId()).getFirst();

        AjustementDTO modification = ligne(ajust, produit, -6);
        modification.setId(saisie.getId());
        AjustementDTO resultat = services.ajustementService.update(modification);
        viderLeCache();

        assertEquals(-6, resultat.getQtyMvt(), "la quantité est remplacée, pas cumulée");
        Ajustement relue = services.ajustementRepository.findById(saisie.getId()).orElseThrow();
        assertEquals(20, relue.getStockBefore(), "le recalcul se fait sur le stock physique, sans les UG");
        assertEquals(14, relue.getStockAfter());
        assertEquals(20, stockEnBase(produit, rayon), "le stock n'est écrit qu'à la validation");
    }

    @Test
    @DisplayName("Supprimer une ligne la retire de l'ajustement")
    void suppressionDUneLigne() {
        Ajust ajust = ajustEnCours("saisie");
        Produit premier = produitEnStock(unique("PREMIER"), 20);
        Produit second = produitEnStock(unique("SECOND"), 20);
        services.ajustementService.createOrUpdate(ligne(ajust, premier, -2));
        services.ajustementService.createOrUpdate(ligne(ajust, second, -3));
        viderLeCache();
        Integer aSupprimer = services.ajustementRepository.findAllByAjustId(ajust.getId()).getFirst().getId();

        services.ajustementService.deleteItem(aSupprimer);
        viderLeCache();

        assertEquals(1, services.ajustementRepository.findAllByAjustId(ajust.getId()).size());
    }

    @Test
    @DisplayName("Supprimer un ajustement en brouillon emporte ses lignes")
    void suppressionDUnBrouillon() {
        Ajust ajust = ajustEnCours("à jeter");
        Produit produit = produitEnStock(unique("DOLIPRANE"), 20);
        services.ajustementService.createOrUpdate(ligne(ajust, produit, -2));
        viderLeCache();

        services.ajustementService.delete(ajust.getId());
        viderLeCache();

        assertTrue(services.ajustRepository.findById(ajust.getId()).isEmpty());
        assertEquals(0, compter("SELECT count(*) FROM ajustement WHERE ajust_id = " + ajust.getId()),
            "les lignes partent avec l'en-tête");
        assertEquals(20, stockEnBase(produit, rayon));
    }

    @Test
    @DisplayName("Un ajustement déjà validé ne se supprime pas")
    void suppressionRefuseeSurUnAjustementValide() {
        Ajust ajust = ajustEnCours("validé");
        Produit produit = produitEnStock(unique("DOLIPRANE"), 20);
        services.ajustementService.createOrUpdate(ligne(ajust, produit, -2));
        viderLeCache();
        services.ajustementService.saveAjust(validation(ajust, "validation"));
        viderLeCache();

        services.ajustementService.delete(ajust.getId());
        viderLeCache();

        assertTrue(services.ajustRepository.findById(ajust.getId()).isPresent(),
            "un mouvement de stock déjà journalisé ne peut plus être effacé");
    }

    // ===== outils =====

    /** Une ligne de création : l'en-tête n'existe pas encore, {@code ajustId} est donc vide. */
    private AjustementDTO ligneSansAjust(Produit produit, int quantite) {
        AjustementDTO dto = new AjustementDTO();
        dto.setProduitId(produit.getId());
        dto.setQtyMvt(quantite);
        return dto;
    }
}
