package com.kobe.warehouse.service.customer.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.Dci;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.ProduitDci;
import com.kobe.warehouse.domain.UninsuredCustomer;
import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.domain.enumeration.StatutDci;
import com.kobe.warehouse.domain.enumeration.StatutLegal;
import com.kobe.warehouse.repository.CustomerTraitementChroniqueRepository;
import com.kobe.warehouse.service.customer.DerogationAuthorizer;
import com.kobe.warehouse.service.customer.TraitementARenouvelerDTO;
import com.kobe.warehouse.service.customer.TraitementChroniqueDTO;
import com.kobe.warehouse.service.customer.TraitementChroniqueDTO.Ordonnance;
import com.kobe.warehouse.service.customer.TraitementChroniqueDTO.Suivi;
import com.kobe.warehouse.service.customer.TraitementChroniqueSaisieDTO;
import com.kobe.warehouse.service.customer.TraitementChroniqueService;
import com.kobe.warehouse.service.dashboard.integration.AbstractDashboardIntegrationTest;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Fiche client, lot 4 (docs/PLAN-FICHE-CLIENT.md) : traitements chroniques déclarés par molécule,
 * échéance de renouvellement, et renouvellement exceptionnel après expiration de l'ordonnance.
 */
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Fiche client — traitements chroniques")
class TraitementChroniqueIntegrationTest extends AbstractDashboardIntegrationTest {

    private final AppConfigurationService configuration = mock(AppConfigurationService.class);
    private TraitementChroniqueService service;
    private CashRegister caisse;
    private UninsuredCustomer patient;
    private LocalDate jour;
    private Dci metformine;
    private Produit princeps;
    private Produit generique;

    @BeforeEach
    void preparer() {
        DerogationAuthorizer authorizer = mock(DerogationAuthorizer.class);
        when(authorizer.utilisateurCourant()).thenReturn(utilisateur);
        when(configuration.getRappelRenouvellementJours()).thenReturn(5);
        when(configuration.getRenouvellementExceptionnelMois()).thenReturn(3);
        service = new TraitementChroniqueService(IntegrationPostgresDatabase.bean(CustomerTraitementChroniqueRepository.class), em, configuration, authorizer);

        caisse = caisseOuverte(0);
        jour = aujourdHui();
        patient = client(unique("CHRONIQUE"), "Awa", "0700000050");
        metformine = dci(unique("METFORMINE"));
        princeps = produitAvec(metformine);
        generique = produitAvec(metformine);
    }

    @Test
    @DisplayName("déclaré par molécule, le générique renouvelle le traitement ; l'échéance proche le signale")
    void parMolecule() {
        TraitementChroniqueDTO traitement = service.declarer(patient.getId(), saisie(metformine, null, 30, null, null));
        assertThat(traitement.suivi()).isEqualTo(Suivi.SANS_DELIVRANCE);

        delivrer(princeps, jour.minusDays(40));
        delivrer(generique, jour.minusDays(27));

        TraitementChroniqueDTO suivi = service.traitements(patient.getId()).getFirst();
        assertThat(suivi.derniereDelivrance()).isEqualTo(jour.minusDays(27));
        assertThat(suivi.dernierProduitId()).isEqualTo(generique.getId());
        assertThat(suivi.prochaineDelivrance()).isEqualTo(jour.plusDays(3));
        assertThat(suivi.suivi()).isEqualTo(Suivi.A_RENOUVELER);
        assertThat(service.aRenouveler()).extracting(TraitementARenouvelerDTO::customerId).contains(patient.getId());
    }

    @Test
    @DisplayName("un produit imposé ne se renouvelle que par lui-même")
    void produitImpose() {
        service.declarer(patient.getId(), saisie(metformine, princeps, 30, null, null));
        delivrer(generique, jour.minusDays(2));

        assertThat(service.traitements(patient.getId()).getFirst().suivi()).isEqualTo(Suivi.SANS_DELIVRANCE);

        delivrer(princeps, jour.minusDays(1));
        assertThat(service.traitements(patient.getId()).getFirst().suivi()).isEqualTo(Suivi.A_JOUR);
    }

    @Test
    @DisplayName("en retard au-delà de l'échéance, en rupture après un cycle manqué ; arrêté, il n'est plus suivi")
    void retardEtRupture() {
        TraitementChroniqueDTO traitement = service.declarer(patient.getId(), saisie(metformine, null, 30, null, null));
        delivrer(generique, jour.minusDays(40));
        assertThat(service.traitements(patient.getId()).getFirst().suivi()).isEqualTo(Suivi.EN_RETARD);

        UninsuredCustomer autre = client(unique("CHRONIQUE"), "Yao", null);
        service.declarer(autre.getId(), saisie(metformine, null, 30, null, null));
        delivrerA(autre, generique, jour.minusDays(70));
        assertThat(service.traitements(autre.getId()).getFirst().suivi()).isEqualTo(Suivi.RUPTURE);

        service.modifier(patient.getId(), traitement.id(), arret(saisie(metformine, null, 30, null, null)));
        em.flush();
        assertThat(service.traitements(patient.getId()).getFirst().suivi()).isEqualTo(Suivi.ARRETE);
        assertThat(service.aRenouveler()).extracting(TraitementARenouvelerDTO::customerId).doesNotContain(patient.getId());
    }

