package com.kobe.warehouse.service.facturation.integration;

import static org.mockito.Mockito.lenient;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.domain.CashFund;
import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.GroupeTiersPayant;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.RepartitionTiersPayantParTva;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.enumeration.CashFundStatut;
import com.kobe.warehouse.domain.enumeration.CashFundType;
import com.kobe.warehouse.domain.enumeration.CashRegisterStatut;
import com.kobe.warehouse.domain.enumeration.InvoiceStatut;
import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.domain.enumeration.OrigineVente;
import com.kobe.warehouse.domain.enumeration.PaymentStatus;
import com.kobe.warehouse.domain.enumeration.PrioriteTiersPayant;
import com.kobe.warehouse.domain.enumeration.SalesStatut;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.ThirdPartySaleStatut;
import com.kobe.warehouse.domain.enumeration.TiersPayantCategorie;
import com.kobe.warehouse.domain.enumeration.TiersPayantStatut;
import com.kobe.warehouse.domain.enumeration.TypeAssure;
import com.kobe.warehouse.domain.enumeration.TypePrescription;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
 * Socle des tests d'intégration de la facturation.
 *
 * <p>Chaque test s'exécute dans une transaction annulée à la fin : la base revient d'elle-même à
 * l'état laissé par Flyway, et l'ordre des tests cesse d'être un paramètre du résultat.
 *
 * <p>Le jeu d'essai est celui du tiers payant : un assuré, son compte chez un organisme, et des
 * <em>dossiers</em> ({@code third_party_sale_line}) rattachés à des ventes assurance closes. C'est
 * sur ces dossiers que l'édition travaille — elle les regroupe par organisme et en fait des
 * factures. Chaque dossier porte sa ventilation par taux de TVA, stockée en {@code jsonb} : c'est
 * la matière première des totaux de la facture.
 */
@Testcontainers(disabledWithoutDocker = true)
abstract class AbstractFacturationIntegrationTest {

    protected static final int MAGASIN_ID = 1;

    protected static EntityManager em;

    /** Suffixe unique par fixture : noms d'organisme et codes assuré sont uniques en base. */
    private static final AtomicInteger COMPTEUR = new AtomicInteger();

    protected FacturationServicesUnderTest services;
    protected AppUser utilisateur;
    protected Magasin magasin;
    protected CashRegister caisse;

    private TransactionStatus transaction;

    @BeforeAll
    static void demarrerLaBase() {
        em = SharedEntityManagerCreator.createSharedEntityManager(IntegrationPostgresDatabase.bean(EntityManagerFactory.class));
    }

