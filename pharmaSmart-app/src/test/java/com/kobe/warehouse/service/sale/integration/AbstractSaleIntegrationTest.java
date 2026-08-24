package com.kobe.warehouse.service.sale.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.domain.CashFund;
import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.FamilleProduit;
import com.kobe.warehouse.domain.Fournisseur;
import com.kobe.warehouse.domain.FournisseurProduit;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.StockProduit;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.Tva;
import com.kobe.warehouse.domain.UninsuredCustomer;
import com.kobe.warehouse.domain.enumeration.CashFundStatut;
import com.kobe.warehouse.domain.enumeration.CashFundType;
import com.kobe.warehouse.domain.enumeration.CashRegisterStatut;
import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.domain.enumeration.OrigineVente;
import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.domain.enumeration.PrioriteTiersPayant;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.TypePrescription;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.TiersPayantCategorie;
import com.kobe.warehouse.domain.enumeration.TiersPayantStatut;
import com.kobe.warehouse.domain.enumeration.TypeAssure;
import com.kobe.warehouse.domain.enumeration.TypeProduit;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Socle des tests d'intégration des services de vente.
 *
 * <p>Chaque test s'exécute dans une transaction annulée à la fin : la base revient d'elle-même à
 * l'état laissé par Flyway, et l'ordre des tests cesse d'être un paramètre du résultat. Cette
 * transaction est aussi ce qui rend les services testables tels quels — hors contexte Spring Boot,
 * personne n'ouvre de transaction pour eux, et les entités chargées seraient détachées.
 *
 * <p>Les données de référence (magasin 1, utilisateurs, stockages 1 et 3, TVA) viennent des
 * migrations : les tests n'ajoutent que ce qui leur est propre — produits, stocks, ventes.
 */
@Testcontainers(disabledWithoutDocker = true)
abstract class AbstractSaleIntegrationTest {

    protected static final int MAGASIN_ID = 1;
    protected static final int STORAGE_RAYON_ID = 1;
    protected static final int STORAGE_RESERVE_ID = 3;

    protected static EntityManager em;

    protected SaleServicesUnderTest services;
    protected AppUser caissier;
    protected Magasin magasin;
    protected Storage rayon;
    protected Storage reserve;
    protected CashRegister caisse;

    private TransactionStatus transaction;

    @BeforeAll
    static void demarrerLaBase() {
        em = SharedEntityManagerCreator.createSharedEntityManager(SalePostgresDatabase.bean(EntityManagerFactory.class));
    }

    @BeforeEach
    void ouvrirLaTransaction() {
        transaction = SalePostgresDatabase.transactionManager().getTransaction(new DefaultTransactionDefinition());
        services = new SaleServicesUnderTest(em);

        caissier = em.find(AppUser.class, 1);
        magasin = em.find(Magasin.class, MAGASIN_ID);
        rayon = em.find(Storage.class, STORAGE_RAYON_ID);
        reserve = em.find(Storage.class, STORAGE_RESERVE_ID);
        caisse = ouvrirUneCaisse();

        lenient().when(services.storageService.getUser()).thenReturn(caissier);
        lenient().when(services.storageService.getUserFormImport()).thenReturn(caissier);
        lenient().when(services.storageService.getDefaultConnectedUserMainStorage()).thenReturn(rayon);
        lenient().when(services.storageService.getDefaultConnectedUserReserveStorage()).thenReturn(null);
        lenient().when(services.storageService.getConnectedUserMagasin()).thenReturn(magasin);
        lenient().when(services.cashRegisterService.getLastOpiningUserCashRegisterByUser(any())).thenReturn(caisse);
        lenient().when(services.appConfigurationService.getCancelSaleMaxDays()).thenReturn(30);
        lenient().when(services.appConfigurationService.getDelaiValiditeAvoir()).thenReturn(90);
        lenient().when(services.appConfigurationService.getDelaiRetourClient()).thenReturn(15);
        lenient().when(services.appConfigurationService.getMagasin()).thenReturn(magasin);
    }

    @AfterEach
    void annulerLaTransaction() {
        if (transaction != null && !transaction.isCompleted()) {
            SalePostgresDatabase.transactionManager().rollback(transaction);
        }
    }

    // ===== fabriques du jeu d'essai =====

    protected CashRegister ouvrirUneCaisse() {
        CashFund fond = new CashFund();
        fond.setUser(caissier);
        fond.setAmount(0);
        fond.setCreated(LocalDateTime.now());
        fond.setUpdated(LocalDateTime.now());
        fond.setCashFundType(CashFundType.AUTO);
        fond.setStatut(CashFundStatut.VALIDETED);
        em.persist(fond);

        CashRegister cashRegister = new CashRegister();
        cashRegister.setCashFund(fond);
        cashRegister.setUser(caissier);
        cashRegister.setInitAmount(0L);
        cashRegister.setBeginTime(LocalDateTime.now());
        cashRegister.setCreated(LocalDateTime.now());
        cashRegister.setUpdated(LocalDateTime.now());
        cashRegister.setStatut(CashRegisterStatut.OPEN);
        em.persist(cashRegister);
        return cashRegister;
    }

