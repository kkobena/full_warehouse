package com.kobe.warehouse.service.ordonnance.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.Dci;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.ProduitDci;
import com.kobe.warehouse.domain.RetourClient;
import com.kobe.warehouse.domain.RetourClientLine;
import com.kobe.warehouse.domain.enumeration.MotifRetourClient;
import com.kobe.warehouse.domain.SalesLine;
import com.kobe.warehouse.domain.UninsuredCustomer;
import com.kobe.warehouse.domain.enumeration.NatureVente;
import com.kobe.warehouse.domain.enumeration.StatutDci;
import com.kobe.warehouse.domain.enumeration.StatutOrdonnance;
import com.kobe.warehouse.domain.pharmacovigilance.Interaction;
import com.kobe.warehouse.domain.pharmacovigilance.NiveauInteraction;
import com.kobe.warehouse.domain.pharmacovigilance.ReferentielInteractionVersion;
import com.kobe.warehouse.repository.AlerteSanteDerogationRepository;
import com.kobe.warehouse.repository.AppConfigurationRepository;
import com.kobe.warehouse.repository.ClasseInteractionRepository;
import com.kobe.warehouse.repository.ContreIndicationRepository;
import com.kobe.warehouse.repository.CustomerDossierSanteRepository;
import com.kobe.warehouse.repository.CustomerRepository;
import com.kobe.warehouse.repository.CustomerTraitementChroniqueRepository;
import com.kobe.warehouse.repository.DciClasseRepository;
import com.kobe.warehouse.repository.InteractionRepository;
import com.kobe.warehouse.repository.OrdonnanceDelivranceRepository;
import com.kobe.warehouse.repository.OrdonnanceRepository;
import com.kobe.warehouse.repository.OrdonnanceVenteRepository;
import com.kobe.warehouse.repository.PrescripteurRepository;
import com.kobe.warehouse.repository.ProduitDciRepository;
import com.kobe.warehouse.repository.ProduitRefSpecialiteRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.RefDciRepository;
import com.kobe.warehouse.repository.ReferentielInteractionVersionRepository;
import com.kobe.warehouse.repository.SalesLineRepository;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.repository.VentePrescripteurRepository;
import com.kobe.warehouse.repository.OrdonnanceVenteRepository;
import com.kobe.warehouse.service.ordonnance.OrdonnanceObligatoireService;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import org.springframework.cache.CacheManager;
import com.kobe.warehouse.service.customer.DerogationAuthorizer;
import com.kobe.warehouse.service.dashboard.integration.AbstractDashboardIntegrationTest;
import com.kobe.warehouse.service.dto.controle.AlerteControleDTO;
import com.kobe.warehouse.service.dto.controle.ControleResultatDTO;
import com.kobe.warehouse.service.dto.ordonnance.OrdonnanceCreationDTO;
import com.kobe.warehouse.service.dto.ordonnance.OrdonnanceDTO;
import com.kobe.warehouse.service.dto.ordonnance.OrdonnanceLigneCreationDTO;
import com.kobe.warehouse.service.dto.ordonnance.PrescripteurDTO;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.ordonnance.OrdonnanceService;
import com.kobe.warehouse.service.ordonnance.PrescripteurService;
import com.kobe.warehouse.service.ordonnance.controle.ControleOrdonnanceService;
import com.kobe.warehouse.service.ordonnance.controle.MoleculesProduitService;
import com.kobe.warehouse.service.ordonnance.controle.TraitementsEnCoursService;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Lots 2 et 3 du plan d'ordonnance : les dépôts Spring Data (JPQL, fonctions SQL appelées par
 * {@code function()}) et les services, contre la vraie base.
 */
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Ordonnance — suivi et contrôle")
class OrdonnanceControleIntegrationTest extends AbstractDashboardIntegrationTest {

