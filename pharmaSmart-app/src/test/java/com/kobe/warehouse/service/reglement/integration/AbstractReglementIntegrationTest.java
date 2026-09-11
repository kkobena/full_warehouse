package com.kobe.warehouse.service.reglement.integration;

import static org.mockito.Mockito.lenient;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.domain.CashFund;
import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.FactureTiersPayant;
import com.kobe.warehouse.domain.GroupeTiersPayant;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.ThirdPartySaleLine;
import com.kobe.warehouse.domain.ThirdPartySales;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.UninsuredCustomer;
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
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Socle des tests d'intégration des services de règlement.
 *
 * <p>Chaque test s'exécute dans une transaction annulée à la fin : la base revient d'elle-même à
 * l'état laissé par Flyway, et l'ordre des tests cesse d'être un paramètre du résultat. Cette
 * transaction est aussi ce qui rend les services testables tels quels — hors contexte Spring Boot,
 * personne n'ouvre de transaction pour eux, et {@code getReferenceById} rendrait des mandataires
 * inutilisables.
 *
 * <p>Le jeu d'essai s'arrête au strict nécessaire : le règlement ne lit ni produit, ni ligne de
 * vente, ni stock. Il lit des <em>dossiers</em> ({@code third_party_sale_line}) rattachés à une
 * facture, et — pour le différé — des ventes portant un reste à payer. Les données de référence
 * (magasin 1, utilisateur 1, modes de paiement) viennent des migrations.
 */
@Testcontainers(disabledWithoutDocker = true)
abstract class AbstractReglementIntegrationTest {

    protected static final int MAGASIN_ID = 1;

    protected static EntityManager em;

    /** Suffixe unique par fixture : les numéros de facture et les noms d'organisme sont uniques en base. */
    private static final AtomicInteger COMPTEUR = new AtomicInteger();

    protected ReglementServicesUnderTest services;
    protected AppUser caissier;
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
        services = new ReglementServicesUnderTest(em);

        caissier = em.find(AppUser.class, 1);
        magasin = em.find(Magasin.class, MAGASIN_ID);
        caisse = ouvrirUneCaisse();

        lenient().when(services.cashRegisterService.getCashRegister()).thenReturn(caisse);
        lenient().when(services.userService.getUser()).thenReturn(caissier);
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

    protected AssuredCustomer assure(String nom, String prenom) {
        AssuredCustomer assure = new AssuredCustomer();
        assure.setFirstName(prenom);
        assure.setLastName(nom);
        assure.setCode(unique("ASS"));
        assure.setTypeAssure(TypeAssure.PRINCIPAL);
        assure.setStatus(Status.ENABLE);
        assure.setCreatedAt(LocalDateTime.now());
        assure.setUpdatedAt(LocalDateTime.now());
        em.persist(assure);
        return assure;
    }

    protected TiersPayant tiersPayant(String nom) {
        return tiersPayant(nom, null);
    }

    protected TiersPayant tiersPayant(String nom, GroupeTiersPayant groupe) {
        TiersPayant tiersPayant = new TiersPayant();
        tiersPayant.setName(unique(nom));
        tiersPayant.setFullName(nom);
        tiersPayant.setStatut(TiersPayantStatut.ACTIF);
        tiersPayant.setCategorie(TiersPayantCategorie.ASSURANCE);
        tiersPayant.setCreated(LocalDateTime.now());
        tiersPayant.setUpdated(LocalDateTime.now());
        tiersPayant.setUser(caissier);
        tiersPayant.setGroupeTiersPayant(groupe);
        em.persist(tiersPayant);
        return tiersPayant;
    }

    protected GroupeTiersPayant groupe(String nom) {
        GroupeTiersPayant groupe = new GroupeTiersPayant();
        groupe.setName(unique(nom));
        em.persist(groupe);
        return groupe;
    }

    /** Le compte d'un assuré chez un organisme : c'est lui que le dossier facturé référence. */
    protected ClientTiersPayant compte(TiersPayant tiersPayant) {
        ClientTiersPayant compte = new ClientTiersPayant();
        compte.setTiersPayant(tiersPayant);
        compte.setAssuredCustomer(assure("ASSURE", tiersPayant.getFullName()));
        compte.setNum(unique("NUM"));
        compte.setTaux(80);
        compte.setPriorite(PrioriteTiersPayant.R0);
        compte.setStatut(TiersPayantStatut.ACTIF);
        compte.setCreated(LocalDateTime.now());
        compte.setUpdated(LocalDateTime.now());
        em.persist(compte);
        return compte;
    }

    /**
     * La vente assurance qui porte les dossiers. Le règlement ne la lit pas, mais
     * {@code third_party_sale_line} ne peut exister sans elle : c'est une contrainte de la base,
     * pas une commodité du test.
     */
    protected ThirdPartySales venteAssurance(int montant) {
        ThirdPartySales vente = new ThirdPartySales();
        vente.setId(services.saleIdGeneratorService.nextId());
        vente.setNumberTransaction("IT" + vente.getId().getId());
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
        vente.setUser(caissier);
        vente.setSeller(caissier);
        vente.setCaissier(caissier);
        vente.setMagasin(magasin);
        vente.setCashRegister(caisse);
        em.persist(vente);
        return vente;
    }

