package com.kobe.warehouse.service.customer.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.AssuredCustomer;
import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.ClientTiersPayant;
import com.kobe.warehouse.domain.Customer;
import com.kobe.warehouse.domain.CustomerAllergie;
import com.kobe.warehouse.domain.CustomerDossierSante;
import com.kobe.warehouse.domain.DifferePayment;
import com.kobe.warehouse.domain.DifferePaymentItem;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.RelanceDiffere;
import com.kobe.warehouse.domain.Sales;
import com.kobe.warehouse.domain.TiersPayant;
import com.kobe.warehouse.domain.UninsuredCustomer;
import com.kobe.warehouse.domain.enumeration.CategorieChiffreAffaire;
import com.kobe.warehouse.domain.enumeration.PrioriteTiersPayant;
import com.kobe.warehouse.domain.enumeration.Status;
import com.kobe.warehouse.domain.enumeration.TiersPayantCategorie;
import com.kobe.warehouse.domain.enumeration.TiersPayantStatut;
import com.kobe.warehouse.domain.enumeration.TypeAssure;
import com.kobe.warehouse.domain.enumeration.TypeFinancialTransaction;
import com.kobe.warehouse.repository.CustomerAllergieRepository;
import com.kobe.warehouse.repository.CustomerDossierSanteRepository;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.service.LogsService;
import com.kobe.warehouse.service.StorageService;
import com.kobe.warehouse.service.customer.AttestationDepensesDTO;
import com.kobe.warehouse.service.customer.CustomerDocumentService;
import com.kobe.warehouse.service.customer.DonneesClientDTO;
import com.kobe.warehouse.service.customer.DonneesPersonnellesClientService;
import com.kobe.warehouse.service.customer.DossierSanteService;
import com.kobe.warehouse.service.customer.DoublonClientDTO;
import com.kobe.warehouse.service.customer.DoublonClientGroupeDTO;
import com.kobe.warehouse.service.customer.FusionClientApercuDTO;
import com.kobe.warehouse.service.customer.FusionClientRequestDTO;
import com.kobe.warehouse.service.customer.FusionClientService;
import com.kobe.warehouse.service.customer.LigneReleveDTO;
import com.kobe.warehouse.service.customer.ReleveCompteDTO;
import com.kobe.warehouse.service.dashboard.integration.AbstractDashboardIntegrationTest;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.id_generator.TransactionIdGeneratorService;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Fiche client, lot 5 (docs/PLAN-FICHE-CLIENT.md) : doublons et fusion, relevé de compte,
 * attestation de dépenses, export et anonymisation des données d'un client.
 */
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Fiche client — qualité du fichier et documents")
class QualiteFichierClientIntegrationTest extends AbstractDashboardIntegrationTest {

    private FusionClientService fusion;
    private CustomerDocumentService documents;
    private DonneesPersonnellesClientService donnees;
    private SalesRepository sales;
    private TransactionIdGeneratorService transactionIds;
    private CashRegister caisse;

    @BeforeEach
    void preparer() {
        LogsService logs = mock(LogsService.class);
        StorageService storage = mock(StorageService.class);
        when(storage.getUser()).thenReturn(utilisateur);
        AppConfigurationService configuration = mock(AppConfigurationService.class);
        when(configuration.getDevise()).thenReturn("FCFA");
        sales = IntegrationPostgresDatabase.bean(SalesRepository.class);
        transactionIds = new TransactionIdGeneratorService(em);

        fusion = new FusionClientService(em, logs);
        documents = new CustomerDocumentService(em, templateEngine(), storage, configuration);
        DossierSanteService dossierSante = new DossierSanteService(
            null,
            IntegrationPostgresDatabase.bean(CustomerDossierSanteRepository.class),
            IntegrationPostgresDatabase.bean(CustomerAllergieRepository.class),
            null,
            null,
            em
        );
        donnees = new DonneesPersonnellesClientService(em, dossierSante, documents, sales, logs);
        caisse = caisseOuverte(0);
    }

    // ── Doublons ──