    private PrescripteurService prescripteurService;
    private OrdonnanceService ordonnanceService;
    private MoleculesProduitService moleculesProduitService;
    private ControleOrdonnanceService controleService;
    private UninsuredCustomer client;

    @BeforeEach
    void preparer() {
        UserService userService = mock(UserService.class);
        when(userService.getUser()).thenReturn(utilisateur);
        var ordonnanceRepository = IntegrationPostgresDatabase.bean(OrdonnanceRepository.class);
        var produitRepository = IntegrationPostgresDatabase.bean(ProduitRepository.class);
        var customerRepository = IntegrationPostgresDatabase.bean(CustomerRepository.class);

        prescripteurService = new PrescripteurService(IntegrationPostgresDatabase.bean(PrescripteurRepository.class), ordonnanceRepository, userService);
        ordonnanceService = new OrdonnanceService(
            ordonnanceRepository,
            IntegrationPostgresDatabase.bean(OrdonnanceDelivranceRepository.class),
            IntegrationPostgresDatabase.bean(OrdonnanceVenteRepository.class),
            IntegrationPostgresDatabase.bean(PrescripteurRepository.class),
            customerRepository,
            produitRepository,
            IntegrationPostgresDatabase.bean(SalesRepository.class),
            IntegrationPostgresDatabase.bean(SalesLineRepository.class),
            IntegrationPostgresDatabase.bean(ProduitRefSpecialiteRepository.class),
            IntegrationPostgresDatabase.bean(CustomerTraitementChroniqueRepository.class),
            IntegrationPostgresDatabase.bean(ProduitDciRepository.class),
            IntegrationPostgresDatabase.bean(VentePrescripteurRepository.class),
            userService
        );
        moleculesProduitService = new MoleculesProduitService(
            IntegrationPostgresDatabase.bean(ProduitDciRepository.class),
            IntegrationPostgresDatabase.bean(ProduitRefSpecialiteRepository.class),
            produitRepository
        );
        var traitementsEnCours = new TraitementsEnCoursService(
            IntegrationPostgresDatabase.bean(SalesLineRepository.class),
            IntegrationPostgresDatabase.bean(CustomerTraitementChroniqueRepository.class),
            IntegrationPostgresDatabase.bean(RefDciRepository.class),
            moleculesProduitService,
            90
        );
        controleService = new ControleOrdonnanceService(
            moleculesProduitService,
            traitementsEnCours,
            IntegrationPostgresDatabase.bean(InteractionRepository.class),
            IntegrationPostgresDatabase.bean(ContreIndicationRepository.class),
            IntegrationPostgresDatabase.bean(DciClasseRepository.class),
            IntegrationPostgresDatabase.bean(ClasseInteractionRepository.class),
            IntegrationPostgresDatabase.bean(ReferentielInteractionVersionRepository.class),
            customerRepository,
            IntegrationPostgresDatabase.bean(CustomerDossierSanteRepository.class),
            IntegrationPostgresDatabase.bean(AlerteSanteDerogationRepository.class),
            mock(DerogationAuthorizer.class),
            new AppConfigurationService(userService, IntegrationPostgresDatabase.bean(AppConfigurationRepository.class), new org.springframework.beans.factory.support.StaticListableBeanFactory().getBeanProvider(CacheManager.class)),
            em
        );
        client = client(unique("ORDO"), "Awa", "0700000030");
    }

    // ── Prescripteurs ──

