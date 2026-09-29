package com.kobe.warehouse.security.navaccess;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.kobe.warehouse.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Exige, sur {@code /api/**}, le droit {@code nav_item_role} déclaré par
 * {@link RequiresNavAccess} (docs/PLAN-SECURISATION-ENDPOINTS.md).
 *
 * <p>Un intercepteur plutôt qu'un aspect : il voit le verbe HTTP réel, y compris sur les
 * contrôleurs déclarés en {@code @RequestMapping(method = …)}, et l'URI pour le journal.
 * {@code /api/admin/**} est déjà réservé à l'administrateur par la chaîne de filtres.
 */
public class NavAccessInterceptor implements HandlerInterceptor {

    /** Logger dédié, pour isoler les refus de la période d'audit dans la configuration Logback. */
    private static final Logger AUDIT = LoggerFactory.getLogger("nav-access-audit");
    private static final Logger LOG = LoggerFactory.getLogger(NavAccessInterceptor.class);

    private final NavAccessService navAccessService;
    private final NavAccessMode mode;
    // Un refus répété à chaque rafraîchissement d'écran noierait le journal : une ligne par heure suffit.
    private final Cache<String, Boolean> alreadyLogged = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofHours(1))
        .maximumSize(10_000)
        .build();

    public NavAccessInterceptor(NavAccessService navAccessService, NavAccessMode mode) {
        this.navAccessService = navAccessService;
        this.mode = mode;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (mode == NavAccessMode.OFF || !(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        String uri = request.getRequestURI();
        if (!uri.startsWith("/api/") || uri.startsWith("/api/admin/")) {
            return true;
        }
        if (!(NavAccessRules.resolve(handlerMethod.getMethod(), handlerMethod.getBeanType()) instanceof NavAccessRules.Requires requires)) {
            return true;
        }

        NavAction action = requires.action() == NavAction.AUTO ? NavAction.fromHttpMethod(request.getMethod()) : requires.action();
        List<String> codes = List.of(requires.codes());
        if (navAccessService.isAllowed(codes, action)) {
            return true;
        }

        if (mode == NavAccessMode.AUDIT) {
            String login = SecurityUtils.getCurrentUserLogin().orElse("?");
            String endpoint = request.getMethod() + ' ' + uri;
            String handlerName = handlerMethod.getBeanType().getSimpleName() + '#' + handlerMethod.getMethod().getName();
            if (alreadyLogged.asMap().putIfAbsent(login + '|' + handlerName, Boolean.TRUE) == null) {
                AUDIT.warn(
                    "Accès non autorisé (audit, laissé passer) : utilisateur={} rôles={} endpoint={} handler={} codes={} droit={}",
                    login,
                    navAccessService.currentUserRoles(),
                    endpoint,
                    handlerName,
                    codes,
                    action
                );
            }
            return true;
        }

        String libelle = navAccessService.libelleOf(codes);
        LOG.warn("Accès refusé à {} {} ({} sur {})", request.getMethod(), uri, action, codes);
        throw new NavAccessDeniedException(libelle);
    }
}
