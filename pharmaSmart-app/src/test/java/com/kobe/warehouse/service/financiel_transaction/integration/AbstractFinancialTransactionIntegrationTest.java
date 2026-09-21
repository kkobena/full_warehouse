package com.kobe.warehouse.service.financiel_transaction.integration;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.CashFund;
import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.FamilleProduit;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.SalePayment;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.Storage;
import com.kobe.warehouse.domain.Tva;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.CashFundStatut;
import com.kobe.warehouse.domain.enumeration.CashFundType;
import com.kobe.warehouse.domain.enumeration.CashRegisterStatut;
import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.domain.enumeration.OrigineVente;
import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import com.kobe.warehouse.domain.enumeration.TypePrescription;
import com.kobe.warehouse.domain.enumeration.TypeProduit;
import com.kobe.warehouse.service.id_generator.SaleIdGeneratorService;
import com.kobe.warehouse.service.id_generator.SaleLineIdGeneratorService;
import com.kobe.warehouse.service.id_generator.TransactionIdGeneratorService;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Socle des tests d'intégration des états financiers.
 *
 * <p>Chaque test s'exécute dans une transaction annulée à la fin : la base revient d'elle-même à
 * l'état laissé par Flyway, et l'ordre des tests cesse d'être un paramètre du résultat.
 *
 * <p>Ce paquet a une particularité qui justifie à elle seule des tests d'intégration : la balance de
 * caisse et l'état de TVA ne sont pas calculés en Java mais par des <b>fonctions stockées</b> qui
 * rendent du JSON, que le service relit ensuite avec Jackson. Le contrat entre la fonction et le DTO
 * n'est donc écrit nulle part — ni dans le schéma, ni dans le code Java — et une clé renommée d'un
 * côté sans l'autre ne produit aucune erreur : la valeur disparaît simplement de l'état.
 */
@Testcontainers
abstract class AbstractFinancialTransactionIntegrationTest {

    protected static final int MAGASIN_ID = 1;
    protected static final int STORAGE_RAYON_ID = 1;

    protected static EntityManager em;

    /** Suffixe unique par fixture : codes et libellés sont uniques en base. */
    private static final AtomicInteger COMPTEUR = new AtomicInteger();

    protected AppUser utilisateur;
    protected Magasin magasin;
    protected Storage rayon;

    protected SaleIdGeneratorService saleIdGeneratorService;
    protected SaleLineIdGeneratorService saleLineIdGeneratorService;
    protected TransactionIdGeneratorService transactionIdGeneratorService;

    private TransactionStatus transaction;

    @BeforeAll
    static void demarrerLaBase() {
        em = SharedEntityManagerCreator.createSharedEntityManager(IntegrationPostgresDatabase.bean(EntityManagerFactory.class));
    }

    @BeforeEach
    void ouvrirLaTransaction() {
        transaction = IntegrationPostgresDatabase.transactionManager().getTransaction(new DefaultTransactionDefinition());

        utilisateur = em.find(AppUser.class, 1);
        magasin = em.find(Magasin.class, MAGASIN_ID);
        rayon = em.find(Storage.class, STORAGE_RAYON_ID);

        saleIdGeneratorService = new SaleIdGeneratorService(em);
        saleLineIdGeneratorService = new SaleLineIdGeneratorService(em);
        transactionIdGeneratorService = new TransactionIdGeneratorService(em);
    }

    @AfterEach
    void annulerLaTransaction() {
        if (transaction != null && !transaction.isCompleted()) {
            IntegrationPostgresDatabase.transactionManager().rollback(transaction);
        }
    }

    // ===== repères de date =====

    /**
     * La date d'aujourd'hui telle que <b>la base</b> la voit.
     *
     * <p>Les horodatages sont stockés en UTC : passé minuit dans un fuseau en avance,
     * {@code LocalDate.now()} désigne déjà le lendemain pour PostgreSQL, et une fonction qui borne
     * sur {@code CURRENT_DATE} ne retrouve plus la vente qu'on vient d'enregistrer.
     */
    protected LocalDate aujourdHui() {
        Object date = em.createNativeQuery("SELECT CURRENT_DATE").getSingleResult();
        return date instanceof java.sql.Date jour ? jour.toLocalDate() : LocalDate.parse(date.toString());
    }

