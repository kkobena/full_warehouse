package com.kobe.warehouse.service.dashboard.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.kobe.warehouse.domain.AppUser;
import com.kobe.warehouse.domain.Authority;
import com.kobe.warehouse.domain.DashboardLayout;
import com.kobe.warehouse.domain.Magasin;
import com.kobe.warehouse.domain.enumeration.DashboardScope;
import com.kobe.warehouse.domain.enumeration.NavTargetType;
import com.kobe.warehouse.domain.nav.NavItem;
import com.kobe.warehouse.domain.nav.NavItemRole;
import com.kobe.warehouse.license.Feature;
import com.kobe.warehouse.repository.AuthorityRepository;
import com.kobe.warehouse.repository.DashboardLayoutAuthorityRepository;
import com.kobe.warehouse.repository.DashboardLayoutRepository;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.repository.nav.NavItemRepository;
import com.kobe.warehouse.repository.nav.NavItemRoleRepository;
import com.kobe.warehouse.security.navaccess.NavAccessService;
import com.kobe.warehouse.service.dashboard.widget.AllowedWidgetDTO;
import com.kobe.warehouse.service.dashboard.widget.WidgetAuthorizationService;
import com.kobe.warehouse.service.dto.DashboardLayoutDTO;
import com.kobe.warehouse.service.errors.ForbiddenOperationException;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.impl.DashboardLayoutServiceImpl;
import com.kobe.warehouse.service.license.LicenseService;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Droits du dashboard personnalisable : un caissier ne doit ni s'ajouter, ni lire un widget
 * réservé au pharmacien, et personne ne doit toucher aux layouts système de l'accueil par rôle
 * sans être administrateur.
 *
 * <p>Les droits se lisent dans {@code nav_item_role} par une requête JPQL sur un type d'item
 * ajouté pour l'occasion ({@code WIDGET}) : seule une vraie base garantit que la valeur passe et
 * que l'union des rôles se fait bien.
 */
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Dashboard personnalisable — droits des widgets et des layouts")
class DashboardWidgetDroitsIntegrationTest {

    private static final String WIDGET_MARGE = "test-marge";
    private static final String WIDGET_CAISSE = "test-ma-caisse";
    private static final AtomicInteger COMPTEUR = new AtomicInteger();

    private static EntityManager em;

    private final LicenseService licenseService = mock(LicenseService.class);
    private WidgetAuthorizationService widgets;
    private DashboardLayoutServiceImpl layouts;
    private TransactionStatus transaction;

    private AppUser caissier;
    private AppUser pharmacien;
    private AppUser admin;
    private NavItem widgetMarge;

    @BeforeAll
    static void demarrerLaBase() {
        em = SharedEntityManagerCreator.createSharedEntityManager(IntegrationPostgresDatabase.bean(EntityManagerFactory.class));
    }

    @BeforeEach
    void preparer() {
        transaction = IntegrationPostgresDatabase.transactionManager().getTransaction(new DefaultTransactionDefinition());
        when(licenseService.hasFeature(any())).thenReturn(true);

        widgets = new WidgetAuthorizationService(
            new NavAccessService(
                IntegrationPostgresDatabase.bean(NavItemRoleRepository.class),
                IntegrationPostgresDatabase.bean(NavItemRepository.class),
                IntegrationPostgresDatabase.bean(UserRepository.class),
                licenseService,
                new NoOpCacheManager()
            )
        );
        layouts = new DashboardLayoutServiceImpl(
            IntegrationPostgresDatabase.bean(DashboardLayoutRepository.class),
            IntegrationPostgresDatabase.bean(DashboardLayoutAuthorityRepository.class),
            IntegrationPostgresDatabase.bean(UserRepository.class),
            IntegrationPostgresDatabase.bean(AuthorityRepository.class),
            widgets
        );

        caissier = utilisateur("ROLE_CAISSIER");
        pharmacien = utilisateur("ROLE_PHARMACIEN");
        admin = utilisateur("ROLE_ADMIN");

        widgetMarge = widget(WIDGET_MARGE, "dashboard-perso.widgets.finances");
        accorder(widgetMarge, "ROLE_PHARMACIEN", true);
        // configuré mais refusé : le cas qu'un « absent = refusé » ne suffit pas à couvrir
        accorder(widgetMarge, "ROLE_CAISSIER", false);

        NavItem widgetCaisse = widget(WIDGET_CAISSE, "dashboard-perso.widgets.caisse");
        accorder(widgetCaisse, "ROLE_CAISSIER", true);
        em.flush();
    }

    @AfterEach
    void annuler() {
        SecurityContextHolder.clearContext();
        if (transaction != null && !transaction.isCompleted()) {
            IntegrationPostgresDatabase.transactionManager().rollback(transaction);
        }
    }

    @Nested
    @DisplayName("Catalogue et données des widgets")
    class Widgets {