    /** Un dossier facturé : la part que l'organisme doit sur une vente, et ce qu'il en a réglé. */
    protected ThirdPartySaleLine dossier(ClientTiersPayant compte, int montant) {
        ThirdPartySaleLine dossier = new ThirdPartySaleLine();
        dossier.setId(services.assuranceItemIdGeneratorService.nextId());
        dossier.setSale(venteAssurance(montant));
        dossier.setClientTiersPayant(compte);
        dossier.setNumBon(unique("BON"));
        dossier.setMontant(montant);
        dossier.setMontantRegle(0);
        dossier.setTaux((short) compte.getTaux());
        dossier.setTauxVente((short) 100);
        dossier.setStatut(ThirdPartySaleStatut.ACTIF);
        dossier.setCreated(LocalDateTime.now());
        dossier.setUpdated(LocalDateTime.now());
        dossier.setEffectiveUpdateDate(LocalDateTime.now());
        em.persist(dossier);
        return dossier;
    }

    /** Une facture individuelle et ses dossiers ; son montant est la somme des dossiers. */
    protected FactureTiersPayant facture(TiersPayant tiersPayant, List<ThirdPartySaleLine> dossiers) {
        FactureTiersPayant facture = nouvelleFacture();
        facture.setTiersPayant(tiersPayant);
        em.persist(facture);
        rattacher(facture, dossiers);
        em.flush();
        return facture;
    }

    /**
     * Une facture de groupe : elle n'a pas de dossier à elle, seulement des factures filles. C'est
     * la forme que règlent {@code ReglementGroupeFactureService} et sa variante par sélection.
     */
    protected FactureTiersPayant factureGroupe(GroupeTiersPayant groupe, List<FactureTiersPayant> filles) {
        FactureTiersPayant facture = nouvelleFacture();
        facture.setGroupeTiersPayant(groupe);
        em.persist(facture);
        filles.forEach(fille -> fille.setGroupeFactureTiersPayant(facture));
        facture.getFactureTiersPayants().addAll(filles);
        em.flush();
        return facture;
    }

    private FactureTiersPayant nouvelleFacture() {
        FactureTiersPayant facture = new FactureTiersPayant();
        facture.setId(services.factureIdGeneratorService.nextId());
        facture.setNumFacture(unique("FAC"));
        facture.setInvoiceDate(LocalDate.now());
        facture.setCreated(LocalDateTime.now());
        facture.setUpdated(LocalDateTime.now());
        facture.setUser(caissier);
        facture.setStatut(InvoiceStatut.NOT_PAID);
        facture.setMontantRegle(0);
        facture.setMontantTtc(BigDecimal.ZERO);
        facture.setMontantTva(BigDecimal.ZERO);
        facture.setMontantNet(BigDecimal.ZERO);
        facture.setMontantHt(BigDecimal.ZERO);
        return facture;
    }

    private void rattacher(FactureTiersPayant facture, List<ThirdPartySaleLine> dossiers) {
        dossiers.forEach(dossier -> dossier.setFactureTiersPayant(facture));
        facture.getFacturesDetails().addAll(dossiers);
    }

    protected int montantDe(List<ThirdPartySaleLine> dossiers) {
        return dossiers.stream().mapToInt(ThirdPartySaleLine::getMontant).sum();
    }

    // ===== jeu d'essai du différé =====

    protected UninsuredCustomer clientDiffere(String nom, String prenom) {
        UninsuredCustomer customer = new UninsuredCustomer();
        customer.setFirstName(prenom);
        customer.setLastName(nom);
        customer.setCode(unique("CLI"));
        customer.setTypeAssure(TypeAssure.PRINCIPAL);
        customer.setStatus(Status.ENABLE);
        customer.setCreatedAt(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());
        em.persist(customer);
        em.flush();
        return customer;
    }

    /** Une vente à crédit close : c'est son {@code restToPay} que le règlement différé apure. */
    protected CashSale venteDiffere(UninsuredCustomer client, int montant, int dejaPaye) {
        CashSale vente = new CashSale();
        vente.setId(services.saleIdGeneratorService.nextId());
        vente.setNumberTransaction("DI" + vente.getId().getId());
        vente.setStatut(SalesStatut.CLOSED);
        vente.setPaymentStatus(montant > dejaPaye ? PaymentStatus.IMPAYE : PaymentStatus.PAYE);
        vente.setNatureVente(NatureVente.COMPTANT);
        vente.setOrigineVente(OrigineVente.DIRECT);
        vente.setTypePrescription(TypePrescription.PRESCRIPTION);
        vente.setCreatedAt(LocalDateTime.now());
        vente.setUpdatedAt(LocalDateTime.now());
        vente.setEffectiveUpdateDate(LocalDateTime.now());
        vente.setSalesAmount(montant);
        vente.setNetAmount(montant);
        vente.setAmountToBePaid(montant);
        vente.setAmountToBeTakenIntoAccount(montant);
        vente.setPayrollAmount(dejaPaye);
        vente.setRestToPay(montant - dejaPaye);
        vente.setDiffere(true);
        vente.setCustomer(client);
        vente.setUser(caissier);
        vente.setSeller(caissier);
        vente.setCaissier(caissier);
        vente.setMagasin(magasin);
        vente.setCashRegister(caisse);
        em.persist(vente);
        em.flush();
        return vente;
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
}
