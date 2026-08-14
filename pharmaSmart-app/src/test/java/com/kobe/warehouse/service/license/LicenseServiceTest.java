package com.kobe.warehouse.service.license;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.LicenseState;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.enumeration.TypeMagasin;
import com.kobe.warehouse.license.BindingPolicy;
import com.kobe.warehouse.license.Feature;
import com.kobe.warehouse.license.HardwareFingerprintProvider;
import com.kobe.warehouse.license.LicenseInfo;
import com.kobe.warehouse.license.LicensePayload;
import com.kobe.warehouse.license.LicenseProperties;
import com.kobe.warehouse.license.LicenseSigner;
import com.kobe.warehouse.license.LicenseStatus;
import com.kobe.warehouse.license.LicenseType;
import com.kobe.warehouse.license.LicenseVerifier;
import com.kobe.warehouse.license.SupportContacts;
import com.kobe.warehouse.repository.LicenseAuditRepository;
import com.kobe.warehouse.repository.LicenseStateRepository;
import com.kobe.warehouse.repository.MagasinRepository;
import com.kobe.warehouse.repository.ProduitRepository;
import com.kobe.warehouse.repository.SalesRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.dao.DataAccessResourceFailureException;

/**
 * Comportement du moteur de statuts et des liaisons anti-partage.
 *
 * <p>Les dépendances de persistance sont simulées : ce qu'on veut éprouver ici, ce sont les
 * <em>décisions</em> — bloquer, alerter, laisser passer — et non le mapping JPA.
 */
class LicenseServiceTest {

    private static final String OFFICINE = "PHARMACIE DE LA PAIX";

    @TempDir
    Path tempDir;

    private KeyPair keys;
    private LicenseProperties properties;
    private LicenseStateRepository licenseStateRepository;
    private LicenseAuditRepository licenseAuditRepository;
    private MagasinRepository magasinRepository;
    private SalesRepository salesRepository;
    private ProduitRepository produitRepository;
    private HardwareFingerprintProvider fingerprintProvider;

