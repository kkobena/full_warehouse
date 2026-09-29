package com.kobe.warehouse.security.navaccess;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.Authority;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.enumeration.NavTargetType;
import com.kobe.warehouse.domain.nav.NavItem;
import com.kobe.warehouse.domain.nav.NavItemRole;
import com.kobe.warehouse.license.Feature;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.repository.nav.NavItemRepository;
import com.kobe.warehouse.repository.nav.NavItemRoleRepository;
import com.kobe.warehouse.service.license.LicenseService;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Droits des endpoints lus dans {@code nav_item_role} (docs/PLAN-SECURISATION-ENDPOINTS.md) : refus
 * d'un rôle sans droit, union des rôles, droit retiré appliqué sans reconnexion, et cohérence des
 * codes déclarés dans les contrôleurs avec ceux des migrations.
 */
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Sécurisation des endpoints — droits lus en base")
class NavAccessIntegrationTest {

    private static final AtomicInteger COMPTEUR = new AtomicInteger();
    private static EntityManager em;

    private final LicenseService licenseService = mock(LicenseService.class);
    private NavAccessService service;
    private TransactionStatus transaction;
    private NavItem ecran;
    private NavItemRole droitPharmacien;

    @BeforeAll
    static void demarrerLaBase() {
        em = SharedEntityManagerCreator.createSharedEntityManager(IntegrationPostgresDatabase.bean(EntityManagerFactory.class));
    }

    @BeforeEach
    void preparer() {
        transaction = IntegrationPostgresDatabase.transactionManager().getTransaction(new DefaultTransactionDefinition());
        when(licenseService.hasFeature(any())).thenReturn(true);
        service = new NavAccessService(
            IntegrationPostgresDatabase.bean(NavItemRoleRepository.class),
            IntegrationPostgresDatabase.bean(NavItemRepository.class),
            IntegrationPostgresDatabase.bean(UserRepository.class),
            licenseService,
            new NoOpCacheManager()
        );

        ecran = new NavItem().setCode("test-endpoint-" + COMPTEUR.incrementAndGet()).setLibelle("Écran de test").setTargetType(NavTargetType.ROUTE);
        em.persist(ecran);
        droitPharmacien = new NavItemRole().setNavItem(ecran).setRoleName("ROLE_PHARMACIEN").setCanAccess(true).setCanExport(true);
        em.persist(droitPharmacien);
        // configuré mais refusé : le cas qu'un « absent = refusé » ne suffit pas à couvrir
        em.persist(new NavItemRole().setNavItem(ecran).setRoleName("ROLE_CAISSIER").setCanDisplay(false).setCanAccess(false));
        em.persist(new NavItemRole().setNavItem(ecran).setRoleName("ROLE_VENDEUR").setCanAccess(false).setCanCreate(true));
        em.flush();
    }

    @AfterEach
    void annuler() {
        SecurityContextHolder.clearContext();
        if (transaction != null && !transaction.isCompleted()) {
            IntegrationPostgresDatabase.transactionManager().rollback(transaction);
        }
    }

    @Test
    @DisplayName("un caissier est refusé, le pharmacien lit et exporte mais ne crée pas")
    void refusDuCaissier() {
        connecter(utilisateur("ROLE_CAISSIER"));
        assertThat(service.isAllowed(codes(), NavAction.ACCESS)).isFalse();

        connecter(utilisateur("ROLE_PHARMACIEN"));
        assertThat(service.isAllowed(codes(), NavAction.ACCESS)).isTrue();
        assertThat(service.isAllowed(codes(), NavAction.EXPORT)).isTrue();
        assertThat(service.isAllowed(codes(), NavAction.CREATE)).isFalse();
    }

    @Test
    @DisplayName("un utilisateur à deux rôles cumule leurs droits")
    void unionDesRoles() {
        connecter(utilisateur("ROLE_PHARMACIEN", "ROLE_VENDEUR"));
        assertThat(service.isAllowed(codes(), NavAction.ACCESS)).isTrue();
        assertThat(service.isAllowed(codes(), NavAction.CREATE)).isTrue();
    }

    @Test
    @DisplayName("un droit retiré s'applique sans reconnexion")
    void droitRetire() {
        connecter(utilisateur("ROLE_PHARMACIEN"));
        assertThat(service.isAllowed(codes(), NavAction.ACCESS)).isTrue();

        droitPharmacien.setCanAccess(false);
        em.flush();
        assertThat(service.isAllowed(codes(), NavAction.ACCESS)).isFalse();
    }

    @Test
    @DisplayName("l'un des codes listés suffit ; un code inconnu ne donne rien")
    void unCodeSuffit() {
        connecter(utilisateur("ROLE_PHARMACIEN"));
        assertThat(service.isAllowed(List.of("code-inexistant", ecran.getCode()), NavAction.ACCESS)).isTrue();
        assertThat(service.isAllowed(List.of("code-inexistant"), NavAction.ACCESS)).isFalse();
    }

    @Test
    @DisplayName("l'administrateur passe sans ligne de droit, comme dans l'AuthGuard du front")
    void administrateur() {
        connecter(utilisateur("ROLE_ADMIN"));
        assertThat(service.isAllowed(List.of("code-inexistant"), NavAction.DELETE)).isTrue();
    }

    @Test
    @DisplayName("un écran désactivé ou hors licence n'est plus accessible")
    void desactiveOuHorsLicence() {
        connecter(utilisateur("ROLE_PHARMACIEN"));
        ecran.setRequiredFeature(Feature.COMPTABILITE.name());
        em.flush();
        when(licenseService.hasFeature(Feature.COMPTABILITE)).thenReturn(false);
        assertThat(service.isAllowed(codes(), NavAction.ACCESS)).isFalse();

        when(licenseService.hasFeature(Feature.COMPTABILITE)).thenReturn(true);
        ecran.setActif(false);
        em.flush();
        assertThat(service.isAllowed(codes(), NavAction.ACCESS)).isFalse();
    }

    @Test
    @DisplayName("le message de refus nomme l'écran")
    void libelle() {
        assertThat(service.libelleOf(List.of("code-inexistant", ecran.getCode()))).isEqualTo("Écran de test");
    }

    @Test
    @DisplayName("chaque code déclaré dans un contrôleur existe dans nav_item")
    void codesDeclaresExistent() {
        Set<String> declares = NavAccessHandlers.scan()
            .stream()
            .map(NavAccessHandlers.Handler::rule)
            .filter(NavAccessRules.Requires.class::isInstance)
            .flatMap(rule -> List.of(((NavAccessRules.Requires) rule).codes()).stream())
            .collect(Collectors.toCollection(TreeSet::new));
        Set<String> connus = em
            .createQuery("SELECT n.code FROM NavItem n", String.class)
            .getResultStream()
            .collect(Collectors.toSet());
        assertThat(declares).isNotEmpty();
        assertThat(declares).as("codes absents des migrations").isSubsetOf(connus);
    }

    // ===== fixtures =====

    private List<String> codes() {
        return List.of(ecran.getCode());
    }

    private static void connecter(AppUser user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user.getLogin(), null, List.of()));
    }

    private static AppUser utilisateur(String... roles) {
        AppUser user = new AppUser();
        user.setLogin("nav" + COMPTEUR.incrementAndGet());
        user.setPassword("x".repeat(60));
        user.setActivated(true);
        user.setLangKey("fr");
        user.setMagasin(em.find(Magasin.class, 1));
        user.setAuthorities(Set.of(roles).stream().map(r -> em.find(Authority.class, r)).collect(Collectors.toSet()));
        em.persist(user);
        em.flush();
        return user;
    }
}