    @Test
    @DisplayName("prénom et nom inversés, même téléphone : les deux fiches forment un groupe")
    void doublonsParNom() {
        String nom = unique("KOUASSI");
        UninsuredCustomer a = client(nom, "Awa", "+225 0700000041");
        UninsuredCustomer b = client("Awa", nom, "0700000041");
        UninsuredCustomer autre = client(nom, "Yao", null);

        DoublonClientGroupeDTO groupe = groupeDe(a);

        assertThat(groupe.clients()).extracting(DoublonClientDTO::id).containsExactlyInAnyOrder(a.getId(), b.getId());
        assertThat(groupe.criteres()).containsExactly("Même nom", "Même téléphone");
        assertThat(groupeDe(autre)).isNull();
    }

    @Test
    @DisplayName("même téléphone et même date de naissance, nom mal orthographié : doublon")
    void doublonsParTelephoneEtNaissance() {
        LocalDate naissance = LocalDate.of(1985, 3, 12);
        AssuredCustomer a = assure(unique("KONAN"), "0700000042", naissance);
        AssuredCustomer b = assure(unique("KONANN"), "0700000042", naissance);

        assertThat(groupeDe(a).clients()).extracting(DoublonClientDTO::id).containsExactlyInAnyOrder(a.getId(), b.getId());
    }

    // ── Fusion ──

    @Test
    @DisplayName("la fusion rattache ventes, règlements et relances à la fiche conservée, et désactive la source")
    void fusionStandard() {
        UninsuredCustomer cible = client(unique("FUSION"), "Awa", "0700000043");
        UninsuredCustomer source = client(unique("FUSION"), "Awa", null);
        source.setEmail("awa@exemple.ci");
        CashSale vente = venteDifferee(caisse, 10_000, 8_000, source, aujourdHui());
        reglement(vente, 3_000, aujourdHui());
        em.persist(new RelanceDiffere().setCustomerId(source.getId()).setTelephone("0700000043").setMontant(5_000).setMessage("relance").setUser(utilisateur));
        em.flush();

        FusionClientApercuDTO apercu = fusion.apercu(cible.getId(), List.of(source.getId()));
        assertThat(apercu.rejets()).isEmpty();
        assertThat(apercu.counts()).containsEntry("ventes", 1).containsEntry("reglementsDifferes", 1);

        fusion.fusionner(new FusionClientRequestDTO(cible.getId(), List.of(source.getId())));

        assertThat(sales.getDiffereSoldeByCustomerId(cible.getId())).isEqualByComparingTo("5000");
        assertThat(sales.getDiffereSoldeByCustomerId(source.getId())).isNull();
        Customer conservee = em.find(Customer.class, cible.getId());
        assertThat(conservee.getEmail()).isEqualTo("awa@exemple.ci");
        assertThat(conservee.getPhone()).isEqualTo("0700000043");
        assertThat(em.find(Customer.class, source.getId()).getStatus()).isEqualTo(Status.DISABLE);
        assertThat(
            em.createQuery("SELECT COUNT(r) FROM RelanceDiffere r WHERE r.customerId = :id", Long.class).setParameter("id", cible.getId()).getSingleResult()
        ).isEqualTo(1L);
        assertThat(
            em
                .createQuery("SELECT COUNT(p) FROM DifferePayment p WHERE p.differeCustomer.id = :id", Long.class)
                .setParameter("id", cible.getId())
                .getSingleResult()
        ).isEqualTo(1L);
    }

