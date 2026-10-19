package com.kobe.warehouse.service.dashboard.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.kobe.warehouse.service.dashboard.widget.WidgetAuthorizationService;
import com.kobe.warehouse.service.dashboard.widget.WidgetDataProvider;
import com.kobe.warehouse.service.dashboard.widget.provider.VentesWidgets;
import com.kobe.warehouse.service.settings.AppConfigurationService;
import com.kobe.warehouse.test.IntegrationPostgresDatabase;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Un widget n'existe vraiment que si trois choses s'accordent : son fournisseur de données, son
 * {@code nav_item} (qui porte les droits) et les rôles que la migration lui accorde. Un fournisseur
 * sans {@code nav_item} n'est accessible à personne ; un {@code nav_item} sans fournisseur propose un
 * widget qui échoue au chargement. Ce test fait le rapprochement sur la base migrée.
 */
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Dashboard personnalisable — catalogue des widgets en base")
class DashboardWidgetCatalogueIntegrationTest {

    /** Widgets de mise en page : affichés par le front seul, sans fournisseur. */
    private static final Set<String> SANS_FOURNISSEUR = Set.of("note", "titre-section");

    private static final AppConfigurationService DEVISE = mock(AppConfigurationService.class);

    private static EntityManager em;

    @BeforeAll
    static void demarrerLaBase() {
        em = SharedEntityManagerCreator.createSharedEntityManager(IntegrationPostgresDatabase.bean(EntityManagerFactory.class));
    }

    @Test
    @DisplayName("chaque fournisseur a son nav_item, et chaque nav_item de données son fournisseur")
    void fournisseursEtNavItemsConcordent() throws Exception {
        Set<String> fournisseurs = lireLesClesDesFournisseurs();
        Set<String> enBase = em
            .createQuery("SELECT n.code FROM NavItem n WHERE n.targetType = com.kobe.warehouse.domain.enumeration.NavTargetType.WIDGET", String.class)
            .getResultList()
            .stream()
            .map(code -> code.substring(WidgetAuthorizationService.CODE_PREFIX.length()))
            .filter(key -> !SANS_FOURNISSEUR.contains(key))
            .collect(Collectors.toSet());

        assertThat(enBase).containsExactlyInAnyOrderElementsOf(fournisseurs);
    }

    @Test
    @DisplayName("le caissier reçoit ses widgets de caisse, pas les chiffres de l'officine")
    void droitsDuCaissier() {
        Set<String> caissier = widgetsDuRole("ROLE_CAISSIER");
        assertThat(caissier).contains("ma-caisse", "mes-encaissements", "mes-ventes-recentes", "livraisons-du-jour", "note");
        assertThat(caissier).doesNotContain("ca-net", "marge-brute", "panier-moyen", "achats-periode", "analyse-abc");
    }

    @Test
    @DisplayName("le pharmacien reçoit les chiffres de l'officine")
    void droitsDuPharmacien() {
        assertThat(widgetsDuRole("ROLE_PHARMACIEN")).contains("ca-net", "marge-brute", "top-produits", "alertes-officine", "achats-periode");
    }

    @Test
    @DisplayName("le pharmacien ouvre les rapports dont son accueil affiche les chiffres")
    void pharmacienAccedeAuxRapportsDeSonAccueil() {
        List<String> sections = em
            .createQuery(
                "SELECT r.navItem.code FROM NavItemRole r WHERE r.roleName = 'ROLE_PHARMACIEN' AND r.canAccess = true " +
                "AND r.navItem.code IN ('rapport-stock.stock-valuation', 'rapport-partners.supplier-performance')",
                String.class
            )
            .getResultList();
        assertThat(sections).containsExactlyInAnyOrder("rapport-stock.stock-valuation", "rapport-partners.supplier-performance");
    }

    private static Set<String> widgetsDuRole(String role) {
        return em
            .createQuery(
                "SELECT r.navItem.code FROM NavItemRole r WHERE r.roleName = :role AND r.canDisplay = true " +
                "AND r.navItem.targetType = com.kobe.warehouse.domain.enumeration.NavTargetType.WIDGET",
                String.class
            )
            .setParameter("role", role)
            .getResultList()
            .stream()
            .map(code -> code.substring(WidgetAuthorizationService.CODE_PREFIX.length()))
            .collect(Collectors.toSet());
    }

    /**
     * Appelle chaque méthode {@code @Bean} des configurations de widgets avec des services simulés,
     * pour lire sa clé. Les configurations sont découvertes par balayage du paquet : en ajouter une
     * ne demande rien ici.
     */
    private static Set<String> lireLesClesDesFournisseurs() throws Exception {
        Set<String> keys = new HashSet<>();
        for (Object configuration : instancierLesConfigurations()) {
            for (Method method : configuration.getClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(Bean.class) && WidgetDataProvider.class.isAssignableFrom(method.getReturnType())) {
                    method.setAccessible(true);
                    Object[] args = Arrays.stream(method.getParameterTypes()).map(type -> (Object) mock(type)).toArray();
                    keys.add(((WidgetDataProvider) method.invoke(configuration, args)).key());
                }
            }
        }
        return keys;
    }

    /** Les {@code @Configuration} du paquet des fournisseurs, construites avec la devise simulée. */
    private static List<Object> instancierLesConfigurations() throws Exception {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Configuration.class));
        List<Object> configurations = new ArrayList<>();
        for (BeanDefinition definition : scanner.findCandidateComponents(VentesWidgets.class.getPackageName())) {
            Constructor<?> constructor = Class.forName(definition.getBeanClassName()).getDeclaredConstructor(AppConfigurationService.class);
            constructor.setAccessible(true);
            configurations.add(constructor.newInstance(DEVISE));
        }
        assertThat(configurations).hasSizeGreaterThanOrEqualTo(5);
        return configurations;
    }
}
