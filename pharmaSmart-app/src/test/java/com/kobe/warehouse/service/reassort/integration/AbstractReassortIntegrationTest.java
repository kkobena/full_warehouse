package com.kobe.warehouse.service.reassort.integration;

import static org.mockito.Mockito.lenient;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.FamilleProduit;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.LigneReassort;
import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.LotStockLocation;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.RepartitionStockProduit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.SuggestionReassort;
import com.kobe.warehouse.domain.enumeration.StatutLot;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.StatutReassort;
import com.kobe.warehouse.domain.enumeration.StorageType;
import com.kobe.warehouse.domain.enumeration.TypeProduit;
import com.kobe.warehouse.domain.enumeration.TypeReassort;
import com.kobe.warehouse.domain.Tva;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Socle des tests d'intégration du réassort.
 *
 * <p>Chaque test s'exécute dans une transaction annulée à la fin : la base revient d'elle-même à
 * l'état laissé par Flyway, et l'ordre des tests cesse d'être un paramètre du résultat.
 *
 * <p>Le jeu d'essai tourne autour d'un produit présent dans deux emplacements et de ses lots
 * répartis entre eux — c'est exactement ce qu'un transfert de stock modifie du même geste, et
 * c'est là que les quatre écritures (les deux {@code stock_produit}, la trace de répartition, le
 * journal de mouvement et {@code lot_stock_location}) doivent se retrouver d'accord. Les
 * emplacements 1 (rayon) et 3 (réserve) viennent des migrations, comme le magasin, l'utilisateur
 * et les taux de TVA.
 */
@Testcontainers(disabledWithoutDocker = true)
abstract class AbstractReassortIntegrationTest {

    protected static final int MAGASIN_ID = 1;
    protected static final int STORAGE_RAYON_ID = 1;
    protected static final int STORAGE_RESERVE_ID = 3;

    protected static EntityManager em;

    /** Suffixe unique par fixture : codes CIP, numéros de lot et libellés sont uniques en base. */
    private static final AtomicInteger COMPTEUR = new AtomicInteger();

    protected ReassortServicesUnderTest services;
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
        services = new ReassortServicesUnderTest(em);

        utilisateur = em.find(AppUser.class, 1);
        magasin = em.find(Magasin.class, MAGASIN_ID);
        rayon = em.find(Storage.class, STORAGE_RAYON_ID);
        reserve = em.find(Storage.class, STORAGE_RESERVE_ID);

        lenient().when(services.storageService.getUser()).thenReturn(utilisateur);
        lenient().when(services.storageService.getConnectedUserMagasin()).thenReturn(magasin);
        lenient().when(services.storageService.getDefaultConnectedUserMainStorage()).thenReturn(rayon);
        lenient().when(services.storageService.getDefaultConnectedUserReserveStorage()).thenReturn(reserve);
        lenient().when(services.storageService.getOne(STORAGE_RAYON_ID)).thenReturn(rayon);
        lenient().when(services.storageService.getOne(STORAGE_RESERVE_ID)).thenReturn(reserve);
        lenient()
            .when(services.storageService.getStorageByMagasinIdAndType(MAGASIN_ID, StorageType.PRINCIPAL))
            .thenReturn(rayon);
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

    /**
     * Un produit actif et son fournisseur principal. Le couple n'est pas décoratif : le journal de
     * mouvement y lit le prix d'achat et le prix de vente, et l'écran de suivi y lit le code CIP.
     */
    protected Produit produit(String libelle) {
        Produit produit = new Produit();
        produit.setLibelle(libelle);
        produit.setTypeProduit(TypeProduit.DETAIL);
        produit.setStatus(Status.ENABLE);
        produit.setCostAmount(6_000);
        produit.setRegularUnitPrice(10_000);
        produit.setNetUnitPrice(10_000);
        produit.setItemCostAmount(6_000);
        produit.setItemRegularUnitPrice(10_000);
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

        Fournisseur fournisseur = new Fournisseur();
        String code = unique("FRS");
        fournisseur.setLibelle("FOURNISSEUR " + code);
        fournisseur.setCode(code);
        em.persist(fournisseur);

        FournisseurProduit fournisseurProduit = new FournisseurProduit();
        fournisseurProduit.setProduit(produit);
        fournisseurProduit.setFournisseur(fournisseur);
        fournisseurProduit.setCodeCip(unique("CIP"));
        fournisseurProduit.setPrixAchat(6_000);
        fournisseurProduit.setPrixUni(10_000);
        em.persist(fournisseurProduit);
        produit.setFournisseurProduitPrincipal(fournisseurProduit);
        produit.getFournisseurProduits().add(fournisseurProduit);
        em.flush();
        return produit;
    }