    @Test
    @DisplayName("la recherche ignore accents et casse, et la fusion déplace les ordonnances")
    void prescripteurs() {
        PrescripteurDTO kone = prescripteurService.creer(new PrescripteurDTO(null, "KONÉ", "Amadou", "Généraliste", "CI-" + unique("N"), null, null, true));
        PrescripteurDTO doublon = prescripteurService.creer(new PrescripteurDTO(null, "Kone", "A.", null, null, null, null, true));
        em.flush();

        assertThat(prescripteurService.rechercher("kone", 10)).extracting(PrescripteurDTO::id).contains(kone.id(), doublon.id());
        assertThat(prescripteurService.rechercher("Koné", 10)).extracting(PrescripteurDTO::id).contains(kone.id());
        assertThat(prescripteurService.rechercher("", 5)).isNotEmpty();

        assertThatThrownBy(() -> prescripteurService.creer(new PrescripteurDTO(null, "Autre", null, null, kone.numeroOrdre(), null, null, true)))
            .isInstanceOf(GenericError.class);

        Produit produit = produit(unique("P"), 0);
        OrdonnanceDTO ordonnance = ordonnanceService.creer(creation(doublon.id(), produit, 1, 0));
        em.flush();
        prescripteurService.fusionner(doublon.id(), kone.id());

        assertThat(ordonnanceService.detail(ordonnance.id()).prescripteurId()).isEqualTo(kone.id());
        assertThat(prescripteurService.lire(doublon.id()).actif()).isFalse();
        assertThat(prescripteurService.rechercher("Kone", 10)).extracting(PrescripteurDTO::id).doesNotContain(doublon.id());
    }

    // ── Suivi ──

    @Test
    @DisplayName("seule une vente clôturée et non annulée consomme l'ordonnance")
    void resteADelivrer() {
        Produit produit = produit(unique("P"), 0);
        OrdonnanceDTO ordonnance = ordonnanceService.creer(creation(null, produit, 2, 1));
        assertThat(ordonnance.lignes()).singleElement().satisfies(l -> assertThat(l.resteADelivrer()).isEqualTo(4));
        assertThat(ordonnance.statut()).isEqualTo(StatutOrdonnance.EN_COURS);
        assertThat(ordonnanceService.duClient(client.getId(), StatutOrdonnance.EN_COURS)).hasSize(1);

        CashSale vente = vente(caisseOuverte(0), NatureVente.COMPTANT, 1000, 0, false, client, LocalDate.now());
        SalesLine ligne = ligneDeVente(vente, produit, 1);
        Integer ligneId = ordonnance.lignes().get(0).id();

        // La ligne ne se lie qu'à une vente déjà rattachée.
        assertThatThrownBy(() -> ordonnanceService.lierLigneDeVente(ordonnance.id(), ligneId, ligne.getId().getId(), ligne.getSaleDate())).isInstanceOf(GenericError.class);

        ordonnanceService.rattacherVente(ordonnance.id(), vente.getId().getId(), vente.getSaleDate());
        OrdonnanceDTO apres = ordonnanceService.lierLigneDeVente(ordonnance.id(), ligneId, ligne.getId().getId(), ligne.getSaleDate());
        em.flush();
        assertThat(apres.lignes().get(0).quantiteDelivree()).isEqualTo(1);
        assertThat(apres.lignes().get(0).resteADelivrer()).isEqualTo(3);
        assertThat(apres.ventes()).hasSize(1);

        // Un retour client validé sur la ligne de vente rend sa quantité à l'ordonnance.
        RetourClient retour = new RetourClient()
            .setReference(unique("RET"))
            .setMotif(MotifRetourClient.ERREUR_QUANTITE)
            .setValidatedAt(java.time.LocalDateTime.now())
            .setCreatedBy(utilisateur);
        retour.setMontantTotal(0);
        em.persist(retour);
        em.persist(
            new RetourClientLine()
                .setRetourClient(retour)
                .setProduit(produit)
                .setQuantite(1)
                .setQuantiteInit(1)
                .setPrixUnitaire(0)
                .setMontant(0)
                .setOriginalSalesLineId(ligne.getId().getId())
                .setOriginalSalesLineDate(ligne.getSaleDate())
        );
        em.flush();
        em.clear();
        OrdonnanceDTO apresRetour = ordonnanceService.detail(ordonnance.id());
        assertThat(apresRetour.lignes().get(0).quantiteDelivree()).isZero();
        assertThat(apresRetour.lignes().get(0).resteADelivrer()).isEqualTo(4);

        // L'annulation de la vente rétablit le reste à délivrer, sans rien défaire.
        vente.setCanceled(true);
        em.flush();
        em.clear();
        assertThat(ordonnanceService.detail(ordonnance.id()).lignes().get(0).quantiteDelivree()).isZero();

        // Détacher la vente retire aussi ses délivrances.
        OrdonnanceDTO detachee = ordonnanceService.detacherVente(ordonnance.id(), vente.getId().getId(), vente.getSaleDate());
        assertThat(detachee.ventes()).isEmpty();
    }

