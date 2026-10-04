package com.kobe.warehouse.service.sale.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.AvoirClient;
import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.DefaultPayment;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Logs;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.UninsuredCustomer;
import com.kobe.warehouse.domain.enumeration.AvoirClientStatut;
import com.kobe.warehouse.domain.enumeration.ModeClotureAvoir;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.domain.enumeration.PaimentStatut;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import com.kobe.warehouse.domain.enumeration.TypeDeliveryReceipt;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.sale.dto.AvoirClientDocumentDTO;
import com.kobe.warehouse.service.sale.dto.CloturerAvoirRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageRequest;

/**
 * {@link com.kobe.warehouse.service.sale.AvoirClientDocumentService} sur un vrai PostgreSQL.
 *
 * <p>L'avoir client est une dette de l'officine envers un patient qu'elle n'a pas pu servir en
 * entier. Il se solde par tranches : chaque clôture partielle écrit une utilisation et laisse
 * l'avoir ouvert tant que le montant n'est pas épuisé. Ce cumul, la remise à zéro de
 * {@code quantity_avoir} sur la ligne d'origine et le rattachement à la commande de réassort sont
 * trois écritures liées que seule la base peut confirmer.
 */
@DisplayName("AvoirClientDocumentService — avoirs client sur PostgreSQL")
class AvoirClientDocumentServiceIntegrationTest extends AbstractSaleIntegrationTest {

    @Test
    @DisplayName("Une vente servie en partie ouvre un avoir du montant manquant")
    void ouvertureDUnAvoir() {
        Produit produit = produitEnStock("AMLODIPINE", 2_000, 1_200, 0, 10);
        UninsuredCustomer client = client("BAMBA", "Ali", "CLI-AV-1");
        SalesLine ligne = venteFermee(produit, 5, 2).getSalesLines().iterator().next();

        services.avoirClientDocumentService.createAvoirsFromSale(ligne, client);
        viderLeCache();

        AvoirClient avoir = avoirDeLaLigne(ligne);
        assertNotNull(avoir.getReference(), "l'avoir porte une référence issue du compteur en base");
        assertEquals(AvoirClientStatut.OUVERT, avoir.getStatut());
        assertEquals(3, avoir.getQuantite());
        assertEquals(6_000, avoir.getMontant(), "3 manquants × 2 000");
        assertEquals(0, avoir.getMontantUtilise());
        assertEquals(LocalDate.now().plusDays(90), avoir.getDateExpiration(), "délai de validité configuré");
        assertEquals(client.getId(), avoir.getCustomer().getId());
    }

    @Test
    @DisplayName("Annuler la vente d'origine passe l'avoir en ANNULE sans l'effacer")
    void annulationDUnAvoir() {
        Produit produit = produitEnStock("CANDESARTAN", 1_500, 900, 0, 10);
        SalesLine ligne = venteFermee(produit, 4, 1).getSalesLines().iterator().next();
        services.avoirClientDocumentService.createAvoirsFromSale(ligne, client("SOW", "Bineta", "CLI-AV-2"));
        viderLeCache();

        services.avoirClientDocumentService.cancelAvoirsFromSale(ligne.getId().getId());
        viderLeCache();

        assertEquals(AvoirClientStatut.ANNULE, avoirDeLaLigne(ligne).getStatut(), "la trace reste, le solde ne compte plus");
    }