    protected static String unique(String prefixe) {
        return prefixe + "-" + COMPTEUR.incrementAndGet();
    }

    // ===== référentiel =====

    protected Tva tva(int taux) {
        return em.createQuery("SELECT t FROM Tva t WHERE t.taux = :taux", Tva.class).setParameter("taux", taux).getSingleResult();
    }

    protected FamilleProduit premiereFamille() {
        return em.createQuery("SELECT f FROM FamilleProduit f ORDER BY f.id", FamilleProduit.class).setMaxResults(1).getSingleResult();
    }

    protected PaymentMode modePaiement(String code) {
        return em.find(PaymentMode.class, code);
    }

    // ===== produits =====

    protected Produit produit(String libelle, int prixVente, int tauxTva) {
        Produit produit = new Produit();
        produit.setLibelle(libelle);
        produit.setTypeProduit(TypeProduit.PACKAGE);
        produit.setStatus(Status.ENABLE);
        produit.setCostAmount(prixVente * 6 / 10);
        produit.setRegularUnitPrice(prixVente);
        produit.setNetUnitPrice(prixVente);
        produit.setItemCostAmount(prixVente * 6 / 10);
        produit.setItemRegularUnitPrice(prixVente);
        produit.setItemQty(1);
        produit.setPrixMnp(0);
        produit.setQtySeuilMini(5);
        produit.setDeconditionnable(false);
        produit.setCreatedAt(LocalDateTime.now());
        produit.setUpdatedAt(LocalDateTime.now());
        produit.setTva(tva(tauxTva));
        produit.setFamille(premiereFamille());
        em.persist(produit);
        em.flush();
        return produit;
    }

    // ===== caisse =====

    protected CashRegister caisseOuverte() {
        CashFund fond = new CashFund();
        fond.setUser(utilisateur);
        fond.setAmount(0);
        fond.setCreated(LocalDateTime.now());
        fond.setUpdated(LocalDateTime.now());
        fond.setCashFundType(CashFundType.AUTO);
        fond.setStatut(CashFundStatut.VALIDETED);
        em.persist(fond);

        CashRegister caisse = new CashRegister();
        caisse.setCashFund(fond);
        caisse.setUser(utilisateur);
        caisse.setInitAmount(0L);
        caisse.setBeginTime(aujourdHui().atTime(8, 0));
        caisse.setCreated(LocalDateTime.now());
        caisse.setUpdated(LocalDateTime.now());
        caisse.setStatut(CashRegisterStatut.OPEN);
        em.persist(caisse);
        em.flush();
        return caisse;
    }

    // ===== ventes =====

    /**
     * Une vente comptant clôturée, comptée en chiffre d'affaires.
     *
     * <p>Les trois conditions — {@code CLOSED}, non annulée, {@code ca = 'CA'} — sont celles que les
     * fonctions stockées exigent : en manquer une seule rend la vente invisible à la balance.
     */
    protected CashSale venteFermee(LocalDate date, int montantTtc, int remise) {
        CashSale vente = new CashSale();
        vente.setSaleDate(date);
        vente.setId(saleIdGeneratorService.nextId());
        vente.setNumberTransaction("FIN" + vente.getId().getId());
        vente.setStatut(SalesStatut.CLOSED);
        vente.setPaymentStatus(PaymentStatus.PAYE);
        vente.setNatureVente(NatureVente.COMPTANT);
        vente.setOrigineVente(OrigineVente.DIRECT);
        vente.setTypePrescription(TypePrescription.PRESCRIPTION);
        vente.setCategorieChiffreAffaire(CategorieChiffreAffaire.CA);
        vente.setCanceled(false);
        vente.setCreatedAt(date.atTime(LocalTime.of(10, 0).plusSeconds(COMPTEUR.incrementAndGet())));
        vente.setUpdatedAt(LocalDateTime.now());
        vente.setEffectiveUpdateDate(LocalDateTime.now());
        vente.setSalesAmount(montantTtc);
        vente.setNetAmount(montantTtc - remise);
        vente.setDiscountAmount(remise);
        vente.setAmountToBePaid(montantTtc - remise);
        vente.setPayrollAmount(montantTtc - remise);
        vente.setRestToPay(0);
        vente.setAmountToBeTakenIntoAccount(montantTtc - remise);
        vente.setUser(utilisateur);
        vente.setSeller(utilisateur);
        vente.setCaissier(utilisateur);
        vente.setMagasin(magasin);
        em.persist(vente);
        em.flush();
        return vente;
    }