    @Test
    @DisplayName("assurés : un même organisme se fond en un tiers payant, un autre change de fiche à une priorité libre")
    void fusionTiersPayants() {
        TiersPayant mugef = tiersPayant("MUGEF");
        TiersPayant ascoma = tiersPayant("ASCOMA");
        AssuredCustomer cible = assure(unique("TP"), null, null);
        AssuredCustomer source = assure(unique("TP"), null, LocalDate.of(1990, 1, 1));
        compte(cible, mugef, PrioriteTiersPayant.R0, 1_000L, null);
        compte(source, mugef, PrioriteTiersPayant.R0, 500L, LocalDate.of(2030, 1, 1));
        compte(source, ascoma, PrioriteTiersPayant.R0, null, null);
        AssuredCustomer ayantDroit = assure(unique("AD"), null, null);
        ayantDroit.setTypeAssure(TypeAssure.AYANT_DROIT);
        ayantDroit.setAssurePrincipal(source);
        em.flush();

        fusion.fusionner(new FusionClientRequestDTO(cible.getId(), List.of(source.getId())));

        List<ClientTiersPayant> comptes = em
            .createQuery("SELECT c FROM ClientTiersPayant c WHERE c.assuredCustomer.id = :id ORDER BY c.priorite", ClientTiersPayant.class)
            .setParameter("id", cible.getId())
            .getResultList();
        assertThat(comptes).hasSize(2);
        assertThat(comptes.get(0).getTiersPayant().getId()).isEqualTo(mugef.getId());
        assertThat(comptes.get(0).getConsoMensuelle()).isEqualTo(1_500L);
        assertThat(comptes.get(0).getDateFinValidite()).isEqualTo(LocalDate.of(2030, 1, 1));
        assertThat(comptes.get(1).getTiersPayant().getId()).isEqualTo(ascoma.getId());
        assertThat(comptes.get(1).getPriorite()).isEqualTo(PrioriteTiersPayant.R1);
        assertThat(em.find(AssuredCustomer.class, ayantDroit.getId()).getAssurePrincipal().getId()).isEqualTo(cible.getId());
        assertThat(em.find(AssuredCustomer.class, cible.getId()).getDatNaiss()).isEqualTo(LocalDate.of(1990, 1, 1));
    }

    @Test
    @DisplayName("allergies réunies sans doublon, dossier santé repris par la fiche conservée")
    void fusionDossierSante() {
        UninsuredCustomer cible = client(unique("SANTE"), "Awa", null);
        UninsuredCustomer source = client(unique("SANTE"), "Awa", null);
        em.persist(new CustomerAllergie().setCustomerId(cible.getId()).setLibelle("Pénicilline"));
        em.persist(new CustomerAllergie().setCustomerId(source.getId()).setLibelle("PENICILLINE"));
        em.persist(new CustomerAllergie().setCustomerId(source.getId()).setLibelle("Aspirine"));
        em.persist(new CustomerDossierSante().setCustomerId(source.getId()).setPathologies(List.of("Diabète")).setNote("suivi"));
        em.flush();

        fusion.fusionner(new FusionClientRequestDTO(cible.getId(), List.of(source.getId())));

        assertThat(
            em.createQuery("SELECT a.libelle FROM CustomerAllergie a WHERE a.customerId = :id", String.class).setParameter("id", cible.getId()).getResultList()
        ).containsExactlyInAnyOrder("Pénicilline", "Aspirine");
        CustomerDossierSante dossier = em.find(CustomerDossierSante.class, cible.getId());
        assertThat(dossier.getPathologies()).containsExactly("Diabète");
        assertThat(em.find(CustomerDossierSante.class, source.getId())).isNull();
    }

    @Test
    @DisplayName("un assuré ne se fusionne pas avec un client standard ; une cible désactivée est refusée")
    void fusionRefusee() {
        UninsuredCustomer standard = client(unique("REFUS"), "Awa", null);
        AssuredCustomer assure = assure(unique("REFUS"), null, null);

        FusionClientApercuDTO apercu = fusion.apercu(standard.getId(), List.of(assure.getId()));
        assertThat(apercu.rejets()).containsKey(assure.getId());
        assertThat(apercu.sourceIds()).isEmpty();
        assertThatThrownBy(() -> fusion.fusionner(new FusionClientRequestDTO(standard.getId(), List.of(assure.getId()))))
            .isInstanceOf(GenericError.class)
            .extracting("errorKey")
            .isEqualTo("fusionClientRejetee");

        standard.setStatus(Status.DISABLE);
        em.flush();
        UninsuredCustomer autre = client(unique("REFUS"), "Awa", null);
        assertThatThrownBy(() -> fusion.apercu(standard.getId(), List.of(autre.getId())))
            .isInstanceOf(GenericError.class)
            .extracting("errorKey")
            .isEqualTo("fusionCibleInactive");
    }

    // ── Documents ──

