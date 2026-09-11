package com.kobe.warehouse.service.stock.integration;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.Commande;
import com.kobe.warehouse.domain.FamilleProduit;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.LotStockLocation;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.OrderLine;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.Tva;
import com.kobe.warehouse.domain.enumeration.OrderStatut;
import com.kobe.warehouse.domain.enumeration.PutawayMode;
import com.kobe.warehouse.domain.enumeration.StatutLot;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.TypeDeliveryReceipt;
import com.kobe.warehouse.domain.enumeration.TypeProduit;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Socle des tests d'intégration des services de stock.
 *
 * <p>Chaque test s'exécute dans une transaction annulée à la fin : la base revient d'elle-même à
 * l'état laissé par Flyway, et l'ordre des tests cesse d'être un paramètre du résultat.
 *
 * <p>Le jeu d'essai tourne autour de trois objets : un produit, son stock par emplacement
 * ({@code stock_produit}) et ses lots avec leur répartition ({@code lot_stock_location}). Les
 * emplacements 1 (rayon) et 3 (réserve) viennent des migrations, comme le magasin, l'utilisateur et
 * les taux de TVA.
 */
@Testcontainers(disabledWithoutDocker = true)
abstract class AbstractStockIntegrationTest {

    protected static final int MAGASIN_ID = 1;
    protected static final int STORAGE_RAYON_ID = 1;
    protected static final int STORAGE_RESERVE_ID = 3;

    protected static EntityManager em;

    /** Suffixe unique par fixture : codes CIP, numéros de lot et libellés sont uniques en base. */
    private static final AtomicInteger COMPTEUR = new AtomicInteger();

    protected StockServicesUnderTest services;
    protected AppUser utilisateur;
    protected Magasin magasin;
    protected Storage rayon;
    protected Storage reserve;

    private TransactionStatus transaction;

    @BeforeAll
    static void demarrerLaBase() {
        em = SharedEntityManagerCreator.createSharedEntityManager(IntegrationPostgresDatabase.bean(EntityManagerFactory.class));
    }

    @BeforeEach
    void ouvrirLaTransaction() {
        transaction = IntegrationPostgresDatabase.transactionManager().getTransaction(new DefaultTransactionDefinition());
        services = new StockServicesUnderTest(em);

        utilisateur = em.find(AppUser.class, 1);
        magasin = em.find(Magasin.class, MAGASIN_ID);
        rayon = em.find(Storage.class, STORAGE_RAYON_ID);
        reserve = em.find(Storage.class, STORAGE_RESERVE_ID);

        lenient().when(services.storageService.getUser()).thenReturn(utilisateur);
        lenient().when(services.userService.getUser()).thenReturn(utilisateur);
        lenient().when(services.storageService.getConnectedUserMagasin()).thenReturn(magasin);
        lenient().when(services.storageService.getDefaultConnectedUserMainStorage()).thenReturn(rayon);
        lenient().when(services.storageService.getDefaultConnectedUserReserveStorage()).thenReturn(reserve);
        lenient().when(services.storageService.getOne(STORAGE_RAYON_ID)).thenReturn(rayon);
        lenient().when(services.storageService.getOne(STORAGE_RESERVE_ID)).thenReturn(reserve);
        lenient().when(services.appConfigurationService.getNombreJourAlertPeremption()).thenReturn(90);
        // Réglages de réception par défaut : pas de gestion de lot, aucun seuil de péremption,
        // aucun rangement automatique. Les tests qui éprouvent ces règles les rehaussent eux-mêmes.
        lenient().when(services.appConfigurationService.useLot()).thenReturn(Optional.of(false));
        lenient().when(services.appConfigurationService.getReceptionMinExpiryDays()).thenReturn(0L);
        lenient().when(services.appConfigurationService.getPutawayMode()).thenReturn(PutawayMode.ALL_RAYON);
        lenient().when(services.appConfigurationService.getSeuilVariationPrix()).thenReturn(20);
    }