        @Test
        @DisplayName("le caissier ne voit que ses widgets, le pharmacien que les siens")
        void catalogueParRole() {
            connecter(caissier);
            assertThat(widgets.findAllowedForCurrentUser()).extracting(AllowedWidgetDTO::key).contains(WIDGET_CAISSE).doesNotContain(WIDGET_MARGE);

            connecter(pharmacien);
            assertThat(widgets.findAllowedForCurrentUser()).extracting(AllowedWidgetDTO::key).contains(WIDGET_MARGE).doesNotContain(WIDGET_CAISSE);
        }

        @Test
        @DisplayName("un caissier ne peut pas lire les données d'un widget pharmacien")
        void donneesRefusees() {
            connecter(caissier);
            assertThatThrownBy(() -> widgets.checkCanLoad(WIDGET_MARGE))
                .isInstanceOf(ForbiddenOperationException.class)
                .extracting("errorKey")
                .isEqualTo(WidgetAuthorizationService.ERROR_NON_AUTORISE);
        }

        @Test
        @DisplayName("un utilisateur à deux rôles cumule leurs widgets")
        void unionDesRoles() {
            AppUser polyvalent = utilisateur("ROLE_CAISSIER", "ROLE_PHARMACIEN");
            connecter(polyvalent);
            assertThat(widgets.findAllowedForCurrentUser()).extracting(AllowedWidgetDTO::key).contains(WIDGET_MARGE, WIDGET_CAISSE);
        }

        @Test
        @DisplayName("un widget désactivé n'est plus autorisé à personne")
        void widgetDesactive() {
            widgetMarge.setActif(false);
            em.flush();
            connecter(pharmacien);
            assertThat(widgets.findAllowedForCurrentUser()).extracting(AllowedWidgetDTO::key).doesNotContain(WIDGET_MARGE);
        }

        @Test
        @DisplayName("hors licence : proposé grisé, mais ses données restent refusées")
        void horsLicence() {
            widgetMarge.setRequiredFeature(Feature.COMPTABILITE.name());
            em.flush();
            when(licenseService.hasFeature(Feature.COMPTABILITE)).thenReturn(false);
            connecter(pharmacien);

            assertThat(widgets.findAllowedForCurrentUser()).contains(new AllowedWidgetDTO(WIDGET_MARGE, false));
            assertThatThrownBy(() -> widgets.checkCanLoad(WIDGET_MARGE))
                .isInstanceOf(ForbiddenOperationException.class)
                .extracting("errorKey")
                .isEqualTo(WidgetAuthorizationService.ERROR_NON_SOUSCRIT);
        }
    }

    @Nested
    @DisplayName("Enregistrement d'un layout")
    class Enregistrement {

        @Test
        @DisplayName("un caissier ne peut pas enregistrer un layout qui contient un widget pharmacien")
        void layoutRefuse() {
            connecter(caissier);
            assertThatThrownBy(() -> layouts.save(layout(WIDGET_CAISSE, WIDGET_MARGE)))
                .isInstanceOf(ForbiddenOperationException.class)
                .extracting("errorKey")
                .isEqualTo(WidgetAuthorizationService.ERROR_NON_AUTORISE);
        }

        @Test
        @DisplayName("un layout de widgets autorisés est enregistré comme dashboard personnalisable")
        void layoutAccepte() {
            connecter(pharmacien);
            DashboardLayoutDTO enregistre = layouts.save(layout(WIDGET_MARGE));
            assertThat(enregistre.getComponentKey()).isEqualTo("CUSTOM");
        }

        @Test
        @DisplayName("un seul accueil personnel, même si l'ancien était public")
        void unSeulAccueil() {
            connecter(pharmacien);
            DashboardLayoutDTO publicParDefaut = layout(WIDGET_MARGE);
            publicParDefaut.setScope(DashboardScope.PUBLIC);
            publicParDefaut.setIsDefault(true);
            layouts.save(publicParDefaut);

            DashboardLayoutDTO nouveau = layout(WIDGET_MARGE);
            nouveau.setIsDefault(true);
            DashboardLayoutDTO enregistre = layouts.save(nouveau);
            em.flush();

            assertThat(layouts.findDefaultForCurrentUser()).map(DashboardLayoutDTO::getId).contains(enregistre.getId());
        }
    }

    @Nested
    @DisplayName("Layouts système et layouts des autres")
    class Protection {

        @Test
        @DisplayName("un non-administrateur ne peut ni modifier ni supprimer un layout système")
        void layoutSystemeProtege() {
            DashboardLayout homeBase = homeBase();
            connecter(pharmacien);

            DashboardLayoutDTO modification = layouts.findOne(homeBase.getId()).orElseThrow();
            modification.setDescription("détourné");
            assertThatThrownBy(() -> layouts.update(modification)).isInstanceOf(ForbiddenOperationException.class).extracting("errorKey").isEqualTo("adminRequis");
            assertThatThrownBy(() -> layouts.delete(homeBase.getId())).isInstanceOf(ForbiddenOperationException.class).extracting("errorKey").isEqualTo("adminRequis");
        }