    @Test
    @DisplayName("Une clôture pour la totalité solde l'avoir et libère la ligne de vente")
    void clotureTotale() {
        Produit produit = produitEnStock("RAMIPRIL", 1_000, 600, 0, 20);
        SalesLine ligne = venteFermee(produit, 5, 2).getSalesLines().iterator().next();
        AvoirClient avoir = avoirOuvert(ligne);

        AvoirClientDocumentDTO solde = services.avoirClientDocumentService.cloturerAvoir(
            avoir.getId(),
            new CloturerAvoirRequest(ModeClotureAvoir.REMBOURSEMENT_ESPECES, "remboursé au comptoir", null)
        );
        viderLeCache();

        assertEquals(AvoirClientStatut.CLOTURE, solde.statut());
        AvoirClient relu = em.find(AvoirClient.class, avoir.getId());
        assertEquals(3_000, relu.getMontantUtilise(), "le montant restant est consommé d'un coup");
        assertEquals(0, relu.getMontantRestant());
        assertNotNull(relu.getClotureLe());
        assertEquals(caissier.getId(), relu.getClosedBy().getId());
        assertEquals(0, em.find(SalesLine.class, ligne.getId()).getQuantityAvoir(), "la ligne d'origine ne doit plus rien");
        assertEquals(1, compter("SELECT count(*) FROM avoir_client_utilisation WHERE avoir_client_id = " + avoir.getId()));
    }

    @ParameterizedTest
    @EnumSource(value = ModeClotureAvoir.class, names = {"REMBOURSEMENT_ESPECES", "REMBOURSEMENT_CB"})
    @DisplayName("Un remboursement sort de la caisse ouverte, au mode de paiement choisi")
    void remboursementSortDeLaCaisse(ModeClotureAvoir mode) {
        Produit produit = produitEnStock("PERINDOPRIL-" + mode, 1_000, 600, 0, 20);
        AvoirClient avoir = avoirOuvert(venteFermee(produit, 5, 2).getSalesLines().iterator().next());

        services.avoirClientDocumentService.cloturerAvoir(avoir.getId(), new CloturerAvoirRequest(mode, "client pressé", 1_000));
        em.flush();
        viderLeCache();

        List<DefaultPayment> sorties = em
            .createQuery("select p from DefaultPayment p where p.typeFinancialTransaction = :t and p.commentaire like :c", DefaultPayment.class)
            .setParameter("t", TypeFinancialTransaction.SORTIE_CAISSE)
            .setParameter("c", "%" + avoir.getReference() + "%")
            .getResultList();
        assertEquals(1, sorties.size(), "un remboursement = un mouvement de caisse");
        DefaultPayment sortie = sorties.getFirst();
        assertEquals(1_000, sortie.getPaidAmount(), "la tranche remboursée, pas tout l'avoir");
        assertTrue(sortie.isCredit(), "sens sortie : l'argent quitte le tiroir");
        assertEquals(mode == ModeClotureAvoir.REMBOURSEMENT_ESPECES ? "CASH" : "CB", sortie.getPaymentMode().getCode());
        assertEquals(caisse.getId(), sortie.getCashRegister().getId(), "rattaché à la caisse ouverte de l'utilisateur");
        assertTrue(sortie.getCommentaire().contains("client pressé"));
    }

    @Test
    @DisplayName("Sans caisse ouverte, le remboursement est refusé et l'avoir reste intact")
    void remboursementSansCaisseOuverte() {
        Produit produit = produitEnStock("VALSARTAN", 1_000, 600, 0, 20);
        AvoirClient avoir = avoirOuvert(venteFermee(produit, 5, 2).getSalesLines().iterator().next());
        when(services.cashRegisterService.getOpiningCashRegisterByUser(any())).thenReturn(Optional.empty());

        assertThrows(GenericError.class, () -> services.avoirClientDocumentService.cloturerAvoir(
            avoir.getId(), new CloturerAvoirRequest(ModeClotureAvoir.REMBOURSEMENT_ESPECES, null, null)));
        viderLeCache();

        AvoirClient relu = em.find(AvoirClient.class, avoir.getId());
        assertEquals(AvoirClientStatut.OUVERT, relu.getStatut());
        assertEquals(0, relu.getMontantUtilise());
        assertEquals(0, compter("SELECT count(*) FROM avoir_client_utilisation WHERE avoir_client_id = " + avoir.getId()));
    }

