package com.kobe.warehouse.service.customer.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.Authority;
import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.CashSale;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.UninsuredCustomer;
import com.kobe.warehouse.repository.CustomerRepository;
import com.kobe.warehouse.repository.LimiteCreditDerogationRepository;
import com.kobe.warehouse.repository.RelanceDiffereRepository;
import com.kobe.warehouse.repository.SalesRepository;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.repository.nav.NavItemRepository;
import com.kobe.warehouse.repository.nav.NavItemRoleRepository;
import com.kobe.warehouse.security.navaccess.NavAccessService;
import com.kobe.warehouse.service.SmsService;
import com.kobe.warehouse.service.UserService;
import com.kobe.warehouse.service.UtilisationCleSecuriteService;
import com.kobe.warehouse.service.customer.DerogationAuthorizer;
import com.kobe.warehouse.service.customer.DerogationLimiteCreditDTO;
import com.kobe.warehouse.service.customer.LimiteCreditService;
import com.kobe.warehouse.service.customer.RelanceDiffereDTO;
import com.kobe.warehouse.service.customer.RelanceDiffereService;
import com.kobe.warehouse.service.customer.SituationCreditDTO;
import com.kobe.warehouse.service.dashboard.integration.AbstractDashboardIntegrationTest;
import com.kobe.warehouse.service.errors.ForbiddenOperationException;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.license.LicenseService;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Fiche client, lot 3 (docs/PLAN-FICHE-CLIENT.md) : limite de crédit globale contrôlée à la clôture
 * d'une vente différée, dérogation tracée, et relance SMS des différés.
 */
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Fiche client — crédit")
class CreditClientIntegrationTest extends AbstractDashboardIntegrationTest {

    private static final String CLE_PHARMACIEN = "8642";

    private final AppConfigurationService configuration = mock(AppConfigurationService.class);
    private final SmsService sms = mock(SmsService.class);
    private LimiteCreditService limite;
    private RelanceDiffereService relance;
    private UninsuredCustomer client;
    private CashRegister caisse;
    private AppUser caissier;
    private AppUser pharmacien;

    @BeforeEach
    void preparer() {
        UserRepository userRepository = IntegrationPostgresDatabase.bean(UserRepository.class);
        LicenseService licenseService = mock(LicenseService.class);
        when(licenseService.hasFeature(any())).thenReturn(true);
        NavAccessService navAccess = new NavAccessService(
            IntegrationPostgresDatabase.bean(NavItemRoleRepository.class),
            IntegrationPostgresDatabase.bean(NavItemRepository.class),
            userRepository,
            licenseService,
            new NoOpCacheManager()
        );
        UserService userService = mock(UserService.class);
        when(userService.getUser()).thenAnswer(i -> userRepository.findOneByLogin(SecurityContextHolder.getContext().getAuthentication().getName()).orElse(null));
        when(userService.getUserByPwdOrSecurityKey(anyString())).thenAnswer(i ->
            userRepository.findOneByActionAuthorityKey(DigestUtils.sha256Hex((String) i.getArgument(0)))
        );
        UtilisationCleSecuriteService cles = mock(UtilisationCleSecuriteService.class);
        NavItemRepository navItems = IntegrationPostgresDatabase.bean(NavItemRepository.class);
        when(cles.hasPrivilege(anyString(), anyString())).thenAnswer(i -> navItems.existByCodeAndRoleName(i.getArgument(0), i.getArgument(1)));
        DerogationAuthorizer authorizer = new DerogationAuthorizer(navAccess, userService, cles);
        SalesRepository sales = IntegrationPostgresDatabase.bean(SalesRepository.class);

        limite = new LimiteCreditService(configuration, sales, IntegrationPostgresDatabase.bean(LimiteCreditDerogationRepository.class), authorizer);
        relance = new RelanceDiffereService(
            IntegrationPostgresDatabase.bean(CustomerRepository.class),
            sales,
            IntegrationPostgresDatabase.bean(RelanceDiffereRepository.class),
            sms,
            configuration,
            authorizer
        );
        when(configuration.getDevise()).thenReturn("FCFA");

        caisse = caisseOuverte(0);
        client = client(unique("CREDIT"), "Awa", "0700000030");
        caissier = utilisateur("ROLE_CAISSIER", null);
        pharmacien = utilisateur("ROLE_PHARMACIEN", CLE_PHARMACIEN);
        connecter(caissier);
    }

    @Test
    @DisplayName("la situation donne l'encours et la marge avant la limite ; sans limite, pas de marge")
    void situation() {
        venteDifferee(caisse, 10_000, 8_000, client, LocalDate.now());
        when(configuration.getLimiteCreditClient()).thenReturn(10_000);
        assertThat(limite.situation(client.getId())).isEqualTo(new SituationCreditDTO(10_000, 8_000, 2_000L));

        when(configuration.getLimiteCreditClient()).thenReturn(0);
        assertThat(limite.situation(client.getId()).disponible()).isNull();
    }

