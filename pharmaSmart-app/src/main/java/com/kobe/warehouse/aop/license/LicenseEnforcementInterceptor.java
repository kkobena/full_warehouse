package com.kobe.warehouse.aop.license;

import com.kobe.warehouse.domain.enumeration.LicenseAuditEventType;
import com.kobe.warehouse.license.LicenseInfo;
import com.kobe.warehouse.license.LicenseViolationException;
import com.kobe.warehouse.service.license.LicenseService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Filet de sécurité de l'exigence B4, complémentaire de {@link LicenseEnforcementAspect}.
 *
 * <p>L'aspect couvre le cas nominal — un {@code @RestController} annoté {@code @PostMapping} et
 * consorts. Il laisse toutefois passer les contrôleurs hérités déclarés en
 * {@code @RequestMapping(method = POST)}, ainsi que les endpoints exposés hors de {@code /api/}
 * comme {@code /api-user-account}. Cet intercepteur raisonne sur le <em>verbe HTTP réel</em> et ne
 * dépend d'aucune annotation de mapping : il rattrape ces cas.
 *
 * <p>Les deux mécanismes partagent la même source de vérité ({@code LicenseService}) et la même
 * liste blanche ({@link LicenseExempt}), il n'y a donc pas de divergence possible entre eux.
 */
public class LicenseEnforcementInterceptor implements HandlerInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(LicenseEnforcementInterceptor.class);

    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final LicenseService licenseService;

    public LicenseEnforcementInterceptor(LicenseService licenseService) {
        this.licenseService = licenseService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!WRITE_METHODS.contains(request.getMethod()) || !(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        if (isExempt(handlerMethod)) {
            return true;
        }

        LicenseInfo info = licenseService.currentStatus();
        if (!info.readOnly()) {
            return true;
        }

        String operation = request.getMethod() + ' ' + request.getRequestURI();
        LOG.warn("Écriture refusée pour cause de licence ({}) : {}", info.status(), operation);
        licenseService.audit(LicenseAuditEventType.BLOCKED_WRITE, operation, null);

        throw new LicenseViolationException(
            info.message(),
            LicenseViolationException.ERROR_KEY_EXPIRED,
            LicenseEnforcementAspect.payloadOf(info)
        );
    }

    private boolean isExempt(HandlerMethod handlerMethod) {
        return (
            AnnotatedElementUtils.hasAnnotation(handlerMethod.getMethod(), LicenseExempt.class) ||
            AnnotatedElementUtils.hasAnnotation(handlerMethod.getBeanType(), LicenseExempt.class)
        );
    }
}