    @ParameterizedTest
    @EnumSource(value = ModeClotureAvoir.class, names = {"BON_AVOIR", "COMPENSATION_VENTE", "RETOUR_PRODUIT"})
    @DisplayName("Les autres modes ne touchent pas à la caisse")
    void autresModesSansMouvementDeCaisse(ModeClotureAvoir mode) {
        Produit produit = produitEnStock("IRBESARTAN-" + mode, 1_000, 600, 0, 20);
        AvoirClient avoir = avoirOuvert(venteFermee(produit, 5, 2).getSalesLines().iterator().next());

        services.avoirClientDocumentService.cloturerAvoir(avoir.getId(), new CloturerAvoirRequest(mode, null, null));
        em.flush();

        assertEquals(0, em.createQuery("select count(p) from DefaultPayment p where p.commentaire like :c", Long.class)
            .setParameter("c", "%" + avoir.getReference() + "%").getSingleResult());
    }

    @Test
    @DisplayName("Deux clôtures partielles cumulent et ne soldent qu'à la dernière")
    void cloturesPartielles() {
        Produit produit = produitEnStock("BISOPROLOL", 1_000, 600, 0, 20);
        SalesLine ligne = venteFermee(produit, 5, 2).getSalesLines().iterator().next();
        AvoirClient avoir = avoirOuvert(ligne);

        services.avoirClientDocumentService.cloturerAvoir(
            avoir.getId(),
            new CloturerAvoirRequest(ModeClotureAvoir.BON_AVOIR, "premier acompte", 1_000)
        );
        viderLeCache();

        AvoirClient apresPremiere = em.find(AvoirClient.class, avoir.getId());
        assertEquals(AvoirClientStatut.OUVERT, apresPremiere.getStatut(), "il reste 2 000 à servir");
        assertEquals(2_000, apresPremiere.getMontantRestant());

        services.avoirClientDocumentService.cloturerAvoir(
            avoir.getId(),
            new CloturerAvoirRequest(ModeClotureAvoir.BON_AVOIR, "solde", 2_000)
        );
        viderLeCache();

        AvoirClient apresSeconde = em.find(AvoirClient.class, avoir.getId());
        assertEquals(AvoirClientStatut.CLOTURE, apresSeconde.getStatut());
        assertEquals(3_000, apresSeconde.getMontantUtilise());
        assertEquals(
            2,
            compter("SELECT count(*) FROM avoir_client_utilisation WHERE avoir_client_id = " + avoir.getId()),
            "chaque tranche laisse sa propre trace : c'est le justificatif de caisse"
        );
    }

    @Test
    @DisplayName("On ne peut pas utiliser plus que le montant restant")
    void montantSuperieurAuRestant() {
        Produit produit = produitEnStock("ATORVASTATINE", 1_000, 600, 0, 20);
        AvoirClient avoir = avoirOuvert(venteFermee(produit, 4, 1).getSalesLines().iterator().next());

        var requete = new CloturerAvoirRequest(ModeClotureAvoir.BON_AVOIR, null, 99_000);
        assertThrows(GenericError.class, () -> services.avoirClientDocumentService.cloturerAvoir(avoir.getId(), requete));
    }

    @Test
    @DisplayName("Un avoir déjà soldé ne se clôture pas une seconde fois")
    void clotureDejaFaite() {
        Produit produit = produitEnStock("SIMVASTATINE", 1_000, 600, 0, 20);
        AvoirClient avoir = avoirOuvert(venteFermee(produit, 4, 1).getSalesLines().iterator().next());
        services.avoirClientDocumentService.cloturerAvoir(
            avoir.getId(),
            new CloturerAvoirRequest(ModeClotureAvoir.REMBOURSEMENT_CB, null, null)
        );
        viderLeCache();

        var requete = new CloturerAvoirRequest(ModeClotureAvoir.REMBOURSEMENT_CB, null, null);
        assertThrows(GenericError.class, () -> services.avoirClientDocumentService.cloturerAvoir(avoir.getId(), requete));
    }