        @Test
        @DisplayName("l'administrateur modifie un layout système sans lui faire perdre son composant")
        void adminGardeLeComposant() {
            DashboardLayout homeBase = homeBase();
            connecter(admin);

            DashboardLayoutDTO modification = layouts.findOne(homeBase.getId()).orElseThrow();
            modification.setComponentKey(null);
            modification.setDescription("Accueil pharmacien");
            assertThat(layouts.update(modification).getComponentKey()).isEqualTo("PHARMACIEN");
        }

        @Test
        @DisplayName("dupliquer un layout système garde son composant (plantait sur une colonne NOT NULL)")
        void cloneGardeLeComposant() {
            connecter(pharmacien);
            assertThat(layouts.clone(homeBase().getId(), "Mon accueil").getComponentKey()).isEqualTo("PHARMACIEN");
        }

        @Test
        @DisplayName("le layout privé d'un autre n'est ni lisible ni duplicable")
        void layoutPriveInaccessible() {
            connecter(pharmacien);
            Integer prive = layouts.save(layout(WIDGET_MARGE)).getId();

            connecter(caissier);
            assertThat(layouts.findOne(prive)).isEmpty();
            assertThatThrownBy(() -> layouts.clone(prive, "copie")).isInstanceOf(GenericError.class);
        }

        @Test
        @DisplayName("la liste « Charger » ne propose pas les layouts système")
        void listeSansLayoutSysteme() {
            connecter(pharmacien);
            assertThat(layouts.findAllForCurrentUser()).extracting(DashboardLayoutDTO::getUserId).doesNotContainNull();
        }

        @Test
        @DisplayName("un layout système ne peut pas devenir un accueil personnel")
        void accueilSystemeRefuse() {
            connecter(pharmacien);
            assertThatThrownBy(() -> layouts.setAsDefault(homeBase().getId())).isInstanceOf(GenericError.class).extracting("errorKey").isEqualTo("layoutSysteme");
        }

        @Test
        @DisplayName("seul l'administrateur attribue un accueil à un rôle")
        void accueilParRoleReserveAdmin() {
            Integer id = homeBase().getId();
            connecter(pharmacien);
            assertThatThrownBy(() -> layouts.setAsDefaultForAuthority(id, "ROLE_CAISSIER"))
                .isInstanceOf(ForbiddenOperationException.class)
                .extracting("errorKey")
                .isEqualTo("adminRequis");
            assertThatThrownBy(() -> layouts.findAllForAuthority("ROLE_CAISSIER")).isInstanceOf(ForbiddenOperationException.class);
        }
    }

    // ===== fixtures =====

    private static void connecter(AppUser user) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user.getLogin(), null, List.of()));
    }

    private static AppUser utilisateur(String... roles) {
        AppUser user = new AppUser();
        user.setLogin("dash" + COMPTEUR.incrementAndGet());
        user.setPassword("x".repeat(60));
        user.setActivated(true);
        user.setLangKey("fr");
        user.setMagasin(em.find(Magasin.class, 1));
        user.setAuthorities(Set.of(roles).stream().map(r -> em.find(Authority.class, r)).collect(java.util.stream.Collectors.toSet()));
        em.persist(user);
        em.flush();
        return user;
    }

    private static NavItem widget(String key, String categorie) {
        NavItem parent = em.createQuery("SELECT n FROM NavItem n WHERE n.code = :code", NavItem.class).setParameter("code", categorie).getSingleResult();
        NavItem item = new NavItem()
            .setCode(WidgetAuthorizationService.CODE_PREFIX + key)
            .setLibelle(key)
            .setParent(parent)
            .setNiveau(3)
            .setTargetType(NavTargetType.WIDGET);
        em.persist(item);
        return item;
    }

    private static void accorder(NavItem item, String role, boolean canDisplay) {
        em.persist(new NavItemRole().setNavItem(item).setRoleName(role).setCanDisplay(canDisplay).setCanAccess(canDisplay));
    }

    private static DashboardLayout homeBase() {
        return em
            .createQuery("SELECT d FROM DashboardLayout d WHERE d.name = 'home-base' AND d.user IS NULL", DashboardLayout.class)
            .getSingleResult();
    }

    private static DashboardLayoutDTO layout(String... widgetKeys) {
        StringBuilder items = new StringBuilder();
        for (int i = 0; i < widgetKeys.length; i++) {
            items.append(i == 0 ? "" : ",").append("{\"id\":\"w%d\",\"x\":0,\"y\":0,\"w\":4,\"h\":3,\"widgetKey\":\"%s\"}".formatted(i, widgetKeys[i]));
        }
        DashboardLayoutDTO dto = new DashboardLayoutDTO();
        dto.setName("Test " + COMPTEUR.incrementAndGet());
        dto.setScope(DashboardScope.PRIVATE);
        dto.setLayoutConfig("{\"version\":2,\"items\":[" + items + "]}");
        return dto;
    }
}
