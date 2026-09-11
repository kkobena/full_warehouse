package com.kobe.warehouse.service.stock.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.CommandeId;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.MotifBed;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.domain.enumeration.TypeDeliveryReceipt;
import com.kobe.warehouse.service.dto.BedDTO;
import com.kobe.warehouse.service.dto.BedLigneDTO;
import com.kobe.warehouse.service.dto.BedSummaryDTO;
import com.kobe.warehouse.service.errors.GenericError;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

/**
 * Le bon d'entrée directe fait entrer du stock sans commande fournisseur — un retour client, un
 * échantillon, une régularisation. Il s'écrit dans les mêmes tables qu'une commande
 * ({@code commande} et {@code order_line}), qui sont <b>partitionnées par date</b> et portent une
 * clé composite {@code (id, date)}. Charger, modifier ou supprimer une ligne suppose donc de
 * reconstituer cette clé correctement : une erreur de date ne se voit pas sur une doublure, elle
 * se voit quand la base ne trouve rien dans la bonne partition.
 *
 * <p>Deux autres choses n'existent qu'en base : la <b>numérotation</b>
 * {@code BED-AAAAMMJJ-NNN}, qui compte les bons déjà émis dans la journée, et la
 * <b>validation</b>, qui crédite réellement le stock de chaque produit par le service produit.
 */
@DisplayName("BedService — bons d'entrée directe sur PostgreSQL")
class BedServiceIntegrationTest extends AbstractStockIntegrationTest {

    // ===== création et numérotation =====

    @Test
    @DisplayName("Créer un bon l'ouvre en brouillon avec une référence datée du jour")
    void creationDunBon() {
        viderLeCache();

        BedDTO bon = services.bedService.createBed(
            new BedDTO().setMotifBed(MotifBed.RETOUR_CLIENT).setCommentaireBed("retour d'un client")
        );
        viderLeCache();

        assertNotNull(bon.getId());
        assertEquals(LocalDate.now(), bon.getOrderDate());
        assertEquals(OrderStatut.REQUESTED, bon.getOrderStatus());
        assertEquals(MotifBed.RETOUR_CLIENT, bon.getMotifBed());
        assertTrue(
            bon.getReceiptReference().startsWith("BED-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-"),
            "la référence porte la date du jour : " + bon.getReceiptReference()
        );

        Commande relu = em.find(Commande.class, new CommandeId(bon.getId(), bon.getOrderDate()));
        assertEquals(TypeDeliveryReceipt.DIRECT, relu.getType(), "un bon d'entrée n'est pas une commande fournisseur");
        assertEquals(0, relu.getGrossAmount());
        assertNotNull(relu.getOrderReference(), "il reçoit aussi un numéro de commande");
    }

    @Test
    @DisplayName("Deux bons émis le même jour reçoivent des numéros qui se suivent")
    void numerotationIncrementaleDansLaJournee() {
        viderLeCache();

        String premier = services.bedService.createBed(new BedDTO().setMotifBed(MotifBed.ECHANTILLON)).getReceiptReference();
        viderLeCache();
        String second = services.bedService.createBed(new BedDTO().setMotifBed(MotifBed.ECHANTILLON)).getReceiptReference();
        viderLeCache();

        String prefixe = "BED-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-";
        assertEquals(prefixe + "001", premier);
        assertEquals(prefixe + "002", second);
    }

    // ===== lignes =====

    @Test
    @DisplayName("Ajouter une ligne cumule son montant sur le total du bon")
    void ajoutDeLignes() {
        Produit produit = produitReference("DOLIPRANE");
        Produit autre = produitReference("EFFERALGAN");
        BedDTO bon = bonEnBrouillon();
        viderLeCache();

        services.bedService.addLigne(bon.getId(), bon.getOrderDate(), ligne(produit, 10, 600));
        BedDTO apresDeuxLignes = services.bedService.addLigne(bon.getId(), bon.getOrderDate(), ligne(autre, 4, 1_000));
        viderLeCache();

        assertEquals(2, apresDeuxLignes.getLignes().size());
        assertEquals(10_000, em.find(Commande.class, new CommandeId(bon.getId(), bon.getOrderDate())).getGrossAmount(), "10 × 600 + 4 × 1 000");
        assertEquals(2, compter("SELECT COUNT(*) FROM order_line WHERE commande_id = " + bon.getId()));

        BedLigneDTO premiere = apresDeuxLignes
            .getLignes()
            .stream()
            .filter(l -> l.getProduitId().equals(produit.getId()))
            .findFirst()
            .orElseThrow();
        assertEquals(10, premiere.getQuantite());
        assertEquals(600, premiere.getPrixAchat());
        assertEquals(produit.getFournisseurProduitPrincipal().getCodeCip(), premiere.getCodeCip());
    }

