package com.kobe.warehouse.service.inventaire.integration;

import static org.mockito.Mockito.lenient;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.FamilleProduit;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.InventoryLot;
import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Rayon;
import com.kobe.warehouse.domain.RayonProduit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.StoreInventory;
import com.kobe.warehouse.domain.StoreInventoryLine;
import com.kobe.warehouse.domain.Tva;
import com.kobe.warehouse.domain.enumeration.InventoryCategory;
import com.kobe.warehouse.domain.enumeration.StatutLot;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.TypeProduit;
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
 * Socle des tests d'intégration des services d'inventaire.
 *
 * <p>Chaque test s'exécute dans une transaction annulée à la fin : la base revient d'elle-même à
 * l'état laissé par Flyway, et l'ordre des tests cesse d'être un paramètre du résultat. C'est
 * indispensable ici : un inventaire MAGASIN prend <i>tous</i> les produits actifs de la base, donc
 * ceux qu'un test voisin aurait laissés derrière lui.
 *
 * <p>Le jeu d'essai tourne autour du produit, de son stock par emplacement, de ses lots et du
 * rayon où il est rangé — les quatre sources dont le périmètre d'un inventaire est tiré. Les
 * emplacements 1 (rayon) et 3 (réserve) viennent des migrations, comme le magasin, l'utilisateur
 * et les taux de TVA.
 */
@Testcontainers(disabledWithoutDocker = true)
abstract class AbstractInventaireIntegrationTest {

    protected static final int MAGASIN_ID = 1;
    protected static final int STORAGE_RAYON_ID = 1;
    protected static final int STORAGE_RESERVE_ID = 3;

    protected static EntityManager em;

    /** Suffixe unique par fixture : codes CIP, numéros de lot et libellés sont uniques en base. */
    private static final AtomicInteger COMPTEUR = new AtomicInteger();

    protected InventaireServicesUnderTest services;
    protected AppUser utilisateur;
    protected Magasin magasin;
    protected Storage rayon;
    protected Storage reserve;

    private TransactionStatus transaction;

    @BeforeAll
    static void demarrerLaBase() {
        em = SharedEntityManagerCreator.createSharedEntityManager(
            IntegrationPostgresDatabase.bean(EntityManagerFactory.class));
    }