    /** Une ligne de vente. Les montants de l'en-tête doivent rester cohérents avec ses lignes. */
    protected SalesLine ligneDeVente(Sales vente, Produit produit, int quantite, int remise) {
        SalesLine ligne = new SalesLine();
        ligne.setId(saleLineIdGeneratorService.nextId());
        ligne.setSaleDate(vente.getSaleDate());
        ligne.setSales(vente);
        ligne.setProduit(produit);
        ligne.setQuantityRequested(quantite);
        ligne.setQuantitySold(quantite);
        ligne.setQuantityAvoir(0);
        ligne.setQuantityUg(0);
        ligne.setRegularUnitPrice(produit.getRegularUnitPrice());
        ligne.setNetUnitPrice(produit.getRegularUnitPrice());
        ligne.setCostAmount(produit.getCostAmount());
        ligne.setSalesAmount(quantite * produit.getRegularUnitPrice());
        ligne.setDiscountAmount(remise);
        // L'état de TVA calcule la remise à partir du taux porté par la ligne, non du montant :
        // renseigner l'un sans l'autre laisserait la remise invisible de cet état.
        if (ligne.getSalesAmount() > 0) {
            ligne.setTauxRemise((float) remise / ligne.getSalesAmount());
        }
        ligne.setTaxValue(produit.getTva().getTaux());
        ligne.setAmountToBeTakenIntoAccount(ligne.getSalesAmount() - remise);
        ligne.setCreatedAt(vente.getCreatedAt());
        ligne.setUpdatedAt(LocalDateTime.now());
        ligne.setEffectiveUpdateDate(LocalDateTime.now());
        em.persist(ligne);
        em.flush();
        return ligne;
    }

    /** Un encaissement rattaché à la vente et à la caisse. */
    protected SalePayment reglement(Sales vente, CashRegister caisse, String codeMode, int montant) {
        SalePayment paiement = new SalePayment();
        // payment_transaction est partitionnée par date : son identifiant est composite et explicite.
        paiement.setId(transactionIdGeneratorService.nextId());
        paiement.setSale(vente);
        paiement.setCashRegister(caisse);
        paiement.setPaymentMode(modePaiement(codeMode));
        paiement.setPaidAmount(montant);
        paiement.setExpectedAmount(montant);
        paiement.setMontantVerse(montant);
        paiement.setReelAmount(montant);
        paiement.setTransactionDate(vente.getSaleDate());
        paiement.setCreatedAt(vente.getCreatedAt());
        paiement.setCategorieChiffreAffaire(CategorieChiffreAffaire.CA);
        paiement.setTypeFinancialTransaction(TypeFinancialTransaction.CASH_SALE);
        paiement.setCredit(false);
        paiement.setPartAssure(0);
        paiement.setPartTiersPayant(montant);
        em.persist(paiement);
        em.flush();
        return paiement;
    }

    /**
     * Une vente comptant complète : en-tête, ligne et encaissement cohérents entre eux.
     *
     * @param remise remise accordée sur la ligne et reportée sur l'en-tête
     */
    protected CashSale venteEncaissee(LocalDate date, Produit produit, int quantite, int remise, String codeMode) {
        int montantTtc = quantite * produit.getRegularUnitPrice();
        CashSale vente = venteFermee(date, montantTtc, remise);
        ligneDeVente(vente, produit, quantite, remise);
        reglement(vente, caisseOuverte(), codeMode, montantTtc - remise);
        em.flush();
        return vente;
    }
}