    @Test
    @DisplayName("Corriger une ligne rectifie le total sans le recalculer de zéro")
    void correctionDuneLigne() {
        Produit produit = produitReference("A CORRIGER");
        BedDTO bon = bonEnBrouillon();
        viderLeCache();

        BedDTO avec = services.bedService.addLigne(bon.getId(), bon.getOrderDate(), ligne(produit, 10, 600));
        BedLigneDTO ligne = avec.getLignes().getFirst();
        viderLeCache();

        services.bedService.updateLigne(
            bon.getId(),
            bon.getOrderDate(),
            ligne.getId(),
            ligne.getOrderDate(),
            new BedLigneDTO().setQuantite(3).setPrixAchat(500)
        );
        viderLeCache();

        assertEquals(1_500, em.find(Commande.class, new CommandeId(bon.getId(), bon.getOrderDate())).getGrossAmount());
        assertEquals(3, compter("SELECT quantity_requested FROM order_line WHERE id = " + ligne.getId()));
        assertEquals(3, compter("SELECT quantity_received FROM order_line WHERE id = " + ligne.getId()));
    }

    @Test
    @DisplayName("Retirer une ligne la retranche du total et l'efface")
    void suppressionDuneLigne() {
        Produit produit = produitReference("A RETIRER");
        Produit garde = produitReference("A GARDER");
        BedDTO bon = bonEnBrouillon();
        viderLeCache();

        BedDTO avec = services.bedService.addLigne(bon.getId(), bon.getOrderDate(), ligne(produit, 10, 600));
        services.bedService.addLigne(bon.getId(), bon.getOrderDate(), ligne(garde, 2, 1_000));
        BedLigneDTO aRetirer = avec.getLignes().getFirst();
        viderLeCache();

        services.bedService.removeLigne(bon.getId(), bon.getOrderDate(), aRetirer.getId(), aRetirer.getOrderDate());
        viderLeCache();

        assertEquals(2_000, em.find(Commande.class, new CommandeId(bon.getId(), bon.getOrderDate())).getGrossAmount());
        assertEquals(1, compter("SELECT COUNT(*) FROM order_line WHERE commande_id = " + bon.getId()));
    }

    // ===== validation =====

    @Test
    @DisplayName("Valider un bon crédite le stock de chaque produit et fige les quantités avant/après")
    void validationCrediteLeStock() {
        Produit produit = produitReference("A RENTRER");
        stock(produit, rayon, 12);
        BedDTO bon = bonEnBrouillon();
        viderLeCache();

        BedDTO avec = services.bedService.addLigne(bon.getId(), bon.getOrderDate(), ligne(produit, 8, 600));
        BedLigneDTO ligne = avec.getLignes().getFirst();
        viderLeCache();

        BedDTO valide = services.bedService.validateBed(bon.getId(), bon.getOrderDate(), null, null, "reçu ce jour");
        viderLeCache();

        assertEquals(OrderStatut.CLOSED, valide.getOrderStatus());
        assertEquals(20, compter("SELECT qty_stock FROM stock_produit WHERE produit_id = " + produit.getId()));
        assertEquals(12, compter("SELECT init_stock FROM order_line WHERE id = " + ligne.getId()));
        assertEquals(20, compter("SELECT final_stock FROM order_line WHERE id = " + ligne.getId()));
        assertEquals(
            LocalDate.now(),
            em.find(Commande.class, new CommandeId(bon.getId(), bon.getOrderDate())).getReceiptDate()
        );
    }

    @Test
    @DisplayName("Un bon validé ne se modifie plus, ne se revalide pas et ne se supprime pas")
    void bonValideEstFige() {
        Produit produit = produitReference("FIGE");
        stock(produit, rayon, 5);
        BedDTO bon = bonEnBrouillon();
        viderLeCache();

        BedDTO avec = services.bedService.addLigne(bon.getId(), bon.getOrderDate(), ligne(produit, 3, 600));
        BedLigneDTO ligne = avec.getLignes().getFirst();
        services.bedService.validateBed(bon.getId(), bon.getOrderDate(), null, null, null);
        viderLeCache();

        Integer bonId = bon.getId();
        LocalDate date = bon.getOrderDate();
        Integer ligneId = ligne.getId();
        LocalDate ligneDate = ligne.getOrderDate();

        assertTrue(
            assertThrows(GenericError.class, () -> services.bedService.addLigne(bonId, date, new BedLigneDTO()))
                .getMessage()
                .contains("ne peut plus être modifié")
        );
        assertTrue(
            assertThrows(GenericError.class, () -> services.bedService.removeLigne(bonId, date, ligneId, ligneDate))
                .getMessage()
                .contains("ne peut plus être modifié")
        );
        assertTrue(
            assertThrows(GenericError.class, () -> services.bedService.validateBed(bonId, date, null, null, null))
                .getMessage()
                .contains("déjà validé")
        );
        assertTrue(
            assertThrows(GenericError.class, () -> services.bedService.deleteBed(bonId, date))
                .getMessage()
                .contains("brouillon")
        );
    }

