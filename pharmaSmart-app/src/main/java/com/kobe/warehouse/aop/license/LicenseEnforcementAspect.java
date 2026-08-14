package com.kobe.warehouse.aop.license;

import com.kobe.warehouse.domain.enumeration.LicenseAuditEventType;
import com.kobe.warehouse.license.LicenseInfo;
import com.kobe.warehouse.license.LicenseViolationException;
import com.kobe.warehouse.service.license.LicenseService;
import java.util.HashMap;
import java.util.Map;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;

/**
 * Blocage des écritures lorsque la licence n'est plus valide — exigence B4.
 *
 * <p>L'aspect s'intercale <strong>avant</strong> les intercepteurs transactionnels : refuser une
 * écriture après l'ouverture d'une transaction laisserait des rollbacks inutiles derrière soi.
 *
 * <p>Seules les méthodes de mutation sont visées. La lecture, l'impression et les exports restent
 * accessibles : l'obligation de traçabilité pharmaceutique interdit de couper l'accès à
 * l'historique, même impayé.
 */
@Aspect
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class LicenseEnforcementAspect {

    private static final Logger LOG = LoggerFactory.getLogger(LicenseEnforcementAspect.class);

    private final LicenseService licenseService;

    public LicenseEnforcementAspect(LicenseService licenseService) {
        this.licenseService = licenseService;
    }

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    void restControllers() {}

    @Pointcut(
        "@annotation(org.springframework.web.bind.annotation.PostMapping)" +
        " || @annotation(org.springframework.web.bind.annotation.PutMapping)" +
        " || @annotation(org.springframework.web.bind.annotation.PatchMapping)" +
        " || @annotation(org.springframework.web.bind.annotation.DeleteMapping)"
    )
    void writeMappings() {}

    @Before("restControllers() && writeMappings()")
    public void checkLicense(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        if (isExempt(signature)) {
            return;
        }

        LicenseInfo info = licenseService.currentStatus();
        if (!info.readOnly()) {
            return;
        }

        String operation = signature.getDeclaringType().getSimpleName() + '#' + signature.getName();
        LOG.warn("Écriture refusée pour cause de licence ({}) : {}", info.status(), operation);
        licenseService.audit(LicenseAuditEventType.BLOCKED_WRITE, operation, null);

        throw new LicenseViolationException(info.message(), LicenseViolationException.ERROR_KEY_EXPIRED, payloadOf(info));
    }

    /**
     * Refuse l'accès à un module non souscrit (§3.6).
     *
     * <p>Le pointcut vise <strong>toutes</strong> les méthodes des types annotés, pas seulement les
     * mutations : un module absent de l'abonnement ne doit rien renvoyer du tout, y compris en
     * lecture. C'est ce qui distingue cette règle du blocage d'expiration.
     */
    @Before("@within(com.kobe.warehouse.aop.license.RequiresFeature) || @annotation(com.kobe.warehouse.aop.license.RequiresFeature)")
    public void checkFeature(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        RequiresFeature required = AnnotatedElementUtils.findMergedAnnotation(signature.getMethod(), RequiresFeature.class);
        if (required == null) {
            required = AnnotatedElementUtils.findMergedAnnotation(signature.getDeclaringType(), RequiresFeature.class);
        }
        if (required == null || licenseService.hasFeature(required.value())) {
            return;
        }

        LicenseInfo info = licenseService.currentStatus();
        LOG.warn(
            "Accès refusé au module {} non souscrit : {}#{}",
            required.value(),
            signature.getDeclaringType().getSimpleName(),
            signature.getName()
        );

        Map<String, Object> payload = payloadOf(info);
        payload.put("missingFeature", required.value().name());
        throw new LicenseViolationException(
            "Le module « %s » n'est pas inclus dans votre abonnement. Contactez votre revendeur pour le souscrire.".formatted(
                required.value().name()
            ),
            LicenseViolationException.ERROR_KEY_FEATURE_NOT_INCLUDED,
            payload
        );
    }

    private boolean isExempt(MethodSignature signature) {
        return (
            AnnotatedElementUtils.hasAnnotation(signature.getMethod(), LicenseExempt.class) ||
            AnnotatedElementUtils.hasAnnotation(signature.getDeclaringType(), LicenseExempt.class)
        );
    }

    /**
     * Le {@code payload} embarque toujours les coordonnées du revendeur : un utilisateur bloqué doit
     * pouvoir appeler quelqu'un sans quitter son écran (§3.1).
     */
    static Map<String, Object> payloadOf(LicenseInfo info) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("status", info.status().name());
        payload.put("licenseType", info.licenseType().name());
        if (info.expiresAt() != null) {
            payload.put("expiresAt", info.expiresAt().toString());
        }
        payload.put("support", info.support());
        return payload;
    }
}