    @AfterEach
    void annulerLaTransaction() {
        if (transaction != null && !transaction.isCompleted()) {
            IntegrationPostgresDatabase.transactionManager().rollback(transaction);
        }
    }

    // ===== fabriques du jeu d'essai =====

    protected static String unique(String prefixe) {
        return prefixe + "-" + COMPTEUR.incrementAndGet();
    }

    protected Tva tva(int taux) {
        return em.createQuery("SELECT t FROM Tva t WHERE t.taux = :taux", Tva.class).setParameter("taux", taux).getSingleResult();
    }

    protected Produit produit(String libelle) {
        return produit(libelle, 10_000, 6_000);
    }

    protected Produit produit(String libelle, int prixVente, int coutAchat) {
        Produit produit = new Produit();
        produit.setLibelle(libelle);
        produit.setTypeProduit(TypeProduit.DETAIL);
        produit.setStatus(Status.ENABLE);
        produit.setCostAmount(coutAchat);
        produit.setRegularUnitPrice(prixVente);
        produit.setNetUnitPrice(prixVente);
        produit.setItemCostAmount(coutAchat);
        produit.setItemRegularUnitPrice(prixVente);
        produit.setItemQty(1);
        produit.setPrixMnp(0);
        produit.setDeconditionnable(false);
        produit.setCreatedAt(LocalDateTime.now());
        produit.setUpdatedAt(LocalDateTime.now());
        produit.setTva(tva(0));
        produit.setFamille(
            em.createQuery("SELECT f FROM FamilleProduit f ORDER BY f.id", FamilleProduit.class).setMaxResults(1).getSingleResult()
        );
        em.persist(produit);
        em.flush();
        return produit;
    }

    /** Un produit référencé chez un fournisseur : c'est ce couple qu'une ligne de commande vise. */
    protected FournisseurProduit fournisseurProduit(Produit produit) {
        Fournisseur fournisseur = new Fournisseur();
        String code = unique("FRS");
        fournisseur.setLibelle("FOURNISSEUR " + code);
        fournisseur.setCode(code);
        em.persist(fournisseur);

        FournisseurProduit fournisseurProduit = new FournisseurProduit();
        fournisseurProduit.setProduit(produit);
        fournisseurProduit.setFournisseur(fournisseur);
        fournisseurProduit.setCodeCip(unique("CIP"));
        fournisseurProduit.setPrixAchat(produit.getCostAmount());
        fournisseurProduit.setPrixUni(produit.getRegularUnitPrice());
        em.persist(fournisseurProduit);
        produit.setFournisseurProduitPrincipal(fournisseurProduit);
        em.flush();
        return fournisseurProduit;
    }

    protected StockProduit stock(Produit produit, Storage storage, int quantite) {
        StockProduit stockProduit = new StockProduit();
        stockProduit.setProduit(produit);
        stockProduit.setStorage(storage);
        stockProduit.setQtyStock(quantite);
        stockProduit.setQtyVirtual(quantite);
        stockProduit.setQtyUG(0);
        stockProduit.setCreatedAt(LocalDateTime.now());
        stockProduit.setUpdatedAt(LocalDateTime.now());
        em.persist(stockProduit);
        produit.getStockProduits().add(stockProduit);
        em.flush();
        return stockProduit;
    }

    protected Produit produitEnStock(String libelle, int quantiteRayon) {
        Produit produit = produit(libelle);
        stock(produit, rayon, quantiteRayon);
        return produit;
    }