    @BeforeEach
    void ouvrirLaTransaction() {
        transaction = IntegrationPostgresDatabase.transactionManager().getTransaction(new DefaultTransactionDefinition());
        services = new FacturationServicesUnderTest(em);

        utilisateur = em.find(AppUser.class, 1);
        magasin = em.find(Magasin.class, MAGASIN_ID);
        caisse = ouvrirUneCaisse();

        lenient().when(services.userService.getUser()).thenReturn(utilisateur);
        lenient().when(services.storageService.getUser()).thenReturn(utilisateur);
        lenient().when(services.appConfigurationService.findParamResetInvoiceNumberEveryYear()).thenReturn(Optional.empty());
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

    protected CashRegister ouvrirUneCaisse() {
        CashFund fond = new CashFund();
        fond.setUser(utilisateur);
        fond.setAmount(0);
        fond.setCreated(LocalDateTime.now());
        fond.setUpdated(LocalDateTime.now());
        fond.setCashFundType(CashFundType.AUTO);
        fond.setStatut(CashFundStatut.VALIDETED);
        em.persist(fond);

        CashRegister cashRegister = new CashRegister();
        cashRegister.setCashFund(fond);
        cashRegister.setUser(utilisateur);
        cashRegister.setInitAmount(0L);
        cashRegister.setBeginTime(LocalDateTime.now());
        cashRegister.setCreated(LocalDateTime.now());
        cashRegister.setUpdated(LocalDateTime.now());
        cashRegister.setStatut(CashRegisterStatut.OPEN);
        em.persist(cashRegister);
        return cashRegister;
    }

    protected GroupeTiersPayant groupe(String nom) {
        GroupeTiersPayant groupe = new GroupeTiersPayant();
        groupe.setName(unique(nom));
        em.persist(groupe);
        em.flush();
        return groupe;
    }

    protected TiersPayant tiersPayant(String nom) {
        return tiersPayant(nom, TiersPayantCategorie.ASSURANCE, null);
    }

    protected TiersPayant tiersPayant(String nom, TiersPayantCategorie categorie) {
        return tiersPayant(nom, categorie, null);
    }

    protected TiersPayant tiersPayant(String nom, TiersPayantCategorie categorie, GroupeTiersPayant groupe) {
        TiersPayant tiersPayant = new TiersPayant();
        tiersPayant.setName(unique(nom));
        tiersPayant.setFullName(nom);
        tiersPayant.setStatut(TiersPayantStatut.ACTIF);
        tiersPayant.setCategorie(categorie);
        tiersPayant.setCreated(LocalDateTime.now());
        tiersPayant.setUpdated(LocalDateTime.now());
        tiersPayant.setUser(utilisateur);
        tiersPayant.setGroupeTiersPayant(groupe);
        em.persist(tiersPayant);
        em.flush();
        return tiersPayant;
    }

    protected AssuredCustomer assure(String nom) {
        AssuredCustomer assure = new AssuredCustomer();
        assure.setFirstName("Assuré");
        assure.setLastName(nom);
        assure.setCode(unique("ASS"));
        assure.setTypeAssure(TypeAssure.PRINCIPAL);
        assure.setStatus(Status.ENABLE);
        assure.setCreatedAt(LocalDateTime.now());
        assure.setUpdatedAt(LocalDateTime.now());
        em.persist(assure);
        em.flush();
        return assure;
    }

    protected ClientTiersPayant compte(TiersPayant tiersPayant) {
        ClientTiersPayant compte = new ClientTiersPayant();
        compte.setTiersPayant(tiersPayant);
        compte.setAssuredCustomer(assure(tiersPayant.getFullName()));
        compte.setNum(unique("NUM"));
        compte.setTaux(80);
        compte.setPriorite(PrioriteTiersPayant.R0);
        compte.setStatut(TiersPayantStatut.ACTIF);
        compte.setCreated(LocalDateTime.now());
        compte.setUpdated(LocalDateTime.now());
        em.persist(compte);
        em.flush();
        return compte;
    }

    /** Une vente assurance close : c'est sa date qui décide si le dossier entre dans la période. */
    protected ThirdPartySales venteAssurance(int montant, LocalDate date, AssuredCustomer client) {
        ThirdPartySales vente = new ThirdPartySales();
        vente.setId(services.saleIdGeneratorService.nextId());
        vente.setSaleDate(date);
        vente.setNumberTransaction("FA" + vente.getId().getId());
        vente.setStatut(SalesStatut.CLOSED);
        vente.setPaymentStatus(PaymentStatus.PAYE);
        vente.setNatureVente(NatureVente.ASSURANCE);
        vente.setOrigineVente(OrigineVente.DIRECT);
        vente.setTypePrescription(TypePrescription.PRESCRIPTION);
        vente.setCreatedAt(LocalDateTime.now());
        vente.setUpdatedAt(LocalDateTime.now());
        vente.setEffectiveUpdateDate(LocalDateTime.now());
        vente.setSalesAmount(montant);
        vente.setNetAmount(montant);
        vente.setAmountToBePaid(montant);
        vente.setAmountToBeTakenIntoAccount(montant);
        vente.setUser(utilisateur);
        vente.setSeller(utilisateur);
        vente.setCaissier(utilisateur);
        vente.setMagasin(magasin);
        vente.setCashRegister(caisse);
        // L'écran des dossiers à facturer lit l'assuré depuis la vente : sans lui, la lecture casse.
        vente.setCustomer(client);
        em.persist(vente);
        em.flush();
        return vente;
    }

    protected ThirdPartySaleLine dossier(ClientTiersPayant compte, int montant) {
        return dossier(compte, montant, LocalDate.now(), List.of(repartition(montant, 0)));
    }

    protected ThirdPartySaleLine dossier(ClientTiersPayant compte, int montant, LocalDate dateVente) {
        return dossier(compte, montant, dateVente, List.of(repartition(montant, 0)));
    }

    /**
     * Un dossier facturable. Ses {@code repartitions} sont ce que l'édition agrège pour établir les
     * totaux de la facture, taux par taux.
     */
    protected ThirdPartySaleLine dossier(
        ClientTiersPayant compte,
        int montant,
        LocalDate dateVente,
        List<RepartitionTiersPayantParTva> repartitions
    ) {
        ThirdPartySaleLine dossier = new ThirdPartySaleLine();
        dossier.setId(services.assuranceItemIdGeneratorService.nextId());
        dossier.setSaleDate(dateVente);
        dossier.setSale(venteAssurance(montant, dateVente, compte.getAssuredCustomer()));
        dossier.setClientTiersPayant(compte);
        dossier.setNumBon(unique("BON"));
        dossier.setMontant(montant);
        dossier.setMontantRegle(0);
        dossier.setTaux((short) compte.getTaux());
        dossier.setTauxVente((short) 100);
        dossier.setStatut(ThirdPartySaleStatut.ACTIF);
        dossier.setRepartitions(repartitions);
        dossier.setCreated(LocalDateTime.now());
        dossier.setUpdated(LocalDateTime.now());
        dossier.setEffectiveUpdateDate(LocalDateTime.now());
        em.persist(dossier);
        em.flush();
        return dossier;
    }

    /** Une ventilation à un taux donné : TTC = net ici, la TVA étant calculée à part. */
    protected RepartitionTiersPayantParTva repartition(int montantTtc, int taux) {
        double tva = montantTtc * taux / 100.0;
        return new RepartitionTiersPayantParTva(montantTtc, tva, montantTtc, montantTtc - tva, taux);
    }

    /** Une facture déjà émise, pour éprouver ce que l'édition suivante accepte de reprendre. */
    protected FactureTiersPayant facture(TiersPayant tiersPayant, boolean provisoire, List<ThirdPartySaleLine> dossiers) {
        return facture(tiersPayant, provisoire, dossiers, LocalDate.now());
    }

    /** La même, à une date d'émission choisie : le récapitulatif mensuel compare des mois. */
    protected FactureTiersPayant facture(
        TiersPayant tiersPayant,
        boolean provisoire,
        List<ThirdPartySaleLine> dossiers,
        LocalDate dateEmission
    ) {
        FactureTiersPayant facture = new FactureTiersPayant();
        facture.setId(services.factureIdGeneratorService.nextId());
        // Le numéro suit le format que relit la numérotation : « année_NNNN ». Un numéro d'une
        // autre forme ferait échouer l'édition suivante, qui en extrait l'année et l'indice.
        facture.setNumFacture(LocalDate.now().getYear() + "_" + String.format("%04d", COMPTEUR.incrementAndGet()));
        facture.setInvoiceDate(dateEmission);
        facture.setCreated(LocalDateTime.now());
        facture.setUpdated(LocalDateTime.now());
        facture.setUser(utilisateur);
        facture.setTiersPayant(tiersPayant);
        facture.setFactureProvisoire(provisoire);
        facture.setStatut(InvoiceStatut.NOT_PAID);
        facture.setMontantRegle(0);
        facture.setMontantTtc(BigDecimal.ZERO);
        facture.setMontantTva(BigDecimal.ZERO);
        facture.setMontantNet(BigDecimal.ZERO);
        facture.setMontantHt(BigDecimal.ZERO);
        em.persist(facture);
        dossiers.forEach(dossier -> {
            dossier.setFactureTiersPayant(facture);
            facture.getFacturesDetails().add(dossier);
        });
        em.flush();
        return facture;
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

    protected List<FactureTiersPayant> facturesDe(int generationCode) {
        return em
            .createQuery(
                "SELECT f FROM FactureTiersPayant f WHERE f.generationCode = :code ORDER BY f.numFacture",
                FactureTiersPayant.class
            )
            .setParameter("code", generationCode)
            .getResultList();
    }
}