    @Test
    @DisplayName("relevé : solde de début, ventes au débit, règlements au crédit ; le solde final est l'encours")
    void releve() {
        LocalDate jour = aujourdHui();
        UninsuredCustomer client = client(unique("RELEVE"), "Awa", null);
        CashSale ancienne = venteDifferee(caisse, 10_000, 8_000, client, jour.minusDays(40));
        venteDifferee(caisse, 5_000, 5_000, client, jour.minusDays(5));
        reglement(ancienne, 3_000, jour.minusDays(2));

        ReleveCompteDTO releve = documents.releve(client.getId(), jour.minusDays(10), jour);

        assertThat(releve.soldeInitial()).isEqualTo(8_000);
        assertThat(releve.lignes()).extracting(LigneReleveDTO::debit).containsExactly(5_000L, 0L);
        assertThat(releve.lignes()).extracting(LigneReleveDTO::credit).containsExactly(0L, 3_000L);
        assertThat(releve.lignes()).extracting(LigneReleveDTO::solde).containsExactly(13_000L, 10_000L);
        assertThat(releve.soldeFinal()).isEqualTo(sales.getDiffereSoldeByCustomerId(client.getId()).longValue());
    }

    @Test
    @DisplayName("attestation : achats de la période avec leurs produits ; les ventes annulées n'y figurent pas")
    void attestation() {
        LocalDate jour = aujourdHui();
        UninsuredCustomer client = client(unique("ATTEST"), "Awa", null);
        Produit doliprane = produit(unique("DOLIPRANE"), 0);
        CashSale vente = venteDifferee(caisse, 3_000, 0, client, jour);
        ligneDeVente(vente, doliprane, 2);
        CashSale annulee = venteDifferee(caisse, 9_000, 0, client, jour);
        annulee.setCanceled(true);
        venteDifferee(caisse, 7_000, 0, client, jour.minusMonths(2));
        em.flush();

        AttestationDepensesDTO attestation = documents.attestation(client.getId(), jour.minusDays(7), jour);

        assertThat(attestation.depenses()).hasSize(1);
        assertThat(attestation.depenses().getFirst().produits()).singleElement().satisfies(p -> {
            assertThat(p.libelle()).isEqualTo(doliprane.getLibelle());
            assertThat(p.quantite()).isEqualTo(2);
        });
        assertThat(attestation.total()).isEqualTo(3_000);
        assertThat(attestation.totalClient()).isEqualTo(3_000);
    }

    @Test
    @DisplayName("les deux documents sortent en PDF")
    void pdf() {
        LocalDate jour = aujourdHui();
        UninsuredCustomer client = client(unique("PDF"), "Awa", null);
        venteDifferee(caisse, 5_000, 5_000, client, jour);

        for (byte[] pdf : List.of(documents.relevePdf(client.getId(), jour.minusDays(30), jour), documents.attestationPdf(client.getId(), jour.minusDays(30), jour))) {
            assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        }
    }

    // ── Données personnelles ──

    @Test
    @DisplayName("l'export reprend identité, achats et compte")
    void export() {
        UninsuredCustomer client = client(unique("EXPORT"), "Awa", "0700000044");
        venteDifferee(caisse, 5_000, 2_000, client, aujourdHui());

        DonneesClientDTO export = donnees.exporter(client.getId());

        assertThat(export.identite().telephone()).isEqualTo("0700000044");
        assertThat(export.achats()).hasSize(1);
        assertThat(export.compte()).extracting(LigneReleveDTO::debit).containsExactly(2_000L);
    }

    @Test
    @DisplayName("l'anonymisation est refusée tant qu'un différé reste dû, puis efface l'identité et le dossier santé")
    void anonymisation() {
        UninsuredCustomer client = client(unique("ANONYME"), "Awa", "0700000045");
        CashSale vente = venteDifferee(caisse, 5_000, 2_000, client, aujourdHui());
        em.persist(new CustomerAllergie().setCustomerId(client.getId()).setLibelle("Aspirine"));
        em.flush();

        assertThatThrownBy(() -> donnees.anonymiser(client.getId())).isInstanceOf(GenericError.class).extracting("errorKey").isEqualTo("anonymisationAvecEncours");

        reglement(vente, 2_000, aujourdHui());
        donnees.anonymiser(client.getId());
        viderLeCache();

        Customer anonyme = em.find(Customer.class, client.getId());
        assertThat(anonyme.getFirstName()).isEqualTo("ANONYME");
        assertThat(anonyme.getLastName()).isEqualTo(anonyme.getCode());
        assertThat(anonyme.getPhone()).isNull();
        assertThat(anonyme.getStatus()).isEqualTo(Status.DISABLE);
        assertThat(
            em.createQuery("SELECT COUNT(a) FROM CustomerAllergie a WHERE a.customerId = :id", Long.class).setParameter("id", client.getId()).getSingleResult()
        ).isZero();
        // Les délivrances restent rattachées à la fiche : seule l'identité disparaît.
        assertThat(
            em.createQuery("SELECT COUNT(s) FROM Sales s WHERE s.customer.id = :id", Long.class).setParameter("id", client.getId()).getSingleResult()
        ).isEqualTo(1L);
    }

