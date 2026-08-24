package com.kobe.warehouse.service.sale.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.kobe.warehouse.domain.AvoirClient;
import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.UninsuredCustomer;
import com.kobe.warehouse.domain.enumeration.AvoirClientStatut;
import com.kobe.warehouse.domain.enumeration.ModeClotureAvoir;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.domain.enumeration.PaimentStatut;
import com.kobe.warehouse.domain.enumeration.TypeDeliveryReceipt;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.sale.dto.AvoirClientDocumentDTO;
import com.kobe.warehouse.service.sale.dto.CloturerAvoirRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
    @DisplayName("Un avoir ne se clôture pas si le stock ne couvre pas la quantité due")
    void stockInsuffisant() {
        Produit produit = produitEnStock("ROSUVASTATINE", 1_000, 600, 0, 1);
        AvoirClient avoir = avoirOuvert(venteFermee(produit, 5, 1).getSalesLines().iterator().next());

        var requete = new CloturerAvoirRequest(ModeClotureAvoir.RETOUR_PRODUIT, null, null);
        GenericError echec = assertThrows(
            GenericError.class,
            () -> services.avoirClientDocumentService.cloturerAvoir(avoir.getId(), requete)
        );

        assertTrue(echec.getMessage().contains("Stock insuffisant"), echec.getMessage());
        verify(services.avoirClientNotificationService, never()).notifierProduitsDisponibles(any());
    }

    @Test
    @DisplayName("La clôture en retour produit prévient le client, une fois le solde épuisé")
    void notificationEnRetourProduit() {
        Produit produit = produitEnStock("PANTOPRAZOLE", 1_000, 600, 0, 50);
        AvoirClient avoir = avoirOuvert(venteFermee(produit, 5, 2).getSalesLines().iterator().next());

        services.avoirClientDocumentService.cloturerAvoir(
            avoir.getId(),
            new CloturerAvoirRequest(ModeClotureAvoir.RETOUR_PRODUIT, "produits arrivés", null)
        );

        verify(services.avoirClientNotificationService).notifierProduitsDisponibles(any(AvoirClient.class));
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

    // ===== outils =====

    private AvoirClient avoirOuvert(SalesLine ligne) {
        services.avoirClientDocumentService.createAvoirsFromSale(ligne, client("DIOP", "Mor", "CLI-" + ligne.getId().getId()));
        viderLeCache();
        return avoirDeLaLigne(ligne);
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