    @BeforeEach
    void setUp() throws Exception {
        keys = LicenseSigner.generateKeyPair();

        properties = new LicenseProperties();
        properties.setEnabled(true);
        properties.setFilePath(tempDir.resolve("license.lic").toString());

        licenseStateRepository = mock(LicenseStateRepository.class);
        licenseAuditRepository = mock(LicenseAuditRepository.class);
        magasinRepository = mock(MagasinRepository.class);
        salesRepository = mock(SalesRepository.class);
        produitRepository = mock(ProduitRepository.class);
        fingerprintProvider = mock(HardwareFingerprintProvider.class);

        when(licenseStateRepository.findSingleton()).thenReturn(Optional.empty());
        when(licenseStateRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(fingerprintProvider.fingerprint()).thenReturn("sha256:poste-a");

        Magasin magasin = new Magasin();
        magasin.setName(OFFICINE);
        when(magasinRepository.findFirstByTypeMagasinOrderByIdAsc(TypeMagasin.OFFICINE)).thenReturn(Optional.of(magasin));
    }

    private LicenseService service() {
        return new LicenseService(
            properties,
            new LicenseVerifier(List.of(keys.getPublic())),
            fingerprintProvider,
            licenseStateRepository,
            licenseAuditRepository,
            magasinRepository,
            salesRepository,
            produitRepository,
            // L'arbre de navigation est filtré par les modules souscrits : sa purge à l'activation
            // est un effet de bord, pas une décision à éprouver ici.
            new NoOpCacheManager()
        );
    }

    private LicensePayload payload(LocalDate expiresAt, BindingPolicy binding, String magasinName, String fingerprint) {
        return new LicensePayload(
            "lic-1",
            LicenseType.SUBSCRIPTION,
            "CI-ABJ-0042",
            magasinName,
            magasinName,
            null,
            "STANDARD",
            Instant.now(),
            LocalDate.now(ZoneOffset.UTC).minusYears(1),
            expiresAt,
            7,
            10,
            null,
            null,
            EnumSet.of(Feature.CAISSE),
            fingerprint,
            binding,
            new SupportContacts("PharmaSmart CI", List.of("+225 07 00 00 00 00"), List.of(), null, null)
        );
    }

    private void installLicense(LicensePayload payload) throws Exception {
        Files.writeString(Path.of(properties.getFilePath()), new LicenseSigner(keys.getPrivate()).sign(payload), StandardCharsets.UTF_8);
    }

    private LocalDate inDays(long days) {
        return LocalDate.now(ZoneOffset.UTC).plusDays(days);
    }

    // ------------------------------------------------------------------ moteur de statuts

    @Test
    void uneLicenceLargementValideNeDeclencheNiToastNiBanniere() throws Exception {
        installLicense(payload(inDays(200), BindingPolicy.MAGASIN, OFFICINE, null));

        LicenseInfo info = service().currentStatus();

        assertThat(info.status()).isEqualTo(LicenseStatus.VALID);
        assertThat(info.readOnly()).isFalse();
        assertThat(info.showBanner()).isFalse();
    }

    @Test
    void aVingtCinqJoursDeLEcheanceLeToastSuffitSansBanniere() throws Exception {
        installLicense(payload(inDays(25), BindingPolicy.MAGASIN, OFFICINE, null));

        LicenseInfo info = service().currentStatus();

        assertThat(info.status()).isEqualTo(LicenseStatus.EXPIRING_SOON);
        assertThat(info.showBanner()).isFalse();
        assertThat(info.readOnly()).isFalse();
    }

    @Test
    void aDixJoursDeLEcheanceLaBanniereApparaitMaisLEcritureResteAutorisee() throws Exception {
        installLicense(payload(inDays(10), BindingPolicy.MAGASIN, OFFICINE, null));

        LicenseInfo info = service().currentStatus();

        assertThat(info.status()).isEqualTo(LicenseStatus.EXPIRING_CRITICAL);
        assertThat(info.showBanner()).isTrue();
        assertThat(info.readOnly()).isFalse();
    }

    @Test
    void pendantLaPeriodeDeGraceLEcritureResteAutorisee() throws Exception {
        installLicense(payload(inDays(-3), BindingPolicy.MAGASIN, OFFICINE, null));

        LicenseInfo info = service().currentStatus();

        assertThat(info.status()).isEqualTo(LicenseStatus.GRACE);
        assertThat(info.readOnly()).isFalse();
        assertThat(info.showBanner()).isTrue();
    }

    @Test
    void auDelaDeLaGraceLApplicationPasseEnLectureSeule() throws Exception {
        installLicense(payload(inDays(-30), BindingPolicy.MAGASIN, OFFICINE, null));

        LicenseService service = service();

        assertThat(service.currentStatus().status()).isEqualTo(LicenseStatus.EXPIRED);
        assertThat(service.isWriteAllowed()).isFalse();
    }

    @Test
    void aucunFichierNiCopieEnBaseDonneLeStatutAbsente() {
        LicenseInfo info = service().currentStatus();

        assertThat(info.status()).isEqualTo(LicenseStatus.MISSING);
        assertThat(info.readOnly()).isTrue();
    }

    @Test
    void lorsqueLeControleEstDesactiveLeStatutNeDependDAucunFichier() {
        properties.setEnabled(false);

        LicenseInfo info = service().currentStatus();

        assertThat(info.status()).isEqualTo(LicenseStatus.VALID);
        assertThat(info.readOnly()).isFalse();
    }

    // ------------------------------------------------------------------ anti-partage (§3.4)

    @Test
    void uneLicenceDelivreeAUneAutreOfficineEstRefusee() throws Exception {
        installLicense(payload(inDays(200), BindingPolicy.MAGASIN, "PHARMACIE DU PLATEAU", null));

        LicenseInfo info = service().currentStatus();

        assertThat(info.status()).isEqualTo(LicenseStatus.INVALID);
        assertThat(info.readOnly()).isTrue();
        assertThat(info.message()).contains("PHARMACIE DU PLATEAU");
    }

    @Test
    void laCasseEtLesAccentsDeLaRaisonSocialeNeBloquentPas() throws Exception {
        installLicense(payload(inDays(200), BindingPolicy.MAGASIN, "Pharmacie de la Paix", null));

        assertThat(service().currentStatus().status()).isEqualTo(LicenseStatus.VALID);
    }

    @Test
    void unChangementDeServeurRecentAvertitSansBloquer() throws Exception {
        installLicense(payload(inDays(200), BindingPolicy.MAGASIN_AND_HARDWARE, OFFICINE, "sha256:ancien-poste"));
        LicenseState state = new LicenseState().setFingerprintMismatchSince(Instant.now().minus(Duration.ofDays(3)));
        when(licenseStateRepository.findSingleton()).thenReturn(Optional.of(state));

        LicenseInfo info = service().currentStatus();

        assertThat(info.status()).isEqualTo(LicenseStatus.VALID);
        assertThat(info.readOnly()).isFalse();
    }

    @Test
    void passeLeDelaiDeRegularisationLEmpreinteDivergenteBloque() throws Exception {
        installLicense(payload(inDays(200), BindingPolicy.MAGASIN_AND_HARDWARE, OFFICINE, "sha256:ancien-poste"));
        LicenseState state = new LicenseState().setFingerprintMismatchSince(Instant.now().minus(Duration.ofDays(20)));
        when(licenseStateRepository.findSingleton()).thenReturn(Optional.of(state));

        LicenseInfo info = service().currentStatus();

        assertThat(info.status()).isEqualTo(LicenseStatus.INVALID);
        assertThat(info.readOnly()).isTrue();
    }

    @Test
    void unReculDeLHorlogeSystemeEstDetecte() throws Exception {
        installLicense(payload(inDays(200), BindingPolicy.MAGASIN, OFFICINE, null));
        // Sonde écrite « dans le futur » du point de vue de l'horloge courante : c'est la trace
        // qu'on a reculé la date de la machine.
        LicenseState state = new LicenseState().setLastSeenInstant(Instant.now().plus(Duration.ofDays(180)));
        when(licenseStateRepository.findSingleton()).thenReturn(Optional.of(state));

        LicenseInfo info = service().currentStatus();

        assertThat(info.status()).isEqualTo(LicenseStatus.CLOCK_TAMPERED);
        assertThat(info.readOnly()).isTrue();
    }

    @Test
    void supprimerLeFichierNeSuffitPasSiLaLicenceEstEnBase() throws Exception {
        String token = new LicenseSigner(keys.getPrivate()).sign(payload(inDays(200), BindingPolicy.MAGASIN, OFFICINE, null));
        when(licenseStateRepository.findSingleton()).thenReturn(Optional.of(new LicenseState().setLicenseToken(token)));

        // Aucun fichier sur disque : le repli sur la copie en base doit prendre le relais.
        assertThat(service().currentStatus().status()).isEqualTo(LicenseStatus.VALID);
    }

    @Test
    void uneLicencePasEncoreValideEstRefusee() throws Exception {
        LicensePayload future = new LicensePayload(
            "lic-future",
            LicenseType.SUBSCRIPTION,
            null,
            OFFICINE,
            null,
            null,
            null,
            Instant.now(),
            inDays(30),
            inDays(400),
            0,
            null,
            null,
            null,
            null,
            null,
            BindingPolicy.MAGASIN,
            null
        );
        installLicense(future);

        LicenseInfo info = service().currentStatus();

        assertThat(info.status()).isEqualTo(LicenseStatus.INVALID);
        assertThat(info.message()).contains("pas encore valide");
    }

    // ------------------------------------------------------------------ mode démonstration (§3.5)

    private LicensePayload demoPayload(Integer maxSales, Integer maxProduits) {
        return new LicensePayload(
            "lic-demo",
            LicenseType.DEMO,
            null,
            null,
            null,
            null,
            null,
            Instant.now(),
            null,
            // Échéance volontairement lointaine : on éprouve ici le comportement propre à la
            // démonstration, pas le moteur d'échéance. Une démo réelle dure 30 jours et serait donc
            // d'emblée en EXPIRING_SOON, ce qui masquerait ce qu'on cherche à vérifier.
            inDays(200),
            0,
            null,
            maxSales,
            maxProduits,
            null,
            null,
            // Une démo s'installe partout : c'est voulu, et c'est ce qui impose les restrictions.
            BindingPolicy.NONE,
            null
        );
    }

    @Test
    void uneDemoSAnnonceEnPermanenceMemeLargementValide() throws Exception {
        installLicense(demoPayload(null, null));

        LicenseInfo info = service().currentStatus();

        assertThat(info.status()).isEqualTo(LicenseStatus.VALID);
        assertThat(info.demo()).isTrue();
        // Bannière permanente quel que soit le temps restant : un document de démo ne doit jamais
        // pouvoir passer pour un document réel.
        assertThat(info.showBanner()).isTrue();
        assertThat(info.readOnly()).isFalse();
    }

    @Test
    void uneDemoBasculeEnLectureSeuleAuDelaDuQuotaDeVentes() throws Exception {
        installLicense(demoPayload(500, 1000));
        when(salesRepository.count()).thenReturn(500L);

        LicenseInfo info = service().currentStatus();

        assertThat(info.status()).isEqualTo(LicenseStatus.DEMO_QUOTA_REACHED);
        assertThat(info.readOnly()).isTrue();
        assertThat(info.message()).contains("500 ventes");
    }

    @Test
    void uneDemoBasculeEnLectureSeuleAuDelaDuQuotaDeProduits() throws Exception {
        installLicense(demoPayload(500, 1000));
        when(salesRepository.count()).thenReturn(10L);
        when(produitRepository.count()).thenReturn(1000L);

        LicenseInfo info = service().currentStatus();

        assertThat(info.status()).isEqualTo(LicenseStatus.DEMO_QUOTA_REACHED);
        assertThat(info.readOnly()).isTrue();
        assertThat(info.message()).contains("1000 produits");
    }

    @Test
    void uneDemoSousLesQuotasResteEcrivable() throws Exception {
        installLicense(demoPayload(500, 1000));
        when(salesRepository.count()).thenReturn(120L);
        when(produitRepository.count()).thenReturn(300L);

        assertThat(service().isWriteAllowed()).isTrue();
    }

    @Test
    void uneLicenceReelleNeCompteJamaisLesVolumes() throws Exception {
        installLicense(payload(inDays(200), BindingPolicy.MAGASIN, OFFICINE, null));

        service().currentStatus();

        // Les quotas ne concernent que la démonstration : une officine en production ne doit pas
        // payer deux COUNT(*) à chaque rafraîchissement.
        org.mockito.Mockito.verify(salesRepository, org.mockito.Mockito.never()).count();
        org.mockito.Mockito.verify(produitRepository, org.mockito.Mockito.never()).count();
    }

    @Test
    void unTrialNestPasBrideCommeUneDemo() throws Exception {
        LicensePayload trial = new LicensePayload(
            "lic-trial",
            LicenseType.TRIAL,
            null,
            OFFICINE,
            null,
            null,
            null,
            Instant.now(),
            null,
            inDays(20),
            0,
            null,
            null,
            null,
            null,
            null,
            BindingPolicy.MAGASIN,
            null
        );
        installLicense(trial);

        LicenseInfo info = service().currentStatus();

        // Un essai est un abonnement à durée courte : rien n'y est bridé, tout y est exploitable.
        assertThat(info.demo()).isFalse();
        assertThat(info.readOnly()).isFalse();
        org.mockito.Mockito.verify(salesRepository, org.mockito.Mockito.never()).count();
    }

    // ------------------------------------------------------------------ activation

    @Test
    void activerUneLicenceValideRetablitLEcritureImmediatement() throws Exception {
        installLicense(payload(inDays(-30), BindingPolicy.MAGASIN, OFFICINE, null));
        LicenseService service = service();
        assertThat(service.isWriteAllowed()).isFalse();

        String renouvellement = new LicenseSigner(keys.getPrivate()).sign(payload(inDays(365), BindingPolicy.MAGASIN, OFFICINE, null));
        LicenseInfo info = service.activate(renouvellement.getBytes(StandardCharsets.UTF_8), "license.lic");

        // Sans redémarrage ni rechargement : le cache est reconstruit par activate() lui-même.
        assertThat(info.status()).isEqualTo(LicenseStatus.VALID);
        assertThat(service.isWriteAllowed()).isTrue();
    }

    @Test
    void deposerUneLicenceDejaExpireeEstRefuseAvecUnMessageExploitable() throws Exception {
        String perimee = new LicenseSigner(keys.getPrivate()).sign(payload(inDays(-90), BindingPolicy.MAGASIN, OFFICINE, null));

        assertThatThrownBy(() -> service().activate(perimee.getBytes(StandardCharsets.UTF_8), "license.lic"))
            .isInstanceOf(LicenseActivationException.class)
            .hasMessageContaining("déjà expirée");
    }

    @Test
    void deposerLaLicenceDuneAutreOfficineEstRefuse() throws Exception {
        String autre = new LicenseSigner(keys.getPrivate()).sign(payload(inDays(365), BindingPolicy.MAGASIN, "PHARMACIE DU PLATEAU", null));

        assertThatThrownBy(() -> service().activate(autre.getBytes(StandardCharsets.UTF_8), "license.lic"))
            .isInstanceOf(LicenseActivationException.class)
            .hasMessageContaining("PHARMACIE DU PLATEAU");
    }

    @Test
    void unFichierAltereEstRefuseSansEcraserLaLicenceEnPlace() throws Exception {
        assertThatThrownBy(() -> service().activate("pas-une-licence".getBytes(StandardCharsets.UTF_8), "license.lic"))
            .isInstanceOf(LicenseActivationException.class);
    }

    @Test
    void leperimetreCouvertDofficeResteAccordeMemeSiLaLicenceNeLeMentionnePas() throws Exception {
        // La licence ne liste que CAISSE : sous une liste blanche exhaustive, la comptabilité
        // disparaîtrait. Ce n'est pas le modèle retenu — seules les options se souscrivent.
        installLicense(payload(inDays(200), BindingPolicy.MAGASIN, OFFICINE, null));
        LicenseService service = service();

        assertThat(service.currentStatus().features()).isEqualTo(Set.of(Feature.CAISSE));
        assertThat(service.hasFeature(Feature.CAISSE)).isTrue();
        assertThat(service.hasFeature(Feature.COMPTABILITE)).isTrue();
        assertThat(service.hasFeature(Feature.MULTI_DEPOT)).isTrue();
    }

    @Test
    void uneOptionNonListeeEstRefusee() throws Exception {
        installLicense(payload(inDays(200), BindingPolicy.MAGASIN, OFFICINE, null));
        LicenseService service = service();

        for (Feature feature : Feature.values()) {
            if (feature.isOptional()) {
                assertThat(service.hasFeature(feature)).as("option %s", feature).isFalse();
            }
        }
    }

    @Test
    void uneOptionExplicitementSouscriteEstAccordee() throws Exception {
        LicensePayload avecOption = new LicensePayload(
            "lic-option",
            LicenseType.SUBSCRIPTION,
            null,
            OFFICINE,
            null,
            null,
            null,
            Instant.now(),
            null,
            inDays(365),
            0,
            null,
            null,
            null,
            EnumSet.of(Feature.CALLEBASSE),
            null,
            BindingPolicy.MAGASIN,
            null
        );
        installLicense(avecOption);
        LicenseService service = service();

        assertThat(service.hasFeature(Feature.CALLEBASSE)).isTrue();
        assertThat(service.hasFeature(Feature.EXCLUSION_RAYON)).isFalse();
        // Souscrire une option ne retire rien du périmètre couvert d'office.
        assertThat(service.hasFeature(Feature.FACTURATION)).isTrue();
    }

    @Test
    void uneLicenceAncienneSansListeConserveLexistantSansOuvrirDoption() throws Exception {
        LicensePayload sansFeatures = new LicensePayload(
            "lic-legacy",
            LicenseType.SUBSCRIPTION,
            null,
            OFFICINE,
            null,
            null,
            null,
            Instant.now(),
            null,
            inDays(365),
            0,
            null,
            null,
            null,
            null,
            null,
            BindingPolicy.MAGASIN,
            null
        );
        installLicense(sansFeatures);
        LicenseService service = service();

        for (Feature feature : Feature.values()) {
            assertThat(service.hasFeature(feature)).as("feature %s", feature).isEqualTo(!feature.isOptional());
        }
    }

    @Test
    void unFichierIllisibleNeFaitPasEchouerLEvaluation() throws IOException {
        Files.writeString(Path.of(properties.getFilePath()), "", StandardCharsets.UTF_8);

        assertThat(service().currentStatus().status()).isEqualTo(LicenseStatus.MISSING);
    }

    // ------------------------------------------------------------------ fichier sur disque

    /**
     * Le fichier disque n'est qu'un miroir de la base. S'il manque — suppression, restauration de
     * poste, ou licence activée quand le chemin par défaut pointait encore le profil du compte de
     * service — il doit se rétablir seul, sans redemander le {@code .lic} au client.
     */
    @Test
    void unFichierAbsentEstRetabliDepuisLaCopieEnBase() throws Exception {
        String token = new LicenseSigner(keys.getPrivate()).sign(payload(inDays(100), BindingPolicy.MAGASIN, OFFICINE, null));
        LicenseState state = new LicenseState().setId(LicenseState.SINGLETON_ID).setLicenseToken(token);
        when(licenseStateRepository.findSingleton()).thenReturn(Optional.of(state));
        Path file = Path.of(properties.getFilePath());
        assertThat(file).doesNotExist();

        assertThat(service().currentStatus().status()).isEqualTo(LicenseStatus.VALID);

        assertThat(file).exists().hasContent(token);
    }

    /**
     * Sous Windows le backend tourne en service {@code LocalSystem} : {@code user.home} y désigne
     * le profil système, hors de portée du client. L'emplacement par défaut doit donc être le
     * répertoire de données machine.
     */
    @Test
    void lEmplacementParDefautNeDependPasDuProfilUtilisateur() {
        LicenseProperties defaults = new LicenseProperties();

        Path resolved = defaults.resolveFilePath();

        assertThat(resolved.getFileName()).hasToString("license.lic");
        String programData = System.getenv("PROGRAMDATA");
        if (programData != null && !programData.isBlank()) {
            assertThat(resolved).isEqualTo(Path.of(programData, "PharmaSmart", "license.lic"));
        }
    }

    // ------------------------------------------------------------------ décompte

    /**
     * Le message est un compteur, pas une phrase mémorisée : le jour de l'échéance, il doit dire
     * « aujourd'hui » et non répéter le nombre de jours calculé à l'évaluation initiale.
     */
    @Test
    void leJourDeLEcheanceLeMessageAnnonceAujourdHuiEtNonUnResteDeJours() throws Exception {
        installLicense(payload(LocalDate.now(ZoneOffset.UTC), BindingPolicy.MAGASIN, OFFICINE, null));

        LicenseInfo info = service().currentStatus();

        assertThat(info.daysRemaining()).isZero();
        assertThat(info.message()).contains("expire aujourd'hui").doesNotContain("dans 0");
    }

    @Test
    void laVeilleDeLEcheanceLeMessageAnnonceDemain() throws Exception {
        installLicense(payload(inDays(1), BindingPolicy.MAGASIN, OFFICINE, null));

        assertThat(service().currentStatus().message()).contains("expire demain");
    }

    /**
     * Le décompte est reconstruit à chaque lecture : il ne doit rien devoir à l'instant où
     * l'évaluation a eu lieu. On le vérifie en observant que deux lectures successives d'un même
     * service produisent un message cohérent avec l'échéance, sans passer par une réévaluation.
     */
    @Test
    void leDecompteEstRecalculeALaLectureEtNonFigeDansLeCache() throws Exception {
        installLicense(payload(inDays(10), BindingPolicy.MAGASIN, OFFICINE, null));
        LicenseService service = service();

        LicenseInfo first = service.currentStatus();
        LicenseInfo second = service.currentStatus();

        assertThat(first.message()).contains("dans 10 jours");
        assertThat(second.message()).isEqualTo(first.message());
        assertThat(second.daysRemaining()).isEqualTo(10);
    }

    /** Pendant la grâce, c'est le délai <em>restant</em> qui compte, pas la durée contractuelle. */
    @Test
    void pendantLaGraceLeMessageDecompteLeDelaiRestant() throws Exception {
        // Grâce de 7 jours, échéance dépassée de 3 : il en reste 4.
        installLicense(payload(inDays(-3), BindingPolicy.MAGASIN, OFFICINE, null));

        assertThat(service().currentStatus().message()).contains("Il vous reste 4 jours");
    }

    // ------------------------------------------------------------------ panne d'infrastructure

    /**
     * Scénario observé en production : le service démarre avant que PostgreSQL n'accepte les
     * connexions, l'acquisition Hikari expire au bout de 30 s. L'officine ne doit pas se retrouver
     * en lecture seule pour autant — et surtout pas jusqu'au prochain redémarrage.
     */
    @Test
    void uneBaseInjoignableNeBasculePasEnLectureSeule() {
        when(licenseStateRepository.findSingleton()).thenThrow(
            new DataAccessResourceFailureException("Connection is not available, request timed out after 30014ms")
        );

        LicenseInfo info = service().currentStatus();

        assertThat(info.status()).isEqualTo(LicenseStatus.UNKNOWN);
        assertThat(info.readOnly()).isFalse();
        assertThat(info.showBanner()).isFalse();
    }

    /** Le détail technique (requête SQL, trace JDBC) appartient aux logs, pas à l'écran de caisse. */
    @Test
    void aucunMessageTechniqueNEstExposeAuFrontend() {
        when(licenseStateRepository.findSingleton()).thenThrow(
            new DataAccessResourceFailureException("select ls1_0.id from pharma_smart.license_state ls1_0 where ls1_0.id=?")
        );

        assertThat(service().currentStatus().message()).doesNotContain("select", "pharma_smart", "Hikari");
    }

    /**
     * Une panne survenant alors qu'une licence valide était déjà connue ne doit rien changer pour
     * l'utilisateur : le dernier statut établi reste servi le temps que la base revienne.
     */
    @Test
    void unePanneApresEvaluationConserveLeDernierStatutConnu() throws Exception {
        installLicense(payload(inDays(200), BindingPolicy.MAGASIN, OFFICINE, null));
        LicenseService service = service();
        assertThat(service.currentStatus().status()).isEqualTo(LicenseStatus.VALID);

        when(licenseStateRepository.findSingleton()).thenThrow(new DataAccessResourceFailureException("pool exhausted"));

        assertThat(service.refresh().status()).isEqualTo(LicenseStatus.VALID);
        assertThat(service.isWriteAllowed()).isTrue();
    }
}