    @Test
    @DisplayName("Le produit ne se remet pas tant que le stock reste négatif")
    void stockInsuffisant() {
        // La vente a sorti 4 unités absentes : stock à −4 ; une réception de 2 le porte à −2.
        Produit produit = produitEnStock("ROSUVASTATINE", 1_000, 600, 0, -2);
        AvoirClient avoir = avoirOuvert(venteFermee(produit, 5, 1).getSalesLines().iterator().next());

        var requete = new CloturerAvoirRequest(ModeClotureAvoir.RETOUR_PRODUIT, null, null);
        GenericError echec = assertThrows(
            GenericError.class,
            () -> services.avoirClientDocumentService.cloturerAvoir(avoir.getId(), requete)
        );

        assertTrue(echec.getMessage().contains("Stock insuffisant"), echec.getMessage());
        verify(services.avoirClientNotificationService, never()).notifierProduitsDisponibles(any());
    }

    /** Bug 2 du plan PLAN-VENTE-SUR-STOCK-ERRONE : la dette était comptée deux fois. */
    @Test
    @DisplayName("Une réception de la quantité exacte due suffit à remettre le produit")
    void receptionExacteDeLaQuantiteDue() {
        // Stock −3 après la vente, +3 reçus : stock 0, les 3 boîtes sont en rayon pour le client.
        Produit produit = produitEnStock("VALSARTAN", 1_000, 600, 0, 0);
        AvoirClient avoir = avoirOuvert(venteFermee(produit, 3, 0).getSalesLines().iterator().next());

        AvoirClientDocumentDTO solde = services.avoirClientDocumentService.cloturerAvoir(
            avoir.getId(),
            new CloturerAvoirRequest(ModeClotureAvoir.RETOUR_PRODUIT, "remis au client", null)
        );
        viderLeCache();

        assertEquals(AvoirClientStatut.CLOTURE, solde.statut());
        assertEquals(0, stockRayon(produit), "qty_stock a été débité à la vente, il ne bouge plus");
        verify(services.lotService).adjustLots(any(Produit.class), org.mockito.ArgumentMatchers.eq(-3));
        verify(services.lotStockLocationService).debitFefo(any(Produit.class), any(), org.mockito.ArgumentMatchers.eq(3));
    }

