package com.kobe.warehouse.service.dashboard.widget;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kobe.warehouse.domain.Authority;
import com.kobe.warehouse.domain.enumeration.NavTargetType;
import com.kobe.warehouse.domain.nav.NavItem;
import com.kobe.warehouse.license.Feature;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.repository.nav.NavItemRoleRepository;
import com.kobe.warehouse.security.SecurityUtils;
import com.kobe.warehouse.service.errors.GenericError;
import com.kobe.warehouse.service.license.LicenseService;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Droits d'usage des widgets du dashboard personnalisable.
 *
 * <p>Un widget est autorisé si l'un des rôles de l'utilisateur a {@code can_display} sur le
 * {@code nav_item} {@code widget.<clé>}. La vérification lit la base à chaque appel, et non les
 * autorités du jeton : le jeton ne porte que le premier rôle de l'utilisateur, et un droit retiré
 * doit s'appliquer sans attendre son renouvellement.
 *
 * <p>Les refus sont des {@link GenericError} (400 avec message) : une {@code AccessDeniedException}
 * sortirait aujourd'hui en 500 par {@code ExceptionTranslator}. À aligner sur le 403 commun du
 * plan de sécurisation (docs/PLAN-SECURISATION-ENDPOINTS.md), un 403 ne déconnectant pas.
 */
@Service
@Transactional(readOnly = true)
public class WidgetAuthorizationService {

    public static final String CODE_PREFIX = "widget.";
    public static final String ERROR_NON_AUTORISE = "widgetNonAutorise";
    public static final String ERROR_NON_SOUSCRIT = "widgetNonSouscrit";

    private static final Logger LOG = LoggerFactory.getLogger(WidgetAuthorizationService.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final NavItemRoleRepository navItemRoleRepository;
    private final UserRepository userRepository;
    private final LicenseService licenseService;

    public WidgetAuthorizationService(
        NavItemRoleRepository navItemRoleRepository,
        UserRepository userRepository,
        LicenseService licenseService
    ) {
        this.navItemRoleRepository = navItemRoleRepository;
        this.userRepository = userRepository;
        this.licenseService = licenseService;
    }

    public List<AllowedWidgetDTO> findAllowedForCurrentUser() {
        return allowedItems()
            .stream()
            .map(item -> new AllowedWidgetDTO(keyOf(item), isFeatureSubscribed(item)))
            .toList();
    }

    /** Refuse un widget non autorisé au rôle, ou autorisé mais hors licence. */
    public void checkCanLoad(String key) {
        NavItem item = allowedItems()
            .stream()
            .filter(i -> keyOf(i).equals(key))
            .findFirst()
            .orElseThrow(() -> nonAutorise(key));
        if (!isFeatureSubscribed(item)) {
            throw new GenericError("Ce widget n'est pas inclus dans votre abonnement.", ERROR_NON_SOUSCRIT);
        }
    }

    /**
     * Refuse d'enregistrer un layout qui contient un widget non autorisé à son auteur. La licence
     * n'est pas vérifiée ici : elle ne conditionne que l'affichage des données.
     *
     * <p>Seuls les items qui portent un {@code widgetKey} sont contrôlés ; ceux de l'ancien format
     * n'affichent aucune donnée.
     */
    public void checkLayoutConfig(String layoutConfig) {
        Set<String> keys = widgetKeysOf(layoutConfig);
        if (keys.isEmpty()) {
            return;
        }
        Set<String> allowed = allowedItems().stream().map(WidgetAuthorizationService::keyOf).collect(Collectors.toSet());
        keys
            .stream()
            .filter(key -> !allowed.contains(key))
            .findFirst()
            .ifPresent(key -> {
                throw nonAutorise(key);
            });
    }

    private List<NavItem> allowedItems() {
        Set<String> roles = currentUserRoles();
        if (roles.isEmpty()) {
            return List.of();
        }
        return navItemRoleRepository.findDisplayableItemsByRoles(roles, NavTargetType.WIDGET);
    }

    private Set<String> currentUserRoles() {
        return SecurityUtils.getCurrentUserLogin()
            .flatMap(userRepository::findOneWithAuthoritiesByLogin)
            .map(user -> user.getAuthorities().stream().map(Authority::getName).collect(Collectors.toSet()))
            .orElse(Set.of());
    }

    private static Set<String> widgetKeysOf(String layoutConfig) {
        if (!StringUtils.hasText(layoutConfig)) {
            return Set.of();
        }
        JsonNode items;
        try {
            items = JSON.readTree(layoutConfig).path("items");
        } catch (Exception e) {
            throw new GenericError("Configuration du dashboard illisible.", "layoutConfigInvalide");
        }
        Set<String> keys = new LinkedHashSet<>();
        items.forEach(item -> {
            String key = item.path("widgetKey").asText("");
            if (!key.isBlank()) {
                keys.add(key);
            }
        });
        return keys;
    }

    private static String keyOf(NavItem item) {
        return item.getCode().startsWith(CODE_PREFIX) ? item.getCode().substring(CODE_PREFIX.length()) : item.getCode();
    }

    private static GenericError nonAutorise(String key) {
        return new GenericError("Le widget « %s » n'est pas autorisé pour votre profil.".formatted(key), ERROR_NON_AUTORISE);
    }

    // Même règle que NavItemServiceImpl pour les menus : une valeur inconnue n'impose aucune contrainte.
    private boolean isFeatureSubscribed(NavItem item) {
        String required = item.getRequiredFeature();
        if (!StringUtils.hasText(required)) {
            return true;
        }
        try {
            return licenseService.hasFeature(Feature.valueOf(required.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            LOG.warn("Module inconnu « {} » sur le widget « {} » : contrainte ignorée", required, item.getCode());
            return true;
        }
    }
}