    @BeforeEach
    void ouvrirLaTransaction() {
        transaction = IntegrationPostgresDatabase.transactionManager().getTransaction(new DefaultTransactionDefinition());
        services = new InventaireServicesUnderTest(em);

        utilisateur = em.find(AppUser.class, 1);
        magasin = em.find(Magasin.class, MAGASIN_ID);
        rayon = em.find(Storage.class, STORAGE_RAYON_ID);
        reserve = em.find(Storage.class, STORAGE_RESERVE_ID);

        lenient().when(services.userService.getUser()).thenReturn(utilisateur);
        lenient().when(services.storageService.getUser()).thenReturn(utilisateur);
        lenient().when(services.storageService.getConnectedUserMagasin()).thenReturn(magasin);
        lenient().when(services.storageService.getDefaultConnectedUserMainStorage()).thenReturn(rayon);
        lenient().when(services.storageService.getDefaultConnectedUserReserveStorage()).thenReturn(reserve);
        lenient().when(services.storageService.getOne(STORAGE_RAYON_ID)).thenReturn(rayon);
        lenient().when(services.storageService.getOne(STORAGE_RESERVE_ID)).thenReturn(reserve);
        // Sans gestion de lot par défaut : les tests qui l'éprouvent la rehaussent eux-mêmes.
        lenient().when(services.appConfigurationService.useGestionLotInventaire()).thenReturn(false);
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

    protected FamilleProduit premiereFamille() {
        return em.createQuery("SELECT f FROM FamilleProduit f ORDER BY f.id", FamilleProduit.class).setMaxResults(1).getSingleResult();
    }

    /**
     * Un produit actif référencé chez un fournisseur. Le couple produit / fournisseur principal
     * n'est pas décoratif : c'est lui qui porte le code CIP de l'import CSV et les prix sur
     * lesquels la valorisation de l'inventaire est calculée.
     */
    protected Produit produit(String libelle, int prixVente, int coutAchat) {
        return produit(libelle, prixVente, coutAchat, premiereFamille());
    }

    protected Produit produit(String libelle, int prixVente, int coutAchat, FamilleProduit famille) {
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
        produit.setFamille(famille);
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
        fournisseurProduit.setPrixAchat(coutAchat);
        fournisseurProduit.setPrixUni(prixVente);
        em.persist(fournisseurProduit);
        produit.setFournisseurProduitPrincipal(fournisseurProduit);
        produit.getFournisseurProduits().add(fournisseurProduit);
        em.flush();
        return produit;
    }

    protected Produit produit(String libelle) {
        return produit(libelle, 10_000, 6_000);
    }

    protected String codeCip(Produit produit) {
        return produit.getFournisseurProduitPrincipal().getCodeCip();
    }

    protected Produit produitDesactive(String libelle) {
        Produit produit = produit(libelle);
        produit.setStatus(Status.DELETED);
        em.flush();
        return produit;
    }

    protected StockProduit stock(Produit produit, Storage storage, int quantite) {
        return stock(produit, storage, quantite, null);
    }

    protected StockProduit stock(Produit produit, Storage storage, int quantite, Integer seuilMini) {
        StockProduit stockProduit = new StockProduit();
        stockProduit.setProduit(produit);
        stockProduit.setStorage(storage);
        stockProduit.setQtyStock(quantite);
        stockProduit.setQtyVirtual(quantite);
        stockProduit.setQtyUG(0);
        stockProduit.setSeuilMini(seuilMini);
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

    /** Un rayon de l'emplacement donné, et le rangement du produit dedans. */
    protected Rayon rayon(Storage storage, String libelle) {
        Rayon rayon = new Rayon();
        rayon.setCode(unique("RAY"));
        rayon.setLibelle(libelle);
        rayon.setStorage(storage);
        em.persist(rayon);
        em.flush();
        return rayon;
    }

    protected RayonProduit ranger(Produit produit, Rayon rayon) {
        RayonProduit rayonProduit = new RayonProduit().setProduit(produit).setRayon(rayon);
        em.persist(rayonProduit);
        em.flush();
        return rayonProduit;
    }

    // ===== inventaires =====

    protected StoreInventory inventaire(InventoryCategory categorie) {
        return inventaire(categorie, rayon);
    }

    protected StoreInventory inventaire(InventoryCategory categorie, Storage storage) {
        StoreInventory inventaire = new StoreInventory();
        inventaire.setCreatedAt(LocalDateTime.now());
        inventaire.setUpdatedAt(LocalDateTime.now());
        inventaire.setInventoryValueCostBegin(0L);
        inventaire.setInventoryAmountBegin(0L);
        inventaire.setInventoryValueCostAfter(0L);
        inventaire.setInventoryAmountAfter(0L);
        inventaire.setGapCost(0);
        inventaire.setGapAmount(0);
        inventaire.setUser(utilisateur);
        inventaire.setStorage(storage);
        inventaire.setInventoryCategory(categorie);
        em.persist(inventaire);
        em.flush();
        return inventaire;
    }

    /** Une ligne non comptée : c'est l'état dans lequel la création d'un inventaire les laisse. */
    protected StoreInventoryLine ligne(StoreInventory inventaire, Produit produit) {
        StoreInventoryLine ligne = new StoreInventoryLine();
        ligne.setStoreInventory(inventaire);
        ligne.setProduit(produit);
        ligne.setStorage(inventaire.getStorage());
        ligne.setUpdated(false);
        ligne.setUpdatedAt(LocalDateTime.now());
        em.persist(ligne);
        em.flush();
        return ligne;
    }

    /** Une ligne comptée, valorisée comme le ferait la saisie écran. */
    protected StoreInventoryLine ligneComptee(StoreInventory inventaire, Produit produit, int theorique, int compte) {
        StoreInventoryLine ligne = ligne(inventaire, produit);
        ligne.applyCount(theorique, compte, utilisateur);
        em.flush();
        return ligne;
    }

    protected InventoryLot ligneDeLot(StoreInventoryLine ligne, Lot lot, Integer quantiteComptee) {
        InventoryLot inventoryLot = new InventoryLot();
        inventoryLot.setStoreInventoryLine(ligne);
        inventoryLot.setLot(lot);
        inventoryLot.setQuantityInit(lot.getCurrentQuantity());
        inventoryLot.setQuantityOnHand(quantiteComptee);
        inventoryLot.setGap(quantiteComptee != null ? quantiteComptee - lot.getCurrentQuantity() : 0);
        inventoryLot.setUpdated(quantiteComptee != null);
        inventoryLot.setUpdatedAt(LocalDateTime.now());
        em.persist(inventoryLot);
        em.flush();
        return inventoryLot;
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

    protected long lignesDe(StoreInventory inventaire) {
        return compter("SELECT count(*) FROM store_inventory_line WHERE store_inventory_id = " + inventaire.getId());
    }

    protected long lignesDe(Long inventaireId) {
        return compter("SELECT count(*) FROM store_inventory_line WHERE store_inventory_id = " + inventaireId);
    }

    protected long lotsDe(Long inventaireId) {
        return compter(
            """
            SELECT count(*) FROM inventory_lot il
            JOIN store_inventory_line sil ON sil.id = il.store_inventory_line_id
            WHERE sil.store_inventory_id = %d
            """.formatted(inventaireId)
        );
    }
}
