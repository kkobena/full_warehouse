package com.kobe.warehouse.security.navaccess;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.kobe.warehouse.constant.EntityConstant;
import com.kobe.warehouse.domain.Authority;
import com.kobe.warehouse.domain.enumeration.NavTargetType;
import com.kobe.warehouse.domain.nav.NavItem;
import com.kobe.warehouse.license.Feature;
import com.kobe.warehouse.repository.UserRepository;
import com.kobe.warehouse.repository.nav.NavItemRepository;
import com.kobe.warehouse.repository.nav.NavItemRoleRepository;
import com.kobe.warehouse.security.AuthoritiesConstants;
import com.kobe.warehouse.security.SecurityUtils;
import com.kobe.warehouse.service.license.LicenseService;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Droits {@code nav_item_role} de l'utilisateur courant, lus en base pour l'union de tous ses
 * rôles, et non dans le jeton : un droit retiré s'applique sans reconnexion.
 *
 * <p>Les droits fusionnés sont mis en cache par ensemble de rôles
 * ({@link EntityConstant#NAV_ACCESS_CACHE}), vidé par l'écran d'attribution des menus. Les rôles
 * d'un utilisateur sont relus au plus toutes les 30 secondes. La licence est évaluée à chaque appel.
 */
@Service
@Transactional(readOnly = true)
public class NavAccessService {

    private static final Logger LOG = LoggerFactory.getLogger(NavAccessService.class);

    private final NavItemRoleRepository navItemRoleRepository;
    private final NavItemRepository navItemRepository;
    private final UserRepository userRepository;
    private final LicenseService licenseService;
    private final CacheManager cacheManager;
    private final com.github.benmanes.caffeine.cache.Cache<String, Set<String>> rolesByLogin = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofSeconds(30))
        .maximumSize(500)
        .build();

    public NavAccessService(
        NavItemRoleRepository navItemRoleRepository,
        NavItemRepository navItemRepository,
        UserRepository userRepository,
        LicenseService licenseService,
        CacheManager cacheManager
    ) {
        this.navItemRoleRepository = navItemRoleRepository;
        this.navItemRepository = navItemRepository;
        this.userRepository = userRepository;
        this.licenseService = licenseService;
        this.cacheManager = cacheManager;
    }

    /** L'administrateur passe toujours, comme dans l'{@code AuthGuard} du front. */
    public boolean isAllowed(Collection<String> codes, NavAction action) {
        Set<String> roles = currentUserRoles();
        if (roles.contains(AuthoritiesConstants.ADMIN)) {
            return true;
        }
        Map<String, NavGrant> grants = grantsFor(roles);
        return codes
            .stream()
            .map(grants::get)
            .filter(Objects::nonNull)
            .anyMatch(grant -> grant.allows(action) && isFeatureSubscribed(grant));
    }

    /** Items d'un type donné sur lesquels l'utilisateur courant a le droit demandé, sans passe-droit admin. */
    public List<NavGrant> grantsOfType(NavTargetType targetType, NavAction action) {
        return grantsFor(currentUserRoles())
            .values()
            .stream()
            .filter(grant -> grant.targetType() == targetType && grant.allows(action))
            .toList();
    }

    public Set<String> currentUserRoles() {
        return SecurityUtils.getCurrentUserLogin().map(login -> rolesByLogin.get(login, this::loadRoles)).orElse(Set.of());
    }

    /** Libellé du premier code connu, pour le message de refus. */
    public String libelleOf(Collection<String> codes) {
        return codes
            .stream()
            .map(navItemRepository::findByCode)
            .filter(Objects::nonNull)
            .map(NavItem::getLibelle)
            .findFirst()
            .orElseGet(() -> String.join(", ", codes));
    }

    // Même règle que NavItemServiceImpl pour les menus : une valeur inconnue n'impose aucune contrainte.
    public boolean isFeatureSubscribed(NavGrant grant) {
        String required = grant.requiredFeature();
        if (!StringUtils.hasText(required)) {
            return true;
        }
        try {
            return licenseService.hasFeature(Feature.valueOf(required.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            LOG.warn("Module inconnu « {} » sur « {} » : contrainte ignorée", required, grant.code());
            return true;
        }
    }

    private Map<String, NavGrant> grantsFor(Set<String> roles) {
        if (roles.isEmpty()) {
            return Map.of();
        }
        Cache cache = cacheManager.getCache(EntityConstant.NAV_ACCESS_CACHE);
        if (cache == null) {
            return loadGrants(roles);
        }
        return cache.get(String.join(",", new TreeSet<>(roles)), () -> loadGrants(roles));
    }

    private Map<String, NavGrant> loadGrants(Set<String> roles) {
        Map<String, NavGrant> grants = new HashMap<>();
        navItemRoleRepository
            .findAllActiveByRoleNames(roles)
            .forEach(role -> grants.merge(role.getNavItem().getCode(), NavGrant.of(role), NavGrant::merge));
        return Map.copyOf(grants);
    }

    private Set<String> loadRoles(String login) {
        return userRepository
            .findOneWithAuthoritiesByLogin(login)
            .map(user -> user.getAuthorities().stream().map(Authority::getName).collect(Collectors.toUnmodifiableSet()))
            .orElse(Set.of());
    }
}