    /**
     * Un lot du produit. {@code peremption} est ce qui décide de l'ordre FEFO ; {@code null} range
     * le lot en dernier.
     */
    protected Lot lot(Produit produit, String numLot, LocalDate peremption, int quantite) {
        Lot lot = new Lot();
        lot.setNumLot(numLot);
        lot.setProduit(produit);
        lot.setQuantity(quantite);
        lot.setCurrentQuantity(quantite);
        lot.setFreeQty(0);
        lot.setExpiryDate(peremption);
        lot.setCreatedDate(LocalDateTime.now());
        lot.setUpdated(LocalDateTime.now());
        lot.setPrixAchat(produit.getCostAmount());
        lot.setPrixUnit(produit.getRegularUnitPrice());
        lot.setStatut(StatutLot.AVAILABLE);
        em.persist(lot);
        em.flush();
        return lot;
    }

    /** Le lot et sa présence sur un emplacement — les deux faces de la même quantité. */
    protected Lot lotSurEmplacement(Produit produit, String numLot, LocalDate peremption, Storage storage, int quantite) {
        Lot lot = lot(produit, numLot, peremption, quantite);
        emplacement(lot, storage, quantite);
        return lot;
    }

    protected LotStockLocation emplacement(Lot lot, Storage storage, int quantite) {
        LotStockLocation emplacement = new LotStockLocation(lot, storage, quantite);
        em.persist(emplacement);
        em.flush();
        return emplacement;
    }

    protected int quantiteSurEmplacement(Lot lot, Storage storage) {
        return services.lotStockLocationRepository
            .findByLotIdAndStorageId(lot.getId(), storage.getId())
            .map(LotStockLocation::getQty)
            .orElse(-1);
    }

    // ===== commandes et réceptions =====

    protected Commande commande(Fournisseur fournisseur, OrderStatut statut) {
        Commande commande = new Commande();
        commande.setId(services.commandeIdGeneratorService.getNextIdAsInt());
        commande.setOrderDate(LocalDate.now());
        commande.setOrderReference(unique("CMD"));
        commande.setCreatedAt(LocalDateTime.now());
        commande.setUpdatedAt(LocalDateTime.now());
        commande.setGrossAmount(0);
        commande.setOrderAmount(0);
        commande.setOrderStatus(statut);
        commande.setReceiptDate(LocalDate.now());
        commande.setReceiptReference(unique("BL"));
        commande.setTaxAmount(0);
        commande.setType(TypeDeliveryReceipt.ORDER);
        commande.setUser(utilisateur);
        commande.setFournisseur(fournisseur);
        em.persist(commande);
        em.flush();
        return commande;
    }

    protected OrderLine ligneDeCommande(Commande commande, FournisseurProduit fournisseurProduit, int demande, int recu) {
        OrderLine ligne = new OrderLine();
        ligne.setId(services.orderLineIdGeneratorService.getNextIdAsInt());
        ligne.setOrderDate(commande.getOrderDate());
        ligne.setCommande(commande);
        ligne.setFournisseurProduit(fournisseurProduit);
        ligne.setQuantityRequested(demande);
        ligne.setQuantityReceived(recu);
        ligne.setInitStock(0);
        ligne.setFreeQty(0);
        ligne.setOrderUnitPrice(fournisseurProduit.getPrixUni());
        ligne.setOrderCostAmount(fournisseurProduit.getPrixAchat());
        ligne.setCreatedAt(LocalDateTime.now());
        ligne.setUpdatedAt(LocalDateTime.now());
        em.persist(ligne);
        commande.getOrderLines().add(ligne);
        em.flush();
        return ligne;
    }

    // ===== outils d'assertion =====

    /** Vide le contexte de persistance : la relecture qui suit vient bien de Postgres. */
    protected void viderLeCache() {
        em.flush();
        em.clear();
    }

    protected long compter(String sql) {
        return ((Number) em.createNativeQuery(sql).getSingleResult()).longValue();
    }

    /** Court-circuite l'inférence de Mockito quand un test n'a pas besoin d'un storage précis. */
    protected void nImporteQuelEmplacementEstLeRayon() {
        lenient().when(services.storageService.getOne(anyInt())).thenReturn(rayon);
    }
}