    @Test
    @DisplayName("un générique du même groupe est lié à la ligne prescrite, un produit étranger ne l'est pas")
    void apparierUnGenerique() {
        Produit princeps = produit(unique("PRINCEPS"), 0);
        Produit generique = produit(unique("GENERIQUE"), 0);
        Produit etranger = produit(unique("ETRANGER"), 0);
        int groupe = 900_000 + Math.abs(unique("G").hashCode()) % 90_000;
        em.createNativeQuery("INSERT INTO ref_groupe_generique (id, libelle) VALUES (:id, 'GROUPE ESSAI')").setParameter("id", groupe).executeUpdate();
        retenir(princeps, groupe, "PRINCEPS");
        retenir(generique, groupe, "GENERIQUE");

        OrdonnanceDTO ordonnance = ordonnanceService.creer(creation(null, princeps, 2, 0));
        CashSale vente = vente(caisseOuverte(0), NatureVente.COMPTANT, 1000, 0, false, client, LocalDate.now());
        ligneDeVente(vente, generique, 1);
        ligneDeVente(vente, etranger, 1);
        em.flush();

        var resultat = ordonnanceService.apparierVente(ordonnance.id(), vente.getId().getId(), vente.getSaleDate());
        em.flush();
        assertThat(resultat.lignesLiees()).isEqualTo(1);
        assertThat(resultat.lignesGeneriques()).isEqualTo(1);
        assertThat(resultat.ordonnance().lignes().get(0).quantiteDelivree()).isEqualTo(1);
        assertThat(resultat.ordonnance().lignes().get(0).resteADelivrer()).isEqualTo(1);

        // Relançable : rien n'est lié deux fois.
        assertThat(ordonnanceService.apparierVente(ordonnance.id(), vente.getId().getId(), vente.getSaleDate()).lignesLiees()).isZero();
    }

    /** Spécialité retenue (AUTO) d'un produit dans un groupe générique ; le référentiel n'est pas chargé par les migrations. */
    private void retenir(Produit produit, int groupe, String type) {
        String cis = ("T" + Math.abs(unique("C").hashCode()) % 9_999_999);
        em
            .createNativeQuery(
                "INSERT INTO ref_specialite (cis, libelle, commercialisee, groupe_generique_id, type_generique) VALUES (:cis, :libelle, TRUE, :groupe, :type)"
            )
            .setParameter("cis", cis.substring(0, Math.min(8, cis.length())))
            .setParameter("libelle", produit.getLibelle())
            .setParameter("groupe", groupe)
            .setParameter("type", type)
            .executeUpdate();
        em
            .createNativeQuery("INSERT INTO produit_ref_specialite (produit_id, cis, statut, score, decision) VALUES (:p, :cis, 'SUR', 100, 'AUTO')")
            .setParameter("p", produit.getId())
            .setParameter("cis", cis.substring(0, Math.min(8, cis.length())))
            .executeUpdate();
    }