    @Test
    @DisplayName("une vente différée qui dépasse la limite est refusée à la clôture, avec les montants")
    void clotureRefusee() {
        venteDifferee(caisse, 10_000, 8_000, client, LocalDate.now());
        CashSale nouvelle = venteDifferee(caisse, 3_000, 3_000, client, LocalDate.now());
        when(configuration.getLimiteCreditClient()).thenReturn(10_000);

        assertThatThrownBy(() -> limite.controlerCloture(nouvelle))
            .isInstanceOf(GenericError.class)
            .satisfies(e -> {
                GenericError erreur = (GenericError) e;
                assertThat(erreur.getErrorKey()).isEqualTo(LimiteCreditService.ERROR_KEY);
                @SuppressWarnings("unchecked")
                Map<String, Object> payload = (Map<String, Object>) erreur.getPayload();
                assertThat(payload).containsEntry("encours", 8_000L).containsEntry("montant", 3_000).containsEntry("limite", 10_000);
            });
    }

    @Test
    @DisplayName("sous la limite, sans limite, ou payée comptant : la vente passe")
    void clotureAcceptee() {
        venteDifferee(caisse, 10_000, 5_000, client, LocalDate.now());
        CashSale nouvelle = venteDifferee(caisse, 3_000, 3_000, client, LocalDate.now());
        when(configuration.getLimiteCreditClient()).thenReturn(10_000);
        assertThatCode(() -> limite.controlerCloture(nouvelle)).doesNotThrowAnyException();

        when(configuration.getLimiteCreditClient()).thenReturn(1_000);
        assertThatCode(() -> limite.controlerCloture(venteComptant(caisse, 50_000))).doesNotThrowAnyException();

        when(configuration.getLimiteCreditClient()).thenReturn(0);
        assertThatCode(() -> limite.controlerCloture(nouvelle)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("la clé du pharmacien autorise la vente au-delà de la limite ; sans clé, refus")
    void derogation() {
        venteDifferee(caisse, 10_000, 8_000, client, LocalDate.now());
        CashSale nouvelle = venteDifferee(caisse, 3_000, 3_000, client, LocalDate.now());
        when(configuration.getLimiteCreditClient()).thenReturn(10_000);
        DerogationLimiteCreditDTO sansCle = demande(nouvelle, null);

        assertThatThrownBy(() -> limite.deroger(client.getId(), sansCle)).isInstanceOf(ForbiddenOperationException.class);

        limite.deroger(client.getId(), demande(nouvelle, CLE_PHARMACIEN));
        em.flush();
        assertThatCode(() -> limite.controlerCloture(nouvelle)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("une dérogation n'a pas lieu d'être sous la limite")
    void derogationInutile() {
        CashSale nouvelle = venteDifferee(caisse, 3_000, 3_000, client, LocalDate.now());
        when(configuration.getLimiteCreditClient()).thenReturn(10_000);
        assertThatThrownBy(() -> limite.deroger(client.getId(), demande(nouvelle, CLE_PHARMACIEN)))
            .isInstanceOf(GenericError.class)
            .extracting("errorKey")
            .isEqualTo("limiteCreditNonDepassee");
    }

    @Test
    @DisplayName("la relance envoie le solde par SMS et s'enregistre")
    void relancer() {
        venteDifferee(caisse, 20_000, 12_500, client, LocalDate.now());

        RelanceDiffereDTO envoyee = relance.relancer(client.getId());

        assertThat(envoyee.montant()).isEqualTo(12_500);
        verify(sms).sendSms(eq("0700000030"), contains("12 500 FCFA"));
        em.flush();
        assertThat(relance.historique(client.getId())).extracting(RelanceDiffereDTO::telephone).containsExactly("0700000030");
    }

    @Test
    @DisplayName("pas de relance sans téléphone ni sans solde")
    void relanceImpossible() {
        UninsuredCustomer sansTelephone = client(unique("CREDIT"), "Yao", null);
        venteDifferee(caisse, 5_000, 5_000, sansTelephone, LocalDate.now());

        assertThatThrownBy(() -> relance.relancer(sansTelephone.getId())).isInstanceOf(GenericError.class).extracting("errorKey").isEqualTo("relanceSansTelephone");
        assertThatThrownBy(() -> relance.relancer(client.getId())).isInstanceOf(GenericError.class).extracting("errorKey").isEqualTo("relanceSansEncours");
        verify(sms, never()).sendSms(anyString(), anyString());
    }

    // ===== fixtures =====

    private static DerogationLimiteCreditDTO demande(CashSale vente, String cle) {
        return new DerogationLimiteCreditDTO(vente.getId().getId(), vente.getSaleDate(), vente.getRestToPay(), "client régulier", cle);
    }

    private AppUser utilisateur(String role, String cle) {
        AppUser user = new AppUser();
        user.setLogin(unique("credit").toLowerCase());
        user.setPassword("x".repeat(60));
        user.setActivated(true);
        user.setLangKey("fr");
        user.setMagasin(em.find(Magasin.class, MAGASIN_ID));
        user.setAuthorities(Set.of(role).stream().map(r -> em.find(Authority.class, r)).collect(Collectors.toSet()));
        em.persist(user);
        em.flush();
        if (cle != null) {
            // Posée en SQL, comme le jeu de démonstration : l'entité valide la saisie (chiffres), la base garde son empreinte.
            em
                .createNativeQuery("UPDATE app_user SET action_authority_key = :cle WHERE id = :id")
                .setParameter("cle", DigestUtils.sha256Hex(cle))
                .setParameter("id", user.getId())
                .executeUpdate();
            em.refresh(user);
        }
        return user;
    }

    private static void connecter(AppUser user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user.getLogin(), null, List.of()));
    }
}
