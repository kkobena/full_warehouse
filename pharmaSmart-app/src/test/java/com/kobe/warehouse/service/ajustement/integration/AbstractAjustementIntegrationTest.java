package com.kobe.warehouse.service.ajustement.integration;

import static org.mockito.Mockito.lenient;

import com.kobe.warehouse.domain.Ajust;
import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.FamilleProduit;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Lot;
import com.kobe.warehouse.domain.LotStockLocation;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.MotifAjustement;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.Tva;
import com.kobe.warehouse.domain.enumeration.AjustementStatut;
import com.kobe.warehouse.domain.enumeration.StatutLot;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.StorageType;
import com.kobe.warehouse.domain.enumeration.TypeProduit;
import com.kobe.warehouse.service.dto.AjustDTO;
import com.kobe.warehouse.service.dto.AjustementDTO;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Socle des tests d'intégration de l'ajustement de stock.
 *
 * <p>Chaque test s'exécute dans une transaction annulée à la fin : la base revient d'elle-même à
 * l'état laissé par Flyway, et l'ordre des tests cesse d'être un paramètre du résultat.
 *
 * <p>Le jeu d'essai tourne autour du produit, de son stock par emplacement et de ses lots répartis
 * — les trois choses qu'un ajustement validé modifie du même geste. Les emplacements 1 (rayon) et
 * 3 (réserve) viennent des migrations, comme le magasin, l'utilisateur et les taux de TVA.
 */
@Testcontainers(disabledWithoutDocker = true)
abstract class AbstractAjustementIntegrationTest {

    protected static final int MAGASIN_ID = 1;
    protected static final int STORAGE_RAYON_ID = 1;
    protected static final int STORAGE_RESERVE_ID = 3;

    protected static EntityManager em;

    /** Suffixe unique par fixture : codes CIP, numéros de lot et libellés sont uniques en base. */
    private static final AtomicInteger COMPTEUR = new AtomicInteger();

    protected AjustementServicesUnderTest services;
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
        services = new AjustementServicesUnderTest(em);

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
        lenient().when(services.appConfigurationService.useLot()).thenReturn(java.util.Optional.of(true));
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
     * mouvement y lit le prix d'achat et le prix de vente, tous deux obligatoires en base.
     */
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
            em.createQuery("SELECT f FROM FamilleProduit f ORDER BY f.id", FamilleProduit.class)
                .setMaxResults(1)
                .getSingleResult()
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
        fournisseurProduit.setPrixAchat(coutAchat);
        fournisseurProduit.setPrixUni(prixVente);
        em.persist(fournisseurProduit);
        produit.setFournisseurProduitPrincipal(fournisseurProduit);
        produit.getFournisseurProduits().add(fournisseurProduit);
        em.flush();
        return produit;
    }

    protected String codeCip(Produit produit) {
        return produit.getFournisseurProduitPrincipal().getCodeCip();
    }

    protected StockProduit stock(Produit produit, Storage storage, int quantite) {
        return stock(produit, storage, quantite, 0);
    }

    protected StockProduit stock(Produit produit, Storage storage, int quantite, int unitesGratuites) {
        StockProduit stockProduit = new StockProduit();
        stockProduit.setProduit(produit);
        stockProduit.setStorage(storage);
        stockProduit.setQtyStock(quantite);
        stockProduit.setQtyVirtual(quantite);
        stockProduit.setQtyUG(unitesGratuites);
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
     * Un lot et sa présence sur un emplacement — les deux faces de la même quantité.
     * {@code peremption} décide de l'ordre FEFO du débit.
     */
    protected Lot lotSurEmplacement(Produit produit, String numLot, LocalDate peremption, Storage storage, int quantite) {
        Lot lot = lot(produit, numLot, peremption, quantite);
        em.persist(new LotStockLocation(lot, storage, quantite));
        em.flush();
        return lot;
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

    protected MotifAjustement motif(String libelle) {
        MotifAjustement motif = new MotifAjustement();
        motif.setLibelle(libelle);
        em.persist(motif);
        em.flush();
        return motif;
    }

    // ===== ajustements =====

    /** Un ajustement en brouillon, tel que l'écran le laisse avant validation. */
    protected Ajust ajustEnCours(String commentaire) {
        Ajust ajust = new Ajust();
        ajust.setCommentaire(commentaire);
        ajust.setUser(utilisateur);
        ajust.setDateMtv(LocalDateTime.now());
        ajust.setStatut(AjustementStatut.PENDING);
        em.persist(ajust);
        em.flush();
        return ajust;
    }

    protected AjustementDTO ligne(Ajust ajust, Produit produit, int quantite) {
        AjustementDTO dto = new AjustementDTO();
        dto.setAjustId(ajust.getId());
        dto.setProduitId(produit.getId());
        dto.setQtyMvt(quantite);
        return dto;
    }

    protected AjustDTO validation(Ajust ajust, String commentaire) {
        AjustDTO dto = new AjustDTO();
        dto.setId(ajust.getId());
        dto.setCommentaire(commentaire);
        return dto;
    }

    protected AjustDTO creation(String commentaire, AjustementDTO... lignes) {
        AjustDTO dto = new AjustDTO();
        dto.setCommentaire(commentaire);
        dto.setAjustements(new ArrayList<>(List.of(lignes)));
        return dto;
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
            "SELECT qty_stock FROM stock_produit WHERE produit_id = %d AND storage_id = %d"
                .formatted(produit.getId(), storage.getId())
        );
    }

    protected int quantiteCourante(Lot lot) {
        return (int) compter("SELECT current_quantity FROM lot WHERE id = " + lot.getId());
    }

    protected int quantiteSurEmplacement(Lot lot, Storage storage) {
        return services.lotStockLocationRepository
            .findByLotIdAndStorageId(lot.getId(), storage.getId())
            .map(LotStockLocation::getQty)
            .orElse(-1);
    }
}