    @Test
    @DisplayName("rattacher une vente la déclare sur ordonnance ; une ordonnance plus récente renouvelle le traitement chronique couvert")
    void typeDePrescriptionEtTraitementChronique() {
        Produit produit = produit(unique("P"), 0);
        var traitement = IntegrationPostgresDatabase.bean(CustomerTraitementChroniqueRepository.class).save(
            new com.kobe.warehouse.domain.CustomerTraitementChronique()
                .setCustomerId(client.getId())
                .setProduit(produit)
                .setDureeJours(30)
                .setDateOrdonnance(LocalDate.now().minusMonths(6))
                .setDateFinOrdonnance(LocalDate.now().minusDays(1))
        );
        em.flush();
        OrdonnanceDTO ancienne = ordonnanceService.creer(
            new OrdonnanceCreationDTO(client.getId(), null, LocalDate.now().minusYears(2), 0, null, null, List.of(new OrdonnanceLigneCreationDTO(produit.getId(), null, null, null, 1)))
        );
        em.flush();
        em.clear();
        // Une ordonnance plus ancienne n'écrase rien.
        assertThat(IntegrationPostgresDatabase.bean(CustomerTraitementChroniqueRepository.class).findById(traitement.getId()).orElseThrow().getDateOrdonnance())
            .isEqualTo(LocalDate.now().minusMonths(6));

        OrdonnanceDTO recente = ordonnanceService.creer(
            new OrdonnanceCreationDTO(client.getId(), null, LocalDate.now(), 0, LocalDate.now().plusMonths(6), null, List.of(new OrdonnanceLigneCreationDTO(produit.getId(), null, null, null, 1)))
        );
        em.flush();
        em.clear();
        var renouvele = IntegrationPostgresDatabase.bean(CustomerTraitementChroniqueRepository.class).findById(traitement.getId()).orElseThrow();
        assertThat(renouvele.getDateOrdonnance()).isEqualTo(LocalDate.now());
        assertThat(renouvele.getDateFinOrdonnance()).isEqualTo(LocalDate.now().plusMonths(6));

        // Vente « conseil » rattachée : elle devient une vente sur ordonnance.
        CashSale vente = vente(caisseOuverte(0), NatureVente.COMPTANT, 1000, 0, false, client, LocalDate.now());
        vente.setTypePrescription(com.kobe.warehouse.domain.enumeration.TypePrescription.CONSEIL);
        em.flush();
        ordonnanceService.rattacherVente(recente.id(), vente.getId().getId(), vente.getSaleDate());
        em.flush();
        em.clear();
        assertThat(em.find(com.kobe.warehouse.domain.Sales.class, new com.kobe.warehouse.domain.SaleId(vente.getId().getId(), vente.getSaleDate())).getTypePrescription())
            .isEqualTo(com.kobe.warehouse.domain.enumeration.TypePrescription.PRESCRIPTION);
        assertThat(ancienne.id()).isNotNull();
    }