    protected StockProduit stock(Produit produit, Storage storage, int quantite) {
        return stock(produit, storage, quantite, 0);
    }

    protected StockProduit stock(Produit produit, Storage storage, int quantite, int unitesGratuites) {
        StockProduit stockProduit = new StockProduit();
        stockProduit.setProduit(produit);
        stockProduit.setStorage(storage);
        stockProduit.setQtyStock(quantite);
        stockProduit.setQtyVirtual(quantite + unitesGratuites);
        stockProduit.setQtyUG(unitesGratuites);
        stockProduit.setCreatedAt(LocalDateTime.now());
        stockProduit.setUpdatedAt(LocalDateTime.now());
        em.persist(stockProduit);
        produit.getStockProduits().add(stockProduit);
        em.flush();
        return stockProduit;
    }

    /**
     * Un lot et sa présence sur un emplacement — les deux faces de la même quantité.
     * {@code peremption} décide de l'ordre FEFO du transfert.
     */
    protected Lot lotSurEmplacement(Produit produit, String numLot, LocalDate peremption, Storage storage, int quantite) {
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
        em.persist(new LotStockLocation(lot, storage, quantite));
        em.flush();
        return lot;
    }

    // ===== suggestions =====

    /** Une suggestion ouverte, telle que la détection la laisse avant validation. */
    protected SuggestionReassort suggestionOuverte(TypeReassort typeReassort) {
        SuggestionReassort suggestion = new SuggestionReassort();
        suggestion.setReference(unique("RE"));
        suggestion.setMagasin(magasin);
        suggestion.setLastUserEdit(utilisateur);
        suggestion.setTypeReassort(typeReassort);
        suggestion.setStatut(StatutReassort.OPEN);
        suggestion.setCreatedAt(LocalDateTime.now());
        suggestion.setUpdatedAt(LocalDateTime.now());
        em.persist(suggestion);
        em.flush();
        return suggestion;
    }

    protected LigneReassort ligne(SuggestionReassort suggestion, StockProduit destination, int quantite) {
        LigneReassort ligne = new LigneReassort();
        ligne.setReassort(suggestion);
        ligne.setStockProduit(destination);
        ligne.setQuantity(quantite);
        ligne.setUpdatedAt(LocalDateTime.now());
        em.persist(ligne);
        suggestion.getLigneReassorts().add(ligne);
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

    protected int stockEnBase(Produit produit, Storage storage) {
        return (int) compter(
            "SELECT qty_stock FROM stock_produit WHERE produit_id = %d AND storage_id = %d".formatted(produit.getId(), storage.getId())
        );
    }

    protected int stockVirtuelEnBase(Produit produit, Storage storage) {
        return (int) compter(
            "SELECT qty_virtual FROM stock_produit WHERE produit_id = %d AND storage_id = %d".formatted(produit.getId(), storage.getId())
        );
    }

    protected int quantiteSurEmplacement(Lot lot, Storage storage) {
        return services.lotStockLocationRepository
            .findByLotIdAndStorageId(lot.getId(), storage.getId())
            .map(LotStockLocation::getQty)
            .orElse(0);
    }

    /** La trace de répartition du produit — il n'y en a qu'une par test. */
    protected RepartitionStockProduit repartitionPour(Produit produit) {
        return em
            .createQuery(
                "SELECT r FROM RepartitionStockProduit r WHERE r.stockProduitDestination.produit.id = :produitId",
                RepartitionStockProduit.class
            )
            .setParameter("produitId", produit.getId())
            .getSingleResult();
    }

    protected long mouvementsJournalises(Produit produit) {
        return compter("SELECT COUNT(*) FROM inventory_transaction WHERE produit_id = " + produit.getId());
    }
}