    /** Un produit vendable : prix, coût et taux de TVA sont ce que les calculs de vente lisent. */
    protected Produit produit(String libelle, int prixVente, int coutAchat, int tauxTva) {
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
        produit.setTva(tva(tauxTva));
        produit.setFamille(em.createQuery("SELECT f FROM FamilleProduit f ORDER BY f.id", FamilleProduit.class)
            .setMaxResults(1)
            .getSingleResult());
        em.persist(produit);
        return produit;
    }

    protected Tva tva(int taux) {
        return em.createQuery("SELECT t FROM Tva t WHERE t.taux = :taux", Tva.class).setParameter("taux", taux).getSingleResult();
    }

    /** Le stock rayon du produit ; sans lui, aucune ligne de vente ne peut être créée. */
    protected StockProduit stock(Produit produit, Storage storage, int quantite, int quantiteUg) {
        StockProduit stockProduit = new StockProduit();
        stockProduit.setProduit(produit);
        stockProduit.setStorage(storage);
        stockProduit.setQtyStock(quantite);
        stockProduit.setQtyVirtual(quantite);
        stockProduit.setQtyUG(quantiteUg);
        stockProduit.setCreatedAt(LocalDateTime.now());
        stockProduit.setUpdatedAt(LocalDateTime.now());
        em.persist(stockProduit);
        return stockProduit;
    }

    protected Produit produitEnStock(String libelle, int prixVente, int coutAchat, int tauxTva, int quantite) {
        Produit produit = produit(libelle, prixVente, coutAchat, tauxTva);
        stock(produit, rayon, quantite, 0);
        em.flush();
        return produit;
    }

    protected UninsuredCustomer client(String nom, String prenom, String code) {
        UninsuredCustomer customer = new UninsuredCustomer();
        customer.setFirstName(prenom);
        customer.setLastName(nom);
        customer.setCode(code);
        customer.setTypeAssure(TypeAssure.PRINCIPAL);
        customer.setStatus(Status.ENABLE);
        customer.setCreatedAt(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());
        em.persist(customer);
        em.flush();
        return customer;
    }

    /**
     * Une vente comptant clÃ´turÃ©e portant une ligne. Quand la quantitÃ© servie est infÃ©rieure Ã  la
     * quantitÃ© demandÃ©e, l'Ã©cart alimente {@code quantity_avoir} : c'est le point de dÃ©part des
     * avoirs client et des retours.
     */
    protected CashSale venteFermee(Produit produit, int quantiteDemandee, int quantiteServie) {
        return venteFermee(produit, quantiteDemandee, quantiteServie, LocalDate.now());
    }

    protected CashSale venteFermee(Produit produit, int quantiteDemandee, int quantiteServie, LocalDate date) {
        CashSale vente = new CashSale();
        vente.setSaleDate(date);
        vente.setId(services.saleIdGeneratorService.nextId());
        vente.setNumberTransaction("IT" + vente.getId().getId());
        vente.setStatut(SalesStatut.CLOSED);
        vente.setPaymentStatus(PaymentStatus.PAYE);
        vente.setNatureVente(NatureVente.COMPTANT);
        vente.setOrigineVente(OrigineVente.DIRECT);
        vente.setTypePrescription(TypePrescription.PRESCRIPTION);
        vente.setCreatedAt(LocalDateTime.now());
        vente.setUpdatedAt(LocalDateTime.now());
        vente.setEffectiveUpdateDate(LocalDateTime.now());
        vente.setAmountToBeTakenIntoAccount(0);
        vente.setUser(caissier);
        vente.setSeller(caissier);
        vente.setCaissier(caissier);
        vente.setMagasin(magasin);
        em.persist(vente);

        SalesLine ligne = ajouterLigne(vente, produit, quantiteDemandee, quantiteServie);
        vente.setSalesAmount(ligne.getSalesAmount());
        vente.setNetAmount(ligne.getSalesAmount());
        vente.setAmountToBePaid(ligne.getSalesAmount());
        vente.setPayrollAmount(ligne.getSalesAmount());
        em.flush();
        return vente;
    }