    @Test
    @DisplayName("un produit sur ordonnance ne se clôture qu'avec une ordonnance ou un prescripteur")
    void ordonnanceObligatoire() {
        Produit surOrdonnance = produit(unique("LISTE"), 0);
        surOrdonnance.setStatutLegal(com.kobe.warehouse.domain.enumeration.StatutLegal.LISTE_I);
        Produit libre = produit(unique("LIBRE"), 0);
        libre.setStatutLegal(com.kobe.warehouse.domain.enumeration.StatutLegal.SANS_LISTE);
        em.flush();

        var controle = new OrdonnanceObligatoireService(
            new AppConfigurationService(mock(UserService.class), IntegrationPostgresDatabase.bean(AppConfigurationRepository.class), new org.springframework.beans.factory.support.StaticListableBeanFactory().getBeanProvider(CacheManager.class)),
            IntegrationPostgresDatabase.bean(OrdonnanceVenteRepository.class),
            IntegrationPostgresDatabase.bean(VentePrescripteurRepository.class)
        );
        CashSale venteLibre = vente(caisseOuverte(0), NatureVente.COMPTANT, 1000, 0, false, client, LocalDate.now());
        ligneDeVente(venteLibre, libre, 1);
        em.refresh(venteLibre);
        controle.controlerCloture(venteLibre); // sans liste : rien à exiger

        CashSale vente = vente(caisseOuverte(0), NatureVente.COMPTANT, 1000, 0, false, client, LocalDate.now());
        ligneDeVente(vente, surOrdonnance, 1);
        em.refresh(vente);
        assertThatThrownBy(() -> controle.controlerCloture(vente))
            .isInstanceOfSatisfying(GenericError.class, e -> assertThat(e.getErrorKey()).isEqualTo(OrdonnanceObligatoireService.ERROR_KEY));

        // Un prescripteur déclaré lève l'exigence.
        PrescripteurDTO dr = prescripteurService.creer(new PrescripteurDTO(null, "Dr Obligatoire", null, null, null, null, null, true));
        ordonnanceService.definirPrescripteurDeVente(vente.getId().getId(), vente.getSaleDate(), dr.id());
        em.flush();
        controle.controlerCloture(vente);

        // Une ordonnance rattachée la lève aussi.
        CashSale autre = vente(caisseOuverte(0), NatureVente.COMPTANT, 1000, 0, false, client, LocalDate.now());
        ligneDeVente(autre, surOrdonnance, 1);
        em.refresh(autre);
        assertThatThrownBy(() -> controle.controlerCloture(autre)).isInstanceOf(GenericError.class);
        OrdonnanceDTO ordonnance = ordonnanceService.creer(creation(null, surOrdonnance, 1, 0));
        ordonnanceService.rattacherVente(ordonnance.id(), autre.getId().getId(), autre.getSaleDate());
        em.flush();
        controle.controlerCloture(autre);

        // Paramètre à 0 : plus d'exigence.
        CashSale sansExigence = vente(caisseOuverte(0), NatureVente.COMPTANT, 1000, 0, false, client, LocalDate.now());
        ligneDeVente(sansExigence, surOrdonnance, 1);
        em.refresh(sansExigence);
        var parametres = IntegrationPostgresDatabase.bean(AppConfigurationRepository.class);
        var parametre = parametres.findById(EntityConstant.APP_VENTE_ORDONNANCE_OBLIGATOIRE).orElseThrow();
        parametre.setValue("0");
        parametres.save(parametre);
        em.flush();
        controle.controlerCloture(sansExigence);
    }

    // ── Contrôle ──