    /** Bug 5 du plan PLAN-VENTE-SUR-STOCK-ERRONE : la quantité due restait sortie du stock. */
    @Test
    @DisplayName("Un avoir remboursé sans produit recrédite la quantité due, même stock négatif")
    void remboursementRecrediteLeStock() {
        Produit produit = produitEnStock("IRBESARTAN", 1_000, 600, 0, -3);
        AvoirClient avoir = avoirOuvert(venteFermee(produit, 3, 0).getSalesLines().iterator().next());

        services.avoirClientDocumentService.cloturerAvoir(
            avoir.getId(),
            new CloturerAvoirRequest(ModeClotureAvoir.REMBOURSEMENT_ESPECES, "rupture durable", null)
        );
        viderLeCache();

        assertEquals(0, stockRayon(produit), "la dette annulée ne pèse plus sur le stock");
        verify(services.lotService, never()).adjustLots(any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @ParameterizedTest
    @EnumSource(value = ModeClotureAvoir.class, names = "RETOUR_PRODUIT", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Chaque mode de clôture sans remise du produit recrédite la quantité due")
    void chaqueModeSansProduitRecrediteLeStock(ModeClotureAvoir mode) {
        Produit produit = produitEnStock("TELMISARTAN-" + mode, 1_000, 600, 0, -2);
        AvoirClient avoir = avoirOuvert(venteFermee(produit, 2, 0).getSalesLines().iterator().next());

        services.avoirClientDocumentService.cloturerAvoir(avoir.getId(), new CloturerAvoirRequest(mode, null, null));
        viderLeCache();

        assertEquals(0, stockRayon(produit));
        assertEquals(0, stockVirtuelRayon(produit), "qty_virtual suit qty_stock");
    }

    @Test
    @DisplayName("Un solde en plusieurs tranches ne recrédite le stock qu'à la dernière")
    void recreditALaDerniereTranche() {
        Produit produit = produitEnStock("OLMESARTAN", 1_000, 600, 0, -3);
        AvoirClient avoir = avoirOuvert(venteFermee(produit, 3, 0).getSalesLines().iterator().next());

        services.avoirClientDocumentService.cloturerAvoir(
            avoir.getId(), new CloturerAvoirRequest(ModeClotureAvoir.BON_AVOIR, "acompte", 1_000));
        viderLeCache();
        assertEquals(-3, stockRayon(produit), "l'avoir reste ouvert : la dette tient toujours");

        services.avoirClientDocumentService.cloturerAvoir(
            avoir.getId(), new CloturerAvoirRequest(ModeClotureAvoir.BON_AVOIR, "solde", 2_000));
        viderLeCache();
        assertEquals(0, stockRayon(produit), "recrédité une seule fois, pour toute la quantité");
    }

    @Test
    @DisplayName("La réserve couvre la dette : le produit peut être remis sans toucher au stock")
    void reserveCouvreLaDette() {
        Produit produit = produitEnStock("NEBIVOLOL", 1_000, 600, 0, -2);
        stock(produit, reserve, 2, 0);
        em.flush();
        AvoirClient avoir = avoirOuvert(venteFermee(produit, 2, 0).getSalesLines().iterator().next());

        AvoirClientDocumentDTO solde = services.avoirClientDocumentService.cloturerAvoir(
            avoir.getId(), new CloturerAvoirRequest(ModeClotureAvoir.RETOUR_PRODUIT, null, null));
        viderLeCache();

        assertEquals(AvoirClientStatut.CLOTURE, solde.statut());
        assertEquals(-2, stockRayon(produit), "qty_stock a déjà été débité à la vente");
        verify(services.lotStockLocationService).debitFefo(any(Produit.class), eq(rayon), eq(2));
    }

    @Test
    @DisplayName("Remise partielle en unités : chaque remise sort ses lots et impute son prix, la dernière solde")
    void remisePartielleEnUnites() {
        Produit produit = produitEnStock("SITAGLIPTINE", 1_000, 600, 0, 20);
        AvoirClient avoir = avoirOuvert(venteFermee(produit, 5, 2).getSalesLines().iterator().next());

        AvoirClientDocumentDTO premiere = services.avoirClientDocumentService.cloturerAvoir(
            avoir.getId(), new CloturerAvoirRequest(ModeClotureAvoir.RETOUR_PRODUIT, null, null, 1));
        viderLeCache();

        assertEquals(AvoirClientStatut.OUVERT, premiere.statut());
        AvoirClient apresPremiere = em.find(AvoirClient.class, avoir.getId());
        assertEquals(1, apresPremiere.getQuantiteRemise());
        assertEquals(1_000, apresPremiere.getMontantUtilise(), "1 unité × 1 000");
        verify(services.lotService).adjustLots(any(Produit.class), eq(-1));

        AvoirClientDocumentDTO derniere = services.avoirClientDocumentService.cloturerAvoir(
            avoir.getId(), new CloturerAvoirRequest(ModeClotureAvoir.RETOUR_PRODUIT, null, null));
        viderLeCache();

        assertEquals(AvoirClientStatut.CLOTURE, derniere.statut());
        AvoirClient solde = em.find(AvoirClient.class, avoir.getId());
        assertEquals(3, solde.getQuantiteRemise());
        assertEquals(3_000, solde.getMontantUtilise());
        verify(services.lotService).adjustLots(any(Produit.class), eq(-2));
        assertEquals(20, stockRayon(produit), "rien n'est recrédité : toutes les unités ont été remises");
    }

    @Test
    @DisplayName("Solder en argent après une remise partielle ne recrédite que les unités jamais remises")
    void argentApresRemisePartielle() {
        Produit produit = produitEnStock("DAPAGLIFLOZINE", 1_000, 600, 0, -3);
        AvoirClient avoir = avoirOuvert(venteFermee(produit, 3, 0).getSalesLines().iterator().next());
        em.createQuery("update AvoirClient a set a.quantiteRemise = 1, a.montantUtilise = 1000 where a.id = :id")
            .setParameter("id", avoir.getId()).executeUpdate();
        viderLeCache();

        services.avoirClientDocumentService.cloturerAvoir(
            avoir.getId(), new CloturerAvoirRequest(ModeClotureAvoir.BON_AVOIR, null, null));
        viderLeCache();

        assertEquals(-1, stockRayon(produit), "2 unités recréditées sur les 3 dues, la troisième a été remise");
    }

    @Test
    @DisplayName("Une remise refusée laisse l'avoir, ses utilisations et le stock intacts")
    void remiseRefuseeSansEffet() {
        Produit produit = produitEnStock("CARVEDILOL", 1_000, 600, 0, -1);
        AvoirClient avoir = avoirOuvert(venteFermee(produit, 3, 0).getSalesLines().iterator().next());

        var requete = new CloturerAvoirRequest(ModeClotureAvoir.RETOUR_PRODUIT, null, null);
        assertThrows(GenericError.class, () -> services.avoirClientDocumentService.cloturerAvoir(avoir.getId(), requete));
        viderLeCache();

        AvoirClient relu = em.find(AvoirClient.class, avoir.getId());
        assertEquals(AvoirClientStatut.OUVERT, relu.getStatut());
        assertEquals(0, relu.getMontantUtilise());
        assertEquals(0, compter("SELECT count(*) FROM avoir_client_utilisation WHERE avoir_client_id = " + avoir.getId()));
        assertEquals(-1, stockRayon(produit));
        verify(services.lotService, never()).adjustLots(any(), anyInt());
    }

    /** Migration V2.1.3 : la contrainte logs_transaction_type_check accepte la nouvelle trace. */
    @Test
    @DisplayName("La trace du recrédit est acceptée par la table des logs")
    void traceDuRecreditPersistee() {
        Produit produit = produitEnStock("LOSARTAN", 1_000, 600, 0, -2);
        AvoirClient avoir = avoirOuvert(venteFermee(produit, 2, 0).getSalesLines().iterator().next());

        services.avoirClientDocumentService.cloturerAvoir(
            avoir.getId(), new CloturerAvoirRequest(ModeClotureAvoir.REMBOURSEMENT_ESPECES, null, null));

        ArgumentCaptor<String> commentaire = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> cle = ArgumentCaptor.forClass(String.class);
        verify(services.logsService).create(eq(TransactionType.AVOIR_SOLDE_SANS_PRODUIT), commentaire.capture(), cle.capture());
        em.persist(new Logs()
            .setTransactionType(TransactionType.AVOIR_SOLDE_SANS_PRODUIT)
            .setComments(commentaire.getValue())
            .setIndentityKey(cle.getValue())
            .setUser(caissier));
        em.flush();

        assertEquals(1, compter("SELECT count(*) FROM logs WHERE transaction_type = 'AVOIR_SOLDE_SANS_PRODUIT' AND indentity_key = '" + avoir.getId() + "'"));
    }

    @Test
    @DisplayName("La clôture en retour produit ne prévient pas le client : il est au comptoir")
    void aucuneNotificationALaCloture() {
        Produit produit = produitEnStock("PANTOPRAZOLE", 1_000, 600, 0, 50);
        AvoirClient avoir = avoirOuvert(venteFermee(produit, 5, 2).getSalesLines().iterator().next());

        services.avoirClientDocumentService.cloturerAvoir(
            avoir.getId(),
            new CloturerAvoirRequest(ModeClotureAvoir.RETOUR_PRODUIT, "produits arrivés", null)
        );

        verify(services.avoirClientNotificationService, never()).notifierProduitsDisponibles(any(AvoirClient.class));
    }

    @Test
    @DisplayName("La recherche filtre par statut et ne remonte pas les avoirs annulés")
    void recherchePaginee() {
        Produit produit = produitEnStock("OMEPRAZOLE", 1_000, 600, 0, 50);
        AvoirClient ouvert = avoirOuvert(venteFermee(produit, 5, 2).getSalesLines().iterator().next());
        AvoirClient annule = avoirOuvert(venteFermee(produit, 4, 1).getSalesLines().iterator().next());
        services.avoirClientDocumentService.cancelAvoirsFromSale(annule.getSalesLine().getId().getId());
        viderLeCache();

        var ouverts = services.avoirClientDocumentService.findAll(
            null,
            LocalDate.now(),
            LocalDate.now(),
            AvoirClientStatut.OUVERT,
            PageRequest.of(0, 20)
        );
        var sansFiltre = services.avoirClientDocumentService.findAll(null, null, null, null, PageRequest.of(0, 20));

        assertEquals(1, ouverts.getTotalElements());
        assertEquals(ouvert.getReference(), ouverts.getContent().getFirst().reference());
        assertTrue(
            sansFiltre.getContent().stream().noneMatch(a -> a.statut() == AvoirClientStatut.ANNULE),
            "sans filtre, la recherche ne montre que les avoirs ouverts et clôturés"
        );
    }

    @Test
    @DisplayName("Les avoirs d'un client remontent avec le code CIP de son produit")
    void avoirsDUnClient() {
        Produit produit = produitEnStock("ESOMEPRAZOLE", 1_000, 600, 0, 50);
        FournisseurProduit reference = fournisseurProduit(produit, "CIP-77001");
        UninsuredCustomer client = client("YAO", "Koffi", "CLI-AV-3");
        SalesLine ligne = venteFermee(produit, 5, 2).getSalesLines().iterator().next();
        services.avoirClientDocumentService.createAvoirsFromSale(ligne, client);
        viderLeCache();

        List<AvoirClientDocumentDTO> avoirs = services.avoirClientDocumentService.findAllByCustomer(client.getId());

        assertEquals(1, avoirs.size());
        assertEquals(reference.getCodeCip(), avoirs.getFirst().codeCip());
        assertEquals("Koffi YAO", avoirs.getFirst().customerName());
        assertEquals(3_000, avoirs.getFirst().montantRestant());
    }

    @Test
    @DisplayName("Une commande de réassort s'attache aux avoirs ouverts portant ses produits")
    void rattachementALaCommande() {
        Produit attendu = produitEnStock("LEVETIRACETAM", 5_000, 3_000, 0, 50);
        Produit autre = produitEnStock("GABAPENTINE", 4_000, 2_400, 0, 50);
        AvoirClient avoirAttendu = avoirOuvert(venteFermee(attendu, 5, 2).getSalesLines().iterator().next());
        AvoirClient avoirAutre = avoirOuvert(venteFermee(autre, 3, 1).getSalesLines().iterator().next());
        Commande commande = commandeDe(attendu);
        viderLeCache();

        services.avoirClientDocumentService.linkCommandeToAvoirs(em.find(Commande.class, commande.getId()));
        viderLeCache();

        assertNotNull(em.find(AvoirClient.class, avoirAttendu.getId()).getCommande(), "le produit commandé rattache son avoir");
        assertNull(em.find(AvoirClient.class, avoirAutre.getId()).getCommande(), "les autres avoirs restent en attente");
    }

    @Test
    @DisplayName("À la réception, le client dont le produit arrive est prévenu, une fois la réception validée")
    void notificationALaReception() {
        Produit attendu = produitEnStock("ZOLPIDEM", 5_000, 3_000, 0, 50);
        Produit autre = produitEnStock("ZOPICLONE", 4_000, 2_400, 0, 50);
        AvoirClient avoirAttendu = avoirOuvert(venteFermee(attendu, 5, 2).getSalesLines().iterator().next());
        avoirOuvert(venteFermee(autre, 3, 1).getSalesLines().iterator().next());
        Commande commande = commandeDe(attendu);
        viderLeCache();

        services.avoirClientDocumentService.linkCommandeToAvoirs(em.find(Commande.class, commande.getId()));

        // La transaction du test ne valide jamais : on rejoue à la main ce que le commit déclencherait.
        verify(services.avoirClientNotificationService, never()).notifierProduitsDisponibles(any(AvoirClient.class));
        org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations()
            .forEach(org.springframework.transaction.support.TransactionSynchronization::afterCommit);

        ArgumentCaptor<AvoirClient> prevenus = ArgumentCaptor.forClass(AvoirClient.class);
        verify(services.avoirClientNotificationService).notifierProduitsDisponibles(prevenus.capture());
        assertEquals(avoirAttendu.getId(), prevenus.getValue().getId(), "seul l'avoir du produit reçu est annoncé");
    }

    // ===== outils =====

    private AvoirClient avoirOuvert(SalesLine ligne) {
        services.avoirClientDocumentService.createAvoirsFromSale(ligne, client("DIOP", "Mor", "CLI-" + ligne.getId().getId()));
        viderLeCache();
        return avoirDeLaLigne(ligne);
    }

    private long stockRayon(Produit produit) {
        return compter("SELECT qty_stock FROM stock_produit WHERE produit_id = " + produit.getId() + " AND storage_id = " + rayon.getId());
    }

    private long stockVirtuelRayon(Produit produit) {
        return compter("SELECT qty_virtual FROM stock_produit WHERE produit_id = " + produit.getId() + " AND storage_id = " + rayon.getId());
    }

    private AvoirClient avoirDeLaLigne(SalesLine ligne) {
        return services.avoirClientRepository.findBySalesLineId(ligne.getId().getId()).orElseThrow();
    }

    private Commande commandeDe(Produit produit) {
        FournisseurProduit reference = fournisseurProduit(produit, "CIP-CMD-" + produit.getId());

        int numero = services.saleIdGeneratorService.getNextIdAsInt();
        Commande commande = new Commande();
        commande.setId(numero);
        commande.setOrderDate(LocalDate.now());
        commande.setOrderReference("CMD" + numero);
        commande.setReceiptReference("BL" + numero);
        commande.setGrossAmount(produit.getCostAmount());
        commande.setCreatedAt(LocalDateTime.now());
        commande.setUpdatedAt(LocalDateTime.now());
        commande.setOrderStatus(OrderStatut.REQUESTED);
        commande.setPaimentStatut(PaimentStatut.UNPAID);
        commande.setType(TypeDeliveryReceipt.ORDER);
        commande.setUser(caissier);
        commande.setFournisseur(reference.getFournisseur());
        em.persist(commande);

        OrderLine ligne = new OrderLine();
        ligne.setId(services.saleIdGeneratorService.getNextIdAsInt());
        ligne.setOrderDate(commande.getOrderDate());
        ligne.setCommande(commande);
        ligne.setFournisseurProduit(reference);
        ligne.setInitStock(0);
        ligne.setQuantityRequested(10);
        ligne.setQuantityReceived(0);
        ligne.setOrderAmount(produit.getRegularUnitPrice());
        ligne.setGrossAmount(produit.getCostAmount());
        ligne.setOrderUnitPrice(produit.getRegularUnitPrice());
        ligne.setOrderCostAmount(produit.getCostAmount());
        ligne.setCreatedAt(LocalDateTime.now());
        ligne.setUpdatedAt(LocalDateTime.now());
        em.persist(ligne);
        commande.getOrderLines().add(ligne);
        em.flush();
        return commande;
    }

    private CashSale venteAvecClient(Produit produit, int demande, int servi, UninsuredCustomer client) {
        CashSale vente = venteFermee(produit, demande, servi);
        vente.setCustomer(client);
        em.flush();
        return vente;
    }
}