    protected SalesLine ajouterLigne(Sales vente, Produit produit, int quantiteDemandee, int quantiteServie) {
        SalesLine ligne = new SalesLine();
        ligne.setId(services.salesLineService.getNextId());
        ligne.setSaleDate(vente.getSaleDate());
        ligne.setSales(vente);
        ligne.setProduit(produit);
        ligne.setQuantityRequested(quantiteDemandee);
        ligne.setQuantitySold(quantiteServie);
        ligne.setQuantityAvoir(quantiteDemandee - quantiteServie);
        ligne.setRegularUnitPrice(produit.getRegularUnitPrice());
        ligne.setNetUnitPrice(produit.getRegularUnitPrice());
        ligne.setCostAmount(produit.getCostAmount());
        ligne.setSalesAmount(quantiteDemandee * produit.getRegularUnitPrice());
        ligne.setAmountToBeTakenIntoAccount(ligne.getSalesAmount());
        ligne.setCreatedAt(LocalDateTime.now());
        ligne.setUpdatedAt(LocalDateTime.now());
        ligne.setEffectiveUpdateDate(LocalDateTime.now());
        em.persist(ligne);
        vente.getSalesLines().add(ligne);
        em.flush();
        return ligne;
    }

    /** Le rÃ©fÃ©rencement fournisseur : c'est lui qui porte le code CIP affichÃ© sur les documents. */
    protected FournisseurProduit fournisseurProduit(Produit produit, String codeCip) {
        Fournisseur fournisseur = new Fournisseur();
        fournisseur.setLibelle("FOURNISSEUR " + codeCip);
        fournisseur.setCode(codeCip);
        em.persist(fournisseur);

        FournisseurProduit fournisseurProduit = new FournisseurProduit();
        fournisseurProduit.setProduit(produit);
        fournisseurProduit.setFournisseur(fournisseur);
        fournisseurProduit.setCodeCip(codeCip);
        fournisseurProduit.setPrixAchat(produit.getCostAmount());
        fournisseurProduit.setPrixUni(produit.getRegularUnitPrice());
        em.persist(fournisseurProduit);
        produit.setFournisseurProduitPrincipal(fournisseurProduit);
        em.flush();
        return fournisseurProduit;
    }

    protected AssuredCustomer assure(String nom, String prenom, String code) {
        AssuredCustomer assure = new AssuredCustomer();
        assure.setFirstName(prenom);
        assure.setLastName(nom);
        assure.setCode(code);
        assure.setTypeAssure(TypeAssure.PRINCIPAL);
        assure.setStatus(Status.ENABLE);
        assure.setCreatedAt(LocalDateTime.now());
        assure.setUpdatedAt(LocalDateTime.now());
        em.persist(assure);
        em.flush();
        return assure;
    }

    protected TiersPayant tiersPayant(String nom) {
        TiersPayant tiersPayant = new TiersPayant();
        tiersPayant.setName(nom);
        tiersPayant.setFullName(nom);
        tiersPayant.setStatut(TiersPayantStatut.ACTIF);
        tiersPayant.setCategorie(TiersPayantCategorie.ASSURANCE);
        tiersPayant.setCreated(LocalDateTime.now());
        tiersPayant.setUpdated(LocalDateTime.now());
        tiersPayant.setUser(caissier);
        em.persist(tiersPayant);
        em.flush();
        return tiersPayant;
    }

    /** Un compte d'assure chez un organisme : c'est lui qui porte le taux de prise en charge. */
    protected ClientTiersPayant compte(String organisme, int taux, PrioriteTiersPayant priorite) {
        return compte(assure("ASSURE", organisme, "ASS-" + organisme), tiersPayant(organisme), taux, priorite);
    }

    protected ClientTiersPayant compte(AssuredCustomer assure, String organisme, int taux, PrioriteTiersPayant priorite) {
        return compte(assure, tiersPayant(organisme), taux, priorite);
    }

    protected ClientTiersPayant compte(AssuredCustomer assure, TiersPayant tiersPayant, int taux, PrioriteTiersPayant priorite) {
        ClientTiersPayant compte = new ClientTiersPayant();
        compte.setTiersPayant(tiersPayant);
        compte.setAssuredCustomer(assure);
        compte.setNum("NUM-" + tiersPayant.getName());
        compte.setTaux(taux);
        compte.setPriorite(priorite);
        compte.setStatut(TiersPayantStatut.ACTIF);
        compte.setCreated(LocalDateTime.now());
        compte.setUpdated(LocalDateTime.now());
        em.persist(compte);
        em.flush();
        return compte;
    }

    /** Vide le contexte de persistance : la relecture qui suit vient bien de Postgres. */
    protected void viderLeCache() {
        em.flush();
        em.clear();
    }

    protected long compter(String sql) {
        return ((Number) em.createNativeQuery(sql).getSingleResult()).longValue();
    }
}
