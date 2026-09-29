package com.kobe.warehouse.service.dashboard.widget;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kobe.warehouse.domain.enumeration.NavTargetType;
import com.kobe.warehouse.security.navaccess.NavAccessService;
import com.kobe.warehouse.security.navaccess.NavAction;
import com.kobe.warehouse.security.navaccess.NavGrant;
import com.kobe.warehouse.service.errors.ForbiddenOperationException;
import com.kobe.warehouse.service.errors.GenericError;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Droits d'usage des widgets du dashboard personnalisable.
 *
 * <p>Un widget est autorisé si l'un des rôles de l'utilisateur a {@code can_display} sur le
 * {@code nav_item} {@code widget.<clé>}. Les droits sont ceux de {@link NavAccessService}, lus
 * en base pour l'union des rôles, comme pour les endpoints.
 *
 * <p>Les refus sont des {@link ForbiddenOperationException} (403), comme ceux des endpoints ; un
 * 403 ne déconnecte pas l'utilisateur.
 */
@Service
@Transactional(readOnly = true)
public class WidgetAuthorizationService {

    public static final String CODE_PREFIX = "widget.";
    public static final String ERROR_NON_AUTORISE = "widgetNonAutorise";
    public static final String ERROR_NON_SOUSCRIT = "widgetNonSouscrit";

    private static final ObjectMapper JSON = new ObjectMapper();

    private final NavAccessService navAccessService;

    public WidgetAuthorizationService(NavAccessService navAccessService) {
        this.navAccessService = navAccessService;
    }

    public List<AllowedWidgetDTO> findAllowedForCurrentUser() {
        return allowedItems()
            .stream()
            .map(grant -> new AllowedWidgetDTO(keyOf(grant), navAccessService.isFeatureSubscribed(grant)))
            .toList();
    }

    /** Refuse un widget non autorisé au rôle, ou autorisé mais hors licence. */
    public void checkCanLoad(String key) {
        NavGrant grant = allowedItems()
            .stream()
            .filter(g -> keyOf(g).equals(key))
            .findFirst()
            .orElseThrow(() -> nonAutorise(key));
        if (!navAccessService.isFeatureSubscribed(grant)) {
            throw new ForbiddenOperationException("Ce widget n'est pas inclus dans votre abonnement.", ERROR_NON_SOUSCRIT);
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

    private List<NavGrant> allowedItems() {
        return navAccessService.grantsOfType(NavTargetType.WIDGET, NavAction.DISPLAY);
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

    private static String keyOf(NavGrant grant) {
        return grant.code().startsWith(CODE_PREFIX) ? grant.code().substring(CODE_PREFIX.length()) : grant.code();
    }

    private static ForbiddenOperationException nonAutorise(String key) {
        return new ForbiddenOperationException("Le widget « %s » n'est pas autorisé pour votre profil.".formatted(key), ERROR_NON_AUTORISE);
    }
}