    @Test
    @DisplayName("Un bon vide ou sans motif ne se valide pas")
    void validationRefusee() {
        BedDTO bonVide = bonEnBrouillon();
        viderLeCache();

        assertTrue(
            assertThrows(
                GenericError.class,
                () -> services.bedService.validateBed(bonVide.getId(), bonVide.getOrderDate(), null, null, null)
            )
                .getMessage()
                .contains("au moins une ligne")
        );

        Produit produit = produitReference("SANS MOTIF");
        stock(produit, rayon, 5);
        BedDTO sansMotif = services.bedService.createBed(new BedDTO());
        services.bedService.addLigne(sansMotif.getId(), sansMotif.getOrderDate(), ligne(produit, 2, 600));
        viderLeCache();

        assertTrue(
            assertThrows(
                GenericError.class,
                () -> services.bedService.validateBed(sansMotif.getId(), sansMotif.getOrderDate(), null, null, null)
            )
                .getMessage()
                .contains("motif est obligatoire")
        );
    }

    @Test
    @DisplayName("Un bon en brouillon se supprime, un bon introuvable est refusé")
    void suppressionDunBrouillon() {
        BedDTO bon = bonEnBrouillon();
        viderLeCache();

        services.bedService.deleteBed(bon.getId(), bon.getOrderDate());
        viderLeCache();

        assertEquals(0, compter("SELECT COUNT(*) FROM commande WHERE id = " + bon.getId()));
        assertThrows(GenericError.class, () -> services.bedService.deleteBed(999_999, LocalDate.now()));
        assertThrows(GenericError.class, () -> services.bedService.findById(999_999, LocalDate.now()));
    }

    // ===== recherche =====

    @Test
    @DisplayName("La recherche filtre par motif, statut, référence et période — et ignore les commandes fournisseur")
    void rechercheMultiCritere() {
        Produit produit = produitReference("A CHERCHER");
        stock(produit, rayon, 5);
        BedDTO retourClient = services.bedService.createBed(new BedDTO().setMotifBed(MotifBed.RETOUR_CLIENT));
        services.bedService.addLigne(retourClient.getId(), retourClient.getOrderDate(), ligne(produit, 2, 600));
        services.bedService.validateBed(retourClient.getId(), retourClient.getOrderDate(), null, null, null);

        BedDTO echantillon = services.bedService.createBed(new BedDTO().setMotifBed(MotifBed.ECHANTILLON));
        commande(produit.getFournisseurProduitPrincipal().getFournisseur(), OrderStatut.REQUESTED);
        viderLeCache();

        LocalDate veille = LocalDate.now().minusDays(1);
        LocalDate lendemain = LocalDate.now().plusDays(1);

        assertEquals(
            2,
            services.bedService.findAll(null, null, null, veille, lendemain, PageRequest.of(0, 20)).getTotalElements(),
            "seuls les bons d'entrée directe remontent, pas la commande fournisseur"
        );
        assertEquals(
            1,
            services.bedService.findAll(null, MotifBed.ECHANTILLON, null, null, null, PageRequest.of(0, 20)).getTotalElements()
        );
        assertEquals(
            1,
            services.bedService.findAll(null, null, OrderStatut.CLOSED, null, null, PageRequest.of(0, 20)).getTotalElements()
        );

        BedSummaryDTO trouve = services.bedService
            .findAll(echantillon.getReceiptReference(), null, null, null, null, PageRequest.of(0, 20))
            .getContent()
            .getFirst();
        assertEquals(echantillon.getId(), trouve.getId());
        assertEquals(MotifBed.ECHANTILLON, trouve.getMotifBed());

        assertTrue(
            services.bedService
                .findAll(null, null, null, veille.minusDays(30), veille.minusDays(10), PageRequest.of(0, 20))
                .isEmpty(),
            "hors période, rien ne remonte"
        );
    }

    // ===== fabriques du jeu d'essai propres aux bons d'entrée =====

    private BedDTO bonEnBrouillon() {
        return services.bedService.createBed(new BedDTO().setMotifBed(MotifBed.REGULARISATION));
    }

    private Produit produitReference(String libelle) {
        Produit produit = produit(unique(libelle));
        fournisseurProduit(produit);
        em.flush();
        return produit;
    }

    private BedLigneDTO ligne(Produit produit, int quantite, int prixAchat) {
        FournisseurProduit reference = produit.getFournisseurProduitPrincipal();
        return new BedLigneDTO()
            .setFournisseurProduitId(reference.getId())
            .setQuantite(quantite)
            .setPrixAchat(prixAchat)
            .setPrixVente(produit.getRegularUnitPrice());
    }
}