    // ===== fixtures =====

    private DoublonClientGroupeDTO groupeDe(Customer client) {
        return fusion.doublons().stream().filter(g -> g.clients().stream().anyMatch(c -> c.id().equals(client.getId()))).findFirst().orElse(null);
    }

    private AssuredCustomer assure(String nom, String telephone, LocalDate naissance) {
        AssuredCustomer assure = new AssuredCustomer();
        assure.setFirstName("Assuré");
        assure.setLastName(nom);
        assure.setCode(unique("ASS"));
        assure.setPhone(telephone);
        assure.setDatNaiss(naissance);
        assure.setTypeAssure(TypeAssure.PRINCIPAL);
        assure.setStatus(Status.ENABLE);
        assure.setCreatedAt(LocalDateTime.now());
        assure.setUpdatedAt(LocalDateTime.now());
        em.persist(assure);
        em.flush();
        return assure;
    }

    private TiersPayant tiersPayant(String nom) {
        TiersPayant tiersPayant = new TiersPayant();
        tiersPayant.setName(unique(nom));
        tiersPayant.setFullName(nom);
        tiersPayant.setStatut(TiersPayantStatut.ACTIF);
        tiersPayant.setCategorie(TiersPayantCategorie.ASSURANCE);
        tiersPayant.setCreated(LocalDateTime.now());
        tiersPayant.setUpdated(LocalDateTime.now());
        tiersPayant.setUser(utilisateur);
        em.persist(tiersPayant);
        em.flush();
        return tiersPayant;
    }

    private void compte(AssuredCustomer assure, TiersPayant tiersPayant, PrioriteTiersPayant priorite, Long conso, LocalDate finValidite) {
        ClientTiersPayant compte = new ClientTiersPayant();
        compte.setTiersPayant(tiersPayant);
        compte.setAssuredCustomer(assure);
        compte.setNum(unique("NUM"));
        compte.setTaux(80);
        compte.setPriorite(priorite);
        compte.setStatut(TiersPayantStatut.ACTIF);
        compte.setConsoMensuelle(conso);
        compte.setDateFinValidite(finValidite);
        compte.setCreated(LocalDateTime.now());
        compte.setUpdated(LocalDateTime.now());
        em.persist(compte);
        em.flush();
    }

    private void reglement(Sales vente, int montant, LocalDate date) {
        DifferePayment paiement = new DifferePayment();
        paiement.setId(transactionIds.nextId());
        paiement.setTransactionDate(date);
        paiement.setCashRegister(caisse);
        paiement.setPaymentMode(modePaiement("CASH"));
        paiement.setPaidAmount(montant);
        paiement.setExpectedAmount(montant);
        paiement.setMontantVerse(montant);
        paiement.setReelAmount(montant);
        paiement.setCreatedAt(date.atTime(18, 0));
        paiement.setCategorieChiffreAffaire(CategorieChiffreAffaire.CA);
        paiement.setTypeFinancialTransaction(TypeFinancialTransaction.REGLEMENT_DIFFERE);
        paiement.setDiffereCustomer(vente.getCustomer());
        em.persist(paiement);

        DifferePaymentItem item = new DifferePaymentItem();
        item.setSale(vente);
        item.setDifferePayment(paiement);
        item.setPaidAmount(montant);
        item.setExpectedAmount(montant);
        em.persist(item);

        vente.setRestToPay(vente.getRestToPay() - montant);
        em.flush();
    }

    private static SpringTemplateEngine templateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        resolver.setTemplateMode("HTML");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}