    @Test
    @DisplayName("ordonnance expirée : renouvellement exceptionnel signalé seulement si le paramètre l'autorise")
    void renouvellementExceptionnel() {
        LocalDate fin = jour.minusDays(10);
        service.declarer(patient.getId(), saisie(metformine, null, 30, fin.minusMonths(6), fin));

        TraitementChroniqueDTO sansParametre = service.traitements(patient.getId()).getFirst();
        assertThat(sansParametre.ordonnance()).isEqualTo(Ordonnance.EXPIREE);
        assertThat(sansParametre.renouvellementExceptionnelJusquau()).isNull();
        assertThat(sansParametre.renouvellementExceptionnelMotif()).isNull();

        when(configuration.isRenouvellementExceptionnelEnabled()).thenReturn(true);
        TraitementChroniqueDTO avecParametre = service.traitements(patient.getId()).getFirst();
        assertThat(avecParametre.renouvellementExceptionnelJusquau()).isEqualTo(fin.plusMonths(3));
    }

    @Test
    @DisplayName("renouvellement exceptionnel refusé : ordonnance courte, stupéfiant, ou mois suivant l'expiration passé sans délivrance")
    void renouvellementExceptionnelRefuse() {
        when(configuration.isRenouvellementExceptionnelEnabled()).thenReturn(true);

        service.declarer(patient.getId(), saisie(metformine, null, 30, jour.minusMonths(2), jour.minusDays(1)));
        assertThat(service.traitements(patient.getId()).getFirst().renouvellementExceptionnelMotif()).contains("moins de 3 mois");

        UninsuredCustomer tardif = client(unique("CHRONIQUE"), "Yao", null);
        LocalDate fin = jour.minusDays(45);
        service.declarer(tardif.getId(), saisie(metformine, null, 30, fin.minusMonths(6), fin));
        assertThat(service.traitements(tardif.getId()).getFirst().renouvellementExceptionnelMotif()).contains("mois qui a suivi");

        UninsuredCustomer sousStupefiant = client(unique("CHRONIQUE"), "Koffi", null);
        Produit morphine = produitAvec(dci(unique("MORPHINE")));
        morphine.setStatutLegal(StatutLegal.STUPEFIANTS);
        em.flush();
        service.declarer(sousStupefiant.getId(), saisie(null, morphine, 30, jour.minusMonths(6), jour.minusDays(5)));
        assertThat(service.traitements(sousStupefiant.getId()).getFirst().renouvellementExceptionnelMotif()).contains("Stupéfiant");
    }

    @Test
    @DisplayName("un produit imposé doit contenir la molécule déclarée")
    void produitSansLaMolecule() {
        Produit autre = produitAvec(dci(unique("AMLODIPINE")));

        assertThatThrownBy(() -> service.declarer(patient.getId(), saisie(metformine, autre, 30, null, null)))
            .isInstanceOf(GenericError.class)
            .extracting("errorKey")
            .isEqualTo("traitementProduitSansMolecule");
    }

    // ===== fixtures =====

    private static TraitementChroniqueSaisieDTO saisie(Dci dci, Produit produit, int duree, LocalDate ordonnance, LocalDate fin) {
        return new TraitementChroniqueSaisieDTO(
            dci == null ? null : dci.getId(),
            produit == null ? null : produit.getId(),
            "850 mg",
            "1 cp matin et soir",
            duree,
            ordonnance,
            fin,
            null,
            true
        );
    }

    private static TraitementChroniqueSaisieDTO arret(TraitementChroniqueSaisieDTO s) {
        return new TraitementChroniqueSaisieDTO(s.dciId(), s.produitId(), s.dosage(), s.posologie(), s.dureeJours(), s.dateOrdonnance(), s.dateFinOrdonnance(), s.note(), false);
    }

    private void delivrer(Produit produit, LocalDate date) {
        delivrerA(patient, produit, date);
    }

    private void delivrerA(UninsuredCustomer client, Produit produit, LocalDate date) {
        CashSale vente = vente(caisse, NatureVente.COMPTANT, 1_000, 0, false, client, date);
        ligneDeVente(vente, produit, 1);
    }

    private Dci dci(String libelle) {
        Dci dci = new Dci();
        dci.setCode(unique("DCI"));
        dci.setLibelle(libelle);
        dci.setStatut(StatutDci.ACTIVE);
        em.persist(dci);
        return dci;
    }

    private Produit produitAvec(Dci dci) {
        Produit produit = produit(unique("PRODUIT"), 0);
        em.persist(new ProduitDci().setProduit(produit).setDci(dci).setRang(1));
        em.flush();
        return produit;
    }
}