    @Test
    @DisplayName("les molécules d'un produit passent par produit_dci et par le référentiel ; une CI du panier est levée")
    void controle() {
        Dci warfarineCatalogue = dci("WARFARINE");
        Dci ibuprofeneCatalogue = dci("IBUPROFENE");
        int warfarine = refDci("WARFARINE", warfarineCatalogue);
        int ibuprofene = refDci("IBUPROFENE", ibuprofeneCatalogue);
        Produit coumadine = produitAvec(warfarineCatalogue);
        Produit advil = produitAvec(ibuprofeneCatalogue);
        Produit sansMolecule = produit(unique("LOCAL"), 0);

        var molecules = moleculesProduitService.molecules(List.of(coumadine.getId(), advil.getId(), sansMolecule.getId()));
        assertThat(molecules).containsOnlyKeys(coumadine.getId(), advil.getId());
        assertThat(molecules.get(coumadine.getId())).singleElement().satisfies(m -> assertThat(m.refDciId()).isEqualTo(warfarine));

        // Sans référentiel publié : aucune interaction, et on le dit.
        ControleResultatDTO sansReferentiel = controleService.controler(client.getId(), List.of(coumadine.getId(), advil.getId()));
        assertThat(sansReferentiel.referentielPublie()).isFalse();
        assertThat(sansReferentiel.alertes()).isEmpty();

        var versions = IntegrationPostgresDatabase.bean(ReferentielInteractionVersionRepository.class);
        ReferentielInteractionVersion version = new ReferentielInteractionVersion("TEST", unique("v"), "essai");
        version.publier("essai");
        versions.save(version);
        IntegrationPostgresDatabase
            .bean(InteractionRepository.class)
            .save(new Interaction(version.getId(), warfarine, null, ibuprofene, null, NiveauInteraction.CI, "Risque hémorragique", "Contre-indiqué"));
        em.flush();

        ControleResultatDTO resultat = controleService.controler(client.getId(), List.of(coumadine.getId(), advil.getId(), sansMolecule.getId()));
        assertThat(resultat.referentielPublie()).isTrue();
        // Paramétrage livré : CI, AD et PE bloquent (1), l'APEC n'est qu'un avertissement (0).
        assertThat(resultat.alertes()).singleElement().satisfies(a -> {
            assertThat(a.bloquant()).isTrue();
            assertThat(a.exigeMotif()).isTrue();
        });

        // Le paramètre du niveau CI repassé à 0 : la même alerte n'est plus qu'un avertissement.
        var parametres = IntegrationPostgresDatabase.bean(AppConfigurationRepository.class);
        var parametreCi = parametres.findById(EntityConstant.APP_CONTROLE_ORDONNANCE_BLOQUANT_PREFIXE + "CI").orElseThrow();
        parametreCi.setValue("0");
        parametres.save(parametreCi);
        em.flush();
        ControleResultatDTO avertissement = controleService.controler(client.getId(), List.of(coumadine.getId(), advil.getId()));
        assertThat(avertissement.alertes()).singleElement().satisfies(a -> {
            assertThat(a.bloquant()).isFalse();
            assertThat(a.exigeMotif()).isFalse();
        });

        assertThat(resultat.alertes()).singleElement().satisfies(a -> {
            assertThat(a.niveau()).isEqualTo(NiveauInteraction.CI);
            assertThat(a.type()).isEqualTo("INTERACTION");
            assertThat(a.conduite()).isEqualTo("Contre-indiqué");
            assertThat(a.produitIds()).containsExactlyInAnyOrder(coumadine.getId(), advil.getId());
        });
        assertThat(resultat.nonControles()).extracting(p -> p.produitId()).containsExactly(sansMolecule.getId());

        // Un traitement en cours (achat clôturé du client) interagit avec un produit du panier.
        CashSale vente = vente(caisseOuverte(0), NatureVente.COMPTANT, 1000, 0, false, client, LocalDate.now());
        ligneDeVente(vente, coumadine, 1);
        em.flush();
        ControleResultatDTO avecHistorique = controleService.controler(client.getId(), List.of(advil.getId()));
        assertThat(avecHistorique.alertes()).extracting(AlerteControleDTO::niveau).containsExactly(NiveauInteraction.CI);
        assertThat(avecHistorique.alertes().get(0).message()).contains("en cours");
    }

    // ── fabriques ──

    private OrdonnanceCreationDTO creation(Integer prescripteurId, Produit produit, int quantite, int renouvellements) {
        return new OrdonnanceCreationDTO(
            client.getId(),
            prescripteurId,
            LocalDate.now(),
            renouvellements,
            null,
            null,
            List.of(new OrdonnanceLigneCreationDTO(produit.getId(), null, "1 cp x 3 /j", 5, quantite))
        );
    }

    private Dci dci(String libelle) {
        Dci dci = new Dci();
        dci.setCode(unique("DCI"));
        dci.setLibelle(unique(libelle));
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

    /** Le référentiel BDPM n'est pas chargé par les migrations du dépôt : on y pose la molécule dont le test a besoin. */
    private int refDci(String libelle, Dci dci) {
        Object id = em
            .createNativeQuery("INSERT INTO ref_dci (libelle, dci_id) VALUES (:libelle, :dci) RETURNING id")
            .setParameter("libelle", unique(libelle))
            .setParameter("dci", dci.getId())
            .getSingleResult();
        em.flush();
        return ((Number) id).intValue();
    }
}
